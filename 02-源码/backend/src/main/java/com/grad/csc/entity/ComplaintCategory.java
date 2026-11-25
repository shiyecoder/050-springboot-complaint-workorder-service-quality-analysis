package com.grad.csc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 投诉分类
 */
@Data
@TableName("complaint_category")
public class ComplaintCategory {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类编码 */
    private String categoryCode;

    /** 分类名称 */
    private String categoryName;

    /** 上级分类 ID */
    private Long parentId;

    /** 问题类型（创新因子） */
    private String problemType;

    /** 服务环节（创新因子） */
    private String serviceStage;

    /** 建议级别 P1-P4 */
    private String defaultLevel;

    /** 默认响应时限（小时） */
    private Integer responseHours;

    /** 默认处理时限（小时） */
    private Integer handleHours;

    /** 分类说明 */
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
