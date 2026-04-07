package com.goorm.roomflow.domain.notice.dto.response;

import java.time.LocalDateTime;

public record NoticeRes(
        Long noticeId,
        String title,
        boolean pinned,
        int viewCount,
        LocalDateTime createdAt
) {
}
