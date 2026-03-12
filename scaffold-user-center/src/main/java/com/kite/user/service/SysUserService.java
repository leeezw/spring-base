package com.kite.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kite.common.exception.BusinessException;
import com.kite.common.response.PageResult;
import com.kite.user.entity.SysUser;
import com.kite.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 用户服务
 */
@Service
@RequiredArgsConstructor
public class SysUserService extends ServiceImpl<SysUserMapper, SysUser> {
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    /**
     * 分页查询用户
     */
    public PageResult<SysUser> pageUsers(int pageNum, int pageSize, String username, String nickname, Integer status) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(username), SysUser::getUsername, username)
               .like(StringUtils.hasText(nickname), SysUser::getNickname, nickname)
               .eq(status != null, SysUser::getStatus, status)
               .orderByDesc(SysUser::getCreateTime);
        
        Page<SysUser> page = page(new Page<>(pageNum, pageSize), wrapper);
        
        // 清空密码
        page.getRecords().forEach(user -> user.setPassword(null));
        
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }
    
    /**
     * 新增用户
     */
    public void addUser(SysUser user) {
        // 检查用户名是否存在
        long count = count(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getUsername, user.getUsername()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }
        
        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        save(user);
    }
    
    /**
     * 更新用户
     */
    public void updateUser(SysUser user) {
        SysUser existUser = getById(user.getId());
        if (existUser == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 如果修改了用户名，检查是否重复
        if (!existUser.getUsername().equals(user.getUsername())) {
            long count = count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, user.getUsername())
                .ne(SysUser::getId, user.getId()));
            if (count > 0) {
                throw new BusinessException("用户名已存在");
            }
        }
        
        // 不更新密码
        user.setPassword(null);
        
        updateById(user);
    }
    
    /**
     * 删除用户
     */
    public void deleteUser(Long id) {
        if (id == 1L) {
            throw new BusinessException("不能删除超级管理员");
        }
        removeById(id);
    }
    
    /**
     * 重置密码
     */
    public void resetPassword(Long id, String newPassword) {
        SysUser user = getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        updateById(user);
    }
}
