package com.grad.csc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.csc.entity.WarningRecord;
import org.apache.ibatis.annotations.Mapper;

/** 时限预警记录 Mapper */
@Mapper
public interface WarningRecordMapper extends BaseMapper<WarningRecord> {
}
