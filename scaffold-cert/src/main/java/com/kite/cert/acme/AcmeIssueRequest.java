package com.kite.cert.acme;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * ACME 签发请求参数。
 */
@Data
@Builder
public class AcmeIssueRequest {

    private String directoryUrl;
    private String email;
    private String caType;

    /** 已存在的账户私钥 PEM（明文）；为空则新建账户。 */
    private String accountKeyPem;
    /** 已存在的账户 location URL；为空则新建账户。 */
    private String accountUrl;

    /** ZeroSSL EAB 凭证（明文）。 */
    private String eabKid;
    private String eabHmac;

    /** 待签发域名列表（含通配 *.example.com）。 */
    private List<String> domains;

    /** RSA2048 / EC256 */
    private String keyAlgo;

    /** DNS-01 验证回调。 */
    private ChallengeSolver solver;
}
