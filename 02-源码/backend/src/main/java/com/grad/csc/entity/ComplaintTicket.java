package com.grad.csc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户投诉主档
 */
@Data
@TableName("complaint_ticket")
public class ComplaintTicket {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 客诉编号 TS+日期+序号 */
    private String ticketNo;

    /** 客户 ID */
    private Long customerId;

    /** 客户姓名（冗余） */
    private String customerName;

    /** 联系手机号 */
    private String phone;

    /** 关联服务对象 */
    private Long objectId;

    /** 关联对象名称（冗余） */
    private String objectName;

    /** 投诉渠道 电话/门店/邮件/在线/微信 */
    private String channel;

    /** 问题分类 */
    private Long categoryId;

    /** 分类名称（冗余） */
    private String categoryName;

    /** 问题类型 */
    private String problemType;

    /** 服务环节 */
    private String serviceStage;

    /** 严重等级 P1-P4 */
    private String levelCode;

    /** 投诉主题 */
    private String title;

    /** 投诉内容 */
    private String content;

    /** 客户诉求 */
    private String demand;

    /** 发生时间 */
    private LocalDateTime occurTime;

    /** 发生地点 */
    private String occurPlace;

    /** 责任部门 */
    private Long deptId;

    /** 责任部门名称 */
    private String deptName;

    /** 工单状态（主流程） */
    private String status;

    /** 当前责任人 */
    private Long currentHandlerId;

    /** 当前责任人姓名 */
    private String currentHandlerName;

    /** 受理时间 */
    private LocalDateTime acceptTime;

    /** 响应截止时间 */
    private LocalDateTime responseDeadline;

    /** 处理截止时间 */
    private LocalDateTime deadlineTime;

    /** 处理完成时间 */
    private LocalDateTime finishTime;

    /** 客户确认时间 */
    private LocalDateTime confirmTime;

    /** 归档时间 */
    private LocalDateTime archiveTime;

    /** 是否超时 1 是 0 否 */
    private Integer overdueFlag;

    /** 是否无效工单 1 是 0 否 */
    private Integer invalidFlag;

    /** 是否重复投诉 1 是 0 否 */
    private Integer duplicateFlag;

    /** 重复时关联的主工单 ID */
    private Long mainTicketId;

    /** 来源 客服代录/客户自助 */
    private String source;

    /** 投诉封面证据图路径 */
    private String coverImage;

    /** 原因标签（创新因子，逗号分隔） */
    private String reasonTags;

    /** 关联产品标签（创新因子） */
    private String productTag;

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
