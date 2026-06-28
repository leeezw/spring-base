package com.kite.cert.crypto;

import com.kite.cert.config.CertProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CryptoService 单元测试（无需 Spring 容器/DB/网络）。
 */
class CryptoServiceTest {

    private CryptoService cryptoService;

    @BeforeEach
    void setUp() {
        CertProperties props = new CertProperties();
        props.getCrypto().setSecret("unit-test-secret-key-1234567890-abc");
        cryptoService = new CryptoService(props);
        cryptoService.init();
    }

    @Test
    void encrypt_then_decrypt_roundtrip() {
        String plain = "-----BEGIN PRIVATE KEY-----\nMIIEv...\n-----END PRIVATE KEY-----";
        String cipher = cryptoService.encrypt(plain);
        assertTrue(cipher.startsWith("ENC:"), "密文应带 ENC: 前缀");
        assertNotEquals(plain, cipher);
        assertEquals(plain, cryptoService.decrypt(cipher));
    }

    @Test
    void encrypt_is_idempotent_on_ciphertext() {
        String cipher = cryptoService.encrypt("hello");
        // 已是密文再次加密应原样返回，避免双重加密
        assertEquals(cipher, cryptoService.encrypt(cipher));
    }

    @Test
    void decrypt_plaintext_is_passthrough() {
        // 兼容历史明文：无 ENC: 前缀原样返回
        assertEquals("plain-value", cryptoService.decrypt("plain-value"));
    }

    @Test
    void null_handling() {
        assertNull(cryptoService.encrypt(null));
        assertNull(cryptoService.decrypt(null));
    }

    @Test
    void same_plaintext_produces_different_ciphertext() {
        // 随机 IV 使两次加密结果不同，但都能解回原文
        String c1 = cryptoService.encrypt("same");
        String c2 = cryptoService.encrypt("same");
        assertNotEquals(c1, c2);
        assertEquals("same", cryptoService.decrypt(c1));
        assertEquals("same", cryptoService.decrypt(c2));
    }
}
