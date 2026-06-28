package com.kite.cert.dto.response;

import com.kite.cert.entity.CertAcmeAccount;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ACME 账户响应（不含私钥等敏感信息）。
 */
@Data
public class AcmeAccountResponse {

    private Long id;
    private String name;
    private String caType;
    private String email;
    private String directoryUrl;
    /** 是否已完成账户注册（存在 accountUrl）。 */
    private Boolean registered;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;

    public static AcmeAccountResponse from(CertAcmeAccount e) {
        if (e == null) {
            return null;
        }
        AcmeAccountResponse r = new AcmeAccountResponse();
        r.setId(e.getId());
        r.setName(e.getName());
        r.setCaType(e.getCaType());
        r.setEmail(e.getEmail());
        r.setDirectoryUrl(e.getDirectoryUrl());
        r.setRegistered(e.getAccountUrl() != null && !e.getAccountUrl().isEmpty());
        r.setStatus(e.getStatus());
        r.setRemark(e.getRemark());
        r.setCreateTime(e.getCreateTime());
        return r;
    }
}
