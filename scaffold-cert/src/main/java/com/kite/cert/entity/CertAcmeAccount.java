package com.kite.cert.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kite.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ACME 账户（对接 Let's Encrypt / ZeroSSL）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cert_acme_account")
public class CertAcmeAccount extends BaseEntity {

    private Long tenantId;

    private String name;

    /** letsencrypt / zerossl */
    private String caType;

    private String email;

    private String directoryUrl;

    /** 注册后账户 location URL */
    private String accountUrl;

    /** 账户私钥 PEM（加密存储） */
    private String accountKey;

    /** ZeroSSL EAB kid（加密存储） */
    private String eabKid;

    /** ZeroSSL EAB hmac（加密存储） */
    private String eabHmac;

    /** 1启用 0停用 */
    private Integer status;

    private String remark;
}
