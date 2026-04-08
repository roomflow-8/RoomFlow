package com.goorm.roomflow.domain.notice.service;

import com.goorm.roomflow.domain.notice.dto.request.NoticeReq;
import com.goorm.roomflow.domain.notice.dto.response.NoticeAdminDetailRes;
import com.goorm.roomflow.domain.notice.dto.response.NoticeAdminRes;
import com.goorm.roomflow.domain.notice.entity.Notice;
import com.goorm.roomflow.domain.notice.mapper.NoticeMapper;
import com.goorm.roomflow.domain.notice.repository.NoticeRepository;
import com.goorm.roomflow.domain.user.entity.User;
import com.goorm.roomflow.domain.user.repository.UserJpaRepository;
import com.goorm.roomflow.global.code.ErrorCode;
import com.goorm.roomflow.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminNoticeServiceImpl implements AdminNoticeService {

    private final NoticeRepository noticeRepository;
    private final UserJpaRepository userRepository;

    private final NoticeMapper noticeMapper;

    /**
     * 관리자 공지 목록 조회
     */
    @Override
    public Page<NoticeAdminRes> readNoticeList(int page, int size) {

        log.info("[공지사항-관리자] 목록 조회 시작 - page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("pinned"),
                        Sort.Order.desc("createdAt")
                )
        );

        Page<NoticeAdminRes> noticeAdminList = noticeRepository.findAll(pageable)
                .map(noticeMapper::toNoticeAdminRes);

        log.info("[공지사항-관리자] 목록 조회 완료 - totalElements={}", noticeAdminList.getTotalElements());

        return noticeAdminList;
    }

    /**
     * 관리자 공지 상세 조회
     */

    @Override
    public NoticeAdminDetailRes readNoticeDetail(Long noticeId) {

        log.info("[공지사항-관리자] 상세 조회 시작 - noticeId={}", noticeId);
        Notice notice = loadNotice(noticeId);

        log.info("[공지사항-관리자] 상세 조회 완료 - noticeId={}", noticeId);

        return noticeMapper.toNoticeAdminDetailRes(notice);
    }

    /**
     * 관리자 공지 사항 생성
     */
    @Override
    @Transactional
    public void createNotice(Long userId, NoticeReq noticeReq) {
        log.info("[공지사항-관리자] 공지 생성 시작 - userId={}, title={}", userId, noticeReq.title());

        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Notice notice = Notice.builder()
                .title(noticeReq.title())
                .content(noticeReq.content())
                .pinned(noticeReq.pinned())
                .visible(noticeReq.visible())
                .createdBy(user)
                .build();

        noticeRepository.save(notice);

        log.info("[공지사항-관리자] 공지 생성 완료 - noticeId={}", notice.getNoticeId());
    }

    /**
     * 관리자 공지 사항 수정
     */
    @Override
    @Transactional
    public void modifyNotice(Long userId, Long noticeId, NoticeReq noticeReq) {
        log.info("[공지사항-관리자] 공지 수정 시작 - userId={}, noticeId={}", userId, noticeId);

        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Notice notice = loadNotice(noticeId);
        notice.modify(noticeReq.title(), noticeReq.content(), noticeReq.pinned(), noticeReq.visible(), user);

        log.info("[공지사항-관리자] 공지 수정 완료 - noticeId={}", notice.getNoticeId());
    }

    /**
     * 관리자 공지 사항 삭제
     */
    @Override
    @Transactional
    public void deleteNotice(Long noticeId) {
        log.info("[공지사항-관리자] 공지 삭제 시작 - noticeId={}", noticeId);

        Notice notice = loadNotice(noticeId);
        noticeRepository.delete(notice);

        log.info("[공지사항-관리자] 공지 삭제 완료 - noticeId={}", noticeId);
    }

    private Notice loadNotice(Long noticeId) {
        Notice notice = noticeRepository.findByNoticeId(noticeId).orElseThrow(() -> {
            log.warn("[공지사항-관리자] 공지 삭제 실패 - 공지 없음 noticeId={}", noticeId);
            return new BusinessException(ErrorCode.NOTICE_NOT_FOUND);
        });

        return notice;
    }
}
