package com.grad.csc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 整改任务
 */
@Data
@TableName("rectify_task")
public class RectifyTask {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 整改单号 RC+日期+序号 */
    private String taskNo;

    /** 整改主题 */
    private String title;

    /** 来源聚类主题 */
    private String clusterName;

    /** 责任部门 */
    private Long deptId;

    /** 责任部门名称 */
    private String deptName;

    /** 整改措施 */
    private String measure;

    /** 整改期限 */
    private String deadline;

    /** 整改负责人 */
    private Long ownerId;

    /** 负责人姓名 */
    private String ownerName;

    /** 验收人 */
    private Long acceptId;

    /** 验收人姓名 */
    private String acceptName;

    /** 状态 待下发/整改中/待验收/已验收/已关闭 */
    private String status;

    /** 引发整改的客诉编号（逗号分隔） */
    private String sourceTickets;

    /** 完成时间 */
    private LocalDateTime finishTime;

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
