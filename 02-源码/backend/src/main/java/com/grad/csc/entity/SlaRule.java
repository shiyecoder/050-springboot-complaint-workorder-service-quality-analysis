package com.grad.csc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SLA 规则
 */
@Data
@TableName("sla_rule")
public class SlaRule {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 规则名称 */
    private String ruleName;

    /** 适用分类 */
    private Long categoryId;

    /** 适用级别 */
    private String levelCode;

    /** 响应时限（小时） */
    private Integer responseHours;

    /** 处理时限（小时） */
    private Integer handleHours;

    /** 预警阈值（剩余时限比例） */
    private BigDecimal warnRatio;

    /** 超时自动升级 1 是 0 否 */
    private Integer overdueUpgrade;

    /** 生效日期 */
    private String effectiveDate;

    /** 状态 生效/停用 */
    private String status;

    /** 备注 */
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
