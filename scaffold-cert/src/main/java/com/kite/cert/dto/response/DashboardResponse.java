package com.kite.cert.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 概览统计响应。
 */
@Data
public class DashboardResponse {

    private long total;
    private long active;
    private long expiring;
    private long failed;

    /** 临期证书列表（按剩余天数升序）。 */
    private List<CertificateResponse> expiringList;
}
