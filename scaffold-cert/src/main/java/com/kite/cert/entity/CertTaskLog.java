package com.kite.cert.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kite.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 证书任务执行日志。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cert_task_log")
public class CertTaskLog extends BaseEntity {

    private Long tenantId;

    private Long certificateId;

    /** ISSUE/RENEW/DEPLOY/REVOKE */
    private String type;

    /** MANUAL/SCHEDULED */
    private String triggerSource;

    /** RUNNING/SUCCESS/FAILED */
    private String status;

    private String message;

    private String detail;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;
}
