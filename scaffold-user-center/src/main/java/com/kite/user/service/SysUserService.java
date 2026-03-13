package com.kite.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kite.common.exception.BusinessException;
import com.kite.common.response.PageResult;
import com.kite.user.entity.SysDept;
import com.kite.user.entity.SysRole;
import com.kite.user.entity.SysUser;
import com.kite.user.entity.SysUserRole;
import com.kite.user.entity.SysPost;
import com.kite.user.entity.SysUserPost;
import com.kite.user.mapper.SysDeptMapper;
import com.kite.user.mapper.SysRoleMapper;
import com.kite.user.mapper.SysUserMapper;
import com.kite.user.mapper.SysUserRoleMapper;
import com.kite.user.mapper.SysPostMapper;
import com.kite.user.mapper.SysUserPostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户服务
 */
@Service
@RequiredArgsConstructor
public class SysUserService extends ServiceImpl<SysUserMapper, SysUser> {
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SysDeptMapper deptMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserPostMapper userPostMapper;
    private final SysPostMapper postMapper;
    
    /**
     * 分页查询用户（含部门名、角色列表）
     */
    public PageResult<SysUser> pageUsers(int pageNum, int pageSize, String username, String nickname, Integer status) {
        return pageUsers(pageNum, pageSize, username, nickname, status, null);
    }
    
    /**
     * 分页查询用户（含部门筛选）
     */
    public PageResult<SysUser> pageUsers(int pageNum, int pageSize, String username, String nickname, Integer status, Long deptId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(username), SysUser::getUsername, username)
               .like(StringUtils.hasText(nickname), SysUser::getNickname, nickname)
               .eq(status != null, SysUser::getStatus, status)
               .eq(deptId != null, SysUser::getDeptId, deptId)
               .orderByDesc(SysUser::getCreateTime);
        
        Page<SysUser> page = page(new Page<>(pageNum, pageSize), wrapper);
        List<SysUser> records = page.getRecords();
        
        if (!records.isEmpty()) {
            // 收集所有用户ID和部门ID
            List<Long> userIds = records.stream().map(SysUser::getId).collect(Collectors.toList());
            Set<Long> deptIds = records.stream().map(SysUser::getDeptId).filter(Objects::nonNull).collect(Collectors.toSet());
            
            // 批量查部门
            Map<Long, String> deptNameMap = new HashMap<>();
            if (!deptIds.isEmpty()) {
                List<SysDept> depts = deptMapper.selectBatchIds(deptIds);
                depts.forEach(d -> deptNameMap.put(d.getId(), d.getDeptName()));
            }
            
            // 批量查用户-角色关联
            List<SysUserRole> allUserRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getUserId, userIds));
            Map<Long, List<Long>> userRoleMap = allUserRoles.stream()
                .collect(Collectors.groupingBy(SysUserRole::getUserId,
                    Collectors.mapping(SysUserRole::getRoleId, Collectors.toList())));
            
            // 批量查角色信息
            Set<Long> allRoleIds = allUserRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toSet());
            Map<Long, SysRole> roleInfoMap = new HashMap<>();
            if (!allRoleIds.isEmpty()) {
                List<SysRole> roles = roleMapper.selectBatchIds(allRoleIds);
                roles.forEach(r -> roleInfoMap.put(r.getId(), r));
            }
            
            // 填充到每个用户
            records.forEach(user -> {
                user.setPassword(null);
                user.setDeptName(deptNameMap.get(user.getDeptId()));
                
                List<Long> roleIds = userRoleMap.getOrDefault(user.getId(), Collections.emptyList());
                List<SysUser.RoleInfo> roleInfos = roleIds.stream()
                    .map(roleInfoMap::get)
                    .filter(Objects::nonNull)
                    .map(r -> new SysUser.RoleInfo(r.getId(), r.getRoleCode(), r.getRoleName()))
                    .collect(Collectors.toList());
                user.setRoles(roleInfos);
            });
            
            // 批量查用户-岗位关联
            List<SysUserPost> allUserPosts = userPostMapper.selectList(
                new LambdaQueryWrapper<SysUserPost>().in(SysUserPost::getUserId, userIds));
            Map<Long, List<Long>> userPostMap = allUserPosts.stream()
                .collect(Collectors.groupingBy(SysUserPost::getUserId,
                    Collectors.mapping(SysUserPost::getPostId, Collectors.toList())));
            
            // 批量查岗位信息
            Set<Long> allPostIds = allUserPosts.stream().map(SysUserPost::getPostId).collect(Collectors.toSet());
            Map<Long, SysPost> postInfoMap = new HashMap<>();
            if (!allPostIds.isEmpty()) {
                List<SysPost> posts = postMapper.selectBatchIds(allPostIds);
                posts.forEach(p -> postInfoMap.put(p.getId(), p));
            }
            
            // 填充岗位
            records.forEach(user -> {
                List<Long> postIds = userPostMap.getOrDefault(user.getId(), Collections.emptyList());
                List<SysUser.PostInfo> postInfos = postIds.stream()
                    .map(postInfoMap::get)
                    .filter(Objects::nonNull)
                    .map(p -> new SysUser.PostInfo(p.getId(), p.getPostCode(), p.getPostName()))
                    .collect(Collectors.toList());
                user.setPosts(postInfos);
            });
        }
        
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
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
