package com.kite.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kite.common.exception.BusinessException;
import com.kite.common.response.PageResult;
import com.kite.mybatis.context.TenantContext;
import com.kite.user.entity.SysDept;
import com.kite.user.entity.SysEmployee;
import com.kite.user.entity.SysEmployeeField;
import com.kite.user.entity.SysUser;
import com.kite.user.mapper.SysEmployeeFieldMapper;
import com.kite.user.mapper.SysEmployeeMapper;
import com.kite.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 员工档案服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysEmployeeService extends ServiceImpl<SysEmployeeMapper, SysEmployee> {

    private final SysEmployeeFieldMapper fieldMapper;
    private final SysUserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ══════════════════════════════════════════════════════════
    // 员工 CRUD
    // ══════════════════════════════════════════════════════════

    /**
     * 分页查询员工（含部门、岗位、账号关联名称）
     */
    public PageResult<SysEmployee> pageEmployees(int pageNum, int pageSize,
                                                  String keyword, Long deptId,
                                                  Integer status, Integer empType) {
        LambdaQueryWrapper<SysEmployee> wrapper = new LambdaQueryWrapper<>();
        // 关键词搜索：姓名或工号
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysEmployee::getEmpName, keyword)
                    .or().like(SysEmployee::getEmpCode, keyword));
        }
        if (deptId != null) {
            wrapper.eq(SysEmployee::getDeptId, deptId);
        }
        if (status != null) {
            wrapper.eq(SysEmployee::getStatus, status);
        }
        if (empType != null) {
            wrapper.eq(SysEmployee::getEmpType, empType);
        }
        wrapper.orderByAsc(SysEmployee::getEmpCode);

        // 联表分页（多租户由拦截器自动注入 WHERE e.tenant_id = ?）
        IPage<SysEmployee> page = baseMapper.selectPageWithDetail(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page);
    }

    /**
     * 查询员工详情（含关联名称）
     */
    public SysEmployee getDetail(Long id) {
        SysEmployee employee = baseMapper.selectDetailById(id);
        if (employee == null) {
            throw new BusinessException("员工不存在");
        }
        return employee;
    }

    /**
     * 新增员工
     */
    public void addEmployee(SysEmployee employee) {
        // 工号唯一性校验（同租户）
        checkEmpCodeUnique(employee.getEmpCode(), null);
        // 默认状态：在职
        if (employee.getStatus() == null) {
            employee.setStatus(1);
        }
        save(employee);
    }

    /**
     * 编辑员工
     */
    public void updateEmployee(SysEmployee employee) {
        SysEmployee exist = getById(employee.getId());
        if (exist == null) {
            throw new BusinessException("员工不存在");
        }
        // 工号变更时校验唯一性
        if (!exist.getEmpCode().equals(employee.getEmpCode())) {
            checkEmpCodeUnique(employee.getEmpCode(), employee.getId());
        }
        updateById(employee);
    }

    /**
     * 删除员工（逻辑删除，不解绑账号）
     */
    public void deleteEmployee(Long id) {
        SysEmployee exist = getById(id);
        if (exist == null) {
            throw new BusinessException("员工不存在");
        }
        removeById(id);
    }

    // ══════════════════════════════════════════════════════════
    // 账号绑定
    // ══════════════════════════════════════════════════════════

    /**
     * 开通账号：自动创建 SysUser 并绑定到员工
     *
     * @return { username, defaultPassword }
     */
    @Transactional
    public Map<String, String> createAccount(Long employeeId) {
        SysEmployee employee = getById(employeeId);
        if (employee == null) {
            throw new BusinessException("员工不存在");
        }
        if (employee.getUserId() != null) {
            throw new BusinessException("该员工已开通账号，如需更换请先解绑");
        }

        // 优先用手机号作为用户名，否则用工号
        String username = StringUtils.hasText(employee.getPhone())
                ? employee.getPhone() : employee.getEmpCode();

        // 检查用户名是否已被占用
        long count = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (count > 0) {
            // 回退到工号
            username = employee.getEmpCode();
            long count2 = userMapper.selectCount(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
            if (count2 > 0) {
                throw new BusinessException("用户名 " + username + " 已被占用，请手动绑定已有账号");
            }
        }

        // 默认密码：手机后6位，无手机则 123456
        String defaultPassword = StringUtils.hasText(employee.getPhone())
                && employee.getPhone().length() >= 6
                ? employee.getPhone().substring(employee.getPhone().length() - 6)
                : "123456";

        // 创建系统账号
        SysUser user = new SysUser();
        user.setTenantId(employee.getTenantId());
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(defaultPassword));
        user.setNickname(employee.getEmpName());
        user.setPhone(employee.getPhone());
        user.setEmail(employee.getEmail());
        user.setAvatar(employee.getAvatar());
        user.setDeptId(employee.getDeptId());
        user.setStatus(1);
        userMapper.insert(user);

        // 写回员工表
        SysEmployee update = new SysEmployee();
        update.setId(employeeId);
        update.setUserId(user.getId());
        updateById(update);

        log.info("员工 {} 开通账号成功，username={}", employee.getEmpCode(), username);

        Map<String, String> result = new HashMap<>();
        result.put("username", username);
        result.put("defaultPassword", defaultPassword);
        return result;
    }

    /**
     * 绑定已有账号
     *
     * @param employeeId 员工 ID
     * @param userId     要绑定的 SysUser ID
     */
    @Transactional
    public void bindAccount(Long employeeId, Long userId) {
        SysEmployee employee = getById(employeeId);
        if (employee == null) {
            throw new BusinessException("员工不存在");
        }
        if (employee.getUserId() != null) {
            throw new BusinessException("该员工已绑定账号，如需更换请先解绑");
        }

        // 校验目标账号存在
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("账号不存在");
        }

        // 校验目标账号未被其他员工绑定
        long bound = count(new LambdaQueryWrapper<SysEmployee>()
                .eq(SysEmployee::getUserId, userId));
        if (bound > 0) {
            throw new BusinessException("账号 " + user.getUsername() + " 已被其他员工绑定");
        }

        SysEmployee update = new SysEmployee();
        update.setId(employeeId);
        update.setUserId(userId);
        updateById(update);

        log.info("员工 {} 绑定账号 {} 成功", employee.getEmpCode(), user.getUsername());
    }

    /**
     * 解绑账号（仅清除 userId，不删除 SysUser）
     */
    public void unbindAccount(Long employeeId) {
        SysEmployee employee = getById(employeeId);
        if (employee == null) {
            throw new BusinessException("员工不存在");
        }
        if (employee.getUserId() == null) {
            throw new BusinessException("该员工未绑定账号");
        }

        // MyBatis Plus 更新 null 值需要用 UpdateWrapper
        lambdaUpdate()
                .eq(SysEmployee::getId, employeeId)
                .set(SysEmployee::getUserId, null)
                .update();

        log.info("员工 {} 解绑账号成功", employee.getEmpCode());
    }

    // ══════════════════════════════════════════════════════════
    // 自定义字段定义管理
    // ══════════════════════════════════════════════════════════

    /**
     * 查询当前租户的自定义字段定义列表
     */
    public List<SysEmployeeField> listFields() {
        return fieldMapper.selectList(
                new LambdaQueryWrapper<SysEmployeeField>()
                        .orderByAsc(SysEmployeeField::getSortOrder)
                        .orderByAsc(SysEmployeeField::getId));
    }

    /**
     * 新增自定义字段定义
     */
    public void addField(SysEmployeeField field) {
        // 同租户内 fieldKey 唯一
        long count = fieldMapper.selectCount(
                new LambdaQueryWrapper<SysEmployeeField>()
                        .eq(SysEmployeeField::getFieldKey, field.getFieldKey()));
        if (count > 0) {
            throw new BusinessException("字段 key [" + field.getFieldKey() + "] 已存在");
        }
        fieldMapper.insert(field);
    }

    /**
     * 编辑自定义字段定义
     */
    public void updateField(SysEmployeeField field) {
        SysEmployeeField exist = fieldMapper.selectById(field.getId());
        if (exist == null) {
            throw new BusinessException("字段定义不存在");
        }
        // fieldKey 不允许修改（已有数据依赖此 key）
        field.setFieldKey(null);
        fieldMapper.updateById(field);
    }

    /**
     * 删除自定义字段定义
     * 注意：不会清除已有员工 custom_fields 中对应的 key（保留历史数据）
     */
    public void deleteField(Long fieldId) {
        fieldMapper.deleteById(fieldId);
    }

    // ══════════════════════════════════════════════════════════
    // 私有工具方法
    // ══════════════════════════════════════════════════════════

    /**
     * 校验工号在当前租户内唯一
     *
     * @param empCode   工号
     * @param excludeId 编辑时排除自身 ID，新增传 null
     */
    private void checkEmpCodeUnique(String empCode, Long excludeId) {
        LambdaQueryWrapper<SysEmployee> wrapper = new LambdaQueryWrapper<SysEmployee>()
                .eq(SysEmployee::getEmpCode, empCode);
        if (excludeId != null) {
            wrapper.ne(SysEmployee::getId, excludeId);
        }
        long count = count(wrapper);
        if (count > 0) {
            throw new BusinessException("工号 [" + empCode + "] 已存在");
        }
    }
}
