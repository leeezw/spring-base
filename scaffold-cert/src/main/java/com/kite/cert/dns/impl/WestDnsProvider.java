package com.kite.cert.dns.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpUtil;
import com.kite.cert.dns.DnsChallenge;
import com.kite.cert.dns.DnsProvider;
import com.kite.common.exception.BusinessException;
import com.kite.common.util.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 西部数码（west.cn）DNS Provider。
 *
 * <p>凭证字段：{@code username}（账号）、{@code apiPassword}（API 密码/密钥）。
 * 可选 {@code endpoint} 覆盖默认 API 地址。</p>
 *
 * <p>注意：西部数码 DNS 开放接口的具体字段以官方最新文档为准，若返回异常请核对 endpoint 与鉴权方式后调整。
 * 这里按其常见 v2 接口（username + time + token=md5(username+apiPassword+time)）实现。</p>
 */
@Slf4j
@Component
public class WestDnsProvider implements DnsProvider {

    private static final String DEFAULT_ENDPOINT = "https://api.west.cn/api/v2/domain/dns";

    @Override
    public String type() {
        return "west";
    }

    @Override
    public void addTxtRecord(DnsChallenge challenge, Map<String, String> credential) {
        Map<String, Object> params = baseParams(credential, "adddns");
        params.put("domain", challenge.baseDomain());
        params.put("host", challenge.host());
        params.put("type", "TXT");
        params.put("value", challenge.value());
        params.put("level", "10");
        params.put("ttl", "300");
        String resp = post(endpoint(credential), params);
        ensureSuccess(resp, "添加 TXT 记录");
        log.info("[cert][west] 已添加 TXT 记录 {} -> {}", challenge.fullName(), challenge.value());
    }

    @Override
    public void removeTxtRecord(DnsChallenge challenge, Map<String, String> credential) {
        try {
            // 先查询记录 id 再删除
            Map<String, Object> query = baseParams(credential, "getdns");
            query.put("domain", challenge.baseDomain());
            query.put("host", challenge.host());
            query.put("type", "TXT");
            String listResp = post(endpoint(credential), query);
            JsonNode root = JsonUtils.parseObject(listResp, JsonNode.class);
            if (root == null) {
                return;
            }
            JsonNode items = root.path("data").path("items");
            if (items.isMissingNode()) {
                items = root.path("data");
            }
            if (items.isArray()) {
                for (JsonNode item : items) {
                    String recordValue = item.path("value").asText("");
                    if (challenge.value().equals(recordValue) || recordValue.isEmpty()) {
                        String id = item.path("id").asText(item.path("record_id").asText(""));
                        if (StrUtil.isNotBlank(id)) {
                            Map<String, Object> del = baseParams(credential, "deldns");
                            del.put("domain", challenge.baseDomain());
                            del.put("id", id);
                            post(endpoint(credential), del);
                        }
                    }
                }
            }
            log.info("[cert][west] 已清理 TXT 记录 {}", challenge.fullName());
        } catch (Exception e) {
            // 清理失败不影响主流程
            log.warn("[cert][west] 清理 TXT 记录失败（可忽略）：{}", e.getMessage());
        }
    }

    private Map<String, Object> baseParams(Map<String, String> credential, String act) {
        String username = required(credential, "username");
        String apiPassword = required(credential, "apiPassword");
        String time = String.valueOf(System.currentTimeMillis() / 1000);
        String token = SecureUtil.md5(username + apiPassword + time);
        Map<String, Object> params = new HashMap<>();
        params.put("username", username);
        params.put("time", time);
        params.put("token", token);
        params.put("act", act);
        return params;
    }

    private String endpoint(Map<String, String> credential) {
        String ep = credential.get("endpoint");
        return StrUtil.isNotBlank(ep) ? ep : DEFAULT_ENDPOINT;
    }

    private String post(String url, Map<String, Object> params) {
        try {
            return HttpUtil.post(url, params, 15000);
        } catch (Exception e) {
            throw new BusinessException("调用西部数码 DNS API 失败：" + e.getMessage());
        }
    }

    private void ensureSuccess(String resp, String action) {
        JsonNode root = JsonUtils.parseObject(resp, JsonNode.class);
        // 西部数码成功返回 result=200 / code=200，不同接口字段不一，做宽松判断
        if (root != null) {
            int result = root.path("result").asInt(root.path("code").asInt(-1));
            if (result == 200 || result == 0) {
                return;
            }
            String msg = root.path("msg").asText(root.path("clientMessage").asText("未知错误"));
            throw new BusinessException("西部数码 " + action + " 失败：" + msg);
        }
        throw new BusinessException("西部数码 " + action + " 返回无法解析：" + resp);
    }

    private String required(Map<String, String> credential, String key) {
        String v = credential.get(key);
        if (StrUtil.isBlank(v)) {
            throw new BusinessException("西部数码 DNS 凭证缺少字段：" + key);
        }
        return v;
    }
}
