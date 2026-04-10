package com.goorm.roomflow.domain.notice.service;

import com.goorm.roomflow.domain.notice.dto.request.NoticeReq;
import com.goorm.roomflow.domain.notice.dto.response.AdminNoticeDetailRes;
import com.goorm.roomflow.domain.notice.dto.response.AdminNoticeRes;
import org.springframework.data.domain.Page;

public interface AdminNoticeService {

    Page<AdminNoticeRes> readNoticeList(int page, int size);
    AdminNoticeDetailRes readNoticeDetail(Long noticeId);

    void createNotice(Long userId, NoticeReq noticeReq);
    void modifyNotice(Long userId, Long noticeId, NoticeReq noticeReq);
    void deleteNotice(Long noticeId);
}
