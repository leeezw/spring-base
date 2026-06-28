package com.kite.cert.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kite.cert.config.CertProperties;
import com.kite.cert.entity.CertCertificate;
import com.kite.cert.notify.NotificationService;
import com.kite.cert.service.CertCertificateService;
import com.kite.cert.service.CertIssueService;
import com.kite.mybatis.context.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 证书自动续期定时任务：每日扫描，临期则续期；其余临期证书标记并预警。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CertRenewScheduler {

    private final CertCertificateService certificateService;
    private final CertIssueService issueService;
    private final NotificationService notificationService;
    private final CertProperties certProperties;

    @Scheduled(cron = "${cert.renew.cron:0 0 3 * * ?}")
    public void scan() {
        if (!certProperties.getRenew().isEnabled()) {
            return;
        }
        log.info("[cert][scheduler] 开始扫描证书续期...");
        TenantContext.setIgnore(true);
        try {
            List<CertCertificate> candidates = certificateService.list(new LambdaQueryWrapper<CertCertificate>()
                    .in(CertCertificate::getStatus, List.of("ACTIVE", "EXPIRING"))
                    .isNotNull(CertCertificate::getNotAfter));
            int renewed = 0;
            int warned = 0;
            for (CertCertificate cert : candidates) {
                long daysRemaining = Duration.between(LocalDateTime.now(), cert.getNotAfter()).toDays();
                int renewBefore = cert.getRenewBeforeDays() == null
                        ? certProperties.getRenew().getRenewBeforeDays() : cert.getRenewBeforeDays();

                if (Integer.valueOf(1).equals(cert.getAutoRenew()) && daysRemaining <= renewBefore) {
                    if (renewOne(cert)) {
                        renewed++;
                    }
                } else if (daysRemaining <= certProperties.getRenew().getExpiryWarnDays()
                        && "ACTIVE".equals(cert.getStatus())) {
                    markExpiringAndWarn(cert, daysRemaining);
                    warned++;
                }
            }
            log.info("[cert][scheduler] 扫描完成：续期 {} 张，预警 {} 张", renewed, warned);
        } finally {
            TenantContext.clear();
        }
    }

    private boolean renewOne(CertCertificate cert) {
        try {
            TenantContext.setIgnore(false);
            TenantContext.setTenantId(cert.getTenantId());
            issueService.issueOrRenew(cert, "RENEW", "SCHEDULED");
            return true;
        } catch (Exception e) {
            log.error("[cert][scheduler] 续期失败：{}", cert.getPrimaryDomain(), e);
            return false;
        } finally {
            TenantContext.setIgnore(true);
        }
    }

    private void markExpiringAndWarn(CertCertificate cert, long daysRemaining) {
        try {
            TenantContext.setIgnore(false);
            TenantContext.setTenantId(cert.getTenantId());
            cert.setStatus("EXPIRING");
            certificateService.updateById(cert);
            notificationService.send("证书即将到期：" + cert.getPrimaryDomain(),
                    "证书 " + cert.getPrimaryDomain() + " 将在 " + daysRemaining + " 天后到期，且未开启自动续期，请尽快处理。");
        } catch (Exception e) {
            log.warn("[cert][scheduler] 预警处理失败：{}", cert.getPrimaryDomain(), e);
        } finally {
            TenantContext.setIgnore(true);
        }
    }
}
