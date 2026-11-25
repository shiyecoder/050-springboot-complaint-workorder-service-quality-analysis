package com.grad.csc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 共性问题聚类规则
 */
@Data
@TableName("complaint_ticket_rule")
public class ComplaintTicketRule {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 规则名称 */
    private String ruleName;

    /** 规则版本 v1/v2/v3 */
    private String ruleVersion;

    /** 关联产品权重 */
    private Integer wProduct;

    /** 服务环节权重 */
    private Integer wStage;

    /** 问题类型权重 */
    private Integer wProblem;

    /** 原因标签权重 */
    private Integer wReason;

    /** 责任部门权重 */
    private Integer wDept;

    /** 近30天次数权重 */
    private Integer wFreq;

    /** 高相似阈值 */
    private Integer highThreshold;

    /** 中相似阈值 */
    private Integer midThreshold;

    /** 统计窗口（天） */
    private Integer windowDays;

    /** 生效时间 */
    private LocalDateTime effectiveTime;

    /** 状态 启用/停用 */
    private String status;

    /** 配置人 */
    private String operatorName;

    /** 变更说明 */
    private String remark;

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
