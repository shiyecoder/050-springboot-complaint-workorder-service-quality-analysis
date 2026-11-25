package com.grad.csc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 操作日志
 */
@Data
@TableName("sys_log")
public class SysLog {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人 ID */
    private Long userId;

    /** 操作人 */
    private String userName;

    /** 模块 */
    private String module;

    /** 动作 */
    private String action;

    /** 方法 */
    private String method;

    /** 参数 */
    private String params;

    /** IP */
    private String ip;

    /** 结果 1 成功 0 失败 */
    private Integer status;

    /** 耗时（毫秒） */
    private Integer costMs;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除 0 否 1 是 */
    @TableLogic
    private Integer deleted;
}
