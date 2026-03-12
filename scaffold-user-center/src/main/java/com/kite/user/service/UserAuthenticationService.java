package com.kite.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kite.auth.model.LoginUser;
import com.kite.auth.service.AuthenticationService;
import com.kite.user.entity.SysUser;
import com.kite.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户认证服务实现
 */
@Service
@RequiredArgsConstructor
public class UserAuthenticationService implements AuthenticationService {
    
    private final SysUserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    @Override
    public LoginUser loadUserByUsername(String username) {
        SysUser user = userMapper.selectOne(
            new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getDeleted, 0)
        );
        
        if (user == null || user.getStatus() != 1) {
            return null;
        }
        
        return buildLoginUser(user);
    }
    
    @Override
    public LoginUser loadUserById(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1 || user.getStatus() != 1) {
            return null;
        }
        
        return buildLoginUser(user);
    }
    
    /**
     * 用户名密码认证
     */
    public LoginUser authenticate(String username, String password) {
        SysUser user = userMapper.selectOne(
            new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getDeleted, 0)
        );
        
        if (user == null) {
            return null;
        }
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return null;
        }
        
        if (user.getStatus() != 1) {
            return null;
        }
        
        return buildLoginUser(user);
    }
    
    private LoginUser buildLoginUser(SysUser user) {
        List<String> roles = userMapper.selectRolesByUserId(user.getId());
        List<String> permissions = userMapper.selectPermissionsByUserId(user.getId());
        
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setNickname(user.getNickname());
        loginUser.setAvatar(user.getAvatar());
        loginUser.setRoles(roles);
        loginUser.setPermissions(permissions);
        
        return loginUser;
    }
}
