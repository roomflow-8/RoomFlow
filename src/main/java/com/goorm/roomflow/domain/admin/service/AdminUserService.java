package com.goorm.roomflow.domain.admin.service;

import com.goorm.roomflow.domain.admin.dto.AdminUserDTO;
import com.goorm.roomflow.domain.reservation.entity.ReservationStatus;
import com.goorm.roomflow.domain.reservation.repository.ReservationRepository;
import com.goorm.roomflow.domain.user.entity.User;
import com.goorm.roomflow.domain.user.repository.UserJpaRepository;
import com.goorm.roomflow.domain.user.service.UserService;
import com.goorm.roomflow.global.code.ErrorCode;
import com.goorm.roomflow.global.exception.BusinessException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserJpaRepository userJpaRepository;
    private final ReservationRepository reservationRepository;
    private final UserService userService;
    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String senderEmail; // @RequiredArgsConstructor 제외 대상 (@Value는 필드 주입)

    /*
     * 전체 회원 목록 조회
     * - 탈퇴 대기 회원 포함 전체 반환
     * - 예약/취소 건수는 reservation 테이블에서 직접 집계
     */
    @Transactional(readOnly = true)
    public List<AdminUserDTO> getAllUsers() {
        return userJpaRepository.findAll().stream()
                .map(this::toAdminUserDTO)
                .toList();
    }

    /*
     * 회원 상세 조회
     * - GET /admin/users/{userId}
     */
    @Transactional(readOnly = true)
    public AdminUserDTO getUserById(Long userId) {
        User user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return toAdminUserDTO(user);
    }

    /*
     * 탈퇴 대기 회원 목록 조회
     * - deletedAt != null 인 회원만 반환
     * - 스케줄러(UserDeleteScheduler)가 7일 후 hard delete 처리
     */
    @Transactional(readOnly = true)
    public List<AdminUserDTO> getPendingDeleteUsers() {
        return userJpaRepository.findAllByDeletedAtIsNotNull().stream()
                .map(this::toAdminUserDTO)
                .toList();
    }

    /*
     * 회원 검색 (이름 또는 이메일 키워드)
     * - 대소문자 무시
     */
    @Transactional(readOnly = true)
    public List<AdminUserDTO> searchUsers(String keyword) {
        return userJpaRepository
                .findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword, keyword)
                .stream()
                .map(this::toAdminUserDTO)
                .toList();
    }

    /*
     * 관리자 즉시 강제 삭제
     * - UserDeleteScheduler의 7일 대기 없이 바로 hard delete
     * - 탈퇴 상태(deletedAt != null)가 아닌 회원은 삭제 불가
     */
    @Transactional
    public void forceDeleteUser(Long userId) {
        log.info("[AdminUserService] 관리자 강제 삭제 요청 - userId={}", userId);
        // UserService.hardDeleteUserById 재사용 (소셜 계정 포함 삭제 처리)
        userService.hardDeleteUserById(userId);
        log.info("[AdminUserService] 관리자 강제 삭제 완료 - userId={}", userId);
    }

    /*
     * 관리자 → 특정 회원에게 이메일 전송
     * - subject, content 를 직접 지정해서 발송
     * - 기존 EmailService는 인증코드 전용이라 JavaMailSender 직접 사용
     */
    public void sendEmailToUser(Long userId, String subject, String content) throws MessagingException, java.io.UnsupportedEncodingException {
        // 수신자 이메일 조회
        User user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        log.info("[AdminUserService] 이메일 전송 요청 - userId={}, email={}, subject={}", userId, user.getEmail(), subject);

        MimeMessage message = javaMailSender.createMimeMessage();
        // multipart true - 인라인 이미지 첨부를 위해 multipart 모드 활성화
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(user.getEmail());
        helper.setSubject(subject);
        helper.setFrom(senderEmail, "RoomFlow 관리자");

        // 이메일 HTML 템플릿
        String html =
            "<div style='max-width:600px; margin:0 auto; font-family:Ar정ial, sans-serif;'>" +

                // 헤더 (로고 영역) - cid:logo 로 인라인 이미지 참조 / TODO: S3 업로드 후 img src를 S3 URL로 교체
                "<div style='background-color:#111827; padding:24px 32px; display:flex; align-items:center; gap:12px;'>" +
                    "<div style='background-color:#ffffff; padding:4px; border-radius:6px;'>" +
                        "<img src='cid:logo' alt='RoomFlow' style='width:40px; height:40px; object-fit:contain; display:block;'>" +
                    "</div>" +
                    "<span style='color:white; font-size:28px; font-weight:bold; letter-spacing:2px; margin-left:20px; align-self:center; padding-top:8px;'>RoomFlow</span>" +
                "</div>" +

                // 제목 영역
                "<div style='background-color:#f9fafb; padding:24px 32px; border-bottom:1px solid #e5e7eb;'>" +
                    "<h2 style='margin:0; font-size:18px; color:#111827;'>" + subject + "</h2>" +
                "</div>" +

                // 본문 내용
                "<div style='padding:32px; background-color:#ffffff; color:#374151; font-size:15px; line-height:1.7;'>" +
                    content +
                "</div>" +

                // 푸터
                "<div style='padding:16px 32px; background-color:#f3f4f6; text-align:center;'>" +
                    "<p style='margin:0; font-size:12px; color:#9ca3af;'>본 메일은 RoomFlow 관리자가 발송한 메일입니다.</p>" +
                "</div>" +

            "</div>";

        helper.setText(html, true);

        // 로고 이미지 인라인 첨부 - static/images/logo_tmp.png
        // TODO: S3 업로드 후 ClassPathResource 대신 S3 URL 사용
        org.springframework.core.io.ClassPathResource logo =
                new org.springframework.core.io.ClassPathResource("static/images/logo_tmp.png");
        if (logo.exists()) {
            helper.addInline("logo", logo);
        }

        javaMailSender.send(message);
        log.info("[AdminUserService] 이메일 전송 완료 - userId={}", userId);
    }

    // User 엔티티 → AdminUserDTO 변환
    private AdminUserDTO toAdminUserDTO(User user) {
        // 해당 유저의 전체 예약 건수
        long reservationCount = reservationRepository.countByUserUserId(user.getUserId());

        // 해당 유저의 취소된 예약 건수
        long cancelCount = reservationRepository.countByUserUserIdAndStatus(
                user.getUserId(), ReservationStatus.CANCELLED);

        return new AdminUserDTO(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getCreatedAt(),
                user.getDeletedAt(),
                reservationCount,
                cancelCount
        );
    }
}
