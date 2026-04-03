package com.goorm.roomflow.domain.user.repository;

import com.goorm.roomflow.domain.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserJpaRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = "socialAccounts")
    List<User> findAllByDeletedAtBefore(LocalDateTime deletedAt);

    @EntityGraph(attributePaths = "socialAccounts")
    Optional<User> findWithSocialAccountsByUserId(Long userId);
}