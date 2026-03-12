package com.kite.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kite.user.entity.SysTenant;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户Mapper
 */
@Mapper
public interface SysTenantMapper extends BaseMapper<SysTenant> {
}
