package com.kite.cert.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.kite.cert.crypto.CryptoService;
import com.kite.cert.deploy.DeployerFactory;
import com.kite.cert.dto.request.DeployTargetRequests;
import com.kite.cert.dto.response.DeployTargetResponse;
import com.kite.cert.entity.CertDeployTarget;
import com.kite.cert.mapper.CertDeployTargetMapper;
import com.kite.cert.support.CertTenantSupport;
import com.kite.common.exception.BusinessException;
import com.kite.common.response.PageResult;
import com.kite.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 部署目标管理。
 */
@Service
@RequiredArgsConstructor
public class CertDeployTargetService extends ServiceImpl<CertDeployTargetMapper, CertDeployTarget> {

    private final CryptoService cryptoService;
    private final DeployerFactory deployerFactory;

    public PageResult<DeployTargetResponse> page(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<CertDeployTarget> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(CertDeployTarget::getName, keyword.trim());
        }
        wrapper.orderByDesc(CertDeployTarget::getId);
        IPage<CertDeployTarget> page = this.page(new Page<>(pageNum, pageSize), wrapper);
        List<DeployTargetResponse> records = page.getRecords().stream()
                .map(DeployTargetResponse::from).collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public List<DeployTargetResponse> listAll() {
        LambdaQueryWrapper<CertDeployTarget> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CertDeployTarget::getEnabled, 1).orderByDesc(CertDeployTarget::getId);
        return list(wrapper).stream().map(DeployTargetResponse::from).collect(Collectors.toList());
    }

    public DeployTargetResponse getDetail(Long id) {
        return DeployTargetResponse.from(require(id));
    }

    public void add(DeployTargetRequests.Save req) {
        CertDeployTarget entity = new CertDeployTarget();
        applySave(entity, req);
        entity.setTenantId(CertTenantSupport.currentTenantId());
        save(entity);
    }

    public void update(Long id, DeployTargetRequests.Update req) {
        CertDeployTarget entity = require(id);
        applySave(entity, req);
        updateById(entity);
    }

    public void delete(Long id) {
        require(id);
        removeById(id);
    }

    /** 连通性测试。 */
    public void test(Long id) {
        CertDeployTarget entity = require(id);
        deployerFactory.get(entity.getType()).test(entity.getType(), getConfig(id));
    }

    /** 内部使用：返回解密后的配置键值对。 */
    public Map<String, String> getConfig(Long id) {
        CertDeployTarget entity = require(id);
        String json = cryptoService.decrypt(entity.getConfig());
        if (!StringUtils.hasText(json)) {
            return Collections.emptyMap();
        }
        return JsonUtils.parseObject(json, new TypeReference<Map<String, String>>() {
        });
    }

    public CertDeployTarget require(Long id) {
        CertDeployTarget entity = getById(id);
        if (entity == null) {
            throw new BusinessException("部署目标不存在");
        }
        return entity;
    }

    private void applySave(CertDeployTarget entity, DeployTargetRequests.Save req) {
        entity.setName(req.getName().trim());
        entity.setType(req.getType().trim().toLowerCase());
        if (req.getConfig() != null) {
            entity.setConfig(cryptoService.encrypt(JsonUtils.toJson(req.getConfig())));
        }
        entity.setEnabled(req.getEnabled() == null ? 1 : req.getEnabled());
        entity.setRemark(req.getRemark());
    }
}
