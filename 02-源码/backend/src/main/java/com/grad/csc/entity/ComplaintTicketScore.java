package com.grad.csc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 共性问题聚类计算结果
 */
@Data
@TableName("complaint_ticket_score")
public class ComplaintTicketScore {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 投诉主档 ID */
    private Long ticketId;

    /** 客诉编号 */
    private String ticketNo;

    /** 聚类主题 */
    private String clusterName;

    /** 计算所用规则版本 */
    private String ruleVersion;

    /** 关联产品得分 */
    private Integer sProduct;

    /** 服务环节得分 */
    private Integer sStage;

    /** 问题类型得分 */
    private Integer sProblem;

    /** 原因标签得分 */
    private Integer sReason;

    /** 责任部门得分 */
    private Integer sDept;

    /** 近30天次数得分 */
    private Integer sFreq;

    /** 综合相似度 */
    private BigDecimal similarity;

    /** 结果等级 HIGH/MID/LOW */
    private String level;

    /** 输入快照（可追溯） */
    private String inputSnapshot;

    /** 主要影响因素 */
    private String reasons;

    /** 未满足条件 */
    private String unmet;

    /** 可改进项 */
    private String improve;

    /** 计算时间 */
    private LocalDateTime calcTime;

    /** 人工调整等级 */
    private String manualLevel;

    /** 人工调整原因 */
    private String manualReason;

    /** 调整人 */
    private String manualBy;

    /** 调整时间 */
    private LocalDateTime manualTime;

    /** 是否已转整改 1 是 0 否 */
    private Integer adoptFlag;

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
