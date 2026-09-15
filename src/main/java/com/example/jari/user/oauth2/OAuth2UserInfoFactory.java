package com.example.jari.user.oauth2;

import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;

public class OAuth2UserInfoFactory {

    public static String getEmail(String provider, OAuth2User oAuth2User) {
        Map<String, Object> attrs = oAuth2User.getAttributes();
        return switch (provider.toLowerCase()) {
            case "google" -> (String) attrs.get("email");
            case "github" -> (String) attrs.get("email");
            default -> throw new IllegalArgumentException("Unsupported OAuth2 provider: " + provider);
        };
    }

    public static String getDisplayName(String provider, OAuth2User oAuth2User) {
        Map<String, Object> attrs = oAuth2User.getAttributes();
        return switch (provider.toLowerCase()) {
            case "google" -> (String) attrs.get("name");
            case "github" -> (String) attrs.getOrDefault("name", attrs.get("login"));
            default -> "User";
        };
    }

    public static String getProviderId(String provider, OAuth2User oAuth2User) {
        Map<String, Object> attrs = oAuth2User.getAttributes();
        return switch (provider.toLowerCase()) {
            case "google" -> (String) attrs.get("sub");
            case "github" -> String.valueOf(attrs.get("id"));
            default -> throw new IllegalArgumentException("Unsupported OAuth2 provider: " + provider);
        };
    }
}
