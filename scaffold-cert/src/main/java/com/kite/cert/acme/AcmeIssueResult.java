package com.kite.cert.acme;

/**
 * ACME 签发结果。{@code accountKeyPem}/{@code accountUrl} 在新建账户时回传，供持久化复用。
 */
public record AcmeIssueResult(IssuedCertificate certificate, String accountKeyPem, String accountUrl) {
}
