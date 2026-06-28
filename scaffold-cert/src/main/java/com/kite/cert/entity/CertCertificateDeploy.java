package com.kite.cert.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kite.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 证书-部署目标关联。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cert_certificate_deploy")
public class CertCertificateDeploy extends BaseEntity {

    private Long tenantId;

    private Long certificateId;

    private Long deployTargetId;

    /** SUCCESS/FAILED */
    private String lastDeployStatus;

    private LocalDateTime lastDeployTime;

    private String lastMessage;
}
