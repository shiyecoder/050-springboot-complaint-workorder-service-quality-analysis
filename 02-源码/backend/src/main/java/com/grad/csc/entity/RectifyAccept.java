package com.grad.csc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 整改验收记录
 */
@Data
@TableName("rectify_accept")
public class RectifyAccept {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 整改任务 ID */
    private Long taskId;

    /** 验收结果 通过/不通过 */
    private String acceptResult;

    /** 整改凭证图片路径 */
    private String evidencePath;

    /** 验收意见 */
    private String opinion;

    /** 验收人 */
    private Long acceptorId;

    /** 验收人姓名 */
    private String acceptorName;

    /** 验收时间 */
    private LocalDateTime acceptTime;

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
