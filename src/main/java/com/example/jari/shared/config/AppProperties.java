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

    @Getter
    @Setter
    public static class Security {
        /**
         * Khi true: Tất cả API là public, không bắt buộc JWT;
         * Tự động gán dev user khi không có token để tránh NPE ở các controller.
         * Đổi thành false khi muốn bật lại bảo mật JWT.
         */
        private boolean bypass = true;

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
}
