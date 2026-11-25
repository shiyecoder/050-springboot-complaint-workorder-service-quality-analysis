package com.grad.csc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 部门
 */
@Data
@TableName("sys_org")
public class SysOrg {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 部门名称 */
    private String orgName;

    /** 类型 公司/中心/部门/门店 */
    private String orgType;

    /** 上级 ID */
    private Long parentId;

    /** 负责人 */
    private String contact;

    /** 联系电话 */
    private String phone;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除 0 否 1 是 */
    @TableLogic
    private Integer deleted;
}
