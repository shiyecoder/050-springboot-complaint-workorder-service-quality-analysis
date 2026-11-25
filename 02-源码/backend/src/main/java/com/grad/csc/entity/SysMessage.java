package com.grad.csc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 站内消息
 */
@Data
@TableName("sys_message")
public class SysMessage {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 类型 待处理/待审核/时限预警/待验收/系统通知 */
    private String msgType;

    /** 详情跳转地址 */
    private String linkUrl;

    /** 接收人 */
    private Long receiverId;

    /** 是否已读 1 是 0 否 */
    private Integer isRead;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除 0 否 1 是 */
    @TableLogic
    private Integer deleted;
}
