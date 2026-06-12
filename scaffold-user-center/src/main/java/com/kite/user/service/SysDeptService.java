package com.kite.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kite.common.exception.BusinessException;
import com.kite.common.response.PageResult;
import com.kite.mybatis.context.TenantContext;
import com.kite.user.entity.SysDept;
import com.kite.user.mapper.SysDeptMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SysDeptService extends ServiceImpl<SysDeptMapper, SysDept> {

    public PageResult<SysDept> pageDepts(int pageNum, int pageSize, String deptName, Long storeId, Integer status) {
        String trimmedName = StringUtils.hasText(deptName) ? deptName.trim() : null;
        IPage<SysDept> page = baseMapper.selectPageWithStore(new Page<>(pageNum, pageSize), trimmedName, storeId, status);
        return PageResult.of(page);
    }

    public List<SysDept> getDeptTree(Long storeId) {
        List<SysDept> allDepts = baseMapper.selectTreeWithStore(storeId);
        return buildDeptTree(allDepts, 0L);
    }

    public List<SysDept> listDepts(Long storeId, Integer status) {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        if (storeId != null) {
            wrapper.eq(SysDept::getStoreId, storeId);
        }
        if (status != null) {
            wrapper.eq(SysDept::getStatus, status);
        }
        wrapper.orderByAsc(SysDept::getSortOrder).orderByAsc(SysDept::getId);
        return list(wrapper);
    }

    public SysDept getDeptDetail(Long id) {
        SysDept dept = baseMapper.selectDetailById(id);
        if (dept == null) {
            throw new BusinessException("部门不存在");
        }
        return dept;
    }

    public List<Map<String, Object>> listStoreOptions() {
        return baseMapper.selectStoreOptions();
    }

    private List<SysDept> buildDeptTree(List<SysDept> allDepts, Long parentId) {
        List<SysDept> children = allDepts.stream()
                .filter(dept -> parentId.equals(dept.getParentId()))
                .collect(Collectors.toList());
        for (SysDept child : children) {
            child.setChildren(buildDeptTree(allDepts, child.getId()));
        }
        return children;
    }

    public void addDept(SysDept dept) {
        normalizeDept(dept);
        if (dept.getParentId() == null) {
            dept.setParentId(0L);
        }
        if (dept.getSortOrder() == null) {
            dept.setSortOrder(0);
        }
        dept.setTenantId(resolveTenantId(dept.getTenantId()));
        validateStore(dept.getStoreId());
        validateParent(dept.getParentId(), null, dept.getStoreId());
        this.save(dept);
    }

    public void updateDept(SysDept dept) {
        SysDept existDept = getById(dept.getId());
        if (existDept == null) {
            throw new BusinessException("部门不存在");
        }
        normalizeDept(dept);
        if (dept.getParentId() == null) {
            dept.setParentId(existDept.getParentId());
        }
        if (dept.getSortOrder() == null) {
            dept.setSortOrder(existDept.getSortOrder());
        }
        dept.setTenantId(existDept.getTenantId());
        validateStore(dept.getStoreId());
        if (!dept.getStoreId().equals(existDept.getStoreId())) {
            long childCount = count(new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, dept.getId()));
            if (childCount > 0) {
                throw new BusinessException("存在子部门，不能变更所属门店");
            }
        }
        validateParent(dept.getParentId(), dept.getId(), dept.getStoreId());
        this.updateById(dept);
    }

    public void deleteDept(Long id) {
        long count = this.count(new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id));
        if (count > 0) {
            throw new BusinessException("存在子部门，无法删除");
        }
        this.removeById(id);
    }

    private void validateStore(Long storeId) {
        if (storeId == null || storeId <= 0) {
            throw new BusinessException("所属门店不能为空");
        }
        Long count = baseMapper.countStoreById(storeId);
        if (count == null || count == 0) {
            throw new BusinessException("所属门店不存在");
        }
    }

    private void validateParent(Long parentId, Long currentId, Long storeId) {
        if (parentId == null || parentId == 0L) {
            return;
        }
        if (currentId != null && parentId.equals(currentId)) {
            throw new BusinessException("上级部门不能选择自己");
        }
        SysDept parent = getById(parentId);
        if (parent == null) {
            throw new BusinessException("上级部门不存在");
        }
        if (parent.getStoreId() == null || !parent.getStoreId().equals(storeId)) {
            throw new BusinessException("上级部门必须属于同一门店");
        }
        Long cursor = parent.getParentId();
        while (cursor != null && cursor != 0L) {
            if (currentId != null && cursor.equals(currentId)) {
                throw new BusinessException("上级部门不能选择当前部门的子节点");
            }
            SysDept current = getById(cursor);
            if (current == null) {
                break;
            }
            cursor = current.getParentId();
        }
    }

    private void normalizeDept(SysDept dept) {
        dept.setDeptName(dept.getDeptName() == null ? null : dept.getDeptName().trim());
        dept.setPhone(trimToNull(dept.getPhone()));
        dept.setEmail(trimToNull(dept.getEmail()));
    }

    private Long resolveTenantId(Long tenantId) {
        if (tenantId != null) {
            return tenantId;
        }
        Long currentTenantId = TenantContext.getTenantId();
        return currentTenantId != null ? currentTenantId : 1L;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
