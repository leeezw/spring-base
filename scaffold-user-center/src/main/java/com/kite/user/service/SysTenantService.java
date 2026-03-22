package com.kite.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kite.common.exception.BusinessException;
import com.kite.common.response.PageResult;
import com.kite.mybatis.context.TenantContext;
import com.kite.user.entity.SysRole;
import com.kite.user.entity.SysTenant;
import com.kite.user.entity.SysUser;
import com.kite.user.mapper.SysMenuMapper;
import com.kite.user.mapper.SysPermissionMapper;
import com.kite.user.mapper.SysRoleMapper;
import com.kite.user.mapper.SysRoleMenuMapper;
import com.kite.user.mapper.SysRolePermissionMapper;
import com.kite.user.mapper.SysTenantMapper;
import com.kite.user.mapper.SysUserMapper;
import com.kite.user.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysTenantService {

    private final SysTenantMapper tenantMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysMenuMapper menuMapper;
    private final SysPermissionMapper permissionMapper;
    private final PasswordEncoder passwordEncoder;

    public PageResult<SysTenant> page(int pageNum, int pageSize, String keyword) {
        Page<SysTenant> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysTenant> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(SysTenant::getTenantCode, keyword).or().like(SysTenant::getTenantName, keyword));
        }
        wrapper.eq(SysTenant::getDeleted, 0).orderByDesc(SysTenant::getCreateTime);
        IPage<SysTenant> result = tenantMapper.selectPage(page, wrapper);
        result.getRecords().forEach(this::fillLoginAccount);
        return PageResult.of(result);
    }

    public List<SysTenant> list() {
        List<SysTenant> list = tenantMapper.selectList(new LambdaQueryWrapper<SysTenant>()
                .eq(SysTenant::getDeleted, 0)
                .orderByDesc(SysTenant::getCreateTime));
        list.forEach(this::fillLoginAccount);
        return list;
    }

    public SysTenant getById(Long id) {
        SysTenant tenant = tenantMapper.selectById(id);
        fillLoginAccount(tenant);
        return tenant;
    }

    public SysTenant getByCode(String tenantCode) {
        return tenantMapper.selectOne(new LambdaQueryWrapper<SysTenant>()
                .eq(SysTenant::getTenantCode, tenantCode)
                .eq(SysTenant::getDeleted, 0));
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(SysTenant tenant) {
        normalizeTenant(tenant);
        tenantMapper.insert(tenant);
        Long tenantId = tenant.getId();

        Long originalTenantId = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(tenantId);

            long roleCodeCount = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getRoleCode, "super_admin"));
            if (roleCodeCount > 0) {
                throw new BusinessException("初始化租户失败: 角色编码 super_admin 已存在");
            }

            SysRole adminRole = new SysRole();
            adminRole.setTenantId(tenantId);
            adminRole.setRoleCode("super_admin");
            adminRole.setRoleName("超级管理员");
            adminRole.setDescription("租户超级管理员，拥有全部权限");
            adminRole.setSortOrder(0);
            adminRole.setStatus(1);
            adminRole.setDataScope(1);
            roleMapper.insert(adminRole);
            Long roleId = adminRole.getId();

            String adminUsername = tenant.getTenantCode() + "_admin";
            long usernameCount = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, adminUsername));
            if (usernameCount > 0) {
                throw new BusinessException("初始化租户失败: 用户名 " + adminUsername + " 已存在");
            }

            SysUser adminUser = new SysUser();
            adminUser.setTenantId(tenantId);
            adminUser.setUsername(adminUsername);
            adminUser.setNickname("管理员");
            adminUser.setPassword(passwordEncoder.encode("admin123"));
            adminUser.setStatus(1);
            userMapper.insert(adminUser);
            Long userId = adminUser.getId();

            userRoleMapper.insertUserRole(userId, roleId);

            List<Long> allMenuIds = menuMapper.selectAllMenuIds();
            List<Long> tenantMenuExclude = menuMapper.selectMenuIdsByPath("/system/tenant");
            for (Long menuId : allMenuIds) {
                if (!tenantMenuExclude.contains(menuId)) {
                    roleMenuMapper.insertRoleMenu(roleId, menuId);
                }
            }

            List<Long> allPermIds = permissionMapper.selectBuiltinPermissionIds();
            List<Long> tenantPermExclude = permissionMapper.selectPermissionIdsByCodePrefix("system:tenant");
            for (Long permId : allPermIds) {
                if (!tenantPermExclude.contains(permId)) {
                    rolePermissionMapper.insertRolePermission(roleId, permId);
                }
            }
        } finally {
            if (originalTenantId != null) {
                TenantContext.setTenantId(originalTenantId);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(SysTenant tenant) {
        normalizeTenant(tenant);
        tenantMapper.updateById(tenant);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        SysTenant tenant = new SysTenant();
        tenant.setId(id);
        tenant.setStatus(status);
        tenantMapper.updateById(tenant);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysTenant tenant = new SysTenant();
        tenant.setId(id);
        tenant.setDeleted(1);
        tenantMapper.updateById(tenant);
    }

    private void normalizeTenant(SysTenant tenant) {
        tenant.setTenantCode(tenant.getTenantCode() == null ? null : tenant.getTenantCode().trim());
        tenant.setTenantName(tenant.getTenantName() == null ? null : tenant.getTenantName().trim());
        tenant.setContactName(trimToNull(tenant.getContactName()));
        tenant.setContactPhone(trimToNull(tenant.getContactPhone()));
        tenant.setContactEmail(trimToNull(tenant.getContactEmail()));
        if (tenant.getAccountCount() == null) {
            tenant.setAccountCount(1);
        } else if (tenant.getAccountCount() == 0 || tenant.getAccountCount() < -1) {
            throw new BusinessException("账号额度只能为-1或大于0");
        }
    }

    private void fillLoginAccount(SysTenant tenant) {
        if (tenant == null) {
            return;
        }
        if (tenant.getTenantCode() != null && !tenant.getTenantCode().isEmpty()) {
            tenant.setLoginAccount(tenant.getTenantCode() + "_admin");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
