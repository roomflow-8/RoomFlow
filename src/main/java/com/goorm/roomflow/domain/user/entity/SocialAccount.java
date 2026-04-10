package com.goorm.roomflow.domain.user.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(
		name = "social_accounts",
		uniqueConstraints = {
				@UniqueConstraint(
						name = "uk_social_accounts_provider_user",
						columnNames = {"provider", "provider_user_id"}
				)
		}
)
public class SocialAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long socialAccountId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, length = 30)
	private String provider;

	@Column(nullable = false, length = 100)
	private String providerUserId;

	@Column(name ="refresh_token")
	private String refreshToken;

	@CreatedDate
	@Column(updatable = false, nullable = false)
	private LocalDateTime createdAt;

	@Builder
	public SocialAccount(User user, String provider, String providerUserId, LocalDateTime createdAt) {
		this.user = user;
		this.provider = provider;
		this.providerUserId = providerUserId;
		this.createdAt = createdAt;
	}

	public void updateRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}
}