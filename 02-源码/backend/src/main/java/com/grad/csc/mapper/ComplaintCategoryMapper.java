package com.grad.csc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.csc.entity.ComplaintCategory;
import org.apache.ibatis.annotations.Mapper;

/** 投诉分类 Mapper */
@Mapper
public interface ComplaintCategoryMapper extends BaseMapper<ComplaintCategory> {
}
