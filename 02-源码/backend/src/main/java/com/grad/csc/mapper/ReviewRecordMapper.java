package com.grad.csc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.csc.entity.ReviewRecord;
import org.apache.ibatis.annotations.Mapper;

/** 结果审核记录 Mapper */
@Mapper
public interface ReviewRecordMapper extends BaseMapper<ReviewRecord> {
}
