package com.grad.csc.controller;

import com.grad.csc.common.BizException;
import com.grad.csc.common.PageResult;
import com.grad.csc.common.R;
import com.grad.csc.common.UserContext;
import com.grad.csc.service.BizService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 客诉业务接口。
 * 路径与需求文档「REST 接口建议」一致：主档 /api/complaint-ticket，工单 /api/order。
 * 所有写接口都在 Service 层做状态与权限校验，Controller 只负责参数透传。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BizController {

    private final BizService biz;

    // ---------------------------------------------------------------- 看板
    @GetMapping("/dashboard")
    public R<Map<String, Object>> dashboard(@RequestParam(defaultValue = "30") int period) {
        return R.ok(biz.dashboard(period));
    }

    @GetMapping("/options")
    public R<Map<String, Object>> options() {
        return R.ok(biz.options());
    }

    // ---------------------------------------------------------------- 投诉主档
    @GetMapping("/complaint-ticket/page")
    public R<PageResult<Map<String, Object>>> ticketPage(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) Integer overdue,
            @RequestParam(required = false) String quick,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return R.ok(biz.ticketPage(keyword, channel, categoryId, level, status, deptId,
                dateFrom, dateTo, overdue, quick, page, size));
    }

    @GetMapping("/complaint-ticket/{id}")
    public R<Map<String, Object>> ticketDetail(@PathVariable Long id) {
        return R.ok(biz.ticketDetail(id));
    }

    @PostMapping("/complaint-ticket")
    public R<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return R.ok(biz.createTicket(body));
    }

    @PutMapping("/complaint-ticket/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        biz.updateTicket(id, body);
        return R.ok();
    }

    /** 状态流转：目标状态 + 原因 + 可选处理人/部门 */
    @PostMapping("/complaint-ticket/{id}/transition")
    public R<Map<String, Object>> transition(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long handlerId = body.get("handlerId") == null ? null : Long.valueOf(String.valueOf(body.get("handlerId")));
        Long deptId = body.get("deptId") == null ? null : Long.valueOf(String.valueOf(body.get("deptId")));
        return R.ok(biz.transition(id, (String) body.get("toStatus"),
                (String) body.get("reason"), handlerId, deptId));
    }

    /** 创新点：执行共性问题聚类计算 */
    @PostMapping("/complaint-ticket/{id}/evaluate")
    public R<Map<String, Object>> evaluate(@PathVariable Long id) {
        return R.ok(biz.evaluate(id));
    }

    /** 客户补充证据（附件走上传组件，这里只登记相对路径） */
    @PostMapping("/complaint-ticket/{id}/attachment")
    public R<Void> addAttachment(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        biz.addAttachment(id, (String) body.get("path"), (String) body.get("fileName"),
                (String) body.get("bizType"));
        return R.ok();
    }

    @GetMapping("/complaint-ticket/export")
    public R<Map<String, Object>> export(@RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(required = false) String level) {
        return R.ok(biz.exportTickets(keyword, status, level));
    }

    // ---------------------------------------------------------------- 工单
    @GetMapping("/order/desk")
    public R<Map<String, Object>> orderDesk() {
        return R.ok(biz.orderDesk());
    }

    @GetMapping("/handle/desk")
    public R<Map<String, Object>> handleDesk() {
        return R.ok(biz.handleDesk());
    }

    @PostMapping("/handle/submit")
    public R<Map<String, Object>> submitHandle(@RequestBody Map<String, Object> body) {
        return R.ok(biz.submitHandle(body));
    }

    @PostMapping("/order/{ticketId}/accept")
    public R<Void> accept(@PathVariable Long ticketId) {
        biz.acceptOrder(ticketId);
        return R.ok();
    }

    // ---------------------------------------------------------------- 审核 / 确认 / 回访 / 重开
    @GetMapping("/review/page")
    public R<PageResult<Map<String, Object>>> reviewPage(@RequestParam(required = false) String result,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size) {
        return R.ok(biz.reviewPage(result, page, size));
    }

    @PostMapping("/review/{ticketId}")
    public R<Void> review(@PathVariable Long ticketId, @RequestBody Map<String, Object> body) {
        boolean pass = Boolean.TRUE.equals(body.get("pass")) || "true".equals(String.valueOf(body.get("pass")));
        Integer score = body.get("planScore") == null ? null : Integer.valueOf(String.valueOf(body.get("planScore")));
        biz.review(ticketId, pass, (String) body.get("opinion"), score);
        return R.ok();
    }

    @GetMapping("/confirm/page")
    public R<PageResult<Map<String, Object>>> confirmPage(@RequestParam(required = false) String result,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int size) {
        return R.ok(biz.confirmPage(result, page, size));
    }

    @PostMapping("/confirm/{ticketId}")
    public R<Void> confirm(@PathVariable Long ticketId, @RequestBody Map<String, Object> body) {
        boolean accept = !"false".equals(String.valueOf(body.get("accept")));
        biz.confirm(ticketId, accept, (String) body.get("opinion"));
        return R.ok();
    }

    @GetMapping("/revisit/page")
    public R<PageResult<Map<String, Object>>> revisitPage(@RequestParam(required = false) String solved,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int size) {
        return R.ok(biz.revisitPage(solved, page, size));
    }

    @PostMapping("/revisit")
    public R<Void> revisit(@RequestBody Map<String, Object> body) {
        biz.revisit(body);
        return R.ok();
    }

    @GetMapping("/reopen/page")
    public R<PageResult<Map<String, Object>>> reopenPage(@RequestParam(required = false) String status,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size) {
        return R.ok(biz.reopenPage(status, page, size));
    }

    @PostMapping("/reopen/{id}")
    public R<Void> handleReopen(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean pass = !"false".equals(String.valueOf(body.get("pass")));
        biz.handleReopen(id, pass, (String) body.get("note"));
        return R.ok();
    }

    // ---------------------------------------------------------------- 预警 / 延时
    @GetMapping("/warning/page")
    public R<PageResult<Map<String, Object>>> warningPage(@RequestParam(required = false) String type,
                                                          @RequestParam(required = false) String status,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int size) {
        return R.ok(biz.warningPage(type, status, page, size));
    }

    @PostMapping("/warning/{id}/handle")
    public R<Void> handleWarning(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        biz.handleWarning(id, (String) body.get("note"));
        return R.ok();
    }

    @PostMapping("/warning/scan")
    public R<Map<String, Object>> scanWarning() {
        return R.ok(biz.scanWarning());
    }

    @GetMapping("/delay/page")
    public R<PageResult<Map<String, Object>>> delayPage(@RequestParam(required = false) String status,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return R.ok(biz.delayPage(status, page, size));
    }

    @PostMapping("/delay/{id}/approve")
    public R<Void> approveDelay(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean pass = !"false".equals(String.valueOf(body.get("pass")));
        biz.approveDelay(id, pass, (String) body.get("note"));
        return R.ok();
    }

    @PostMapping("/delay")
    public R<Map<String, Object>> applyDelay(@RequestBody Map<String, Object> body) {
        Long ticketId = Long.valueOf(String.valueOf(body.get("ticketId")));
        int hours = body.get("hours") == null ? 24 : Integer.parseInt(String.valueOf(body.get("hours")));
        return R.ok(biz.applyDelay(ticketId, hours, (String) body.get("reason")));
    }

    // ---------------------------------------------------------------- 创新模块
    @GetMapping("/cluster/page")
    public R<PageResult<Map<String, Object>>> clusterPage(@RequestParam(required = false) String level,
                                                          @RequestParam(required = false) String keyword,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int size) {
        return R.ok(biz.clusterPage(level, keyword, page, size));
    }

    @GetMapping("/cluster/summary")
    public R<List<Map<String, Object>>> clusterSummary() {
        return R.ok(biz.clusterSummary());
    }

    @PostMapping("/cluster/{scoreId}/manual")
    public R<Void> manual(@PathVariable Long scoreId, @RequestBody Map<String, Object> body) {
        biz.manualAdjust(scoreId, (String) body.get("manualLevel"), (String) body.get("reason"));
        return R.ok();
    }

    @GetMapping("/cluster/rule")
    public R<Map<String, Object>> rule() {
        return R.ok(Map.of("active", biz.activeRule(), "versions", biz.ruleList()));
    }

    @PutMapping("/cluster/rule")
    public R<Map<String, Object>> saveRule(@RequestBody Map<String, Object> body) {
        return R.ok(biz.saveRule(body));
    }

    // ---------------------------------------------------------------- 整改 / 知识库
    @GetMapping("/rectify/page")
    public R<PageResult<Map<String, Object>>> rectifyPage(@RequestParam(required = false) String status,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int size) {
        return R.ok(biz.rectifyPage(status, page, size));
    }

    @PostMapping("/rectify")
    public R<Map<String, Object>> createRectify(@RequestBody Map<String, Object> body) {
        return R.ok(biz.createRectify(body));
    }

    @PostMapping("/rectify/{id}/accept")
    public R<Void> acceptRectify(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean pass = !"false".equals(String.valueOf(body.get("pass")));
        biz.acceptRectify(id, pass, (String) body.get("opinion"));
        return R.ok();
    }

    @GetMapping("/knowledge/page")
    public R<PageResult<Map<String, Object>>> knowledgePage(@RequestParam(required = false) String keyword,
                                                            @RequestParam(required = false) String category,
                                                            @RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        return R.ok(biz.knowledgePage(keyword, category, page, size));
    }

    @PostMapping("/knowledge")
    public R<Void> saveKnowledge(@RequestBody Map<String, Object> body) {
        biz.saveKnowledge(body);
        return R.ok();
    }

    // ---------------------------------------------------------------- 档案 / 系统 / 门户
    @GetMapping("/archive/customer/page")
    public R<PageResult<Map<String, Object>>> customerPage(@RequestParam(required = false) String keyword,
                                                           @RequestParam(required = false) String level,
                                                           @RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        return R.ok(biz.customerPage(keyword, level, page, size));
    }

    @GetMapping("/archive/object/page")
    public R<PageResult<Map<String, Object>>> objectPage(@RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) String type,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size) {
        return R.ok(biz.objectPage(keyword, type, page, size));
    }

    @GetMapping("/system/user/page")
    public R<PageResult<Map<String, Object>>> userPage(@RequestParam(required = false) String keyword,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        return R.ok(biz.userPage(keyword, page, size));
    }

    /** 客户门户：本人投诉列表 */
    @GetMapping("/portal/my")
    public R<PageResult<Map<String, Object>>> myTickets(@RequestParam(required = false) String status,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        if (UserContext.userId() == null) {
            throw new BizException(401, "登录状态已失效");
        }
        return R.ok(biz.myTickets(status, page, size));
    }
}
