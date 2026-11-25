package com.grad.csc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.csc.entity.CommRecord;
import org.apache.ibatis.annotations.Mapper;

/** 客户沟通记录 Mapper */
@Mapper
public interface CommRecordMapper extends BaseMapper<CommRecord> {
}
