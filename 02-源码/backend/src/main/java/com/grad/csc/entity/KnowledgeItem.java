package com.grad.csc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 知识库条目
 */
@Data
@TableName("knowledge_item")
public class KnowledgeItem {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 条目编号 KB+序号 */
    private String itemNo;

    /** 标题 */
    private String title;

    /** 分类 */
    private String categoryName;

    /** 关键词（逗号分隔） */
    private String keywords;

    /** 解决方案正文 */
    private String content;

    /** 来源投诉 */
    private Long sourceTicketId;

    /** 撰写人 */
    private Long authorId;

    /** 撰写人姓名 */
    private String authorName;

    /** 查看次数 */
    private Integer viewCount;

    /** 状态 启用/停用 */
    private String status;

    /** 发布时间 */
    private LocalDateTime publishTime;

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
