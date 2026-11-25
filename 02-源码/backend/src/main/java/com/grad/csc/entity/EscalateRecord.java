package com.grad.csc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 升级记录
 */
@Data
@TableName("escalate_record")
public class EscalateRecord {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 投诉主档 ID */
    private Long ticketId;

    /** 工单 ID */
    private Long orderId;

    /** 原等级 */
    private String fromLevel;

    /** 升级后等级 */
    private String toLevel;

    /** 类型 超时升级/人工升级/重大投诉 */
    private String escalateType;

    /** 升级原因 */
    private String reason;

    /** 发起人 */
    private String escalateBy;

    /** 升级接收人 */
    private String escalateTo;

    /** 升级时间 */
    private LocalDateTime escalateTime;

    /** 确认时间 */
    private LocalDateTime confirmTime;

    /** 状态 待确认/已确认 */
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
