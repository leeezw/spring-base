package com.kite.cert.dto.response;

import com.kite.cert.entity.CertTaskLog;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务日志响应。
 */
@Data
public class TaskLogResponse {

    private Long id;
    private Long certificateId;
    private String type;
    private String triggerSource;
    private String status;
    private String message;
    private String detail;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    public static TaskLogResponse from(CertTaskLog e) {
        if (e == null) {
            return null;
        }
        TaskLogResponse r = new TaskLogResponse();
        r.setId(e.getId());
        r.setCertificateId(e.getCertificateId());
        r.setType(e.getType());
        r.setTriggerSource(e.getTriggerSource());
        r.setStatus(e.getStatus());
        r.setMessage(e.getMessage());
        r.setDetail(e.getDetail());
        r.setStartedAt(e.getStartedAt());
        r.setFinishedAt(e.getFinishedAt());
        return r;
    }
}
