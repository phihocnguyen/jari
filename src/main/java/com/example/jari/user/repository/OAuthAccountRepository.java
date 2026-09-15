package com.example.jari.user.repository;

import com.example.jari.user.entity.OAuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, UUID> {
    Optional<OAuthAccount> findByProviderAndProviderUserId(String provider, String providerUserId);
}
