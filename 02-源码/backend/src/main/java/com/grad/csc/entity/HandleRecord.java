package com.grad.csc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 处理记录
 */
@Data
@TableName("handle_record")
public class HandleRecord {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 投诉主档 ID */
    private Long ticketId;

    /** 工单 ID */
    private Long orderId;

    /** 处理步骤序号 */
    private Integer stepNo;

    /** 调查情况与内部意见 */
    private String content;

    /** 解决方案 */
    private String solution;

    /** 处理附件路径 */
    private String attachPath;

    /** 计划完成时间 */
    private LocalDateTime planFinishTime;

    /** 记录人 */
    private Long operatorId;

    /** 记录人姓名 */
    private String operatorName;

    /** 记录时间 */
    private LocalDateTime handleTime;

    /** 是否内部可见 1 是 0 否 */
    private Integer internalFlag;

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
