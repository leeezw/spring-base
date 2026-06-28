package com.kite.cert.dns.impl;

import com.kite.cert.dns.DnsChallenge;
import com.kite.cert.dns.DnsProvider;
import com.kite.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 手动 DNS Provider（兜底）。
 *
 * <p>不具备自动写记录能力。用于尚未接入 DNS API 的场景：需要用户预先手动在 DNS 控制台添加
 * {@code _acme-challenge} TXT 记录，再触发签发。当前同步签发流程无法等待人工操作，
 * 因此直接抛出明确提示，引导用户配置具备 API 的 DNS 服务商（如西部数码）。</p>
 */
@Slf4j
@Component
public class ManualDnsProvider implements DnsProvider {

    @Override
    public String type() {
        return "manual";
    }

    @Override
    public void addTxtRecord(DnsChallenge challenge, Map<String, String> credential) {
        String hint = String.format("请手动添加 TXT 记录：%s = %s（zone=%s, host=%s），添加并生效后再发起签发。",
                challenge.fullName(), challenge.value(), challenge.baseDomain(), challenge.host());
        log.warn("[cert][manual] {}", hint);
        throw new BusinessException("当前为手动 DNS 模式，无法自动签发。" + hint
                + " 建议改用具备 API 的 DNS 服务商（如西部数码）以实现全自动续期。");
    }

    @Override
    public void removeTxtRecord(DnsChallenge challenge, Map<String, String> credential) {
        // 手动模式无需清理
    }
}
