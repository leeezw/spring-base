package com.kite.user.dto.request;

import com.kite.user.entity.GenTable;
import com.kite.user.entity.GenTableColumn;
import com.kite.user.entity.SysDept;
import com.kite.user.entity.SysDict;
import com.kite.user.entity.SysDictItem;
import com.kite.user.entity.SysMenu;
import com.kite.user.entity.SysPermission;
import com.kite.user.entity.SysPost;
import com.kite.user.entity.SysRole;
import com.kite.user.entity.SysTenant;
import com.kite.user.entity.SysUser;

import java.util.List;
import java.util.stream.Collectors;

public final class RequestConverters {

    private RequestConverters() {
    }

    public static SysUser toUser(UserRequests.Create request) {
        SysUser user = new SysUser();
        user.setUsername(trim(request.getUsername()));
        user.setPassword(request.getPassword());
        user.setNickname(trimToNull(request.getNickname()));
        user.setEmail(trimToNull(request.getEmail()));
        user.setPhone(trimToNull(request.getPhone()));
        user.setDeptId(request.getDeptId());
        user.setStatus(request.getStatus());
        return user;
    }

    public static SysUser toUser(UserRequests.Update request) {
        SysUser user = toUserBase(request.getUsername(), request.getPassword(), request.getNickname(), request.getEmail(), request.getPhone(), request.getDeptId());
        user.setId(request.getId());
        user.setStatus(request.getStatus());
        return user;
    }

    public static SysRole toRole(RoleRequests.Create request) {
        SysRole role = new SysRole();
        role.setRoleCode(trim(request.getRoleCode()));
        role.setRoleName(trim(request.getRoleName()));
        role.setDescription(trimToNull(request.getDescription()));
        role.setSortOrder(request.getSortOrder());
        role.setStatus(request.getStatus());
        role.setDataScope(request.getDataScope());
        return role;
    }

    public static SysRole toRole(RoleRequests.Update request) {
        SysRole role = new SysRole();
        role.setId(request.getId());
        role.setRoleCode(trim(request.getRoleCode()));
        role.setRoleName(trim(request.getRoleName()));
        role.setDescription(trimToNull(request.getDescription()));
        role.setSortOrder(request.getSortOrder());
        role.setStatus(request.getStatus());
        role.setDataScope(request.getDataScope());
        return role;
    }

    public static SysPermission toPermission(PermissionRequests.Save request) {
        SysPermission permission = new SysPermission();
        permission.setPermissionCode(trim(request.getPermissionCode()));
        permission.setPermissionName(trim(request.getPermissionName()));
        permission.setPermissionType(request.getPermissionType());
        permission.setParentId(request.getParentId());
        permission.setPath(trimToNull(request.getPath()));
        permission.setComponent(trimToNull(request.getComponent()));
        permission.setIcon(trimToNull(request.getIcon()));
        permission.setSortOrder(request.getSortOrder());
        permission.setStatus(request.getStatus());
        return permission;
    }

    public static SysPermission toPermission(PermissionRequests.Update request) {
        SysPermission permission = toPermission((PermissionRequests.Save) request);
        permission.setId(request.getId());
        return permission;
    }

    public static SysMenu toMenu(MenuRequests.Save request) {
        SysMenu menu = new SysMenu();
        menu.setMenuName(trim(request.getMenuName()));
        menu.setParentId(request.getParentId());
        menu.setPath(trimToNull(request.getPath()));
        menu.setComponent(trimToNull(request.getComponent()));
        menu.setIcon(trimToNull(request.getIcon()));
        menu.setSortOrder(request.getSortOrder());
        menu.setVisible(request.getVisible());
        menu.setStatus(request.getStatus());
        return menu;
    }

    public static SysMenu toMenu(MenuRequests.Update request) {
        SysMenu menu = toMenu((MenuRequests.Save) request);
        menu.setId(request.getId());
        return menu;
    }

    public static SysDept toDept(DeptRequests.Save request) {
        SysDept dept = new SysDept();
        dept.setDeptName(trim(request.getDeptName()));
        dept.setParentId(request.getParentId());
        dept.setLeaderId(request.getLeaderId());
        dept.setPhone(trimToNull(request.getPhone()));
        dept.setEmail(trimToNull(request.getEmail()));
        dept.setSortOrder(request.getSortOrder());
        dept.setStatus(request.getStatus());
        return dept;
    }

    public static SysDept toDept(DeptRequests.Update request) {
        SysDept dept = toDept((DeptRequests.Save) request);
        dept.setId(request.getId());
        return dept;
    }

    public static SysPost toPost(PostRequests.Save request) {
        SysPost post = new SysPost();
        post.setPostName(trim(request.getPostName()));
        post.setPostCode(trim(request.getPostCode()));
        post.setPostCategory(request.getPostCategory());
        post.setDeptId(request.getDeptId());
        post.setDescription(trimToNull(request.getDescription()));
        post.setSortOrder(request.getSortOrder());
        post.setStatus(request.getStatus());
        return post;
    }

    public static SysPost toPost(PostRequests.Update request) {
        SysPost post = toPost((PostRequests.Save) request);
        post.setId(request.getId());
        return post;
    }

    public static SysDict toDict(DictRequests.Save request) {
        SysDict dict = new SysDict();
        dict.setDictName(trim(request.getDictName()));
        dict.setDictCode(trim(request.getDictCode()));
        dict.setDescription(trimToNull(request.getDescription()));
        dict.setSortOrder(request.getSortOrder());
        dict.setStatus(request.getStatus());
        return dict;
    }

    public static SysDict toDict(DictRequests.Update request) {
        SysDict dict = toDict((DictRequests.Save) request);
        dict.setId(request.getId());
        return dict;
    }

    public static SysDictItem toDictItem(DictRequests.ItemSave request) {
        SysDictItem item = new SysDictItem();
        item.setDictId(request.getDictId());
        item.setItemLabel(trim(request.getItemLabel()));
        item.setItemValue(trim(request.getItemValue()));
        item.setItemColor(trimToNull(request.getItemColor()));
        item.setItemIcon(trimToNull(request.getItemIcon()));
        item.setDescription(trimToNull(request.getDescription()));
        item.setSortOrder(request.getSortOrder());
        item.setStatus(request.getStatus());
        item.setIsDefault(request.getIsDefault());
        return item;
    }

    public static SysDictItem toDictItem(DictRequests.ItemUpdate request) {
        SysDictItem item = toDictItem((DictRequests.ItemSave) request);
        item.setId(request.getId());
        return item;
    }

    public static SysTenant toTenant(TenantRequests.Save request) {
        SysTenant tenant = new SysTenant();
        tenant.setTenantCode(trim(request.getTenantCode()));
        tenant.setTenantName(trim(request.getTenantName()));
        tenant.setContactName(trimToNull(request.getContactName()));
        tenant.setContactPhone(trimToNull(request.getContactPhone()));
        tenant.setContactEmail(trimToNull(request.getContactEmail()));
        tenant.setExpireTime(request.getExpireTime());
        tenant.setAccountCount(request.getAccountCount());
        tenant.setStatus(request.getStatus());
        tenant.setLogo(trimToNull(request.getLogo()));
        return tenant;
    }

    public static SysTenant toTenant(TenantRequests.Update request) {
        SysTenant tenant = toTenant((TenantRequests.Save) request);
        tenant.setId(request.getId());
        return tenant;
    }

    public static GenTable toGenTable(GenTableRequests.Update request) {
        GenTable table = new GenTable();
        table.setId(request.getId());
        table.setTableName(trim(request.getTableName()));
        table.setClassName(trim(request.getClassName()));
        table.setPackageName(trim(request.getPackageName()));
        table.setModuleName(trimToNull(request.getModuleName()));
        table.setBusinessName(trim(request.getBusinessName()));
        table.setFunctionName(trimToNull(request.getFunctionName()));
        table.setAuthor(trimToNull(request.getAuthor()));
        if (request.getColumns() != null) {
            List<GenTableColumn> columns = request.getColumns().stream()
                    .map(RequestConverters::toGenTableColumn)
                    .collect(Collectors.toList());
            table.setColumns(columns);
        }
        return table;
    }

    private static GenTableColumn toGenTableColumn(GenTableRequests.ColumnUpdate request) {
        GenTableColumn column = new GenTableColumn();
        column.setId(request.getId());
        column.setColumnComment(trimToNull(request.getColumnComment()));
        column.setJavaField(trim(request.getJavaField()));
        column.setJavaType(trim(request.getJavaType()));
        column.setIsRequired(request.getIsRequired());
        column.setIsInsert(request.getIsInsert());
        column.setIsEdit(request.getIsEdit());
        column.setIsList(request.getIsList());
        column.setIsQuery(request.getIsQuery());
        column.setQueryType(trim(request.getQueryType()));
        column.setHtmlType(trim(request.getHtmlType()));
        column.setDictType(trimToNull(request.getDictType()));
        column.setSortOrder(request.getSortOrder());
        return column;
    }

    private static SysUser toUserBase(String username, String password, String nickname, String email, String phone, Long deptId) {
        SysUser user = new SysUser();
        user.setUsername(trim(username));
        user.setPassword(password);
        user.setNickname(trimToNull(nickname));
        user.setEmail(trimToNull(email));
        user.setPhone(trimToNull(phone));
        user.setDeptId(deptId);
        return user;
    }

    public static String trim(String value) {
        return value == null ? null : value.trim();
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}