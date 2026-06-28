package com.kite.cert.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.kite.cert.crypto.CryptoService;
import com.kite.cert.dto.request.DnsProviderRequests;
import com.kite.cert.dto.response.DnsProviderResponse;
import com.kite.cert.entity.CertDnsProvider;
import com.kite.cert.mapper.CertDnsProviderMapper;
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
 * DNS 服务商管理。
 */
@Service
@RequiredArgsConstructor
public class CertDnsProviderService extends ServiceImpl<CertDnsProviderMapper, CertDnsProvider> {

    private final CryptoService cryptoService;

    public PageResult<DnsProviderResponse> page(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<CertDnsProvider> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(CertDnsProvider::getName, keyword.trim());
        }
        wrapper.orderByDesc(CertDnsProvider::getId);
        IPage<CertDnsProvider> page = this.page(new Page<>(pageNum, pageSize), wrapper);
        List<DnsProviderResponse> records = page.getRecords().stream()
                .map(DnsProviderResponse::from).collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public List<DnsProviderResponse> listAll() {
        LambdaQueryWrapper<CertDnsProvider> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CertDnsProvider::getStatus, 1).orderByDesc(CertDnsProvider::getId);
        return list(wrapper).stream().map(DnsProviderResponse::from).collect(Collectors.toList());
    }

    public DnsProviderResponse getDetail(Long id) {
        return DnsProviderResponse.from(require(id));
    }

    public void add(DnsProviderRequests.Save req) {
        CertDnsProvider entity = new CertDnsProvider();
        applySave(entity, req);
        entity.setTenantId(CertTenantSupport.currentTenantId());
        save(entity);
    }

    public void update(Long id, DnsProviderRequests.Update req) {
        CertDnsProvider entity = require(id);
        applySave(entity, req);
        updateById(entity);
    }

    public void delete(Long id) {
        require(id);
        removeById(id);
    }

    /** 内部使用：返回解密后的凭证键值对。 */
    public Map<String, String> getCredential(Long id) {
        CertDnsProvider entity = require(id);
        String json = cryptoService.decrypt(entity.getCredential());
        if (!StringUtils.hasText(json)) {
            return Collections.emptyMap();
        }
        return JsonUtils.parseObject(json, new TypeReference<Map<String, String>>() {
        });
    }

    public CertDnsProvider require(Long id) {
        CertDnsProvider entity = getById(id);
        if (entity == null) {
            throw new BusinessException("DNS 服务商不存在");
        }
        return entity;
    }

    private void applySave(CertDnsProvider entity, DnsProviderRequests.Save req) {
        entity.setName(req.getName().trim());
        entity.setType(req.getType().trim().toLowerCase());
        if (req.getCredential() != null) {
            entity.setCredential(cryptoService.encrypt(JsonUtils.toJson(req.getCredential())));
        }
        entity.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        entity.setRemark(req.getRemark());
    }
}
