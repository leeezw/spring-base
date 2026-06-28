package com.kite.cert.acme;

/**
 * DNS-01 验证回调：由编排层注入具体 DNS 写入/清理逻辑，使 ACME 客户端与 DNS 服务商解耦。
 */
public interface ChallengeSolver {

    /** 验证前：写入 TXT 记录。 */
    void prepare(String domain, String recordName, String txtValue);

    /** 验证后：清理 TXT 记录（实现应吞异常）。 */
    void cleanup(String domain, String recordName, String txtValue);
}
