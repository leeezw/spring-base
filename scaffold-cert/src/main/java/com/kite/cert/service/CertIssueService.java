package com.kite.cert.service;

import com.kite.cert.acme.AcmeClientService;
import com.kite.cert.acme.AcmeIssueRequest;
import com.kite.cert.acme.AcmeIssueResult;
import com.kite.cert.acme.ChallengeSolver;
import com.kite.cert.acme.IssuedCertificate;
import com.kite.cert.crypto.CryptoService;
import com.kite.cert.dns.DnsChallenge;
import com.kite.cert.dns.DnsProvider;
import com.kite.cert.dns.DnsProviderFactory;
import com.kite.cert.entity.CertAcmeAccount;
import com.kite.cert.entity.CertCertificate;
import com.kite.cert.entity.CertDnsProvider;
import com.kite.cert.mapper.CertCertificateMapper;
import com.kite.cert.util.DomainUtils;
import com.kite.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 证书签发/续期核心编排：ACME 签发 → 持久化 → 自动部署 → 通知 → 任务日志。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CertIssueService {

    private final CertCertificateMapper certificateMapper;
    private final CertAcmeAccountService acmeAccountService;
    private final CertDnsProviderService dnsProviderService;
    private final CertDeployService deployService;
    private final AcmeClientService acmeClientService;
    private final DnsProviderFactory dnsProviderFactory;
    private final CryptoService cryptoService;
    private final CertTaskLogService taskLogService;
    private final com.kite.cert.notify.NotificationService notificationService;

    /**
     * 执行签发或续期。
     *
     * @param certificate   证书记录
     * @param type          ISSUE / RENEW
     * @param triggerSource MANUAL / SCHEDULED
     */
    public void issueOrRenew(CertCertificate certificate, String type, String triggerSource) {
        var task = taskLogService.start(certificate.getId(), type, triggerSource);
        StringBuilder detail = new StringBuilder();
        try {
            markStatus(certificate, "ISSUING", null);

            CertAcmeAccount account = acmeAccountService.getDecrypted(certificate.getAcmeAccountId());
            CertDnsProvider dnsEntity = dnsProviderService.require(certificate.getDnsProviderId());
            DnsProvider provider = dnsProviderFactory.get(dnsEntity.getType());
            Map<String, String> credential = dnsProviderService.getCredential(certificate.getDnsProviderId());

            ChallengeSolver solver = buildSolver(provider, credential);
            List<String> domains = collectDomains(certificate);
            detail.append("域名：").append(String.join(", ", domains)).append("\n");

            AcmeIssueRequest req = AcmeIssueRequest.builder()
                    .directoryUrl(account.getDirectoryUrl())
                    .email(account.getEmail())
                    .caType(account.getCaType())
                    .accountKeyPem(account.getAccountKey())
                    .accountUrl(account.getAccountUrl())
                    .eabKid(account.getEabKid())
                    .eabHmac(account.getEabHmac())
                    .domains(domains)
                    .keyAlgo(certificate.getKeyAlgo())
                    .solver(solver)
                    .build();

            AcmeIssueResult result = acmeClientService.issue(req);

            // 新建账户则回写注册信息
            if (account.getAccountUrl() == null && result.accountUrl() != null) {
                acmeAccountService.persistRegistration(account.getId(), result.accountKeyPem(), result.accountUrl());
            }

            persistIssued(certificate, result.certificate());
            detail.append("签发成功，有效期至：").append(certificate.getNotAfter()).append("\n");

            // 自动部署
            String deployMsg;
            boolean deployOk = true;
            try {
                deployMsg = deployService.deployToBoundTargets(certificate);
            } catch (Exception de) {
                deployOk = false;
                deployMsg = de.getMessage();
            }
            detail.append("部署：\n").append(deployMsg);

            taskLogService.finish(task, deployOk, deployOk ? "签发并部署成功" : "签发成功，但部署存在失败", detail.toString());
            notificationService.send(
                    (deployOk ? "证书签发成功：" : "证书已签发但部署失败：") + certificate.getPrimaryDomain(),
                    detail.toString());
        } catch (Exception e) {
            markStatus(certificate, "FAILED", e.getMessage());
            detail.append("错误：").append(e.getMessage());
            taskLogService.finish(task, false, e.getMessage(), detail.toString());
            notificationService.send("证书" + ("RENEW".equals(type) ? "续期" : "签发") + "失败："
                    + certificate.getPrimaryDomain(), detail.toString());
            log.error("[cert] 证书 {} {} 失败", certificate.getPrimaryDomain(), type, e);
            throw e instanceof BusinessException be ? be : new BusinessException("证书处理失败：" + e.getMessage());
        }
    }

    private ChallengeSolver buildSolver(DnsProvider provider, Map<String, String> credential) {
        return new ChallengeSolver() {
            @Override
            public void prepare(String domain, String recordName, String txtValue) {
                provider.addTxtRecord(toChallenge(recordName, txtValue), credential);
            }

            @Override
            public void cleanup(String domain, String recordName, String txtValue) {
                try {
                    provider.removeTxtRecord(toChallenge(recordName, txtValue), credential);
                } catch (Exception ignore) {
                    // 清理失败不影响主流程
                }
            }

            private DnsChallenge toChallenge(String recordName, String txtValue) {
                String baseDomain = DomainUtils.registrableDomain(recordName);
                String host = DomainUtils.hostRelativeTo(recordName, baseDomain);
                return new DnsChallenge(recordName, txtValue, baseDomain, host);
            }
        };
    }

    private List<String> collectDomains(CertCertificate certificate) {
        Set<String> domains = new LinkedHashSet<>();
        domains.add(certificate.getPrimaryDomain());
        if (certificate.getSanDomains() != null) {
            domains.addAll(certificate.getSanDomains());
        }
        return new ArrayList<>(domains);
    }

    private void persistIssued(CertCertificate certificate, IssuedCertificate issued) {
        certificate.setCertPem(cryptoService.encrypt(issued.certPem()));
        certificate.setChainPem(cryptoService.encrypt(issued.chainPem()));
        certificate.setKeyPem(cryptoService.encrypt(issued.keyPem()));
        certificate.setNotBefore(issued.notBefore());
        certificate.setNotAfter(issued.notAfter());
        certificate.setSerial(issued.serial());
        certificate.setIssuer(issued.issuer());
        certificate.setStatus("ACTIVE");
        certificate.setLastRenewAt(LocalDateTime.now());
        certificate.setLastMessage("签发成功");
        certificateMapper.updateById(certificate);
    }

    private void markStatus(CertCertificate certificate, String status, String message) {
        certificate.setStatus(status);
        if (message != null) {
            certificate.setLastMessage(message.length() > 1000 ? message.substring(0, 1000) : message);
        }
        certificateMapper.updateById(certificate);
    }
}
