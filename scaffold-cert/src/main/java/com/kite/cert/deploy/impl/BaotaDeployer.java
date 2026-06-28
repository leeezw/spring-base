package com.kite.cert.deploy.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.kite.cert.deploy.CertDeployer;
import com.kite.cert.deploy.DeployContext;
import com.kite.common.exception.BusinessException;
import com.kite.common.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 部署证书到宝塔面板站点。
 *
 * <p>配置字段：panelUrl（面板地址，如 http://1.2.3.4:8888）、apiKey（接口密钥）、siteName（站点域名/名称）。</p>
 * <p>鉴权：request_token = md5(request_time + md5(apiKey))。</p>
 */
@Slf4j
@Component
public class BaotaDeployer implements CertDeployer {

    @Override
    public boolean supports(String type) {
        return "baota".equalsIgnoreCase(type);
    }

    @Override
    public void deploy(DeployContext ctx) {
        Map<String, String> config = ctx.getConfig();
        String panelUrl = trimSlash(require(config, "panelUrl"));
        String siteName = require(config, "siteName");

        Map<String, Object> params = signedParams(config);
        params.put("type", "1");
        params.put("siteName", siteName);
        params.put("key", ctx.getKeyPem());
        params.put("csr", ctx.getChainPem());

        String resp = post(panelUrl + "/site?action=SetSSL", params);
        JsonNode root = JsonUtils.parseObject(resp, JsonNode.class);
        if (root == null || !root.path("status").asBoolean(false)) {
            String msg = root == null ? resp : root.path("msg").asText("未知错误");
            throw new BusinessException("宝塔部署失败：" + msg);
        }
        log.info("[cert][baota] {} 部署成功 -> {}", ctx.getPrimaryDomain(), siteName);
    }

    @Override
    public void test(String type, Map<String, String> config) {
        String panelUrl = trimSlash(require(config, "panelUrl"));
        String resp = post(panelUrl + "/system?action=GetSystemTotal", signedParams(config));
        JsonNode root = JsonUtils.parseObject(resp, JsonNode.class);
        if (root == null || root.isEmpty()) {
            throw new BusinessException("宝塔连通测试失败：" + resp);
        }
    }

    private Map<String, Object> signedParams(Map<String, String> config) {
        String apiKey = require(config, "apiKey");
        String time = String.valueOf(System.currentTimeMillis() / 1000);
        Map<String, Object> params = new HashMap<>();
        params.put("request_time", time);
        params.put("request_token", SecureUtil.md5(time + SecureUtil.md5(apiKey)));
        return params;
    }

    private String post(String url, Map<String, Object> params) {
        try {
            return HttpUtil.post(url, params, 15000);
        } catch (Exception e) {
            throw new BusinessException("调用宝塔 API 失败：" + e.getMessage());
        }
    }

    private String trimSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String require(Map<String, String> config, String key) {
        String v = config.get(key);
        if (!StringUtils.hasText(v)) {
            throw new BusinessException("宝塔部署配置缺少字段：" + key);
        }
        return v;
    }
}
