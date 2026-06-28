package com.kite.cert.deploy.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.Method;
import com.fasterxml.jackson.databind.JsonNode;
import com.kite.cert.deploy.CertDeployer;
import com.kite.cert.deploy.DeployContext;
import com.kite.common.exception.BusinessException;
import com.kite.common.util.JsonUtils;
import com.qiniu.util.Auth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 部署证书到七牛云（上传 SSL 证书并绑定到 CDN/OSS 加速域名）。
 *
 * <p>配置字段：accessKey、secretKey、domain（七牛云加速域名）。</p>
 */
@Slf4j
@Component
public class QiniuDeployer implements CertDeployer {

    private static final String API = "https://api.qiniu.com";

    @Override
    public boolean supports(String type) {
        return "qiniu".equalsIgnoreCase(type);
    }

    @Override
    public void deploy(DeployContext ctx) {
        Map<String, String> config = ctx.getConfig();
        Auth auth = auth(config);
        String domain = require(config, "domain");

        // 1. 上传证书
        Map<String, Object> certBody = new HashMap<>();
        certBody.put("name", ctx.getPrimaryDomain() + "-" + System.currentTimeMillis() / 1000);
        certBody.put("common_name", ctx.getPrimaryDomain());
        certBody.put("pri", ctx.getKeyPem());
        certBody.put("ca", ctx.getChainPem());
        JsonNode uploadResp = call(auth, Method.POST, API + "/sslcert", JsonUtils.toJson(certBody));
        String certId = uploadResp.path("certID").asText("");
        if (StrUtil.isBlank(certId)) {
            throw new BusinessException("七牛云上传证书失败：" + uploadResp);
        }

        // 2. 绑定到域名 HTTPS 配置
        Map<String, Object> bindBody = new HashMap<>();
        bindBody.put("certid", certId);
        bindBody.put("forceHttps", false);
        bindBody.put("http2Enable", true);
        call(auth, Method.PUT, API + "/domain/" + domain + "/httpsconf", JsonUtils.toJson(bindBody));

        log.info("[cert][qiniu] {} 部署成功 -> 域名 {} (certID={})", ctx.getPrimaryDomain(), domain, certId);
    }

    @Override
    public void test(String type, Map<String, String> config) {
        Auth auth = auth(config);
        String domain = require(config, "domain");
        call(auth, Method.GET, API + "/domain/" + domain, null);
    }

    private Auth auth(Map<String, String> config) {
        return Auth.create(require(config, "accessKey"), require(config, "secretKey"));
    }

    private JsonNode call(Auth auth, Method method, String url, String body) {
        try {
            byte[] bodyBytes = body == null ? null : body.getBytes(StandardCharsets.UTF_8);
            String contentType = "application/json";
            String token = auth.signRequest(url, bodyBytes, contentType);
            HttpRequest request = HttpRequest.of(url)
                    .method(method)
                    .header("Authorization", "QBox " + token)
                    .header("Content-Type", contentType)
                    .timeout(15000);
            if (body != null) {
                request.body(body);
            }
            String resp = request.execute().body();
            JsonNode root = JsonUtils.parseObject(resp, JsonNode.class);
            if (root == null) {
                throw new BusinessException("七牛云接口返回无法解析：" + resp);
            }
            int code = root.path("code").asInt(0);
            if (code != 0 && code != 200) {
                throw new BusinessException("七牛云接口错误：" + root.path("error").asText(resp));
            }
            return root;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("调用七牛云 API 失败：" + e.getMessage());
        }
    }

    private String require(Map<String, String> config, String key) {
        String v = config.get(key);
        if (!StringUtils.hasText(v)) {
            throw new BusinessException("七牛云部署配置缺少字段：" + key);
        }
        return v;
    }
}
