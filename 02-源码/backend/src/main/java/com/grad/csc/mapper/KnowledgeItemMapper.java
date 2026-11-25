package com.grad.csc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.csc.entity.KnowledgeItem;
import org.apache.ibatis.annotations.Mapper;

/** 知识库条目 Mapper */
@Mapper
public interface KnowledgeItemMapper extends BaseMapper<KnowledgeItem> {
}
