package com.grad.csc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户沟通记录
 */
@Data
@TableName("comm_record")
public class CommRecord {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 投诉主档 ID */
    private Long ticketId;

    /** 工单 ID */
    private Long orderId;

    /** 沟通方式 电话/在线消息/现场/邮件 */
    private String commType;

    /** 沟通时间 */
    private LocalDateTime commTime;

    /** 沟通摘要 */
    private String summary;

    /** 客户反馈 */
    private String customerFeedback;

    /** 沟通人 */
    private Long operatorId;

    /** 沟通人姓名 */
    private String operatorName;

    /** 是否客户可见 1 是 0 否 */
    private Integer visibleFlag;

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
