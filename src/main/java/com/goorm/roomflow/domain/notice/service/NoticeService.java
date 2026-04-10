package com.goorm.roomflow.domain.notice.service;

import com.goorm.roomflow.domain.notice.dto.response.NoticeDetailRes;
import com.goorm.roomflow.domain.notice.dto.response.NoticeRes;
import org.springframework.data.domain.Page;

public interface NoticeService {

    Page<NoticeRes> readNoticeList(int page, int size);
    NoticeDetailRes readNotice(Long noticeId);
    NoticeRes findPreviewNotice();
}
