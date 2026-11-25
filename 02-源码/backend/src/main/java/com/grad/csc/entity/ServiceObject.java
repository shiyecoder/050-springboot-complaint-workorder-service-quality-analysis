package com.grad.csc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 服务对象
 */
@Data
@TableName("service_object")
public class ServiceObject {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 对象编号 */
    private String objectNo;

    /** 对象名称（产品/服务项目/门店） */
    private String objectName;

    /** 对象类型 产品/服务项目/门店/订单 */
    private String objectType;

    /** 对象封面图路径 */
    private String coverImage;

    /** 品牌 */
    private String brand;

    /** 型号/规格 */
    private String model;

    /** 所属门店 */
    private String storeName;

    /** 关联订单号 */
    private String orderNo;

    /** 购买/开通日期 */
    private String purchaseDate;

    /** 保修或服务到期日 */
    private String warrantyEnd;

    /** 状态 在用/已过期/已停用 */
    private String status;

    /** 备注 */
    private String remark;

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
