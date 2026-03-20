package com.goorm.roomflow.domain.user.repository;

import com.goorm.roomflow.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserJpaRepository extends JpaRepository<User, Long> {
    User findByEmail( String email);
}