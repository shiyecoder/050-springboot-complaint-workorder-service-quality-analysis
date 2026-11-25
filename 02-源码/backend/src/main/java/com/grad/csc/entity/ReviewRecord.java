package com.grad.csc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 结果审核记录
 */
@Data
@TableName("review_record")
public class ReviewRecord {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 审核单号 RV+序号 */
    private String reviewNo;

    /** 投诉主档 ID */
    private Long ticketId;

    /** 工单 ID */
    private Long orderId;

    /** 审核结果 通过/驳回 */
    private String reviewResult;

    /** 方案完整度评分 0-100 */
    private Integer planScore;

    /** 证据是否齐全 1 是 0 否 */
    private Integer evidenceFlag;

    /** 责任认定核查结论 */
    private String dutyCheck;

    /** 审核意见 */
    private String opinion;

    /** 审核人 */
    private Long reviewerId;

    /** 审核人姓名 */
    private String reviewerName;

    /** 审核时间 */
    private LocalDateTime reviewTime;

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
