package com.grad.csc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户
 */
@Data
@TableName("sys_user")
public class SysUser {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号 */
    private String username;

    /** BCrypt 密码 */
    private String password;

    /** 姓名 */
    private String realName;

    /** 手机号 */
    private String phone;

    /** 头像路径 */
    private String avatar;

    /** 角色 ID */
    private Long roleId;

    /** 所属部门 */
    private Long orgId;

    /** 数据范围 ALL/ORG/SELF */
    private String dataScope;

    /** 状态 1 正常 0 停用 */
    private Integer status;

    /** 连续登录失败次数 */
    private Integer failCount;

    /** 锁定到期时间 */
    private LocalDateTime lockTime;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除 0 否 1 是 */
    @TableLogic
    private Integer deleted;
}
