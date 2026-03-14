package com.kite.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kite.common.response.PageResult;
import com.kite.mybatis.context.TenantContext;
import com.kite.user.entity.SysRole;
import com.kite.user.entity.SysTenant;
import com.kite.user.entity.SysUser;
import com.kite.user.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 租户服务
 */
@Service
@RequiredArgsConstructor
public class SysTenantService {
    
    private final SysTenantMapper tenantMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * 分页查询租户
     */
    public PageResult<SysTenant> page(int pageNum, int pageSize, String keyword) {
        Page<SysTenant> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysTenant> wrapper = new LambdaQueryWrapper<>();
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w
                .like(SysTenant::getTenantCode, keyword)
                .or()
                .like(SysTenant::getTenantName, keyword)
            );
        }
        
        wrapper.eq(SysTenant::getDeleted, 0)
               .orderByDesc(SysTenant::getCreateTime);
        
        IPage<SysTenant> result = tenantMapper.selectPage(page, wrapper);
        return PageResult.of(result);
    }
    
    /**
     * 查询所有租户
     */
    public List<SysTenant> list() {
        return tenantMapper.selectList(
            new LambdaQueryWrapper<SysTenant>()
                .eq(SysTenant::getDeleted, 0)
                .orderByDesc(SysTenant::getCreateTime)
        );
    }
    
    /**
     * 根据ID查询租户
     */
    public SysTenant getById(Long id) {
        return tenantMapper.selectById(id);
    }
    
    /**
     * 根据租户编码查询
     */
    public SysTenant getByCode(String tenantCode) {
        return tenantMapper.selectOne(
            new LambdaQueryWrapper<SysTenant>()
                .eq(SysTenant::getTenantCode, tenantCode)
                .eq(SysTenant::getDeleted, 0)
        );
    }
    
    /**
     * 新增租户 + 初始化基础数据（超级管理员角色、admin用户、菜单关联）
     */
    @Transactional(rollbackFor = Exception.class)
    public void save(SysTenant tenant) {
        // 1. 插入租户
        tenantMapper.insert(tenant);
        Long tenantId = tenant.getId();
        
        // 2. 临时切换租户上下文（让 MyBatis-Plus 自动填充 tenant_id）
        Long originalTenantId = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(tenantId);
            
            // 3. 创建超级管理员角色
            SysRole adminRole = new SysRole();
            adminRole.setTenantId(tenantId);
            adminRole.setRoleCode("super_admin");
            adminRole.setRoleName("超级管理员");
            adminRole.setDescription("租户超级管理员，拥有全部权限");
            adminRole.setSortOrder(0);
            adminRole.setStatus(1);
            adminRole.setDataScope(1); // 全部数据权限
            roleMapper.insert(adminRole);
            Long roleId = adminRole.getId();
            
            // 4. 创建 admin 用户（username = tenantCode_admin 避免全局唯一冲突）
            String adminUsername = tenant.getTenantCode() + "_admin";
            SysUser adminUser = new SysUser();
            adminUser.setTenantId(tenantId);
            adminUser.setUsername(adminUsername);
            adminUser.setNickname("管理员");
            adminUser.setPassword(passwordEncoder.encode("admin123"));
            adminUser.setStatus(1);
            userMapper.insert(adminUser);
            Long userId = adminUser.getId();
            
            // 5. 绑定用户-角色
            userRoleMapper.insertUserRole(userId, roleId);
            
            // 6. 给超级管理员角色分配全部菜单
            List<Long> allMenuIds = menuMapper.selectAllMenuIds();
            for (Long menuId : allMenuIds) {
                roleMenuMapper.insertRoleMenu(roleId, menuId);
            }
            
            // 7. 给超级管理员角色分配全部权限
            // 权限通过角色-权限关联表，这里暂不处理，由管理员手动分配
            
        } finally {
            // 恢复原租户上下文
            if (originalTenantId != null) {
                TenantContext.setTenantId(originalTenantId);
            }
        }
    }
    
    /**
     * 更新租户
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(SysTenant tenant) {
        tenantMapper.updateById(tenant);
    }
    
    /**
     * 删除租户
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysTenant tenant = new SysTenant();
        tenant.setId(id);
        tenant.setDeleted(1);
        tenantMapper.updateById(tenant);
    }
}
