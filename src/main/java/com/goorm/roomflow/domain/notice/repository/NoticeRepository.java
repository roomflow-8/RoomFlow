package com.goorm.roomflow.domain.notice.repository;

import com.goorm.roomflow.domain.notice.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    Page<Notice> findByVisibleTrue(Pageable pageable);
    Optional<Notice> findByNoticeIdAndVisibleTrue(Long noticeId);

    @EntityGraph(attributePaths = {"createdBy", "updatedBy"})
    Page<Notice> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"createdBy", "updatedBy"})
    Optional<Notice> findByNoticeId(Long noticeId);

    Optional<Notice> findFirstByVisibleTrueOrderByPinnedDescCreatedAtDesc();
}
