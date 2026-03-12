package com.kite.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kite.common.response.PageResult;
import com.kite.user.entity.SysTenant;
import com.kite.user.mapper.SysTenantMapper;
import lombok.RequiredArgsConstructor;
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
     * 新增租户
     */
    @Transactional(rollbackFor = Exception.class)
    public void save(SysTenant tenant) {
        tenantMapper.insert(tenant);
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
