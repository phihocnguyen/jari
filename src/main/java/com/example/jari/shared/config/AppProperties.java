package com.example.jari.shared.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Security security = new Security();
    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private Github github = new Github();

    @Getter
    @Setter
    public static class Security {
        /**
         * Khi true: Kích hoạt luồng OAuth2 login (Google,...).
         * Đổi thành true và cấu hình spring.security.oauth2 khi muốn bật lại OAuth2.
         */
        private boolean oauth2Enabled = false;
    }

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long accessTokenExpiry = 900;
        private long refreshTokenExpiry = 604800;
    }

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:3000");
    }

    @Getter
    @Setter
    public static class Github {
        private String appId = "";
        private String clientId = "";
        private String clientSecret = "";
        /** PEM private key for GitHub App JWT (literal newlines or \\n escaped). */
        private String privateKeyPem = "";
        private String webhookSecret = "";
        /** App slug used in install URL: https://github.com/apps/{slug}/installations/new */
        private String appSlug = "";
        /** Frontend base URL for post-install redirect. */
        private String frontendBaseUrl = "http://localhost:3000";
        /** Backend public base URL used as Setup URL callback host if needed. */
        private String backendBaseUrl = "http://localhost:8080";
    }
}
