package com.kite.cert.dns;

/**
 * 一条 DNS-01 验证记录所需信息。
 *
 * @param fullName   完整记录名，如 _acme-challenge.www.example.com
 * @param value      TXT 记录值（ACME digest）
 * @param baseDomain 可注册域名(zone)，如 example.com
 * @param host       相对 zone 的主机记录，如 _acme-challenge.www
 */
public record DnsChallenge(String fullName, String value, String baseDomain, String host) {
}
