package com.grad.csc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户评价
 */
@Data
@TableName("evaluate_record")
public class EvaluateRecord {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 投诉主档 ID */
    private Long ticketId;

    /** 工单 ID */
    private Long orderId;

    /** 综合评分 1-5 */
    private Integer score;

    /** 服务态度评分 1-5 */
    private Integer attitudeScore;

    /** 处理速度评分 1-5 */
    private Integer speedScore;

    /** 处理质量评分 1-5 */
    private Integer qualityScore;

    /** 评价内容 */
    private String content;

    /** 是否匿名 1 是 0 否 */
    private Integer anonymousFlag;

    /** 客户 ID */
    private Long customerId;

    /** 评价时间 */
    private LocalDateTime evaluateTime;

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
