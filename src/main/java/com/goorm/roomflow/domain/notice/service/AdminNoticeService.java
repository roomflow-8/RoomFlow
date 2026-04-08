package com.goorm.roomflow.domain.notice.service;

import com.goorm.roomflow.domain.notice.dto.request.NoticeReq;
import com.goorm.roomflow.domain.notice.dto.response.NoticeAdminDetailRes;
import com.goorm.roomflow.domain.notice.dto.response.NoticeAdminRes;
import org.springframework.data.domain.Page;

public interface AdminNoticeService {

    Page<NoticeAdminRes> readNoticeList(int page, int size);
    NoticeAdminDetailRes readNoticeDetail(Long noticeId);

    void createNotice(Long userId, NoticeReq noticeReq);
    void modifyNotice(Long userId, Long noticeId, NoticeReq noticeReq);
    void deleteNotice(Long noticeId);
}
