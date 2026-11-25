package com.grad.csc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.csc.entity.SysLog;
import org.apache.ibatis.annotations.Mapper;

/** 操作日志 Mapper */
@Mapper
public interface SysLogMapper extends BaseMapper<SysLog> {
}
