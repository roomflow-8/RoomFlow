package com.goorm.roomflow.domain.user.repository;

import com.goorm.roomflow.domain.user.entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

	Optional<SocialAccount> findByProviderAndProviderUserId(String provider, String providerUserId);
	Optional<SocialAccount> findByUser_UserIdAndProvider(Long userId, String provider);
}
