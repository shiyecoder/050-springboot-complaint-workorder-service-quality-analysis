package com.grad.csc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 时限预警记录
 */
@Data
@TableName("warning_record")
public class WarningRecord {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 投诉主档 ID */
    private Long ticketId;

    /** 工单编号 */
    private String orderNo;

    /** 类型 即将超时/响应超时/处理超时/长期未更新 */
    private String warnType;

    /** 预警级别 提醒/严重 */
    private String warnLevel;

    /** 预警时间 */
    private LocalDateTime warnTime;

    /** 对应截止时间 */
    private LocalDateTime deadlineTime;

    /** 距截止的分钟数（负数为已超时） */
    private Integer gapMinutes;

    /** 状态 未处理/已处理/已忽略 */
    private String status;

    /** 处理人 */
    private Long handlerId;

    /** 处理时间 */
    private LocalDateTime handleTime;

    /** 处理说明 */
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
