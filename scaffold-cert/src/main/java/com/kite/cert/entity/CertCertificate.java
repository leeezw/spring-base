package com.kite.cert.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.kite.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 证书主记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "cert_certificate", autoResultMap = true)
public class CertCertificate extends BaseEntity {

    private Long tenantId;

    private String primaryDomain;

    /** 附加域名(SAN)列表 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> sanDomains;

    /** dns-01 / http-01 */
    private String challengeType;

    private Long acmeAccountId;

    private Long dnsProviderId;

    /** RSA2048 / EC256 */
    private String keyAlgo;

    /** PENDING/ISSUING/ACTIVE/EXPIRING/FAILED */
    private String status;

    private LocalDateTime notBefore;

    private LocalDateTime notAfter;

    private String serial;

    private String issuer;

    /** 证书 PEM（加密存储） */
    private String certPem;

    /** 证书链 PEM（加密存储） */
    private String chainPem;

    /** 私钥 PEM（加密存储） */
    private String keyPem;

    private Integer autoRenew;

    private Integer renewBeforeDays;

    private LocalDateTime lastRenewAt;

    private String lastMessage;
}
