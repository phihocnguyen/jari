package com.example.jari.user.oauth2;

import com.example.jari.shared.security.JwtTokenProvider;
import com.example.jari.user.entity.OAuthAccount;
import com.example.jari.user.entity.User;
import com.example.jari.user.entity.UserStatus;
import com.example.jari.user.repository.OAuthAccountRepository;
import com.example.jari.user.repository.UserRepository;
import com.example.jari.user.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final OAuthAccountRepository oAuthAccountRepository;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String provider = oauthToken.getAuthorizedClientRegistrationId();
        OAuth2User oAuth2User = oauthToken.getPrincipal();

        String email       = OAuth2UserInfoFactory.getEmail(provider, oAuth2User);
        String displayName = OAuth2UserInfoFactory.getDisplayName(provider, oAuth2User);
        String providerId  = OAuth2UserInfoFactory.getProviderId(provider, oAuth2User);

        // Upsert user
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            String username = email.split("@")[0] + "_" + UUID.randomUUID().toString().substring(0, 4);
            return userRepository.save(User.builder()
                .username(username)
                .email(email)
                .displayName(displayName)
                .status(UserStatus.ACTIVE)
                .build());
        });

        // Upsert OAuth account link
        oAuthAccountRepository.findByProviderAndProviderUserId(provider, providerId).orElseGet(() ->
            oAuthAccountRepository.save(OAuthAccount.builder()
                .user(user)
                .provider(provider)
                .providerUserId(providerId)
                .build())
        );

        String accessToken  = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        tokenService.saveRefreshToken(user.getId(), refreshToken, jwtTokenProvider.getRefreshTokenExpirySeconds());

        // Redirect to frontend with tokens
        String redirectUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/auth/callback")
            .queryParam("token", accessToken)
            .queryParam("refresh", refreshToken)
            .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
