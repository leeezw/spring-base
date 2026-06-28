package com.kite.cert.acme;

import com.kite.cert.config.CertProperties;
import com.kite.cert.util.DomainUtils;
import com.kite.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shredzone.acme4j.Account;
import org.shredzone.acme4j.AccountBuilder;
import org.shredzone.acme4j.Authorization;
import org.shredzone.acme4j.Certificate;
import org.shredzone.acme4j.Login;
import org.shredzone.acme4j.Order;
import org.shredzone.acme4j.Session;
import org.shredzone.acme4j.Status;
import org.shredzone.acme4j.challenge.Dns01Challenge;
import org.shredzone.acme4j.util.KeyPairUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ACME 协议客户端（基于 acme4j），负责账户管理与证书签发的底层交互。
 *
 * <p>DNS-01 验证的具体写入/清理通过 {@link ChallengeSolver} 回调注入，本类不关心 DNS 服务商细节。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AcmeClientService {

    private final CertProperties certProperties;

    public AcmeIssueResult issue(AcmeIssueRequest req) {
        try {
            return doIssue(req);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[cert][acme] 签发失败", e);
            throw new BusinessException("证书签发失败：" + e.getMessage());
        }
    }

    private AcmeIssueResult doIssue(AcmeIssueRequest req) throws Exception {
        Session session = new Session(req.getDirectoryUrl());

        KeyPair accountKeyPair = StringUtils.hasText(req.getAccountKeyPem())
                ? KeyPairUtils.readKeyPair(new StringReader(req.getAccountKeyPem()))
                : KeyPairUtils.createKeyPair(2048);

        String resultAccountKeyPem = req.getAccountKeyPem();
        String resultAccountUrl = req.getAccountUrl();

        Account account;
        if (StringUtils.hasText(req.getAccountUrl()) && StringUtils.hasText(req.getAccountKeyPem())) {
            Login login = session.login(URI.create(req.getAccountUrl()).toURL(), accountKeyPair);
            account = login.getAccount();
        } else {
            AccountBuilder builder = new AccountBuilder()
                    .addEmail(req.getEmail())
                    .agreeToTermsOfService()
                    .useKeyPair(accountKeyPair);
            if (StringUtils.hasText(req.getEabKid()) && StringUtils.hasText(req.getEabHmac())) {
                builder.withKeyIdentifier(req.getEabKid(), req.getEabHmac());
            }
            account = builder.create(session);
            resultAccountUrl = account.getLocation().toString();
            resultAccountKeyPem = writeKeyPair(accountKeyPair);
            log.info("[cert][acme] 新建 ACME 账户：{}", resultAccountUrl);
        }

        Order order = account.newOrder().domains(req.getDomains()).create();

        // 第一步：写入所有 TXT 记录
        List<PreparedChallenge> prepared = new ArrayList<>();
        try {
            for (Authorization auth : order.getAuthorizations()) {
                if (auth.getStatus() == Status.VALID) {
                    continue; // 已验证（账户级缓存）
                }
                String domain = auth.getIdentifier().getDomain();
                Dns01Challenge challenge = auth.findChallenge(Dns01Challenge.class)
                        .orElseThrow(() -> new BusinessException("域名 " + domain + " 不支持 DNS-01 验证"));
                String recordName = DomainUtils.acmeRecordName(domain);
                String txt = challenge.getDigest();
                req.getSolver().prepare(domain, recordName, txt);
                prepared.add(new PreparedChallenge(auth, challenge, domain, recordName, txt));
            }

            // 等待 DNS 传播
            if (!prepared.isEmpty()) {
                sleep(certProperties.getAcme().getDnsPropagationSeconds() * 1000L);
            }

            // 第二步：触发校验并轮询
            for (PreparedChallenge pc : prepared) {
                pc.challenge.trigger();
                waitForValid(pc.authorization);
            }
        } finally {
            // 清理 TXT 记录
            for (PreparedChallenge pc : prepared) {
                req.getSolver().cleanup(pc.domain, pc.recordName, pc.txtValue);
            }
        }

        // 第三步：生成域名密钥并 finalize
        KeyPair domainKeyPair = createDomainKeyPair(req.getKeyAlgo());
        order.execute(domainKeyPair);
        waitForOrderValid(order);

        Certificate certificate = order.getCertificate();
        if (certificate == null) {
            throw new BusinessException("签发完成但未取得证书");
        }
        X509Certificate leaf = certificate.getCertificate();

        String certPem = PemUtils.toPem(leaf);
        String chainPem = buildChainPem(certificate);
        String keyPem = writeKeyPair(domainKeyPair);

        IssuedCertificate issued = new IssuedCertificate(
                certPem, chainPem, keyPem,
                toLocalDateTime(leaf.getNotBefore().toInstant()),
                toLocalDateTime(leaf.getNotAfter().toInstant()),
                leaf.getSerialNumber().toString(16),
                leaf.getIssuerX500Principal().getName());

        return new AcmeIssueResult(issued, resultAccountKeyPem, resultAccountUrl);
    }

    private void waitForValid(Authorization auth) throws Exception {
        long deadline = System.currentTimeMillis() + certProperties.getAcme().getChallengeTimeoutSeconds() * 1000L;
        while (auth.getStatus() != Status.VALID) {
            if (auth.getStatus() == Status.INVALID) {
                String detail = auth.getChallenges().stream()
                        .map(c -> c.getError().map(Object::toString).orElse(""))
                        .reduce("", (a, b) -> a + b);
                throw new BusinessException("域名验证失败：" + auth.getIdentifier().getDomain() + " " + detail);
            }
            if (System.currentTimeMillis() > deadline) {
                throw new BusinessException("域名验证超时：" + auth.getIdentifier().getDomain());
            }
            Optional<Instant> retry = auth.fetch();
            sleep(retryMillis(retry));
        }
    }

    private void waitForOrderValid(Order order) throws Exception {
        long deadline = System.currentTimeMillis() + certProperties.getAcme().getChallengeTimeoutSeconds() * 1000L;
        while (order.getStatus() != Status.VALID) {
            if (order.getStatus() == Status.INVALID) {
                throw new BusinessException("证书 finalize 失败：" + order.getError().map(Object::toString).orElse(""));
            }
            if (System.currentTimeMillis() > deadline) {
                throw new BusinessException("证书 finalize 超时");
            }
            Optional<Instant> retry = order.fetch();
            sleep(retryMillis(retry));
        }
    }

    private long retryMillis(Optional<Instant> retry) {
        return retry.map(instant -> Math.max(1000L, instant.toEpochMilli() - System.currentTimeMillis()))
                .orElse(3000L);
    }

    private KeyPair createDomainKeyPair(String keyAlgo) {
        if ("EC256".equalsIgnoreCase(keyAlgo)) {
            return KeyPairUtils.createECKeyPair("secp256r1");
        }
        return KeyPairUtils.createKeyPair(2048);
    }

    private String writeKeyPair(KeyPair keyPair) throws Exception {
        StringWriter sw = new StringWriter();
        KeyPairUtils.writeKeyPair(keyPair, sw);
        return sw.toString();
    }

    private String buildChainPem(Certificate certificate) {
        StringBuilder sb = new StringBuilder();
        for (X509Certificate cert : certificate.getCertificateChain()) {
            sb.append(PemUtils.toPem(cert));
        }
        return sb.toString();
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("签发过程被中断");
        }
    }

    private record PreparedChallenge(Authorization authorization, Dns01Challenge challenge,
                                     String domain, String recordName, String txtValue) {
    }
}
