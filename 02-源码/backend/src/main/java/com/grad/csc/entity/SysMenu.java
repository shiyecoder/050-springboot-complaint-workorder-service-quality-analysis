package com.grad.csc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 菜单
 */
@Data
@TableName("sys_menu")
public class SysMenu {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父菜单 ID */
    private Long parentId;

    /** 菜单名称 */
    private String menuName;

    /** 前端路由 */
    private String path;

    /** 权限标识 */
    private String perm;

    /** 图标名 */
    private String icon;

    /** 排序 */
    private Integer sortNo;

    /** 是否显示 1 是 0 否 */
    private Integer visible;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除 0 否 1 是 */
    @TableLogic
    private Integer deleted;
}
