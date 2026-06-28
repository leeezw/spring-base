package com.kite.cert.dto.response;

import com.kite.cert.entity.CertCertificate;
import lombok.Data;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 证书响应（不含 PEM 私钥等敏感内容）。
 */
@Data
public class CertificateResponse {

    private Long id;
    private String primaryDomain;
    private List<String> sanDomains;
    private String challengeType;
    private Long acmeAccountId;
    private Long dnsProviderId;
    private String keyAlgo;
    private String status;
    private LocalDateTime notBefore;
    private LocalDateTime notAfter;
    private String serial;
    private String issuer;
    private Integer autoRenew;
    private Integer renewBeforeDays;
    private LocalDateTime lastRenewAt;
    private String lastMessage;
    private LocalDateTime createTime;

    /** 剩余有效天数（已签发时计算）。 */
    private Long daysRemaining;

    /** 绑定的部署目标 ID 列表。 */
    private List<Long> deployTargetIds;

    public static CertificateResponse from(CertCertificate e) {
        if (e == null) {
            return null;
        }
        CertificateResponse r = new CertificateResponse();
        r.setId(e.getId());
        r.setPrimaryDomain(e.getPrimaryDomain());
        r.setSanDomains(e.getSanDomains());
        r.setChallengeType(e.getChallengeType());
        r.setAcmeAccountId(e.getAcmeAccountId());
        r.setDnsProviderId(e.getDnsProviderId());
        r.setKeyAlgo(e.getKeyAlgo());
        r.setStatus(e.getStatus());
        r.setNotBefore(e.getNotBefore());
        r.setNotAfter(e.getNotAfter());
        r.setSerial(e.getSerial());
        r.setIssuer(e.getIssuer());
        r.setAutoRenew(e.getAutoRenew());
        r.setRenewBeforeDays(e.getRenewBeforeDays());
        r.setLastRenewAt(e.getLastRenewAt());
        r.setLastMessage(e.getLastMessage());
        r.setCreateTime(e.getCreateTime());
        if (e.getNotAfter() != null) {
            r.setDaysRemaining(Duration.between(LocalDateTime.now(), e.getNotAfter()).toDays());
        }
        return r;
    }
}
