package com.kite.cert.deploy;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.kite.common.exception.BusinessException;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

/**
 * 轻量 SSH 客户端：通过 SFTP 上传证书文件并执行 reload 命令。
 *
 * <p>配置字段：host、port(默认22)、username、authType(password/key)、password、privateKey。</p>
 */
public final class SshClient implements AutoCloseable {

    private final Session session;

    public SshClient(Map<String, String> config) {
        try {
            JSch jsch = new JSch();
            String authType = config.getOrDefault("authType", "password");
            if ("key".equalsIgnoreCase(authType)) {
                String privateKey = require(config, "privateKey");
                String passphrase = config.get("passphrase");
                jsch.addIdentity("cert-deploy", privateKey.getBytes(StandardCharsets.UTF_8), null,
                        passphrase == null ? null : passphrase.getBytes(StandardCharsets.UTF_8));
            }
            String host = require(config, "host");
            int port = Integer.parseInt(config.getOrDefault("port", "22"));
            String username = require(config, "username");
            this.session = jsch.getSession(username, host, port);
            if (!"key".equalsIgnoreCase(authType)) {
                this.session.setPassword(require(config, "password"));
            }
            Properties props = new Properties();
            props.put("StrictHostKeyChecking", "no");
            this.session.setConfig(props);
            this.session.connect(15000);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("SSH 连接失败：" + e.getMessage());
        }
    }

    /** 上传文本内容到远程文件。 */
    public void upload(String content, String remotePath) {
        ChannelSftp sftp = null;
        try {
            sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect(10000);
            try (InputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
                sftp.put(in, remotePath);
            }
        } catch (Exception e) {
            throw new BusinessException("上传文件失败 " + remotePath + "：" + e.getMessage());
        } finally {
            if (sftp != null) {
                sftp.disconnect();
            }
        }
    }

    /** 执行命令，返回退出码。 */
    public int exec(String command) {
        if (!StringUtils.hasText(command)) {
            return 0;
        }
        ChannelExec exec = null;
        try {
            exec = (ChannelExec) session.openChannel("exec");
            exec.setCommand(command);
            exec.connect(10000);
            // 等待执行完成
            long deadline = System.currentTimeMillis() + 30000;
            while (!exec.isClosed() && System.currentTimeMillis() < deadline) {
                Thread.sleep(200);
            }
            return exec.getExitStatus();
        } catch (Exception e) {
            throw new BusinessException("执行命令失败：" + e.getMessage());
        } finally {
            if (exec != null) {
                exec.disconnect();
            }
        }
    }

    private String require(Map<String, String> config, String key) {
        String v = config.get(key);
        if (!StringUtils.hasText(v)) {
            throw new BusinessException("SSH 配置缺少字段：" + key);
        }
        return v;
    }

    @Override
    public void close() {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }
}
