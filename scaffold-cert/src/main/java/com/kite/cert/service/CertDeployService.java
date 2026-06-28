package com.kite.cert.service;

import com.kite.cert.crypto.CryptoService;
import com.kite.cert.deploy.DeployContext;
import com.kite.cert.deploy.DeployerFactory;
import com.kite.cert.entity.CertCertificate;
import com.kite.cert.entity.CertCertificateDeploy;
import com.kite.cert.entity.CertDeployTarget;
import com.kite.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 证书部署编排：把证书推送到其绑定的所有启用部署目标。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CertDeployService {

    private final CertCertificateDeployService certificateDeployService;
    private final CertDeployTargetService deployTargetService;
    private final DeployerFactory deployerFactory;
    private final CryptoService cryptoService;

    /**
     * 部署证书到其绑定的所有目标。
     *
     * @return 部署结果摘要（每个目标一行）
     */
    public String deployToBoundTargets(CertCertificate certificate) {
        List<CertCertificateDeploy> bindings = certificateDeployService.listByCertificate(certificate.getId());
        if (bindings.isEmpty()) {
            return "未绑定部署目标，跳过部署";
        }
        List<String> results = new ArrayList<>();
        int failed = 0;
        for (CertCertificateDeploy binding : bindings) {
            Long targetId = binding.getDeployTargetId();
            CertDeployTarget target = deployTargetService.getById(targetId);
            if (target == null || !Integer.valueOf(1).equals(target.getEnabled())) {
                results.add("目标#" + targetId + " 不存在或已停用，跳过");
                continue;
            }
            try {
                deployOne(certificate, target);
                certificateDeployService.updateResult(certificate.getId(), targetId, true, "部署成功");
                results.add(target.getName() + "：成功");
            } catch (Exception e) {
                failed++;
                certificateDeployService.updateResult(certificate.getId(), targetId, false, e.getMessage());
                results.add(target.getName() + "：失败 - " + e.getMessage());
                log.warn("[cert][deploy] 证书 {} 部署到 {} 失败", certificate.getPrimaryDomain(), target.getName(), e);
            }
        }
        String summary = String.join("\n", results);
        if (failed > 0) {
            throw new BusinessException("部分部署目标失败：\n" + summary);
        }
        return summary;
    }

    private void deployOne(CertCertificate certificate, CertDeployTarget target) {
        DeployContext context = DeployContext.builder()
                .type(target.getType())
                .primaryDomain(certificate.getPrimaryDomain())
                .certPem(cryptoService.decrypt(certificate.getCertPem()))
                .chainPem(cryptoService.decrypt(certificate.getChainPem()))
                .keyPem(cryptoService.decrypt(certificate.getKeyPem()))
                .config(deployTargetService.getConfig(target.getId()))
                .build();
        deployerFactory.get(target.getType()).deploy(context);
    }
}
