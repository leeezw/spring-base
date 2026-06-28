package com.kite.cert.notify;

import cn.hutool.http.HttpRequest;
import com.kite.cert.config.CertProperties;
import com.kite.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 证书事件通知：支持邮件与 Webhook（钉钉/企业微信），按配置开关分发，失败不影响主流程。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final CertProperties certProperties;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    /** 发送通知。title 作为邮件主题/消息标题，content 为正文。 */
    public void send(String title, String content) {
        try {
            sendEmail(title, content);
        } catch (Exception e) {
            log.warn("[cert][notify] 邮件通知失败：{}", e.getMessage());
        }
        try {
            sendWebhook(title, content);
        } catch (Exception e) {
            log.warn("[cert][notify] Webhook 通知失败：{}", e.getMessage());
        }
    }

    private void sendEmail(String title, String content) {
        CertProperties.Notify.Email email = certProperties.getNotify().getEmail();
        if (!email.isEnabled() || !StringUtils.hasText(email.getTo())) {
            return;
        }
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            log.warn("[cert][notify] 已开启邮件通知但未配置 spring.mail，跳过");
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        if (StringUtils.hasText(mailFrom)) {
            message.setFrom(mailFrom);
        }
        message.setTo(email.getTo().split(","));
        message.setSubject("[证书自动化] " + title);
        message.setText(content);
        sender.send(message);
    }

    private void sendWebhook(String title, String content) {
        CertProperties.Notify.Webhook webhook = certProperties.getNotify().getWebhook();
        if (!webhook.isEnabled() || !StringUtils.hasText(webhook.getUrl())) {
            return;
        }
        String text = title + "\n" + content;
        Map<String, Object> body = new HashMap<>();
        if ("wecom".equalsIgnoreCase(webhook.getType())) {
            body.put("msgtype", "text");
            body.put("text", Map.of("content", text));
        } else {
            // 默认钉钉
            body.put("msgtype", "text");
            body.put("text", Map.of("content", text));
        }
        HttpRequest.post(webhook.getUrl()).body(JsonUtils.toJson(body)).timeout(10000).execute();
    }
}
