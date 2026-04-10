package com.goorm.roomflow.domain.notice.dto.response;

import java.time.LocalDateTime;

public record AdminNoticeDetailRes(

        Long noticeId,
        String title,
        String content,
        boolean pinned,
        boolean visible,
        int viewCount,
        String createdByName,
        LocalDateTime createdAt,
        String updatedByName,
        LocalDateTime updatedAt
) {
}
