package com.grad.csc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.csc.entity.CustomerConfirm;
import org.apache.ibatis.annotations.Mapper;

/** 客户确认记录 Mapper */
@Mapper
public interface CustomerConfirmMapper extends BaseMapper<CustomerConfirm> {
}
