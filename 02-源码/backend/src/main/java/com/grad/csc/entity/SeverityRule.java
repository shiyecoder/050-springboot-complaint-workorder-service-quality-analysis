package com.grad.csc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 级别规则
 */
@Data
@TableName("severity_rule")
public class SeverityRule {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 级别编码 P1/P2/P3/P4 */
    private String levelCode;

    /** 级别名称 */
    private String levelName;

    /** 响应时限（小时） */
    private Integer responseHours;

    /** 处理时限（小时） */
    private Integer handleHours;

    /** 超过该小时数触发升级 */
    private Integer escalateHours;

    /** 是否进入重点关注 1 是 0 否 */
    private Integer focusFlag;

    /** 前端标签色 red/amber/blue/gray */
    private String colorClass;

    /** 级别说明 */
    private String description;

    /** 状态 启用/停用 */
    private String status;

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
