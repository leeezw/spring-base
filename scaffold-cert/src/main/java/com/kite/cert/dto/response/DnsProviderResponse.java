package com.kite.cert.dto.response;

import com.kite.cert.entity.CertDnsProvider;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DNS 服务商响应（不含凭证明文）。
 */
@Data
public class DnsProviderResponse {

    private Long id;
    private String name;
    private String type;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;

    public static DnsProviderResponse from(CertDnsProvider e) {
        if (e == null) {
            return null;
        }
        DnsProviderResponse r = new DnsProviderResponse();
        r.setId(e.getId());
        r.setName(e.getName());
        r.setType(e.getType());
        r.setStatus(e.getStatus());
        r.setRemark(e.getRemark());
        r.setCreateTime(e.getCreateTime());
        return r;
    }
}
