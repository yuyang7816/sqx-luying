package com.sqx.app.base.platformcs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sqx.app.base.platformcs.domain.entity.PlatformCsMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PlatformCsMessageMapper extends BaseMapper<PlatformCsMessage> {
}
