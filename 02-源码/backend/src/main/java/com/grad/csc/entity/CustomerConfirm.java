package com.grad.csc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户确认记录
 */
@Data
@TableName("customer_confirm")
public class CustomerConfirm {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 投诉主档 ID */
    private Long ticketId;

    /** 工单 ID */
    private Long orderId;

    /** 结果 接受/提出异议/超期未确认 */
    private String confirmResult;

    /** 客户意见 */
    private String opinion;

    /** 确认时间 */
    private LocalDateTime confirmTime;

    /** 确认截止时间 */
    private LocalDateTime deadlineTime;

    /** 是否系统自动判定 1 是 0 否 */
    private Integer autoFlag;

    /** 提醒次数 */
    private Integer remindCount;

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
