package com.kite.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kite.user.entity.SysDict;
import com.kite.user.mapper.SysDictMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SysDictService extends ServiceImpl<SysDictMapper, SysDict> {

    public Page<SysDict> page(int pageNum, int pageSize, String dictName, Integer status) {
        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(dictName), SysDict::getDictName, dictName);
        wrapper.eq(status != null, SysDict::getStatus, status);
        wrapper.orderByDesc(SysDict::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }
}
