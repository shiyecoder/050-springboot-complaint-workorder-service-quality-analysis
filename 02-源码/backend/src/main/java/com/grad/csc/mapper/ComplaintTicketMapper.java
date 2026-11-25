package com.grad.csc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.csc.entity.ComplaintTicket;
import org.apache.ibatis.annotations.Mapper;

/** 客户投诉主档 Mapper */
@Mapper
public interface ComplaintTicketMapper extends BaseMapper<ComplaintTicket> {
}
