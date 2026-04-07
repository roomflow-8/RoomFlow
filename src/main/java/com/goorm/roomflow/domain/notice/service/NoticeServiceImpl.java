package com.goorm.roomflow.domain.notice.service;

import com.goorm.roomflow.domain.notice.dto.response.NoticeDetailRes;
import com.goorm.roomflow.domain.notice.dto.response.NoticeRes;
import com.goorm.roomflow.domain.notice.entity.Notice;
import com.goorm.roomflow.domain.notice.mapper.NoticeMapper;
import com.goorm.roomflow.domain.notice.repository.NoticeRepository;
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
public class NoticeServiceImpl implements NoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeMapper noticeMapper;

    /**
     * 사용자 공지 목록 조회
     */
    @Override
    public Page<NoticeRes> readNoticeList(int page, int size) {
        log.info("[공지사항-사용자] 목록 조회 시작 - page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("pinned"),
                        Sort.Order.desc("createdAt")
                )
        );

        Page<NoticeRes> noticeList = noticeRepository.findByVisibleTrue(pageable).map(noticeMapper::toNoticeRes);

        log.info("[공지사항-사용자] 목록 조회 완료 - totalElements: {}", noticeList.getTotalElements());

        return noticeList;
    }

    /**
     * 사용자 공지 상세 조회
     */
    @Override
    @Transactional
    public NoticeDetailRes readNotice(Long noticeId) {
        log.info("[공지사항-사용자] 상세 조회 시작 -  noticeId={}", noticeId);

        Notice notice = noticeRepository.findByNoticeIdAndVisibleTrue(noticeId)
                .orElseThrow(() -> {
                    log.warn("[공지사항-사용자] 상세 조회 실패 - 공지 없음 noticeId={}", noticeId);
                    return new BusinessException(ErrorCode.NOTICE_NOT_FOUND);
                });

        notice.increaseViewCount();

        log.info("[공지사항-사용자] 상세 조회 완료 -  noticeId={}", noticeId);
        return noticeMapper.toNoticeDetailRes(notice);
    }

    @Override
    public NoticeRes findPreviewNotice() {
        return noticeRepository.findFirstByVisibleTrueOrderByPinnedDescCreatedAtDesc()
                .map(noticeMapper::toNoticeRes)
                .orElse(null);
    }
}
