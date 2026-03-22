package com.kite.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kite.common.exception.BusinessException;
import com.kite.common.response.PageResult;
import com.kite.user.entity.SysDept;
import com.kite.user.mapper.SysDeptMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysDeptService extends ServiceImpl<SysDeptMapper, SysDept> {

    public PageResult<SysDept> pageDepts(int pageNum, int pageSize, String deptName, Integer status) {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(deptName)) {
            wrapper.like(SysDept::getDeptName, deptName);
        }
        if (status != null) {
            wrapper.eq(SysDept::getStatus, status);
        }
        wrapper.orderByAsc(SysDept::getSortOrder);
        IPage<SysDept> page = this.page(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page);
    }

    public List<SysDept> getDeptTree() {
        List<SysDept> allDepts = this.list(new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getSortOrder));
        return buildDeptTree(allDepts, 0L);
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
        if (dept.getParentId() == null) {
            dept.setParentId(0L);
        }
        if (dept.getSortOrder() == null) {
            dept.setSortOrder(0);
        }
        dept.setDeptName(dept.getDeptName() == null ? null : dept.getDeptName().trim());
        dept.setPhone(trimToNull(dept.getPhone()));
        dept.setEmail(trimToNull(dept.getEmail()));
        validateParent(dept.getParentId(), null);
        this.save(dept);
    }

    public void updateDept(SysDept dept) {
        SysDept existDept = getById(dept.getId());
        if (existDept == null) {
            throw new BusinessException("部门不存在");
        }
        if (dept.getParentId() == null) {
            dept.setParentId(existDept.getParentId());
        }
        if (dept.getSortOrder() == null) {
            dept.setSortOrder(existDept.getSortOrder());
        }
        dept.setDeptName(dept.getDeptName() == null ? null : dept.getDeptName().trim());
        dept.setPhone(trimToNull(dept.getPhone()));
        dept.setEmail(trimToNull(dept.getEmail()));
        validateParent(dept.getParentId(), dept.getId());
        this.updateById(dept);
    }

    public void deleteDept(Long id) {
        long count = this.count(new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id));
        if (count > 0) {
            throw new BusinessException("存在子部门，无法删除");
        }
        this.removeById(id);
    }

    private void validateParent(Long parentId, Long currentId) {
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

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
