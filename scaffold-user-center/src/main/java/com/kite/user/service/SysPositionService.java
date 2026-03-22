package com.kite.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kite.common.exception.BusinessException;
import com.kite.user.entity.SysEmployee;
import com.kite.user.entity.SysPosition;
import com.kite.user.entity.SysPost;
import com.kite.user.mapper.SysEmployeeMapper;
import com.kite.user.mapper.SysPositionMapper;
import com.kite.user.mapper.SysPostMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SysPositionService {

    private final SysPositionMapper positionMapper;
    private final SysPostMapper postMapper;
    private final SysEmployeeMapper employeeMapper;

    public SysPositionService(SysPositionMapper positionMapper, SysPostMapper postMapper, SysEmployeeMapper employeeMapper) {
        this.positionMapper = positionMapper;
        this.postMapper = postMapper;
        this.employeeMapper = employeeMapper;
    }

    public List<SysPosition> list(Long postId, Integer status) {
        LambdaQueryWrapper<SysPosition> wrapper = new LambdaQueryWrapper<>();
        if (postId != null) {
            wrapper.eq(SysPosition::getPostId, postId);
        }
        if (status != null) {
            wrapper.eq(SysPosition::getStatus, status);
        }
        wrapper.orderByAsc(SysPosition::getSortOrder).orderByAsc(SysPosition::getId);
        return positionMapper.selectList(wrapper);
    }

    @Transactional
    public void create(SysPosition position) {
        SysPost post = postMapper.selectById(position.getPostId());
        if (post == null) {
            throw new BusinessException("岗位不存在");
        }
        checkCodeUnique(position.getPositionCode(), null);
        positionMapper.insert(position);
    }

    @Transactional
    public void update(SysPosition position) {
        checkCodeUnique(position.getPositionCode(), position.getId());
        positionMapper.updateById(position);
    }

    @Transactional
    public void delete(Long id) {
        long count = employeeMapper.selectCount(new LambdaQueryWrapper<SysEmployee>()
                .eq(SysEmployee::getPositionId, id));
        if (count > 0) {
            throw new BusinessException("该职位下有员工，无法删除");
        }
        positionMapper.deleteById(id);
    }

    private void checkCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<SysPosition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPosition::getPositionCode, code);
        if (excludeId != null) {
            wrapper.ne(SysPosition::getId, excludeId);
        }
        if (positionMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("职位编码已存在");
        }
    }
}
