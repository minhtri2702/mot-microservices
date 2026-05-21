package com.mot.repository;

import com.mot.entity.SocialAccounts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SocialAccountsRepository extends JpaRepository<SocialAccounts, Long> {
    Optional<SocialAccounts> findByProviderAndProviderId(String provider, String providerId);
}
