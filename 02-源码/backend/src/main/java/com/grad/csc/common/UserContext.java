package com.grad.csc.common;

/**
 * 当前登录用户上下文。由 JWT 过滤器写入，Controller 只读。
 * 使用 ThreadLocal 保证同一请求线程内可取到角色与数据范围。
 */
public class UserContext {

    /**
     * dataScope 取值：ALL=全部数据、ORG=本部门（站点）、SELF=本人负责的数据。
     * 需求文档：驿站员工仅访问所属站点，快递员只查看本人交接批次，收件人只看本人快件。
     */
    public record CurrentUser(Long userId, String username, String realName, String roleCode,
                              Long orgId, String dataScope) {
    }

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static Long userId() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.userId();
    }

    public static String realName() {
        CurrentUser u = HOLDER.get();
        return u == null ? "" : u.realName();
    }

    public static String roleCode() {
        CurrentUser u = HOLDER.get();
        return u == null ? "" : u.roleCode();
    }

    public static Long orgId() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.orgId();
    }

    public static String dataScope() {
        CurrentUser u = HOLDER.get();
        return u == null ? "SELF" : u.dataScope();
    }

    /** 驿站员工：办理入库、上架、取件核验、退件、移库和异常登记 */
    public static boolean isStaff() {
        return "STAFF".equals(roleCode());
    }

    /** 快递员/交接人员：只看本人交接批次，确认退件接收 */
    public static boolean isCourier() {
        return "COURIER".equals(roleCode());
    }

    /** 收件人：查询本人快件、提交代取授权与服务反馈 */
    public static boolean isCustomer() {
        return "CUSTOMER".equals(roleCode());
    }

    /** 站点负责人：维护站点、货架、员工、预警规则、盘点和运营统计 */
    public static boolean isManager() {
        return "MANAGER".equals(roleCode());
    }

    /** 系统管理员拥有全量数据范围 */
    public static boolean isAdmin() {
        return "ADMIN".equals(roleCode());
    }

    public static void clear() {
        HOLDER.remove();
    }
}
