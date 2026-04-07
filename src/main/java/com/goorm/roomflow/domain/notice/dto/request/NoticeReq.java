package com.goorm.roomflow.domain.notice.dto.request;

public record NoticeReq(
        String title,
        String content,
        boolean pinned,
        boolean visible
) {
}
