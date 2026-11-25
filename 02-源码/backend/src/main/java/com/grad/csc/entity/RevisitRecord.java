package com.grad.csc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 回访记录
 */
@Data
@TableName("revisit_record")
public class RevisitRecord {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 回访单号 VZ+序号 */
    private String revisitNo;

    /** 投诉主档 ID */
    private Long ticketId;

    /** 工单 ID */
    private Long orderId;

    /** 回访方式 电话/在线/现场 */
    private String revisitType;

    /** 问题是否解决 1 是 0 否 */
    private Integer solvedFlag;

    /** 满意度 1-5 */
    private Integer satisfaction;

    /** 客户意见 */
    private String opinion;

    /** 后续行动 */
    private String nextAction;

    /** 回访人 */
    private Long revisitorId;

    /** 回访人姓名 */
    private String revisitorName;

    /** 回访时间 */
    private LocalDateTime revisitTime;

    /** 通话时长（分钟） */
    private Integer durationMin;

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
