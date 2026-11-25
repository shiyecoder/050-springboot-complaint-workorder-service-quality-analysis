package com.grad.csc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 延时申请
 */
@Data
@TableName("delay_apply")
public class DelayApply {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请单号 DL+日期+序号 */
    private String applyNo;

    /** 投诉主档 ID */
    private Long ticketId;

    /** 工单 ID */
    private Long orderId;

    /** 原截止时间 */
    private LocalDateTime oldDeadline;

    /** 申请新截止时间 */
    private LocalDateTime newDeadline;

    /** 延长小时数 */
    private Integer delayHours;

    /** 延时原因（不少于 5 字） */
    private String reason;

    /** 申请人 */
    private Long applicantId;

    /** 申请人姓名 */
    private String applicantName;

    /** 申请时间 */
    private LocalDateTime applyTime;

    /** 状态 待审批/已通过/已驳回 */
    private String status;

    /** 审批人 */
    private Long approverId;

    /** 审批人姓名 */
    private String approverName;

    /** 审批时间 */
    private LocalDateTime approveTime;

    /** 审批意见 */
    private String approveNote;

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
