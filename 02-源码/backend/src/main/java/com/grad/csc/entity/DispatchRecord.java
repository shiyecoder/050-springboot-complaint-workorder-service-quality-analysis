package com.grad.csc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 分派记录
 */
@Data
@TableName("dispatch_record")
public class DispatchRecord {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 投诉主档 ID */
    private Long ticketId;

    /** 工单 ID */
    private Long orderId;

    /** 类型 首次分派/转派/升级转派 */
    private String dispatchType;

    /** 原责任部门 */
    private Long fromDeptId;

    /** 新责任部门 */
    private Long toDeptId;

    /** 新责任部门名称 */
    private String toDeptName;

    /** 指定处理人 */
    private Long handlerId;

    /** 处理人姓名 */
    private String handlerName;

    /** 分派/转派原因 */
    private String reason;

    /** 操作人 */
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** 分派时间 */
    private LocalDateTime dispatchTime;

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
