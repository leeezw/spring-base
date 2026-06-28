package com.kite.cert.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kite.cert.config.CertProperties;
import com.kite.cert.crypto.CryptoService;
import com.kite.cert.dto.request.CertificateRequests;
import com.kite.cert.dto.response.CertificateResponse;
import com.kite.cert.dto.response.DashboardResponse;
import com.kite.cert.entity.CertCertificate;
import com.kite.cert.mapper.CertCertificateMapper;
import com.kite.cert.support.CertTenantSupport;
import com.kite.common.exception.BusinessException;
import com.kite.common.response.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 证书管理：CRUD + 签发/续期/部署/下载 编排入口 + 概览统计。
 */
@Service
@RequiredArgsConstructor
public class CertCertificateService extends ServiceImpl<CertCertificateMapper, CertCertificate> {

    private final CertIssueService issueService;
    private final CertDeployService deployService;
    private final CertCertificateDeployService certificateDeployService;
    private final CryptoService cryptoService;
    private final CertProperties certProperties;

    public PageResult<CertificateResponse> page(int pageNum, int pageSize, String keyword, String status) {
        LambdaQueryWrapper<CertCertificate> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(CertCertificate::getPrimaryDomain, keyword.trim());
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(CertCertificate::getStatus, status);
        }
        wrapper.orderByDesc(CertCertificate::getId);
        IPage<CertCertificate> page = this.page(new Page<>(pageNum, pageSize), wrapper);
        List<CertificateResponse> records = page.getRecords().stream()
                .map(CertificateResponse::from).collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public CertificateResponse getDetail(Long id) {
        CertificateResponse response = CertificateResponse.from(require(id));
        response.setDeployTargetIds(certificateDeployService.listTargetIds(id));
        return response;
    }

    /** 新建证书记录并立即签发。 */
    @Transactional(rollbackFor = Exception.class)
    public Long createAndIssue(CertificateRequests.Issue req) {
        List<String> domains = req.getDomains().stream().map(String::trim)
                .filter(StringUtils::hasText).distinct().collect(Collectors.toList());
        if (domains.isEmpty()) {
            throw new BusinessException("域名不能为空");
        }
        CertCertificate cert = new CertCertificate();
        cert.setTenantId(CertTenantSupport.currentTenantId());
        cert.setPrimaryDomain(domains.get(0));
        cert.setSanDomains(domains.size() > 1 ? new ArrayList<>(domains.subList(1, domains.size())) : new ArrayList<>());
        cert.setChallengeType("dns-01");
        cert.setAcmeAccountId(req.getAcmeAccountId());
        cert.setDnsProviderId(req.getDnsProviderId());
        cert.setKeyAlgo(StringUtils.hasText(req.getKeyAlgo()) ? req.getKeyAlgo() : "RSA2048");
        cert.setStatus("PENDING");
        cert.setAutoRenew(req.getAutoRenew() == null ? 1 : req.getAutoRenew());
        cert.setRenewBeforeDays(req.getRenewBeforeDays() == null ? certProperties.getRenew().getRenewBeforeDays() : req.getRenewBeforeDays());
        save(cert);
        certificateDeployService.setBindings(cert.getId(), req.getDeployTargetIds());

        // 立即签发（在事务提交后由调用方感知；签发内部使用独立更新）
        issueService.issueOrRenew(cert, "ISSUE", "MANUAL");
        return cert.getId();
    }

    public void renew(Long id) {
        issueService.issueOrRenew(require(id), "RENEW", "MANUAL");
    }

    public String deployNow(Long id) {
        CertCertificate cert = require(id);
        if (!"ACTIVE".equals(cert.getStatus()) && cert.getCertPem() == null) {
            throw new BusinessException("证书尚未签发，无法部署");
        }
        return deployService.deployToBoundTargets(cert);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        require(id);
        certificateDeployService.setBindings(id, null);
        removeById(id);
    }

    public void updateBindings(Long id, List<Long> targetIds) {
        require(id);
        certificateDeployService.setBindings(id, targetIds);
    }

    /** 导出 PEM（解密）。返回 fullchain/privkey/cert 三项。 */
    public Map<String, String> exportPem(Long id) {
        CertCertificate cert = require(id);
        if (cert.getChainPem() == null) {
            throw new BusinessException("证书尚未签发");
        }
        Map<String, String> pem = new LinkedHashMap<>();
        pem.put("fullchain", cryptoService.decrypt(cert.getChainPem()));
        pem.put("privkey", cryptoService.decrypt(cert.getKeyPem()));
        pem.put("cert", cryptoService.decrypt(cert.getCertPem()));
        return pem;
    }

    public DashboardResponse dashboard() {
        int warnDays = certProperties.getRenew().getExpiryWarnDays();
        LocalDateTime warnLine = LocalDateTime.now().plusDays(warnDays);

        DashboardResponse resp = new DashboardResponse();
        resp.setTotal(count());
        resp.setActive(count(new LambdaQueryWrapper<CertCertificate>().eq(CertCertificate::getStatus, "ACTIVE")));
        resp.setFailed(count(new LambdaQueryWrapper<CertCertificate>().eq(CertCertificate::getStatus, "FAILED")));

        List<CertCertificate> expiring = list(new LambdaQueryWrapper<CertCertificate>()
                .in(CertCertificate::getStatus, List.of("ACTIVE", "EXPIRING"))
                .isNotNull(CertCertificate::getNotAfter)
                .le(CertCertificate::getNotAfter, warnLine)
                .orderByAsc(CertCertificate::getNotAfter));
        resp.setExpiring(expiring.size());
        resp.setExpiringList(expiring.stream().map(CertificateResponse::from)
                .sorted(Comparator.comparing(c -> c.getDaysRemaining() == null ? Long.MAX_VALUE : c.getDaysRemaining()))
                .collect(Collectors.toList()));
        return resp;
    }

    public CertCertificate require(Long id) {
        CertCertificate cert = getById(id);
        if (cert == null) {
            throw new BusinessException("证书不存在");
        }
        return cert;
    }
}
