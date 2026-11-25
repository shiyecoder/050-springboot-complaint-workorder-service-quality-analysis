package com.grad.csc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 协办任务
 */
@Data
@TableName("coop_task")
public class CoopTask {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 投诉主档 ID */
    private Long ticketId;

    /** 主工单 ID */
    private Long orderId;

    /** 协办部门 */
    private Long deptId;

    /** 协办部门名称 */
    private String deptName;

    /** 协办责任人 */
    private Long handlerId;

    /** 协办人姓名 */
    private String handlerName;

    /** 协办任务内容 */
    private String taskContent;

    /** 协办截止时间 */
    private LocalDateTime deadlineTime;

    /** 状态 待接单/处理中/已完成/已退回 */
    private String status;

    /** 协办结果 */
    private String result;

    /** 完成时间 */
    private LocalDateTime finishTime;

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
