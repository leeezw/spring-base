package com.kite.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kite.auth.model.LoginUser;
import com.kite.auth.model.LoginUserContext;
import com.kite.common.exception.BusinessException;
import com.kite.mybatis.context.TenantContext;
import com.kite.user.entity.SysMenu;
import com.kite.user.mapper.SysMenuMapper;
import com.kite.user.service.SysMenuService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    @Override
    public List<SysMenu> getUserMenuTree() {
        LoginUser loginUser = LoginUserContext.get();
        if (loginUser == null) {
            return new ArrayList<>();
        }

        List<Long> menuIds = baseMapper.selectMenuIdsByUserId(loginUser.getUserId());
        if (menuIds.isEmpty()) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysMenu::getId, menuIds)
                .eq(SysMenu::getStatus, 1)
                .eq(SysMenu::getVisible, 1)
                .orderByAsc(SysMenu::getSortOrder);
        List<SysMenu> allMenus = list(wrapper);
        return buildMenuTree(allMenus, 0L);
    }

    @Override
    public List<SysMenu> getMenuTree() {
        Long tenantId = TenantContext.getTenantId();
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysMenu::getTenantId, tenantId == null ? Arrays.asList(0L) : Arrays.asList(0L, tenantId));
        if (tenantId != null && tenantId != 0L) {
            wrapper.ne(SysMenu::getPath, "/system/tenant");
        }
        wrapper.orderByAsc(SysMenu::getSortOrder);
        List<SysMenu> allMenus = list(wrapper);
        return buildMenuTree(allMenus, 0L);
    }

    @Override
    public void addMenu(SysMenu menu) {
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        if (menu.getSortOrder() == null) {
            menu.setSortOrder(0);
        }
        menu.setMenuName(menu.getMenuName() == null ? null : menu.getMenuName().trim());
        menu.setPath(trimToNull(menu.getPath()));
        menu.setComponent(trimToNull(menu.getComponent()));
        menu.setIcon(trimToNull(menu.getIcon()));
        validateParent(menu.getParentId(), null);
        save(menu);
    }

    @Override
    public void updateMenu(SysMenu menu) {
        SysMenu existMenu = getById(menu.getId());
        if (existMenu == null) {
            throw new BusinessException("菜单不存在");
        }
        if (menu.getParentId() == null) {
            menu.setParentId(existMenu.getParentId());
        }
        if (menu.getSortOrder() == null) {
            menu.setSortOrder(existMenu.getSortOrder());
        }
        menu.setTenantId(existMenu.getTenantId());
        menu.setMenuName(menu.getMenuName() == null ? null : menu.getMenuName().trim());
        menu.setPath(trimToNull(menu.getPath()));
        menu.setComponent(trimToNull(menu.getComponent()));
        menu.setIcon(trimToNull(menu.getIcon()));
        validateParent(menu.getParentId(), menu.getId());
        updateById(menu);
    }

    @Override
    public void deleteMenu(Long id) {
        long childrenCount = count(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
        if (childrenCount > 0) {
            throw new BusinessException("存在子菜单，无法删除");
        }
        removeById(id);
    }

    private List<SysMenu> buildMenuTree(List<SysMenu> allMenus, Long parentId) {
        return allMenus.stream()
                .filter(menu -> parentId.equals(menu.getParentId()))
                .peek(menu -> {
                    List<SysMenu> children = buildMenuTree(allMenus, menu.getId());
                    menu.setChildren(children.isEmpty() ? null : children);
                })
                .collect(Collectors.toList());
    }

    private void validateParent(Long parentId, Long currentId) {
        if (parentId == null || parentId == 0L) {
            return;
        }
        if (currentId != null && parentId.equals(currentId)) {
            throw new BusinessException("上级菜单不能选择自己");
        }
        SysMenu parent = getById(parentId);
        if (parent == null) {
            throw new BusinessException("上级菜单不存在");
        }
        Long cursor = parent.getParentId();
        while (cursor != null && cursor != 0L) {
            if (currentId != null && cursor.equals(currentId)) {
                throw new BusinessException("上级菜单不能选择当前菜单的子节点");
            }
            SysMenu current = getById(cursor);
            if (current == null) {
                break;
            }
            cursor = current.getParentId();
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
