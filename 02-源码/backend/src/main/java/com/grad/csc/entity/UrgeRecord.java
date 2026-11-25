package com.grad.csc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 催办记录
 */
@Data
@TableName("urge_record")
public class UrgeRecord {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 投诉主档 ID */
    private Long ticketId;

    /** 工单 ID */
    private Long orderId;

    /** 类型 客服催办/组长催办/系统自动 */
    private String urgeType;

    /** 催办内容 */
    private String urgeContent;

    /** 催办人 */
    private String urgeBy;

    /** 被催办人 */
    private String urgeTo;

    /** 催办时间 */
    private LocalDateTime urgeTime;

    /** 反馈内容 */
    private String feedback;

    /** 反馈时间 */
    private LocalDateTime feedbackTime;

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
