package com.grad.csc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户
 */
@Data
@TableName("customer")
public class Customer {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 客户编号 */
    private String customerNo;

    /** 客户姓名 */
    private String customerName;

    /** 联系手机号 */
    private String phone;

    /** 客户类型 个人/企业/渠道客户 */
    private String customerType;

    /** 所属区域 */
    private String region;

    /** 客户等级 普通/重要/VIP */
    private String customerLevel;

    /** 电子邮箱 */
    private String email;

    /** 联系地址 */
    private String address;

    /** 服务历史摘要 */
    private String serviceHistory;

    /** 客户头像路径 */
    private String avatar;

    /** 关联登录账号 */
    private Long userId;

    /** 状态 正常/停用 */
    private String status;

    /** 创建人 */
    private Long createBy;

    /** 版本号（并发校验） */
    private Integer version;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除 0 否 1 是 */
    @TableLogic
    private Integer deleted;
}
