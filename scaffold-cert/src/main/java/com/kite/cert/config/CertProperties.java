package com.kite.cert.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 证书模块配置（前缀 cert.*）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "cert")
public class CertProperties {

    private Crypto crypto = new Crypto();
    private Acme acme = new Acme();
    private Renew renew = new Renew();
    private Storage storage = new Storage();
    private Notify notify = new Notify();

    @Data
    public static class Crypto {
        /** AES 主密钥，建议 >=32 位，通过环境变量注入。 */
        private String secret;
    }

    @Data
    public static class Acme {
        private String letsencryptDirectory = "https://acme-v02.api.letsencrypt.org/directory";
        private String letsencryptStagingDirectory = "https://acme-staging-v02.api.letsencrypt.org/directory";
        private String zerosslDirectory = "https://acme.zerossl.com/v2/DV90";
        /** DNS TXT 记录写入后等待传播的秒数。 */
        private int dnsPropagationSeconds = 30;
        /** ACME challenge / order 轮询超时秒数。 */
        private int challengeTimeoutSeconds = 180;
    }

    @Data
    public static class Renew {
        /** 续期扫描 cron，默认每天 03:00。 */
        private String cron = "0 0 3 * * ?";
        /** 到期前多少天触发续期。 */
        private int renewBeforeDays = 30;
        /** 到期前多少天发预警通知。 */
        private int expiryWarnDays = 15;
        /** 是否启用定时续期。 */
        private boolean enabled = true;
    }

    @Data
    public static class Storage {
        /** 可选：证书额外导出到磁盘的目录。 */
        private String exportPath;
    }

    @Data
    public static class Notify {
        private Email email = new Email();
        private Webhook webhook = new Webhook();

        @Data
        public static class Email {
            private boolean enabled = false;
            private String to;
        }

        @Data
        public static class Webhook {
            private boolean enabled = false;
            /** dingtalk / wecom */
            private String type = "dingtalk";
            private String url;
        }
    }
}
