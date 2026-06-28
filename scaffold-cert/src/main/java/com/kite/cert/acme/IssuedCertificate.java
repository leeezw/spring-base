package com.kite.cert.acme;

import java.time.LocalDateTime;

/**
 * 一次签发产出的证书材料与元数据。
 *
 * @param certPem   叶子证书 PEM
 * @param chainPem  完整证书链 PEM（叶子 + 中间证书，即 fullchain）
 * @param keyPem    证书私钥 PEM
 * @param notBefore 生效时间
 * @param notAfter  到期时间
 * @param serial    序列号(16进制)
 * @param issuer    签发机构
 */
public record IssuedCertificate(String certPem, String chainPem, String keyPem,
                                LocalDateTime notBefore, LocalDateTime notAfter,
                                String serial, String issuer) {
}
