package com.example.jari.development.github.service;

import com.example.jari.shared.config.AppProperties;
import com.example.jari.shared.exception.BadRequestException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class GithubAppJwtService {

    private final AppProperties appProperties;

    public String createAppJwt() {
        AppProperties.Github gh = appProperties.getGithub();
        if (gh.getAppId() == null || gh.getAppId().isBlank()) {
            throw new BadRequestException("GitHub App is not configured (app-id missing)");
        }
        if (gh.getPrivateKeyPem() == null || gh.getPrivateKeyPem().isBlank()) {
            throw new BadRequestException("GitHub App is not configured (private key missing)");
        }

        Instant now = Instant.now();
        return Jwts.builder()
            .issuer(gh.getAppId())
            .issuedAt(Date.from(now.minusSeconds(60)))
            .expiration(Date.from(now.plusSeconds(9 * 60)))
            .signWith(loadPrivateKey(gh.getPrivateKeyPem()))
            .compact();
    }

    private PrivateKey loadPrivateKey(String pem) {
        try {
            String normalized = pem.replace("\\n", "\n").trim();
            try (PEMParser parser = new PEMParser(new StringReader(normalized))) {
                Object obj = parser.readObject();
                JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
                if (obj instanceof PEMKeyPair keyPair) {
                    return converter.getPrivateKey(keyPair.getPrivateKeyInfo());
                }
                if (obj instanceof PrivateKeyInfo info) {
                    return converter.getPrivateKey(info);
                }
                throw new BadRequestException("Unsupported PEM private key format");
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Invalid GitHub App private key: " + e.getMessage());
        }
    }
}
