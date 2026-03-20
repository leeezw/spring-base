package com.kite.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kite.user.entity.SysEmployeeField;
import org.apache.ibatis.annotations.Mapper;

/**
 * 员工自定义字段定义 Mapper
 */
@Mapper
public interface SysEmployeeFieldMapper extends BaseMapper<SysEmployeeField> {
}
