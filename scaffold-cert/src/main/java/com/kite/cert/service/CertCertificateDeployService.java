package com.kite.cert.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kite.cert.entity.CertCertificateDeploy;
import com.kite.cert.mapper.CertCertificateDeployMapper;
import com.kite.cert.support.CertTenantSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 证书-部署目标 绑定关系服务。
 */
@Service
@RequiredArgsConstructor
public class CertCertificateDeployService extends ServiceImpl<CertCertificateDeployMapper, CertCertificateDeploy> {

    public List<CertCertificateDeploy> listByCertificate(Long certificateId) {
        return list(new LambdaQueryWrapper<CertCertificateDeploy>()
                .eq(CertCertificateDeploy::getCertificateId, certificateId));
    }

    public List<Long> listTargetIds(Long certificateId) {
        return listByCertificate(certificateId).stream()
                .map(CertCertificateDeploy::getDeployTargetId)
                .collect(Collectors.toList());
    }

    /** 覆盖式设置证书绑定的部署目标。 */
    @Transactional(rollbackFor = Exception.class)
    public void setBindings(Long certificateId, List<Long> targetIds) {
        remove(new LambdaQueryWrapper<CertCertificateDeploy>()
                .eq(CertCertificateDeploy::getCertificateId, certificateId));
        if (targetIds == null || targetIds.isEmpty()) {
            return;
        }
        Long tenantId = CertTenantSupport.currentTenantId();
        List<CertCertificateDeploy> rows = targetIds.stream().distinct().map(targetId -> {
            CertCertificateDeploy row = new CertCertificateDeploy();
            row.setTenantId(tenantId);
            row.setCertificateId(certificateId);
            row.setDeployTargetId(targetId);
            return row;
        }).collect(Collectors.toList());
        saveBatch(rows);
    }

    public void updateResult(Long certificateId, Long targetId, boolean success, String message) {
        CertCertificateDeploy row = getOne(new LambdaQueryWrapper<CertCertificateDeploy>()
                .eq(CertCertificateDeploy::getCertificateId, certificateId)
                .eq(CertCertificateDeploy::getDeployTargetId, targetId), false);
        if (row == null) {
            return;
        }
        row.setLastDeployStatus(success ? "SUCCESS" : "FAILED");
        row.setLastDeployTime(LocalDateTime.now());
        row.setLastMessage(message != null && message.length() > 1000 ? message.substring(0, 1000) : message);
        updateById(row);
    }
}
