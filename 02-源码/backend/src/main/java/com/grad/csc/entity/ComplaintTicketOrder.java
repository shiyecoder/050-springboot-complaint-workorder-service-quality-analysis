package com.grad.csc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客诉处理工单
 */
@Data
@TableName("complaint_ticket_order")
public class ComplaintTicketOrder {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单编号 WO+日期+序号 */
    private String orderNo;

    /** 所属投诉主档 */
    private Long ticketId;

    /** 客诉编号（冗余） */
    private String ticketNo;

    /** 当前节点名称 */
    private String nodeName;

    /** 工单状态 */
    private String status;

    /** 处理人 ID */
    private Long handlerId;

    /** 处理人姓名 */
    private String handlerName;

    /** 责任部门 */
    private Long deptId;

    /** 责任部门名称 */
    private String deptName;

    /** 优先级 高/中/低 */
    private String priority;

    /** 分派时间 */
    private LocalDateTime dispatchTime;

    /** 接单时间 */
    private LocalDateTime acceptTime;

    /** 计划完成时间 */
    private LocalDateTime planFinishTime;

    /** 实际完成时间 */
    private LocalDateTime actualFinishTime;

    /** 处理截止时间 */
    private LocalDateTime deadlineTime;

    /** 解决方案 */
    private String solution;

    /** 处理结果 */
    private String result;

    /** 退回原因 */
    private String returnReason;

    /** 是否超时 1 是 0 否 */
    private Integer overdueFlag;

    /** 是否含跨部门协办 1 是 0 否 */
    private Integer coopFlag;

    /** 工单关闭时间 */
    private LocalDateTime closeTime;

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
