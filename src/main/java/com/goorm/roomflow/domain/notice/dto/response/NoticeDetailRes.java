package com.goorm.roomflow.domain.notice.dto.response;

import java.time.LocalDateTime;

public record NoticeDetailRes(
        Long noticeId,
        String title,
        String content,
        boolean pinned,
        int viewCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
