package com.kite.cert.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kite.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * DNS 服务商凭证（DNS-01 验证自动写 TXT 记录）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cert_dns_provider")
public class CertDnsProvider extends BaseEntity {

    private Long tenantId;

    private String name;

    /** west/aliyun/dnspod/cloudflare/manual */
    private String type;

    /** 凭证 JSON（加密存储） */
    private String credential;

    private Integer status;

    private String remark;
}
