package com.kite.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kite.common.response.PageResult;
import com.kite.user.entity.SysDept;
import com.kite.user.mapper.SysDeptMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 部门服务
 */
@Service
public class SysDeptService extends ServiceImpl<SysDeptMapper, SysDept> {
    
    /**
     * 分页查询部门
     */
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
    
    /**
     * 获取部门树
     */
    public List<SysDept> getDeptTree() {
        List<SysDept> allDepts = this.list(new LambdaQueryWrapper<SysDept>()
                .orderByAsc(SysDept::getSortOrder));
        
        return buildDeptTree(allDepts, 0L);
    }
    
    /**
     * 构建部门树
     */
    private List<SysDept> buildDeptTree(List<SysDept> allDepts, Long parentId) {
        List<SysDept> children = allDepts.stream()
                .filter(dept -> parentId.equals(dept.getParentId()))
                .collect(Collectors.toList());
        
        for (SysDept child : children) {
            List<SysDept> subChildren = buildDeptTree(allDepts, child.getId());
            child.setChildren(subChildren);
        }
        
        return children;
    }
    
    /**
     * 新增部门
     */
    public void addDept(SysDept dept) {
        if (dept.getParentId() == null) {
            dept.setParentId(0L);
        }
        this.save(dept);
    }
    
    /**
     * 更新部门
     */
    public void updateDept(SysDept dept) {
        this.updateById(dept);
    }
    
    /**
     * 删除部门
     */
    public void deleteDept(Long id) {
        // 检查是否有子部门
        long count = this.count(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getParentId, id));
        if (count > 0) {
            throw new RuntimeException("存在子部门，无法删除");
        }
        this.removeById(id);
    }
}
