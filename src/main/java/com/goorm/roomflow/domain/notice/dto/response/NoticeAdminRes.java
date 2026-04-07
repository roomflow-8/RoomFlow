package com.goorm.roomflow.domain.notice.dto.response;

import java.time.LocalDateTime;

public record NoticeAdminRes(
        Long noticeId,
        String title,
        boolean pinned,
        boolean visible,
        int viewCount,
        LocalDateTime createdAt,
         LocalDateTime updatedAt
) {}
