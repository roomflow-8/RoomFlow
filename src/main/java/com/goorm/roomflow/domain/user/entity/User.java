package com.goorm.roomflow.domain.user.entity;

import com.goorm.roomflow.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long userId;

	@Column(nullable = false, length = 50)
	private String name;

	@Column(nullable = false, unique = true, length = 255)
	private String email;

	@Column(nullable = false, length = 255)
	private String password;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UserRole role;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@OneToMany(mappedBy = "user")
	private List<SocialAccount> socialAccounts = new ArrayList<>();

	@Builder
	public User(String name, String email, String password, UserRole role, LocalDateTime deletedAt) {
		this.name = name;
		this.email = email;
		this.password = password;
		this.role = role;
		this.deletedAt = deletedAt;
	}

	public void delete() {
		this.deletedAt = LocalDateTime.now();
	}

	public boolean isDeleted() {
		return this.deletedAt != null;
	}

	public void updateName(String name) {
		this.name = name;
	}

	public void updatePassword(String encodedPassword) {
		this.password = encodedPassword;
	}

}