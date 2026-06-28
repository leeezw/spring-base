package com.kite.cert.crypto;

import com.kite.cert.config.CertProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 敏感数据加解密服务（AES-256-GCM）。
 *
 * <p>统一用于加密 ACME 账户私钥、DNS/部署凭证、证书私钥等。密钥来自配置 {@code cert.crypto.secret}
 * （建议通过环境变量注入）。密文格式：Base64( iv(12B) + cipherText + tag(16B) )，并加 "ENC:" 前缀以便识别。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CryptoService {

    private static final String PREFIX = "ENC:";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final CertProperties certProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    private SecretKeySpec keySpec;

    @PostConstruct
    public void init() {
        String secret = certProperties.getCrypto().getSecret();
        if (!StringUtils.hasText(secret)) {
            log.warn("[cert] 未配置 cert.crypto.secret，敏感凭证将无法安全加密，请尽快配置！");
            secret = "scaffold-cert-default-insecure-secret-change-me";
        }
        try {
            // 用 SHA-256 把任意长度 secret 规整为 32 字节 AES-256 密钥
            byte[] key = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            this.keySpec = new SecretKeySpec(key, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("初始化加密密钥失败", e);
        }
    }

    /**
     * 加密。入参为 null 返回 null；已是密文则原样返回（幂等）。
     */
    public String encrypt(String plain) {
        if (plain == null) {
            return null;
        }
        if (plain.startsWith(PREFIX)) {
            return plain;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            return PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("加密失败", e);
        }
    }

    /**
     * 解密。入参为 null 返回 null；非密文（无前缀）原样返回，兼容历史明文。
     */
    public String decrypt(String cipherText) {
        if (cipherText == null) {
            return null;
        }
        if (!cipherText.startsWith(PREFIX)) {
            return cipherText;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(cipherText.substring(PREFIX.length()));
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            byte[] actual = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, IV_LENGTH, actual, 0, actual.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(actual), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("解密失败", e);
        }
    }
}
