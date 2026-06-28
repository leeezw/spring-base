package com.kite.cert.deploy.impl;

import com.kite.cert.deploy.CertDeployer;
import com.kite.cert.deploy.DeployContext;
import com.kite.cert.deploy.SshClient;
import com.kite.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 通过 SSH 部署证书到 Nginx / 西部数码服务器。
 *
 * <p>配置字段：host、port、username、authType(password/key)、password 或 privateKey、
 * fullchainPath（证书链落盘路径）、keyPath（私钥落盘路径）、reloadCmd（重载命令，默认 nginx -s reload）。</p>
 */
@Slf4j
@Component
public class NginxSshDeployer implements CertDeployer {

    @Override
    public boolean supports(String type) {
        return "nginx_ssh".equalsIgnoreCase(type) || "west_ssh".equalsIgnoreCase(type);
    }

    @Override
    public void deploy(DeployContext ctx) {
        Map<String, String> config = ctx.getConfig();
        String fullchainPath = require(config, "fullchainPath");
        String keyPath = require(config, "keyPath");
        String reloadCmd = config.getOrDefault("reloadCmd", "nginx -s reload");
        try (SshClient ssh = new SshClient(config)) {
            ssh.upload(ctx.getChainPem(), fullchainPath);
            ssh.upload(ctx.getKeyPem(), keyPath);
            int code = ssh.exec(reloadCmd);
            if (code != 0) {
                throw new BusinessException("证书已上传，但重载命令返回非 0：" + code + "（" + reloadCmd + "）");
            }
        }
        log.info("[cert][nginx-ssh] {} 部署成功 -> {}", ctx.getPrimaryDomain(), config.get("host"));
    }

    @Override
    public void test(String type, Map<String, String> config) {
        try (SshClient ssh = new SshClient(config)) {
            ssh.exec("echo ok");
        }
    }

    private String require(Map<String, String> config, String key) {
        String v = config.get(key);
        if (!StringUtils.hasText(v)) {
            throw new BusinessException("Nginx/SSH 部署配置缺少字段：" + key);
        }
        return v;
    }
}
