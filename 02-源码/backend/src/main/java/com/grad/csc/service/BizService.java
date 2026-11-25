package com.grad.csc.service;

import com.grad.csc.common.BizException;
import com.grad.csc.common.PageResult;
import com.grad.csc.common.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 客诉业务服务。
 *
 * 设计要点：
 * 1) 状态机集中在 {@link #transition}，任何改状态的操作都必须走这里，前后状态与原因写入
 *    complaint_ticket_record，满足「状态流转必须留痕」的硬性要求。
 * 2) 数据范围在 SQL 层生效（ALL / ORG / SELF），菜单权限只管页面可见性，不能替代数据范围校验。
 * 3) 创新点「结构化共性问题聚类」的权重、窗口、阈值全部读配置表，计算结果连同输入快照落库，
 *    人工调整只写 manual_* 字段，绝不覆盖原始 similarity。
 */
@Service
@RequiredArgsConstructor
public class BizService {

    private final JdbcTemplate jdbc;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 主流程顺序，用于判断是否越级流转 */
    private static final List<String> FLOW = Arrays.asList(
            "待受理", "已分派", "处理中", "待审核", "待客户确认", "待回访", "已归档");

    // ================================================================= 基础工具

    /** 结果集列名 snake_case → camelCase，前端不用记两套命名 */
    private static String camel(String key) {
        StringBuilder sb = new StringBuilder();
        boolean up = false;
        for (char c : key.toCharArray()) {
            if (c == '_') {
                up = true;
                continue;
            }
            sb.append(up ? Character.toUpperCase(c) : c);
            up = false;
        }
        return sb.toString();
    }

    private List<Map<String, Object>> rows(String sql, Object... args) {
        List<Map<String, Object>> raw = jdbc.queryForList(sql, args);
        List<Map<String, Object>> out = new ArrayList<>(raw.size());
        for (Map<String, Object> r : raw) {
            Map<String, Object> m = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : r.entrySet()) {
                Object v = e.getValue();
                if (v instanceof LocalDateTime ldt) {
                    v = ldt.format(FMT);
                } else if (v instanceof BigDecimal bd) {
                    v = bd.setScale(2, RoundingMode.HALF_UP).doubleValue();
                }
                m.put(camel(e.getKey()), v);
            }
            out.add(m);
        }
        return out;
    }

    private Map<String, Object> one(String sql, Object... args) {
        List<Map<String, Object>> l = rows(sql, args);
        return l.isEmpty() ? null : l.get(0);
    }

    private long num(String sql, Object... args) {
        Long v = jdbc.queryForObject(sql, Long.class, args);
        return v == null ? 0L : v;
    }

    private double dbl(String sql, Object... args) {
        Double v = jdbc.queryForObject(sql, Double.class, args);
        return v == null ? 0d : v;
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private int intOf(Object o, int def) {
        if (o == null) {
            return def;
        }
        if (o instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private long longOr(Object o, long def) {
        if (o == null) {
            return def;
        }
        if (o instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private String now() {
        return LocalDateTime.now().format(FMT);
    }

    private Object[] with(List<Object> a, Object... tail) {
        List<Object> l = new ArrayList<>(a);
        l.addAll(Arrays.asList(tail));
        return l.toArray();
    }

    /**
     * 数据范围 SQL 片段（含参数）。
     * 需求文档：客户只看本人投诉；部门处理人只看本部门主办或协办；组长/质量经理与管理员看全量。
     */
    private String scope(String alias, List<Object> args) {
        UserContext.CurrentUser u = UserContext.get();
        if (u == null) {
            return " 1=1 ";
        }
        String scope = u.dataScope() == null ? "ALL" : u.dataScope();
        String role = u.roleCode() == null ? "" : u.roleCode();
        if ("CUSTOMER".equals(role)) {
            args.add(u.userId());
            return " " + alias + ".customer_id in (select c.id from customer c where c.user_id = ?) ";
        }
        if ("ALL".equals(scope)) {
            return " 1=1 ";
        }
        if ("ORG".equals(scope)) {
            args.add(u.orgId() == null ? -1L : u.orgId());
            args.add(u.orgId() == null ? -1L : u.orgId());
            // 本部门主办，或本部门有协办任务
            return " (" + alias + ".dept_id = ? or exists (select 1 from coop_task ct where ct.ticket_id = "
                    + alias + ".id and ct.dept_id = ?)) ";
        }
        args.add(u.userId());
        args.add(u.userId());
        return " (" + alias + ".create_by = ? or " + alias + ".current_handler_id = ?) ";
    }

    // ================================================================= 看板

    /** 服务质量驾驶舱：6 个核心指标 + 7 组图表数据，全部由后端聚合，前端不做二次全表统计 */
    public Map<String, Object> dashboard(int period) {
        int days = period > 0 ? period : 30;
        List<Object> a = new ArrayList<>();
        String sc = scope("t", a);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("period", days);

        long total = num("select count(*) from complaint_ticket t where t.deleted=0 and " + sc
                + " and t.create_time >= date_sub(now(), interval ? day)", with(a, days));
        // 响应时效：受理时间 - 提交时间 的平均小时数
        double respHours = dbl("select ifnull(avg(timestampdiff(minute, t.create_time, t.accept_time))/60,0) "
                + "from complaint_ticket t where t.deleted=0 and t.accept_time is not null "
                + "and t.create_time >= date_sub(now(), interval ? day)", days);
        long archived = num("select count(*) from complaint_ticket t where t.deleted=0 and t.status='已归档' "
                + "and t.create_time >= date_sub(now(), interval ? day)", days);
        long onTime = num("select count(*) from complaint_ticket t where t.deleted=0 and t.status='已归档' "
                + "and ifnull(t.overdue_flag,0)=0 and t.create_time >= date_sub(now(), interval ? day)", days);
        long reopened = num("select count(distinct r.ticket_id) from reopen_record r "
                + "join complaint_ticket t on t.id = r.ticket_id where r.deleted=0 "
                + "and t.create_time >= date_sub(now(), interval ? day)", days);
        double avgSat = dbl("select ifnull(avg(v.satisfaction),0) from revisit_record v "
                + "join complaint_ticket t on t.id = v.ticket_id where v.deleted=0 "
                + "and t.create_time >= date_sub(now(), interval ? day)", days);
        long firstSolve = Math.max(0, archived - reopened);

        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("total", total);
        kpi.put("waitAccept", num("select count(*) from complaint_ticket t where t.deleted=0 and t.status='待受理' and " + sc, a.toArray()));
        kpi.put("handling", num("select count(*) from complaint_ticket t where t.deleted=0 and t.status in ('已分派','处理中') and " + sc, a.toArray()));
        kpi.put("soonDue", num("select count(*) from complaint_ticket t where t.deleted=0 and ifnull(t.overdue_flag,0)=0 "
                + "and t.deadline_time between now() and date_add(now(), interval 4 hour) "
                + "and t.status not in ('已归档','已取消') and " + sc, a.toArray()));
        kpi.put("overdue", num("select count(*) from complaint_ticket t where t.deleted=0 and ifnull(t.overdue_flag,0)=1 "
                + "and t.status not in ('已归档','已取消') and " + sc, a.toArray()));
        kpi.put("waitRevisit", num("select count(*) from complaint_ticket t where t.deleted=0 and t.status='待回访' and " + sc, a.toArray()));
        kpi.put("respHours", Math.round(respHours * 10) / 10.0);
        kpi.put("onTimeRate", archived == 0 ? 0 : Math.round(onTime * 1000.0 / archived) / 10.0);
        kpi.put("firstSolveRate", archived == 0 ? 0 : Math.round(firstSolve * 1000.0 / archived) / 10.0);
        kpi.put("reopenRate", archived == 0 ? 0 : Math.round(reopened * 1000.0 / archived) / 10.0);
        kpi.put("satisfaction", Math.round(avgSat * 10) / 10.0);
        out.put("kpis", kpi);

        // 图表 1：投诉趋势（按天补零，避免前端折线缺口）
        out.put("trend", chart(rows("""
                select date_format(d.day, '%m-%d') as label,
                       (select count(*) from complaint_ticket t where date(t.create_time) = d.day and t.deleted = 0) as value
                from (
                  select date_sub(curdate(), interval seq day) as day
                  from (select 0 seq union all select 1 union all select 2 union all select 3 union all select 4
                        union all select 5 union all select 6 union all select 7 union all select 8 union all select 9
                        union all select 10 union all select 11 union all select 12 union all select 13 union all select 14
                        union all select 15 union all select 16 union all select 17 union all select 18 union all select 19
                        union all select 20 union all select 21 union all select 22 union all select 23 union all select 24
                        union all select 25 union all select 26 union all select 27 union all select 28 union all select 29) s
                ) d order by d.day
                """)));

        // 图表 2：状态分布
        out.put("statusDist", chart(rows("select t.status as label, count(*) as value from complaint_ticket t "
                + "where t.deleted=0 and t.status <> '已取消' and t.create_time >= date_sub(now(), interval ? day) "
                + "group by t.status order by value desc", days)));

        // 图表 3：问题类别 TOP8
        out.put("categoryTop", chart(rows("select t.category_name as label, count(*) as value from complaint_ticket t "
                + "where t.deleted=0 and t.create_time >= date_sub(now(), interval ? day) "
                + "group by t.category_name order by value desc limit 8", days)));

        // 图表 4：责任部门排行（含平均处理时长）
        out.put("deptRank", chart(rows("""
                select t.dept_name as label, count(*) as value,
                       ifnull(round(avg(timestampdiff(minute, t.create_time, ifnull(t.finish_time, now())))/60, 1), 0) as extra
                from complaint_ticket t where t.deleted=0 and t.create_time >= date_sub(now(), interval ? day)
                group by t.dept_name order by value desc limit 8
                """, days)));

        // 图表 5：渠道占比
        out.put("channel", chart(rows("select t.channel as label, count(*) as value from complaint_ticket t "
                + "where t.deleted=0 and t.create_time >= date_sub(now(), interval ? day) "
                + "group by t.channel order by value desc", days)));

        // 图表 6：创新聚类结果分布（extra = 人工调整条数）
        out.put("clusterDist", chart(rows("select s.level as label, count(*) as value, "
                + "sum(case when s.manual_level is not null then 1 else 0 end) as extra "
                + "from complaint_ticket_score s where s.deleted=0 group by s.level")));

        // 图表 7：满意度分布
        out.put("satisfaction", chart(rows("select concat(v.satisfaction, ' 分') as label, count(*) as value "
                + "from revisit_record v where v.deleted=0 and v.satisfaction is not null "
                + "group by v.satisfaction order by v.satisfaction desc")));

        // 重点关注清单（高等级且未归档）
        List<Object> b = new ArrayList<>();
        String sc2 = scope("t", b);
        out.put("focus", rows("select t.id, t.ticket_no, t.title, t.level_code, t.status, t.deadline_time, "
                + "t.dept_name, t.current_handler_name, t.overdue_flag from complaint_ticket t "
                + "where t.deleted=0 and t.level_code in ('P1','P2') and t.status not in ('已归档','已取消') and "
                + sc2 + " order by t.overdue_flag desc, t.deadline_time asc limit 6", b.toArray()));
        return out;
    }

    private Map<String, Object> chart(List<Map<String, Object>> rs) {
        Map<String, Object> m = new LinkedHashMap<>();
        List<String> labels = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        List<Object> extra = new ArrayList<>();
        for (Map<String, Object> r : rs) {
            labels.add(String.valueOf(r.get("label")));
            values.add(r.get("value"));
            extra.add(r.get("extra"));
        }
        m.put("labels", labels);
        m.put("values", values);
        if (extra.stream().anyMatch(Objects::nonNull)) {
            m.put("extra", extra);
        }
        return m;
    }

    // ================================================================= 投诉列表与详情

    /** 投诉台账分页。quick 支持 todo / processing / done / abnormal 四个快捷标签。 */
    public PageResult<Map<String, Object>> ticketPage(String keyword, String channel, Long categoryId,
                                                      String level, String status, Long deptId,
                                                      String dateFrom, String dateTo, Integer overdue,
                                                      String quick, int page, int size) {
        StringBuilder where = new StringBuilder(" where t.deleted=0 ");
        List<Object> args = new ArrayList<>();
        where.append(" and ").append(scope("t", args));
        if (keyword != null && !keyword.isBlank()) {
            where.append(" and (t.ticket_no like ? or t.title like ? or t.customer_name like ? or t.phone like ?) ");
            String k = "%" + keyword.trim() + "%";
            args.add(k);
            args.add(k);
            args.add(k);
            args.add(k);
        }
        if (channel != null && !channel.isBlank()) {
            where.append(" and t.channel = ? ");
            args.add(channel);
        }
        if (categoryId != null) {
            where.append(" and t.category_id = ? ");
            args.add(categoryId);
        }
        if (level != null && !level.isBlank()) {
            where.append(" and t.level_code = ? ");
            args.add(level);
        }
        if (status != null && !status.isBlank()) {
            where.append(" and t.status = ? ");
            args.add(status);
        }
        if (deptId != null) {
            where.append(" and t.dept_id = ? ");
            args.add(deptId);
        }
        if (dateFrom != null && !dateFrom.isBlank()) {
            where.append(" and t.create_time >= ? ");
            args.add(dateFrom + " 00:00:00");
        }
        if (dateTo != null && !dateTo.isBlank()) {
            where.append(" and t.create_time <= ? ");
            args.add(dateTo + " 23:59:59");
        }
        if (overdue != null && overdue == 1) {
            where.append(" and ifnull(t.overdue_flag,0)=1 ");
        }
        if (quick != null && !quick.isBlank()) {
            switch (quick) {
                case "todo" -> where.append(" and t.status in ('待受理','已分派','处理中') ");
                case "processing" -> where.append(" and t.status in ('处理中','待审核') ");
                case "done" -> where.append(" and t.status in ('已归档','待回访') ");
                case "abnormal" -> where.append(" and (ifnull(t.overdue_flag,0)=1 or t.status='待受理' "
                        + "or ifnull(t.duplicate_flag,0)=1) ");
                default -> {
                }
            }
        }
        String base = "select t.id, t.ticket_no, t.customer_name, t.phone, t.channel, t.category_name, "
                + "t.problem_type, t.level_code, t.title, t.dept_name, t.status, t.current_handler_name, "
                + "t.overdue_flag, t.duplicate_flag, t.invalid_flag, t.create_time, t.deadline_time, "
                + "t.finish_time, t.cover_image, t.object_name, t.demand from complaint_ticket t ";
        long total = num("select count(*) from complaint_ticket t " + where, args.toArray());
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 100);
        List<Map<String, Object>> list = rows(base + where
                + " order by field(t.status,'待受理','已分派','处理中','待审核','待客户确认','待回访','已归档','已取消'), "
                + "t.update_time desc limit ? offset ?", with(args, s, (p - 1) * s));
        return PageResult.of(total, p, s, list);
    }

    /** 投诉详情：基础信息 + 工单 + 附件 + 时间轴 + 创新评分，一次返回，前端不做 N 次请求 */
    public Map<String, Object> ticketDetail(Long id) {
        Map<String, Object> t = one("select t.*, c.avatar as customer_avatar, o.cover_image as object_image "
                + "from complaint_ticket t left join customer c on c.id = t.customer_id "
                + "left join service_object o on o.id = t.object_id where t.id = ? and t.deleted=0", id);
        if (t == null) {
            throw new BizException(404, "投诉不存在或已删除");
        }
        checkReadable(longOr(t.get("customerId"), 0), longOr(t.get("deptId"), 0));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ticket", t);
        out.put("order", one("select * from complaint_ticket_order where ticket_id = ? and deleted=0 order by id desc limit 1", id));
        out.put("attachments", rows("select * from complaint_ticket_attachment where ticket_id = ? and deleted=0 order by id", id));
        out.put("timeline", rows("select * from complaint_ticket_record where ticket_id = ? and deleted=0 order by operate_time, id", id));
        out.put("handles", rows("select * from handle_record where ticket_id = ? and deleted=0 order by step_no", id));
        out.put("comms", rows("select * from comm_record where ticket_id = ? and deleted=0 order by comm_time", id));
        out.put("review", one("select * from review_record where ticket_id = ? and deleted=0 order by id desc limit 1", id));
        out.put("confirm", one("select * from customer_confirm where ticket_id = ? and deleted=0 order by id desc limit 1", id));
        out.put("revisit", one("select * from revisit_record where ticket_id = ? and deleted=0 order by id desc limit 1", id));
        out.put("evaluate", one("select * from evaluate_record where ticket_id = ? and deleted=0 order by id desc limit 1", id));
        out.put("reopens", rows("select * from reopen_record where ticket_id = ? and deleted=0 order by id", id));
        out.put("delays", rows("select * from delay_apply where ticket_id = ? and deleted=0 order by id", id));
        out.put("coops", rows("select * from coop_task where ticket_id = ? and deleted=0 order by id", id));
        out.put("warnings", rows("select * from warning_record where ticket_id = ? and deleted=0 order by warn_time desc", id));
        out.put("score", one("select * from complaint_ticket_score where ticket_id = ? and deleted=0 order by id desc limit 1", id));
        return out;
    }

    /** 越权保护：非全量角色只能读自己可见的数据（菜单权限不能替代数据范围校验） */
    private void checkReadable(long customerId, long deptId) {
        UserContext.CurrentUser u = UserContext.get();
        if (u == null) {
            return;
        }
        if ("CUSTOMER".equals(u.roleCode())) {
            long own = num("select count(*) from customer where user_id = ? and id = ?", u.userId(), customerId);
            if (own == 0) {
                throw new BizException(403, "只能查看本人的投诉");
            }
        }
        if ("ORG".equals(u.dataScope())) {
            long main = num("select count(*) from complaint_ticket where id in "
                    + "(select id from complaint_ticket where dept_id = ?) and deleted=0", u.orgId());
            long coop = num("select count(*) from coop_task where dept_id = ? and deleted=0", u.orgId());
            if (deptId != (u.orgId() == null ? -1 : u.orgId()) && main == 0 && coop == 0) {
                throw new BizException(403, "只能查看本部门主办的工单");
            }
        }
    }

    // ================================================================= 新建 / 编辑

    @Transactional
    public Map<String, Object> createTicket(Map<String, Object> body) {
        String title = str(body.get("title"));
        Long customerId = longOr(body.get("customerId"), 0);
        Long objectId = longOr(body.get("objectId"), 0);
        Long categoryId = longOr(body.get("categoryId"), 0);
        if (title == null || title.isBlank()) {
            throw new BizException("投诉主题不能为空");
        }
        if (customerId == 0 || objectId == 0 || categoryId == 0) {
            throw new BizException("客户、关联对象与问题分类为必填项");
        }
        Map<String, Object> cust = one("select customer_name, phone from customer where id = ?", customerId);
        Map<String, Object> obj = one("select object_name from service_object where id = ?", objectId);
        Map<String, Object> cat = one("select category_name, problem_type, service_stage, default_level, "
                + "response_hours, handle_hours from complaint_category where id = ?", categoryId);
        if (cust == null || obj == null || cat == null) {
            throw new BizException("客户、关联对象或问题分类不存在");
        }
        Long deptId = longOr(body.get("deptId"), 4);
        Map<String, Object> dept = one("select org_name from sys_org where id = ?", deptId);
        String deptName = dept == null ? "售后服务一部" : str(dept.get("orgName"));
        String level = str(body.get("levelCode"));
        if (level == null || level.isBlank()) {
            level = str(cat.get("defaultLevel"));
        }
        // 分类带出建议时限，级别规则可覆盖，保证「按分类自动带出建议级别和 SLA」
        int respH = intOf(cat.get("responseHours"), 8);
        int handleH = intOf(cat.get("handleHours"), 72);
        Map<String, Object> lv = one("select response_hours, handle_hours from severity_rule where level_code = ?", level);
        if (lv != null) {
            respH = intOf(lv.get("responseHours"), respH);
            handleH = intOf(lv.get("handleHours"), handleH);
        }
        String no = "TS" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%03d", num("select count(*) from complaint_ticket") + 1);
        jdbc.update("""
                insert into complaint_ticket(ticket_no, customer_id, customer_name, phone, object_id, object_name,
                  channel, category_id, category_name, problem_type, service_stage, level_code, title, content,
                  demand, occur_time, occur_place, dept_id, dept_name, status, accept_time, response_deadline,
                  deadline_time, overdue_flag, invalid_flag, duplicate_flag, source, cover_image, reason_tags,
                  product_tag, create_by, create_time, update_time)
                values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'待受理',null,
                  date_add(now(), interval ? hour), date_add(now(), interval ? hour), 0,0,0,?,?,?,?,?,now(),now())
                """, no, customerId, cust.get("customerName"), cust.get("phone"), objectId, obj.get("objectName"),
                str(body.getOrDefault("channel", "电话")), categoryId, cat.get("categoryName"),
                cat.get("problemType"), cat.get("serviceStage"), level, title,
                str(body.getOrDefault("content", "")), str(body.getOrDefault("demand", "")),
                now(), str(body.getOrDefault("occurPlace", "")), deptId, deptName, respH, handleH,
                str(body.getOrDefault("source", "客服代录")),
                str(body.getOrDefault("coverImage", "/uploads/seed/evidence/01.jpg")),
                str(body.getOrDefault("reasonTags", "沟通不到位")),
                str(body.getOrDefault("productTag", "除湿机")), UserContext.userId());
        Long id = num("select id from complaint_ticket where ticket_no = ?", no);
        record(id, null, "创建", "", "待受理", "新建客诉，编号 " + no, false);
        return Map.of("id", id, "ticketNo", no);
    }

    @Transactional
    public void updateTicket(Long id, Map<String, Object> body) {
        Map<String, Object> t = one("select * from complaint_ticket where id = ? and deleted=0", id);
        if (t == null) {
            throw new BizException("投诉不存在");
        }
        // 并发校验：前端带上 version，与库里不一致说明已被他人修改
        if (body.get("version") != null && intOf(body.get("version"), 0) != intOf(t.get("version"), 1)) {
            throw new BizException(409, "该投诉已被他人更新，请刷新后重试");
        }
        List<String> sets = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        String[][] fields = {{"title", "title"}, {"content", "content"}, {"demand", "demand"},
                {"channel", "channel"}, {"levelCode", "level_code"}, {"occurPlace", "occur_place"}};
        for (String[] f : fields) {
            if (body.containsKey(f[0])) {
                sets.add(f[1] + " = ?");
                args.add(str(body.get(f[0])));
            }
        }
        if (body.get("categoryId") != null) {
            Map<String, Object> cat = one("select category_name, problem_type, service_stage "
                    + "from complaint_category where id = ?", longOr(body.get("categoryId"), 0));
            sets.add("category_id = ?");
            args.add(longOr(body.get("categoryId"), 0));
            sets.add("category_name = ?");
            args.add(cat == null ? null : cat.get("categoryName"));
            sets.add("problem_type = ?");
            args.add(cat == null ? null : cat.get("problemType"));
            sets.add("service_stage = ?");
            args.add(cat == null ? null : cat.get("serviceStage"));
        }
        if (body.get("deptId") != null) {
            long deptId = longOr(body.get("deptId"), 0);
            Map<String, Object> d = one("select org_name from sys_org where id = ?", deptId);
            sets.add("dept_id = ?");
            args.add(deptId);
            sets.add("dept_name = ?");
            args.add(d == null ? null : d.get("orgName"));
        }
        if (sets.isEmpty()) {
            return;
        }
        sets.add("version = version + 1");
        sets.add("update_time = now()");
        args.add(id);
        jdbc.update("update complaint_ticket set " + String.join(", ", sets) + " where id = ?", args.toArray());
        record(id, null, "编辑", str(t.get("status")), str(t.get("status")), "修改投诉信息", false);
    }

    // ================================================================= 状态流转（状态机）

    /**
     * 唯一的状态流转入口。
     * 待受理→已分派→处理中→待审核→待客户确认→待回访→已归档；
     * 允许「退回上一节点」和「取消」，其余越级/回退直接拒绝。
     *
     * @param toStatus  目标状态
     * @param reason    操作原因（退回、驳回、取消时必填，≥5 字）
     * @param handlerId 分派时指定的处理人
     * @param deptId    分派时指定的责任部门
     */
    @Transactional
    public Map<String, Object> transition(Long id, String toStatus, String reason, Long handlerId, Long deptId) {
        Map<String, Object> t = one("select * from complaint_ticket where id = ? and deleted=0", id);
        if (t == null) {
            throw new BizException("投诉不存在");
        }
        String from = str(t.get("status"));
        if (toStatus == null || toStatus.isBlank()) {
            throw new BizException("目标状态不能为空");
        }
        if (Objects.equals(from, toStatus)) {
            throw new BizException("当前已是「" + from + "」状态，请勿重复提交");
        }
        if ("已取消".equals(toStatus) || "处理中".equals(toStatus) && "待审核".equals(from)
                || "处理中".equals(toStatus) && "待客户确认".equals(from)) {
            requireReason(reason);
        } else {
            int fi = FLOW.indexOf(from);
            int ti = FLOW.indexOf(toStatus);
            if (fi < 0 || ti < 0) {
                throw new BizException("不支持的状态流转：" + from + " → " + toStatus);
            }
            if (ti != fi + 1) {
                throw new BizException("不允许越级流转：" + from + " → " + toStatus + "，请按流程逐级推进");
            }
        }
        Long hId = handlerId != null ? handlerId : longOr(t.get("currentHandlerId"), 0);
        Map<String, Object> hu = hId == 0 ? null : one("select real_name from sys_user where id = ?", hId);
        String hName = hu == null ? str(t.get("currentHandlerName")) : str(hu.get("realName"));
        long dId = deptId != null ? deptId : longOr(t.get("deptId"), 0);
        Map<String, Object> du = dId == 0 ? null : one("select org_name from sys_org where id = ?", dId);
        String dName = du == null ? str(t.get("deptName")) : str(du.get("orgName"));

        List<String> sets = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        sets.add("status = ?");
        args.add(toStatus);
        sets.add("current_handler_id = ?");
        args.add(hId == 0 ? null : hId);
        sets.add("current_handler_name = ?");
        args.add(hName);
        if (deptId != null) {
            sets.add("dept_id = ?");
            args.add(dId);
            sets.add("dept_name = ?");
            args.add(dName);
        }
        if ("已分派".equals(toStatus)) {
            sets.add("accept_time = ifnull(accept_time, now())");
        }
        if ("待审核".equals(toStatus)) {
            sets.add("finish_time = now()");
        }
        if ("待回访".equals(toStatus)) {
            sets.add("confirm_time = now()");
        }
        if ("已归档".equals(toStatus)) {
            sets.add("archive_time = now()");
            sets.add("finish_time = ifnull(finish_time, now())");
        }
        // 超时判定：截止时间已过且未归档
        sets.add("overdue_flag = if(deadline_time < now() and ? <> '已归档', 1, ifnull(overdue_flag,0))");
        args.add(toStatus);
        sets.add("version = version + 1");
        sets.add("update_time = now()");
        args.add(id);
        jdbc.update("update complaint_ticket set " + String.join(", ", sets) + " where id = ?", args.toArray());

        // 工单同步：首次流转建工单，其余节点推进工单状态
        Map<String, Object> order = one("select * from complaint_ticket_order where ticket_id = ? "
                + "and deleted=0 order by id desc limit 1", id);
        Long orderId;
        if (order == null) {
            orderId = null;
            if (!"已取消".equals(toStatus)) {
                orderId = createOrder(id, str(t.get("ticketNo")), toStatus, hId, hName, dId, dName);
            }
        } else {
            orderId = longOr(order.get("id"), 0);
            jdbc.update("update complaint_ticket_order set status = ?, node_name = ?, handler_id = ?, "
                            + "handler_name = ?, dept_id = ?, dept_name = ?, actual_finish_time = "
                            + "if(? in ('待审核','待客户确认','待回访','已归档'), now(), actual_finish_time), "
                            + "close_time = if(? = '已归档', now(), close_time), version = version + 1, "
                            + "update_time = now() where id = ?",
                    toStatus, nodeName(toStatus), hId == 0 ? null : hId, hName, dId, dName,
                    toStatus, toStatus, orderId);
        }
        String rtype = switch (toStatus) {
            case "已分派" -> "分派";
            case "已归档" -> "归档";
            case "已取消" -> "作废";
            case "待审核" -> "处理";
            default -> "处理";
        };
        record(id, orderId, rtype, from, toStatus,
                reason == null || reason.isBlank() ? "状态推进至" + toStatus : reason, false);
        if ("已分派".equals(toStatus)) {
            jdbc.update("insert into dispatch_record(ticket_id, order_id, dispatch_type, from_dept_id, to_dept_id, "
                            + "to_dept_name, handler_id, handler_name, reason, operator_id, operator_name, dispatch_time, "
                            + "create_time, update_time) values(?,?,?,?,?,?,?,?,?,?,?,now(),now(),now())",
                    id, orderId, "首次分派", longOr(t.get("deptId"), 0), dId, dName,
                    hId == 0 ? null : hId, hName, "按分类归属分派", UserContext.userId(), UserContext.realName());
            pushMessage(hId, "待处理", "您有新的客诉工单待接单", "/order/desk");
        }
        if ("待审核".equals(toStatus)) {
            Long leader = num("select ifnull(max(id),0) from sys_user where username = 'leader01'");
            pushMessage(leader, "待审核", "处理方案已提交，请及时审核", "/review");
        }
        if ("已归档".equals(toStatus)) {
            pushMessage(longOr(t.get("createBy"), 0), "系统通知", "工单已归档", "/revisit");
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", id);
        res.put("from", from);
        res.put("to", toStatus);
        res.put("orderId", orderId);
        return res;
    }

    private Long createOrder(Long ticketId, String ticketNo, String status, Long hId, String hName,
                             long deptId, String deptName) {
        String no = "WO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%03d", num("select count(*) from complaint_ticket_order") + 1);
        Map<String, Object> t = one("select level_code, deadline_time from complaint_ticket where id = ?", ticketId);
        jdbc.update("""
                insert into complaint_ticket_order(order_no, ticket_id, ticket_no, node_name, status, handler_id,
                  handler_name, dept_id, dept_name, priority, dispatch_time, accept_time, plan_finish_time,
                  deadline_time, overdue_flag, coop_flag, create_by, create_time, update_time)
                values(?,?,?,?,?,?,?,?,?,?,now(),now(),date_sub(ifnull(?,now()), interval 2 hour),?,0,0,?,now(),now())
                """, no, ticketId, ticketNo, nodeName(status), status, hId == 0 ? null : hId, hName,
                deptId, deptName,
                t != null && "P1".equals(str(t.get("levelCode"))) ? "高" : "中",
                t == null ? null : t.get("deadlineTime"), t == null ? null : t.get("deadlineTime"),
                UserContext.userId());
        return num("select id from complaint_ticket_order where order_no = ?", no);
    }

    private String nodeName(String status) {
        return switch (status) {
            case "已分派" -> "接单";
            case "处理中" -> "处理";
            case "待审核" -> "审核";
            case "待客户确认" -> "客户确认";
            case "待回访" -> "回访";
            case "已归档" -> "归档";
            default -> "受理";
        };
    }

    private void requireReason(String reason) {
        if (reason == null || reason.trim().length() < 5) {
            throw new BizException("退回、驳回、取消必须填写不少于 5 个字的原因");
        }
    }

    private void record(Long ticketId, Long orderId, String type, String from, String to,
                        String content, boolean internal) {
        jdbc.update("insert into complaint_ticket_record(ticket_id, order_id, record_type, from_status, to_status, "
                        + "content, operator_id, operator_name, internal_flag, operate_time, create_time, update_time) "
                        + "values(?,?,?,?,?,?,?,?,?,now(),now(),now())",
                ticketId, orderId, type, from, to, content, UserContext.userId(),
                UserContext.realName(), internal ? 1 : 0);
    }

    private void pushMessage(Long receiverId, String type, String title, String link) {
        if (receiverId == null || receiverId == 0) {
            return;
        }
        jdbc.update("insert into sys_message(title, content, msg_type, link_url, receiver_id, is_read, "
                        + "create_time, update_time) values(?,?,?,?,?,0,now(),now())",
                title, title + "，请及时处理。", type, link, receiverId);
    }

    // ================================================================= 创新点：结构化共性问题聚类

    public Map<String, Object> activeRule() {
        Map<String, Object> r = one("select * from complaint_ticket_rule where deleted=0 and status='启用' "
                + "order by effective_time desc limit 1");
        if (r == null) {
            throw new BizException("未配置可用的聚类规则");
        }
        return r;
    }

    /**
     * 执行一次聚类计算：以同产品同环节的近期工单为参照，按六因子加权得到相似度。
     * 计算过程完全可复现——输入快照、规则版本、各项得分全部落库。
     */
    @Transactional
    public Map<String, Object> evaluate(Long ticketId) {
        Map<String, Object> t = one("select * from complaint_ticket where id = ? and deleted=0", ticketId);
        if (t == null) {
            throw new BizException("投诉不存在");
        }
        Map<String, Object> rule = activeRule();
        int win = intOf(rule.get("windowDays"), 30);
        String product = str(t.get("productTag"));
        String stage = str(t.get("serviceStage"));
        String ptype = str(t.get("problemType"));
        long deptId = longOr(t.get("deptId"), 0);
        String tags = str(t.get("reasonTags"));
        if (product == null || product.isBlank() || stage == null || ptype == null) {
            throw new BizException("缺少关联产品/服务环节/问题类型，无法计算，请先补录");
        }

        List<Map<String, Object>> pool = rows("select id, product_tag, service_stage, reason_tags "
                + "from complaint_ticket where deleted=0 and create_time >= date_sub(now(), interval ? day) "
                + "and id <> ?", win, ticketId);
        long cProd = num("select count(*) from complaint_ticket where deleted=0 and product_tag = ? "
                + "and create_time >= date_sub(now(), interval ? day)", product, win);
        long cStage = num("select count(*) from complaint_ticket where deleted=0 and service_stage = ? "
                + "and create_time >= date_sub(now(), interval ? day)", stage, win);
        long cPtype = num("select count(*) from complaint_ticket where deleted=0 and problem_type = ? "
                + "and create_time >= date_sub(now(), interval ? day)", ptype, win);
        long cDept = num("select count(*) from complaint_ticket where deleted=0 and dept_id = ? "
                + "and create_time >= date_sub(now(), interval ? day)", deptId, win);
        long cCombo = num("select count(*) from complaint_ticket where deleted=0 and product_tag = ? "
                + "and service_stage = ? and problem_type = ? and dept_id = ? "
                + "and create_time >= date_sub(now(), interval ? day)", product, stage, ptype, deptId, win);

        // 原因标签：与同产品同环节的历史工单做 Jaccard 重合度
        SetX mine = new SetX(tags);
        SetX union = new SetX("");
        for (Map<String, Object> p : pool) {
            if (Objects.equals(str(p.get("productTag")), product)
                    && Objects.equals(str(p.get("serviceStage")), stage)) {
                union.addAll(new SetX(str(p.get("reasonTags"))));
            }
        }
        double ratioR = union.isEmpty() ? 0 : (double) mine.intersect(union) / mine.unionSize(union);

        // 容量基准与种子数据保持一致：同一取值在窗口内出现多少次算「共性」
        double rP = Math.min(1.0, cProd / 7.0);
        double rS = Math.min(1.0, cStage / 5.0);
        double rT = Math.min(1.0, cPtype / 7.0);
        double rD = Math.min(1.0, cDept / 11.0);
        double rF = Math.min(1.0, cCombo / 4.0);

        int wP = intOf(rule.get("wProduct"), 25);
        int wS = intOf(rule.get("wStage"), 15);
        int wT = intOf(rule.get("wProblem"), 20);
        int wR = intOf(rule.get("wReason"), 15);
        int wD = intOf(rule.get("wDept"), 10);
        int wF = intOf(rule.get("wFreq"), 15);
        int sP = (int) Math.round(wP * rP);
        int sS = (int) Math.round(wS * rS);
        int sT = (int) Math.round(wT * rT);
        int sR = (int) Math.round(wR * ratioR);
        int sD = (int) Math.round(wD * rD);
        int sF = (int) Math.round(wF * rF);
        double sim = Math.round((sP + sS + sT + sR + sD + sF) * 100) / 100.0;

        int hi = intOf(rule.get("highThreshold"), 80);
        int mid = intOf(rule.get("midThreshold"), 60);
        String level = sim >= hi ? "HIGH" : (sim >= mid ? "MID" : "LOW");

        List<String> reasons = new ArrayList<>();
        if (sP >= wP * 0.8) {
            reasons.add("关联产品吻合（近 " + win + " 天同产品 " + cProd + " 单）");
        }
        if (sT >= wT * 0.8) {
            reasons.add("问题类型集中（同类型 " + cPtype + " 单）");
        }
        if (sF >= wF * 0.8) {
            reasons.add("同组合重复出现 " + cCombo + " 次");
        }
        if (sR >= wR * 0.8) {
            reasons.add("原因标签与历史案例高度重合");
        }
        List<String> unmet = new ArrayList<>();
        List<String> improve = new ArrayList<>();
        if (sR < wR * 0.8) {
            unmet.add("原因标签重合度不足");
            improve.add("补录更具体的原因标签");
        }
        if (sF < wF * 0.8) {
            unmet.add("同组合近 " + win + " 天次数偏少");
            improve.add("积累更多同组合样本后再评估");
        }
        if (sS < wS * 0.8) {
            unmet.add("服务环节样本分散");
            improve.add("细化服务环节分类");
        }
        String cluster = product + "·" + stage + "·" + ptype;
        String snapshot = "产品=" + product + ";环节=" + stage + ";问题=" + ptype + ";标签=" + tags
                + ";部门=" + str(t.get("deptName")) + ";近30天=" + cCombo;

        jdbc.update("""
                insert into complaint_ticket_score(ticket_id, ticket_no, cluster_name, rule_version, s_product, s_stage,
                  s_problem, s_reason, s_dept, s_freq, similarity, level, input_snapshot, reasons, unmet, improve,
                  calc_time, adopt_flag, create_time, update_time)
                values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,now(),0,now(),now())
                """, ticketId, t.get("ticketNo"), cluster, rule.get("ruleVersion"), sP, sS, sT, sR, sD, sF,
                sim, level, snapshot,
                reasons.isEmpty() ? "各因子得分普遍偏低" : String.join("；", reasons),
                unmet.isEmpty() ? "无" : String.join("；", unmet),
                improve.isEmpty() ? "无需改进" : String.join("；", improve));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ticketId", ticketId);
        out.put("clusterName", cluster);
        out.put("ruleVersion", rule.get("ruleVersion"));
        out.put("similarity", sim);
        out.put("level", level);
        out.put("snapshot", snapshot);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("关联产品", factor(sP, wP, rP));
        detail.put("服务环节", factor(sS, wS, rS));
        detail.put("问题类型", factor(sT, wT, rT));
        detail.put("原因标签", factor(sR, wR, ratioR));
        detail.put("责任部门", factor(sD, wD, rD));
        detail.put("近30天次数", factor(sF, wF, rF));
        out.put("factors", detail);
        out.put("reasons", reasons);
        out.put("unmet", unmet);
        out.put("improve", improve);
        return out;
    }

    private Map<String, Object> factor(int score, int weight, double ratio) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("score", score);
        m.put("weight", weight);
        m.put("ratio", Math.round(ratio * 100) / 100.0);
        return m;
    }

    /** 轻量标签集合，避免为一次计算引入额外依赖 */
    private static class SetX {
        private final java.util.Set<String> set = new java.util.LinkedHashSet<>();

        SetX(String csv) {
            addAll(csv);
        }

        void addAll(String csv) {
            if (csv == null || csv.isBlank()) {
                return;
            }
            for (String s : csv.split("[,，;；]")) {
                String t = s.trim();
                if (!t.isEmpty()) {
                    set.add(t);
                }
            }
        }

        void addAll(SetX other) {
            set.addAll(other.set);
        }

        boolean isEmpty() {
            return set.isEmpty();
        }

        int intersect(SetX other) {
            int n = 0;
            for (String s : set) {
                if (other.set.contains(s)) {
                    n++;
                }
            }
            return n;
        }

        int unionSize(SetX other) {
            java.util.Set<String> u = new java.util.LinkedHashSet<>(set);
            u.addAll(other.set);
            return Math.max(1, u.size());
        }
    }

    /** 人工调整：只写 manual_*，原始 similarity 与 level 保留不覆盖 */
    @Transactional
    public void manualAdjust(Long scoreId, String manualLevel, String reason) {
        if (reason == null || reason.trim().length() < 5) {
            throw new BizException("人工调整必须填写不少于 5 个字的原因");
        }
        Map<String, Object> s = one("select id from complaint_ticket_score where id = ? and deleted=0", scoreId);
        if (s == null) {
            throw new BizException("计算记录不存在");
        }
        jdbc.update("update complaint_ticket_score set manual_level = ?, manual_reason = ?, manual_by = ?, "
                + "manual_time = now(), update_time = now() where id = ?",
                manualLevel, reason, UserContext.realName(), scoreId);
    }

    /** 聚类结果列表（含人工调整与采纳情况） */
    public PageResult<Map<String, Object>> clusterPage(String level, String keyword, int page, int size) {
        StringBuilder where = new StringBuilder(" where s.deleted=0 ");
        List<Object> args = new ArrayList<>();
        if (level != null && !level.isBlank()) {
            where.append(" and s.level = ? ");
            args.add(level);
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" and (s.cluster_name like ? or s.ticket_no like ?) ");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
        }
        long total = num("select count(*) from complaint_ticket_score s " + where, args.toArray());
        int p = Math.max(page, 1);
        int s = Math.max(size, 1);
        List<Map<String, Object>> list = rows("select s.*, t.title, t.dept_name, t.category_name "
                + "from complaint_ticket_score s left join complaint_ticket t on t.id = s.ticket_id "
                + where + " order by s.similarity desc, s.id desc limit ? offset ?",
                with(args, s, (p - 1) * s));
        return PageResult.of(total, p, s, list);
    }

    /** 聚类汇总：按主题聚合，支撑「一键转整改」 */
    public List<Map<String, Object>> clusterSummary() {
        return rows("""
                select s.cluster_name, count(*) as cnt, round(avg(s.similarity), 1) as avg_score,
                       sum(case when s.level = 'HIGH' then 1 else 0 end) as high_cnt,
                       sum(case when s.manual_level is not null then 1 else 0 end) as manual_cnt,
                       sum(case when s.adopt_flag = 1 then 1 else 0 end) as adopt_cnt,
                       group_concat(s.ticket_no order by s.id separator ',') as tickets
                from complaint_ticket_score s where s.deleted = 0
                group by s.cluster_name order by cnt desc, avg_score desc limit 12
                """);
    }

    /** 规则列表与保存（权重合计必须 100） */
    public List<Map<String, Object>> ruleList() {
        return rows("select * from complaint_ticket_rule where deleted=0 order by effective_time desc");
    }

    @Transactional
    public Map<String, Object> saveRule(Map<String, Object> body) {
        int wP = intOf(body.get("wProduct"), 0);
        int wS = intOf(body.get("wStage"), 0);
        int wT = intOf(body.get("wProblem"), 0);
        int wR = intOf(body.get("wReason"), 0);
        int wD = intOf(body.get("wDept"), 0);
        int wF = intOf(body.get("wFreq"), 0);
        int sum = wP + wS + wT + wR + wD + wF;
        if (sum != 100) {
            throw new BizException("权重合计必须等于 100%，当前为 " + sum + "%");
        }
        String remark = str(body.getOrDefault("remark", "调整权重配置"));
        if (remark == null || remark.trim().length() < 5) {
            throw new BizException("变更说明不少于 5 个字");
        }
        String ver = str(body.get("ruleVersion"));
        if (ver == null || ver.isBlank()) {
            ver = "v" + (num("select count(*) from complaint_ticket_rule") + 1);
        }
        // 旧版本停用，新版本生效；历史计算结果继续挂在旧版本号上，保证可追溯
        jdbc.update("update complaint_ticket_rule set status='停用', update_time=now() where deleted=0");
        jdbc.update("""
                insert into complaint_ticket_rule(rule_name, rule_version, w_product, w_stage, w_problem, w_reason,
                  w_dept, w_freq, high_threshold, mid_threshold, window_days, effective_time, status, operator_name,
                  remark, create_time, update_time)
                values('结构化共性问题聚类规则',?,?,?,?,?,?,?,?,?,?,now(),'启用',?,?,now(),now())
                """, ver, wP, wS, wT, wR, wD, wF,
                intOf(body.get("highThreshold"), 80), intOf(body.get("midThreshold"), 60),
                intOf(body.get("windowDays"), 30), UserContext.realName(), remark);
        return Map.of("ruleVersion", ver, "status", "启用");
    }

    // ================================================================= 工单台

    /** SLA 工单看板：按状态分组 + 待办清单 + 协办任务 */
    public Map<String, Object> orderDesk() {
        List<Object> a = new ArrayList<>();
        String sc = scope("t", a);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("groups", chart(rows("select o.status as label, count(*) as value from complaint_ticket_order o "
                + "join complaint_ticket t on t.id = o.ticket_id where o.deleted=0 and " + sc
                + " group by o.status", a.toArray())));
        List<Object> b = new ArrayList<>();
        String sc2 = scope("t", b);
        out.put("todo", rows("select o.id, o.order_no, o.ticket_id, o.ticket_no, o.status, o.node_name, "
                + "o.handler_name, o.dept_name, o.priority, o.deadline_time, o.overdue_flag, t.title, t.level_code "
                + "from complaint_ticket_order o join complaint_ticket t on t.id = o.ticket_id "
                + "where o.deleted=0 and o.status not in ('已归档','已取消') and " + sc2
                + " order by o.overdue_flag desc, o.deadline_time asc limit 50", b.toArray()));
        out.put("coops", rows("select c.*, t.ticket_no, t.title from coop_task c "
                + "join complaint_ticket t on t.id = c.ticket_id where c.deleted=0 and c.status <> '已完成' "
                + "order by c.deadline_time asc limit 30"));
        return out;
    }

    /** 我的处理台：待本人接单 / 处理中的工单 */
    public Map<String, Object> handleDesk() {
        Long uid = UserContext.userId();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("waiting", rows("select o.*, t.title, t.level_code, t.cover_image, t.category_name, "
                + "t.customer_name, t.service_stage, t.problem_type from complaint_ticket_order o "
                + "join complaint_ticket t on t.id = o.ticket_id "
                + "where o.deleted=0 and o.handler_id = ? and o.status = '已分派' order by o.deadline_time", uid));
        out.put("handling", rows("select o.*, t.title, t.level_code, t.cover_image, t.category_name, "
                + "t.customer_name, t.service_stage, t.problem_type from complaint_ticket_order o "
                + "join complaint_ticket t on t.id = o.ticket_id "
                + "where o.deleted=0 and o.handler_id = ? and o.status in ('处理中','待审核') "
                + "order by o.deadline_time", uid));
        Map<String, Object> st = new LinkedHashMap<>();
        st.put("waiting", num("select count(*) from complaint_ticket_order where deleted=0 and handler_id=? and status='已分派'", uid));
        st.put("handling", num("select count(*) from complaint_ticket_order where deleted=0 and handler_id=? and status='处理中'", uid));
        st.put("done", num("select count(*) from complaint_ticket_order where deleted=0 and handler_id=? and status in ('待回访','已归档')", uid));
        st.put("overdue", num("select count(*) from complaint_ticket_order where deleted=0 and handler_id=? and overdue_flag=1", uid));
        out.put("stats", st);
        return out;
    }

    /** 提交处理方案：写处理记录并推进到待审核 */
    @Transactional
    public Map<String, Object> submitHandle(Map<String, Object> body) {
        Long ticketId = longOr(body.get("ticketId"), 0);
        if (ticketId == 0) {
            throw new BizException("缺少投诉编号");
        }
        String content = str(body.get("content"));
        String solution = str(body.get("solution"));
        if (content == null || content.trim().length() < 5) {
            throw new BizException("调查情况不少于 5 个字");
        }
        if (solution == null || solution.trim().length() < 5) {
            throw new BizException("解决方案不少于 5 个字");
        }
        Map<String, Object> o = one("select * from complaint_ticket_order where ticket_id = ? and deleted=0 "
                + "order by id desc limit 1", ticketId);
        int step = (int) num("select ifnull(max(step_no),0)+1 from handle_record where ticket_id = ?", ticketId);
        jdbc.update("insert into handle_record(ticket_id, order_id, step_no, content, solution, attach_path, "
                        + "plan_finish_time, operator_id, operator_name, handle_time, internal_flag, "
                        + "create_time, update_time) values(?,?,?,?,?,?,?,?,?,now(),?,now(),now())",
                ticketId, o == null ? null : o.get("id"), step, content, solution,
                str(body.getOrDefault("attachPath", "/uploads/seed/evidence/02.jpg")),
                str(body.getOrDefault("planFinishTime", now())), UserContext.userId(),
                UserContext.realName(), intOf(body.get("internalFlag"), 0) == 1 ? 1 : 0);
        if (o != null) {
            jdbc.update("update complaint_ticket_order set solution = ?, result = ?, update_time = now() where id = ?",
                    solution, str(body.getOrDefault("result", "问题已解决，客户同意结案。")), o.get("id"));
        }
        transition(ticketId, "待审核", str(body.getOrDefault("reason", "提交处理方案")), null, null);
        return Map.of("ticketId", ticketId, "step", step);
    }

    /** 接单：已分派 → 处理中 */
    @Transactional
    public void acceptOrder(Long ticketId) {
        transition(ticketId, "处理中", "接单处理", null, null);
    }

    // ================================================================= 审核 / 确认 / 回访 / 重开

    public PageResult<Map<String, Object>> reviewPage(String result, int page, int size) {
        StringBuilder where = new StringBuilder(" where r.deleted=0 ");
        List<Object> args = new ArrayList<>();
        if (result != null && !result.isBlank()) {
            where.append(" and r.review_result = ? ");
            args.add(result);
        }
        long total = num("select count(*) from review_record r " + where, args.toArray());
        int p = Math.max(page, 1);
        int s = Math.max(size, 1);
        return PageResult.of(total, p, s, rows(
                "select r.*, t.title, t.level_code, t.category_name, t.dept_name from review_record r "
                        + "left join complaint_ticket t on t.id = r.ticket_id " + where
                        + " order by r.review_time desc limit ? offset ?", with(args, s, (p - 1) * s)));
    }

    /** 审核：通过转待客户确认；驳回退回处理中，并把原因写进工单 */
    @Transactional
    public void review(Long ticketId, boolean pass, String opinion, Integer planScore) {
        Map<String, Object> t = one("select * from complaint_ticket where id = ? and deleted=0", ticketId);
        if (t == null) {
            throw new BizException("投诉不存在");
        }
        if (!"待审核".equals(str(t.get("status")))) {
            throw new BizException("当前状态为「" + str(t.get("status")) + "」，不在待审核环节，不能重复审核");
        }
        if (opinion == null || opinion.trim().length() < 5) {
            throw new BizException("审核意见不少于 5 个字");
        }
        String no = "RV" + String.format("%05d", num("select count(*) from review_record") + 1);
        Map<String, Object> o = one("select id from complaint_ticket_order where ticket_id = ? and deleted=0 "
                + "order by id desc limit 1", ticketId);
        jdbc.update("insert into review_record(review_no, ticket_id, order_id, review_result, plan_score, "
                        + "evidence_flag, duty_check, opinion, reviewer_id, reviewer_name, review_time, "
                        + "create_time, update_time) values(?,?,?,?,?,?,?,?,?,?,now(),now(),now())",
                no, ticketId, o == null ? null : o.get("id"), pass ? "通过" : "驳回",
                planScore == null ? 85 : planScore, 1,
                "责任认定为" + str(t.get("deptName")), opinion, UserContext.userId(), UserContext.realName());
        if (pass) {
            transition(ticketId, "待客户确认", opinion, null, null);
        } else {
            jdbc.update("update complaint_ticket_order set return_reason = ?, update_time = now() where ticket_id = ?",
                    opinion, ticketId);
            transition(ticketId, "处理中", opinion, null, null);
        }
    }

    public PageResult<Map<String, Object>> confirmPage(String result, int page, int size) {
        StringBuilder where = new StringBuilder(" where c.deleted=0 ");
        List<Object> args = new ArrayList<>();
        if (result != null && !result.isBlank()) {
            where.append(" and c.confirm_result = ? ");
            args.add(result);
        }
        long total = num("select count(*) from customer_confirm c " + where, args.toArray());
        int p = Math.max(page, 1);
        int s = Math.max(size, 1);
        return PageResult.of(total, p, s, rows(
                "select c.*, t.title, t.ticket_no, t.level_code, t.dept_name, t.finish_time from customer_confirm c "
                        + "left join complaint_ticket t on t.id = c.ticket_id " + where
                        + " order by c.confirm_time desc limit ? offset ?", with(args, s, (p - 1) * s)));
    }

    /** 客户确认：接受→待回访；提出异议→退回处理中（留痕） */
    @Transactional
    public void confirm(Long ticketId, boolean accept, String opinion) {
        Map<String, Object> t = one("select * from complaint_ticket where id = ? and deleted=0", ticketId);
        if (t == null) {
            throw new BizException("投诉不存在");
        }
        if (!"待客户确认".equals(str(t.get("status")))) {
            throw new BizException("当前状态为「" + str(t.get("status")) + "」，不在待客户确认环节");
        }
        if (!accept && (opinion == null || opinion.trim().length() < 5)) {
            throw new BizException("提出异议必须填写不少于 5 个字的原因");
        }
        jdbc.update("insert into customer_confirm(ticket_id, order_id, confirm_result, opinion, confirm_time, "
                        + "deadline_time, auto_flag, remind_count, create_time, update_time) "
                        + "values(?,?,?,?,now(), date_add(now(), interval 2 day), 0, 0, now(), now())",
                ticketId, one("select id from complaint_ticket_order where ticket_id = ? and deleted=0 "
                        + "order by id desc limit 1", ticketId),
                accept ? "接受" : "提出异议", opinion == null ? "" : opinion);
        transition(ticketId, accept ? "待回访" : "处理中",
                opinion == null || opinion.isBlank() ? "客户确认接受处理结果" : opinion, null, null);
    }

    public PageResult<Map<String, Object>> revisitPage(String solved, int page, int size) {
        StringBuilder where = new StringBuilder(" where v.deleted=0 ");
        List<Object> args = new ArrayList<>();
        if (solved != null && !solved.isBlank()) {
            where.append(" and v.solved_flag = ? ");
            args.add(intOf(solved, 1));
        }
        long total = num("select count(*) from revisit_record v " + where, args.toArray());
        int p = Math.max(page, 1);
        int s = Math.max(size, 1);
        return PageResult.of(total, p, s, rows(
                "select v.*, t.title, t.ticket_no, t.dept_name, t.customer_name from revisit_record v "
                        + "left join complaint_ticket t on t.id = v.ticket_id " + where
                        + " order by v.revisit_time desc limit ? offset ?", with(args, s, (p - 1) * s)));
    }

    /** 回访登记：写回访 + 评价，并归档（回访是归档前最后一步，未回访不得直接归档） */
    @Transactional
    public void revisit(Map<String, Object> body) {
        Long ticketId = longOr(body.get("ticketId"), 0);
        Map<String, Object> t = one("select * from complaint_ticket where id = ? and deleted=0", ticketId);
        if (t == null) {
            throw new BizException("投诉不存在");
        }
        if (!"待回访".equals(str(t.get("status")))) {
            throw new BizException("当前状态为「" + str(t.get("status")) + "」，不在待回访环节");
        }
        int sat = intOf(body.get("satisfaction"), 4);
        if (sat < 1 || sat > 5) {
            throw new BizException("满意度取值 1-5");
        }
        String no = "VZ" + String.format("%05d", num("select count(*) from revisit_record") + 1);
        Long orderId = num("select ifnull(max(id),0) from complaint_ticket_order where ticket_id = ?", ticketId);
        jdbc.update("insert into revisit_record(revisit_no, ticket_id, order_id, revisit_type, solved_flag, "
                        + "satisfaction, opinion, next_action, revisitor_id, revisitor_name, revisit_time, "
                        + "duration_min, create_time, update_time) values(?,?,?,?,?,?,?,?,?,?,now(),?,now(),now())",
                no, ticketId, orderId == 0 ? null : orderId,
                str(body.getOrDefault("revisitType", "电话")), intOf(body.get("solvedFlag"), 1),
                sat, str(body.getOrDefault("opinion", "")), str(body.getOrDefault("nextAction", "无")),
                UserContext.userId(), UserContext.realName(), intOf(body.get("durationMin"), 8));
        jdbc.update("insert into evaluate_record(ticket_id, order_id, score, attitude_score, speed_score, "
                        + "quality_score, content, anonymous_flag, customer_id, evaluate_time, create_time, update_time) "
                        + "values(?,?,?,?,?,?,?,?,?,now(),now(),now())",
                ticketId, orderId == 0 ? null : orderId, sat, Math.min(5, sat + 1), Math.max(1, sat),
                sat, str(body.getOrDefault("opinion", "回访记录")), 0, t.get("customerId"));
        transition(ticketId, "已归档", "回访完成，工单归档", null, null);
    }

    public PageResult<Map<String, Object>> reopenPage(String status, int page, int size) {
        StringBuilder where = new StringBuilder(" where r.deleted=0 ");
        List<Object> args = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            where.append(" and r.status = ? ");
            args.add(status);
        }
        long total = num("select count(*) from reopen_record r " + where, args.toArray());
        int p = Math.max(page, 1);
        int s = Math.max(size, 1);
        return PageResult.of(total, p, s, rows(
                "select r.*, t.title, t.ticket_no, t.dept_name, t.level_code from reopen_record r "
                        + "left join complaint_ticket t on t.id = r.ticket_id " + where
                        + " order by r.apply_time desc limit ? offset ?", with(args, s, (p - 1) * s)));
    }

    /** 重开审批：受理则新建工单并把主档退回处理中，原工单与记录全部保留 */
    @Transactional
    public void handleReopen(Long reopenId, boolean pass, String note) {
        Map<String, Object> r = one("select * from reopen_record where id = ? and deleted=0", reopenId);
        if (r == null) {
            throw new BizException("重开记录不存在");
        }
        if (!"待受理".equals(str(r.get("status")))) {
            throw new BizException("该重开申请已处理，不能重复受理");
        }
        if (note == null || note.trim().length() < 5) {
            throw new BizException("受理说明不少于 5 个字");
        }
        jdbc.update("update reopen_record set status = ?, handle_time = now(), handle_note = ?, "
                        + "handler_id = ?, update_time = now() where id = ?",
                pass ? "已受理" : "已驳回", note, UserContext.userId(), reopenId);
        if (!pass) {
            return;
        }
        Long ticketId = longOr(r.get("ticketId"), 0);
        Map<String, Object> t = one("select * from complaint_ticket where id = ?", ticketId);
        String no = "WO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%03d", num("select count(*) from complaint_ticket_order") + 1);
        jdbc.update("""
                insert into complaint_ticket_order(order_no, ticket_id, ticket_no, node_name, status, handler_id,
                  handler_name, dept_id, dept_name, priority, dispatch_time, accept_time, plan_finish_time,
                  deadline_time, overdue_flag, coop_flag, create_by, create_time, update_time)
                values(?,?,?,'处理','处理中',?,?,?,?,'高',now(),now(),date_add(now(), interval 2 day),
                  date_add(now(), interval 2 day),0,0,?,now(),now())
                """, no, ticketId, t.get("ticketNo"), t.get("currentHandlerId"), t.get("currentHandlerName"),
                t.get("deptId"), t.get("deptName"), UserContext.userId());
        Long newOrderId = num("select id from complaint_ticket_order where order_no = ?", no);
        jdbc.update("update reopen_record set new_order_id = ? where id = ?", newOrderId, reopenId);
        jdbc.update("update complaint_ticket set status='处理中', archive_time=null, deadline_time="
                + "date_add(now(), interval 2 day), overdue_flag=0, version=version+1, update_time=now() "
                + "where id = ?", ticketId);
        record(ticketId, newOrderId, "重开", "已归档", "处理中", note, false);
    }

    // ================================================================= 预警 / 延时

    public PageResult<Map<String, Object>> warningPage(String type, String status, int page, int size) {
        StringBuilder where = new StringBuilder(" where w.deleted=0 ");
        List<Object> args = new ArrayList<>();
        if (type != null && !type.isBlank()) {
            where.append(" and w.warn_type = ? ");
            args.add(type);
        }
        if (status != null && !status.isBlank()) {
            where.append(" and w.status = ? ");
            args.add(status);
        }
        long total = num("select count(*) from warning_record w " + where, args.toArray());
        int p = Math.max(page, 1);
        int s = Math.max(size, 1);
        return PageResult.of(total, p, s, rows(
                "select w.*, t.title, t.level_code, t.dept_name, t.status as ticket_status, "
                        + "t.current_handler_name from warning_record w left join complaint_ticket t on t.id = w.ticket_id "
                        + where + " order by w.gap_minutes asc limit ? offset ?", with(args, s, (p - 1) * s)));
    }

    @Transactional
    public void handleWarning(Long id, String note) {
        if (note == null || note.trim().length() < 5) {
            throw new BizException("处理说明不少于 5 个字");
        }
        jdbc.update("update warning_record set status='已处理', handler_id=?, handle_time=now(), "
                + "handle_note=?, update_time=now() where id=?", UserContext.userId(), note, id);
    }

    /** 扫描剩余时限不足 4 小时的工单并生成预警（可手动触发，也用于演示） */
    @Transactional
    public Map<String, Object> scanWarning() {
        List<Map<String, Object>> candidates = rows("""
                select t.id, t.ticket_no, t.deadline_time,
                       timestampdiff(minute, now(), t.deadline_time) as gap,
                       (select count(*) from warning_record w where w.ticket_id = t.id and w.deleted = 0) as cnt
                from complaint_ticket t
                where t.deleted = 0 and t.status not in ('已归档','已取消') and t.deadline_time is not null
                  and timestampdiff(minute, now(), t.deadline_time) < 240
                """);
        int created = 0;
        for (Map<String, Object> c : candidates) {
            long gap = longOr(c.get("gap"), 0);
            String type = gap < 0 ? "处理超时" : "即将超时";
            jdbc.update("insert into warning_record(ticket_id, order_no, warn_type, warn_level, warn_time, "
                            + "deadline_time, gap_minutes, status, create_time, update_time) "
                            + "values(?,?,?,?,now(),?,?,'未处理',now(),now())",
                    c.get("id"), c.get("ticketNo"), type, gap < 0 ? "严重" : "提醒",
                    c.get("deadlineTime"), gap);
            created++;
        }
        return Map.of("scanned", candidates.size(), "created", created);
    }

    public PageResult<Map<String, Object>> delayPage(String status, int page, int size) {
        StringBuilder where = new StringBuilder(" where d.deleted=0 ");
        List<Object> args = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            where.append(" and d.status = ? ");
            args.add(status);
        }
        long total = num("select count(*) from delay_apply d " + where, args.toArray());
        int p = Math.max(page, 1);
        int s = Math.max(size, 1);
        return PageResult.of(total, p, s, rows(
                "select d.*, t.title, t.ticket_no, t.level_code, t.dept_name from delay_apply d "
                        + "left join complaint_ticket t on t.id = d.ticket_id " + where
                        + " order by d.apply_time desc limit ? offset ?", with(args, s, (p - 1) * s)));
    }

    @Transactional
    public void approveDelay(Long id, boolean pass, String note) {
        Map<String, Object> d = one("select * from delay_apply where id = ? and deleted=0", id);
        if (d == null) {
            throw new BizException("延时申请不存在");
        }
        if (!"待审批".equals(str(d.get("status")))) {
            throw new BizException("该申请已审批，不能重复提交");
        }
        if (note == null || note.trim().length() < 5) {
            throw new BizException("审批意见不少于 5 个字");
        }
        jdbc.update("update delay_apply set status=?, approver_id=?, approver_name=?, approve_time=now(), "
                        + "approve_note=?, update_time=now() where id=?",
                pass ? "已通过" : "已驳回", UserContext.userId(), UserContext.realName(), note, id);
        if (pass) {
            long ticketId = longOr(d.get("ticketId"), 0);
            jdbc.update("update complaint_ticket set deadline_time = ?, overdue_flag = 0, "
                    + "version = version+1, update_time = now() where id = ?", d.get("newDeadline"), ticketId);
            jdbc.update("update complaint_ticket_order set deadline_time = ?, overdue_flag = 0, "
                    + "update_time = now() where ticket_id = ?", d.get("newDeadline"), ticketId);
            record(ticketId, null, "延时", "", "", "延时申请通过：" + note, false);
        }
    }

    @Transactional
    public Map<String, Object> applyDelay(Long ticketId, int hours, String reason) {
        if (reason == null || reason.trim().length() < 5) {
            throw new BizException("延时原因不少于 5 个字");
        }
        Map<String, Object> t = one("select * from complaint_ticket where id = ? and deleted=0", ticketId);
        if (t == null) {
            throw new BizException("投诉不存在");
        }
        String no = "DL" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%03d", num("select count(*) from delay_apply") + 1);
        jdbc.update("insert into delay_apply(apply_no, ticket_id, order_id, old_deadline, new_deadline, "
                        + "delay_hours, reason, applicant_id, applicant_name, apply_time, status, create_time, "
                        + "update_time) values(?,?,?,?,date_add(?, interval ? hour),?,?,?,?,now(),'待审批',now(),now())",
                no, ticketId,
                one("select id from complaint_ticket_order where ticket_id = ? and deleted=0 order by id desc limit 1", ticketId),
                t.get("deadlineTime"), t.get("deadlineTime"), hours, hours, reason,
                UserContext.userId(), UserContext.realName());
        return Map.of("applyNo", no);
    }

    // ================================================================= 整改 / 知识库

    public PageResult<Map<String, Object>> rectifyPage(String status, int page, int size) {
        StringBuilder where = new StringBuilder(" where r.deleted=0 ");
        List<Object> args = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            where.append(" and r.status = ? ");
            args.add(status);
        }
        long total = num("select count(*) from rectify_task r " + where, args.toArray());
        int p = Math.max(page, 1);
        int s = Math.max(size, 1);
        return PageResult.of(total, p, s, rows(
                "select r.*, (select count(*) from rectify_accept a where a.task_id = r.id and a.deleted=0) "
                        + "as accept_count from rectify_task r " + where
                        + " order by r.create_time desc limit ? offset ?", with(args, s, (p - 1) * s)));
    }

    /** 一键把聚类主题转整改任务 */
    @Transactional
    public Map<String, Object> createRectify(Map<String, Object> body) {
        String title = str(body.get("title"));
        if (title == null || title.isBlank()) {
            throw new BizException("整改主题不能为空");
        }
        long deptId = longOr(body.get("deptId"), 4);
        Map<String, Object> d = one("select org_name from sys_org where id = ?", deptId);
        String no = "RC" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%03d", num("select count(*) from rectify_task") + 1);
        jdbc.update("""
                insert into rectify_task(task_no, title, cluster_name, dept_id, dept_name, measure, deadline,
                  owner_id, owner_name, accept_id, accept_name, status, source_tickets, create_by, create_time,
                  update_time)
                values(?,?,?,?,?,?,?,?,?,?,?,'待下发',?,?,now(),now())
                """, no, title, str(body.getOrDefault("clusterName", "")), deptId,
                d == null ? "售后服务一部" : d.get("orgName"),
                str(body.getOrDefault("measure", "复核该组合全部工单，梳理共性成因并形成标准作业指引。")),
                str(body.getOrDefault("deadline", LocalDateTime.now().plusDays(15).toLocalDate().toString())),
                longOr(body.get("ownerId"), 7), str(body.getOrDefault("ownerName", "何东")),
                num("select ifnull(max(id),0) from sys_user where username='qm01'"), "金雅",
                str(body.getOrDefault("sourceTickets", "")), UserContext.userId());
        return Map.of("taskNo", no);
    }

    @Transactional
    public void acceptRectify(Long taskId, boolean pass, String opinion) {
        if (opinion == null || opinion.trim().length() < 5) {
            throw new BizException("验收意见不少于 5 个字");
        }
        jdbc.update("insert into rectify_accept(task_id, accept_result, evidence_path, opinion, acceptor_id, "
                        + "acceptor_name, accept_time, create_time, update_time) values(?,?,?,?,?,?,now(),now(),now())",
                taskId, pass ? "通过" : "不通过", "/uploads/seed/evidence/03.jpg", opinion,
                UserContext.userId(), UserContext.realName());
        jdbc.update("update rectify_task set status = ?, finish_time = if(? = '已验收', now(), finish_time), "
                + "update_time = now() where id = ?", pass ? "已验收" : "整改中",
                pass ? "已验收" : "整改中", taskId);
    }

    public PageResult<Map<String, Object>> knowledgePage(String keyword, String category, int page, int size) {
        StringBuilder where = new StringBuilder(" where k.deleted=0 ");
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            where.append(" and (k.title like ? or k.keywords like ?) ");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
        }
        if (category != null && !category.isBlank()) {
            where.append(" and k.category_name = ? ");
            args.add(category);
        }
        long total = num("select count(*) from knowledge_item k " + where, args.toArray());
        int p = Math.max(page, 1);
        int s = Math.max(size, 1);
        return PageResult.of(total, p, s, rows(
                "select k.* from knowledge_item k " + where + " order by k.publish_time desc limit ? offset ?",
                with(args, s, (p - 1) * s)));
    }

    @Transactional
    public void saveKnowledge(Map<String, Object> body) {
        String title = str(body.get("title"));
        if (title == null || title.isBlank()) {
            throw new BizException("标题不能为空");
        }
        long id = longOr(body.get("id"), 0);
        if (id == 0) {
            String no = "KB" + String.format("%04d", num("select count(*) from knowledge_item") + 1);
            jdbc.update("insert into knowledge_item(item_no, title, category_name, keywords, content, "
                            + "author_id, author_name, view_count, status, publish_time, create_time, update_time) "
                            + "values(?,?,?,?,?,?,?,0,?,now(),now(),now())",
                    no, title, str(body.getOrDefault("categoryName", "售后服务")),
                    str(body.getOrDefault("keywords", "")), str(body.getOrDefault("content", "")),
                    UserContext.userId(), UserContext.realName(),
                    str(body.getOrDefault("status", "启用")));
        } else {
            jdbc.update("update knowledge_item set title=?, category_name=?, keywords=?, content=?, status=?, "
                            + "update_time=now() where id=?",
                    title, str(body.get("categoryName")), str(body.get("keywords")),
                    str(body.get("content")), str(body.getOrDefault("status", "启用")), id);
        }
    }

    // ================================================================= 档案 / 系统 / 门户

    public PageResult<Map<String, Object>> customerPage(String keyword, String level, int page, int size) {
        StringBuilder where = new StringBuilder(" where c.deleted=0 ");
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            where.append(" and (c.customer_name like ? or c.phone like ? or c.customer_no like ?) ");
            String k = "%" + keyword.trim() + "%";
            args.add(k);
            args.add(k);
            args.add(k);
        }
        if (level != null && !level.isBlank()) {
            where.append(" and c.customer_level = ? ");
            args.add(level);
        }
        long total = num("select count(*) from customer c " + where, args.toArray());
        int p = Math.max(page, 1);
        int s = Math.max(size, 1);
        return PageResult.of(total, p, s, rows(
                "select c.*, (select count(*) from complaint_ticket t where t.customer_id = c.id and t.deleted=0) "
                        + "as ticket_count from customer c " + where
                        + " order by c.id limit ? offset ?", with(args, s, (p - 1) * s)));
    }

    public PageResult<Map<String, Object>> objectPage(String keyword, String type, int page, int size) {
        StringBuilder where = new StringBuilder(" where o.deleted=0 ");
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            where.append(" and (o.object_name like ? or o.object_no like ?) ");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
        }
        if (type != null && !type.isBlank()) {
            where.append(" and o.object_type = ? ");
            args.add(type);
        }
        long total = num("select count(*) from service_object o " + where, args.toArray());
        int p = Math.max(page, 1);
        int s = Math.max(size, 1);
        return PageResult.of(total, p, s, rows(
                "select o.*, (select count(*) from complaint_ticket t where t.object_id = o.id and t.deleted=0) "
                        + "as ticket_count from service_object o " + where
                        + " order by o.id limit ? offset ?", with(args, s, (p - 1) * s)));
    }

    public PageResult<Map<String, Object>> userPage(String keyword, int page, int size) {
        StringBuilder where = new StringBuilder(" where u.deleted=0 ");
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            where.append(" and (u.username like ? or u.real_name like ?) ");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
        }
        long total = num("select count(*) from sys_user u " + where, args.toArray());
        int p = Math.max(page, 1);
        int s = Math.max(size, 1);
        return PageResult.of(total, p, s, rows(
                "select u.id, u.username, u.real_name, u.phone, u.avatar, u.status, u.data_scope, u.org_id, "
                        + "u.last_login_time, r.role_name, r.role_code, "
                        + "(select org_name from sys_org o where o.id = u.org_id) as org_name "
                        + "from sys_user u left join sys_role r on r.id = u.role_id " + where
                        + " order by u.id limit ? offset ?", with(args, s, (p - 1) * s)));
    }

    /** 下拉选项一次取全，前端不用为每个下拉单独请求 */
    public Map<String, Object> options() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("categories", rows("select id, category_name, problem_type, service_stage, default_level, "
                + "response_hours, handle_hours from complaint_category where deleted=0 and status='启用' order by id"));
        m.put("levels", rows("select level_code, level_name, color_class from severity_rule where deleted=0 order by id"));
        m.put("depts", rows("select id, org_name from sys_org where deleted=0 and org_type='部门' order by id"));
        m.put("channels", List.of("电话", "门店", "邮件", "在线", "微信"));
        m.put("statuses", FLOW);
        m.put("customers", rows("select id, customer_name, phone from customer where deleted=0 order by id limit 200"));
        m.put("objects", rows("select id, object_name, object_type from service_object where deleted=0 order by id limit 200"));
        m.put("handlers", rows("select u.id, u.real_name, u.org_id, o.org_name from sys_user u "
                + "left join sys_org o on o.id = u.org_id where u.deleted=0 and u.role_id = 3 order by u.id"));
        m.put("slaRules", rows("select * from sla_rule where deleted=0 order by id"));
        m.put("severityRules", rows("select * from severity_rule where deleted=0 order by id"));
        return m;
    }

    /** 客户门户：只返回本人投诉（数据范围在 scope() 内强制） */
    public PageResult<Map<String, Object>> myTickets(String status, int page, int size) {
        List<Object> args = new ArrayList<>();
        String sc = scope("t", args);
        StringBuilder where = new StringBuilder(" where t.deleted=0 and ").append(sc);
        if (status != null && !status.isBlank()) {
            where.append(" and t.status = ? ");
            args.add(status);
        }
        long total = num("select count(*) from complaint_ticket t " + where, args.toArray());
        int p = Math.max(page, 1);
        int s = Math.max(size, 1);
        return PageResult.of(total, p, s, rows(
                "select t.* from complaint_ticket t " + where
                        + " order by t.create_time desc limit ? offset ?", with(args, s, (p - 1) * s)));
    }

    /** 客户补充证据：只写附件表，不直接改主档状态 */
    @Transactional
    public void addAttachment(Long ticketId, String path, String fileName, String bizType) {
        jdbc.update("insert into complaint_ticket_attachment(ticket_id, order_id, file_name, file_path, file_size, "
                        + "file_type, biz_type, uploader_id, create_time, update_time) "
                        + "values(?,?,?,?,?,?,?,?,now(),now())",
                ticketId, null, fileName == null ? "附件.jpg" : fileName, path,
                180000L, "image/jpeg", bizType == null ? "客户证据" : bizType, UserContext.userId());
    }

    /** 导出当前筛选结果：返回行数据与导出任务信息（前端生成 CSV） */
    public Map<String, Object> exportTickets(String keyword, String status, String level) {
        List<Object> args = new ArrayList<>();
        String sc = scope("t", args);
        StringBuilder where = new StringBuilder(" where t.deleted=0 and ").append(sc);
        if (keyword != null && !keyword.isBlank()) {
            where.append(" and (t.ticket_no like ? or t.title like ?) ");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
        }
        if (status != null && !status.isBlank()) {
            where.append(" and t.status = ? ");
            args.add(status);
        }
        if (level != null && !level.isBlank()) {
            where.append(" and t.level_code = ? ");
            args.add(level);
        }
        List<Map<String, Object>> list = rows("select t.ticket_no, t.customer_name, t.channel, t.category_name, "
                + "t.level_code, t.dept_name, t.status, t.overdue_flag, t.create_time, t.deadline_time, t.title "
                + "from complaint_ticket t " + where + " order by t.create_time desc limit 2000", args.toArray());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("fileName", "客诉台账_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm")) + ".csv");
        m.put("operator", UserContext.realName());
        m.put("count", list.size());
        m.put("finishedAt", now());
        m.put("records", list);
        return m;
    }
}
