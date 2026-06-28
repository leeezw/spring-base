package com.kite.cert.acme;

import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * 证书 PEM 编码辅助。
 */
public final class PemUtils {

    private PemUtils() {
    }

    public static String toPem(X509Certificate certificate) {
        try {
            return wrap("CERTIFICATE", certificate.getEncoded());
        } catch (Exception e) {
            throw new IllegalStateException("证书 PEM 编码失败", e);
        }
    }

    private static String wrap(String type, byte[] der) {
        String base64 = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(der);
        return "-----BEGIN " + type + "-----\n" + base64 + "\n-----END " + type + "-----\n";
    }
}
