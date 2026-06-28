package com.kite.cert.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kite.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 证书部署目标。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cert_deploy_target")
public class CertDeployTarget extends BaseEntity {

    private Long tenantId;

    private String name;

    /** nginx_ssh/west_ssh/baota/qiniu */
    private String type;

    /** 部署配置 JSON（加密存储） */
    private String config;

    /** 1启用 0停用 */
    private Integer enabled;

    private String remark;
}
