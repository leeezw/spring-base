package com.kite.cert.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kite.cert.config.CertProperties;
import com.kite.cert.crypto.CryptoService;
import com.kite.cert.dto.request.AcmeAccountRequests;
import com.kite.cert.dto.response.AcmeAccountResponse;
import com.kite.cert.entity.CertAcmeAccount;
import com.kite.cert.mapper.CertAcmeAccountMapper;
import com.kite.cert.support.CertTenantSupport;
import com.kite.common.exception.BusinessException;
import com.kite.common.response.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ACME 账户管理。
 */
@Service
@RequiredArgsConstructor
public class CertAcmeAccountService extends ServiceImpl<CertAcmeAccountMapper, CertAcmeAccount> {

    private final CryptoService cryptoService;
    private final CertProperties certProperties;

    public PageResult<AcmeAccountResponse> page(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<CertAcmeAccount> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(CertAcmeAccount::getName, keyword.trim())
                    .or().like(CertAcmeAccount::getEmail, keyword.trim());
        }
        wrapper.orderByDesc(CertAcmeAccount::getId);
        IPage<CertAcmeAccount> page = this.page(new Page<>(pageNum, pageSize), wrapper);
        List<AcmeAccountResponse> records = page.getRecords().stream()
                .map(AcmeAccountResponse::from).collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public List<AcmeAccountResponse> listAll() {
        LambdaQueryWrapper<CertAcmeAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CertAcmeAccount::getStatus, 1).orderByDesc(CertAcmeAccount::getId);
        return list(wrapper).stream().map(AcmeAccountResponse::from).collect(Collectors.toList());
    }

    public AcmeAccountResponse getDetail(Long id) {
        return AcmeAccountResponse.from(require(id));
    }

    public void add(AcmeAccountRequests.Save req) {
        CertAcmeAccount entity = new CertAcmeAccount();
        applySave(entity, req);
        entity.setTenantId(CertTenantSupport.currentTenantId());
        save(entity);
    }

    public void update(Long id, AcmeAccountRequests.Update req) {
        CertAcmeAccount entity = require(id);
        String oldDirectory = entity.getDirectoryUrl();
        applySave(entity, req);
        // CA / 目录变更则需重新注册账户
        if (!entity.getDirectoryUrl().equals(oldDirectory)) {
            entity.setAccountUrl(null);
            entity.setAccountKey(null);
        }
        updateById(entity);
    }

    public void delete(Long id) {
        require(id);
        removeById(id);
    }

    /** 内部使用：返回解密后的账户（供签发流程）。 */
    public CertAcmeAccount getDecrypted(Long id) {
        CertAcmeAccount entity = require(id);
        entity.setAccountKey(cryptoService.decrypt(entity.getAccountKey()));
        entity.setEabKid(cryptoService.decrypt(entity.getEabKid()));
        entity.setEabHmac(cryptoService.decrypt(entity.getEabHmac()));
        return entity;
    }

    /** 签发后回写账户注册信息（加密存储）。 */
    public void persistRegistration(Long id, String accountKeyPem, String accountUrl) {
        CertAcmeAccount update = new CertAcmeAccount();
        update.setId(id);
        update.setAccountKey(cryptoService.encrypt(accountKeyPem));
        update.setAccountUrl(accountUrl);
        updateById(update);
    }

    private void applySave(CertAcmeAccount entity, AcmeAccountRequests.Save req) {
        entity.setName(req.getName().trim());
        entity.setCaType(req.getCaType().trim().toLowerCase());
        entity.setEmail(req.getEmail().trim());
        entity.setDirectoryUrl(resolveDirectoryUrl(req));
        entity.setEabKid(cryptoService.encrypt(req.getEabKid()));
        entity.setEabHmac(cryptoService.encrypt(req.getEabHmac()));
        entity.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        entity.setRemark(req.getRemark());
    }

    private String resolveDirectoryUrl(AcmeAccountRequests.Save req) {
        if (StringUtils.hasText(req.getDirectoryUrl())) {
            return req.getDirectoryUrl().trim();
        }
        CertProperties.Acme acme = certProperties.getAcme();
        String caType = req.getCaType().trim().toLowerCase();
        if ("zerossl".equals(caType)) {
            return acme.getZerosslDirectory();
        }
        boolean staging = Boolean.TRUE.equals(req.getStaging());
        return staging ? acme.getLetsencryptStagingDirectory() : acme.getLetsencryptDirectory();
    }

    private CertAcmeAccount require(Long id) {
        CertAcmeAccount entity = getById(id);
        if (entity == null) {
            throw new BusinessException("ACME 账户不存在");
        }
        return entity;
    }
}
