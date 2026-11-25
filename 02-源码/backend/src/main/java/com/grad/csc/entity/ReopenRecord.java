package com.grad.csc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 重开记录
 */
@Data
@TableName("reopen_record")
public class ReopenRecord {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 重开单号 RO+序号 */
    private String reopenNo;

    /** 投诉主档 ID */
    private Long ticketId;

    /** 原工单 ID */
    private Long sourceOrderId;

    /** 新工单 ID */
    private Long newOrderId;

    /** 重开原因（不少于 5 字） */
    private String reopenReason;

    /** 第几次重开 */
    private Integer reopenCount;

    /** 申请人 */
    private String applicant;

    /** 申请时间 */
    private LocalDateTime applyTime;

    /** 状态 待受理/已受理/已驳回 */
    private String status;

    /** 受理人 */
    private Long handlerId;

    /** 受理时间 */
    private LocalDateTime handleTime;

    /** 受理说明 */
    private String handleNote;

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
