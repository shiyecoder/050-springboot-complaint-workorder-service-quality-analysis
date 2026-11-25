package com.grad.csc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 投诉附件
 */
@Data
@TableName("complaint_ticket_attachment")
public class ComplaintTicketAttachment {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 投诉主档 ID */
    private Long ticketId;

    /** 工单 ID */
    private Long orderId;

    /** 原始文件名 */
    private String fileName;

    /** 相对路径 /uploads/... */
    private String filePath;

    /** 文件大小（字节） */
    private Long fileSize;

    /** MIME 类型 */
    private String fileType;

    /** 用途 客户证据/处理凭证/整改凭证 */
    private String bizType;

    /** 上传人 */
    private Long uploaderId;

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
