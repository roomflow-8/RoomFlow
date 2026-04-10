package com.goorm.roomflow.domain.admin.controller;

import com.goorm.roomflow.domain.admin.dto.AdminUserDTO;
import com.goorm.roomflow.domain.admin.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.mail.MessagingException;
import java.io.UnsupportedEncodingException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    /*
     * 회원 목록 페이지
     * - GET /admin/users
     * - 전체 회원 조회 (탈퇴 대기 포함)
     */
    @GetMapping
    public String usersPage(Model model) {
        List<AdminUserDTO> users = adminUserService.getAllUsers();
        model.addAttribute("users", users);
        return "admin/users";
    }

    /*
     * 회원 상세 조회
     * - GET /admin/users/{userId}
     * - 특정 회원의 상세 정보 반환
     */
    @GetMapping("/{userId}")
    public String userDetail(@PathVariable Long userId, Model model) {
        AdminUserDTO user = adminUserService.getUserById(userId);
        model.addAttribute("user", user);
        return "admin/user-detail";
    }

    /*
     * 회원 검색
     * - GET /admin/users/search?keyword=
     * - 이름 또는 이메일 키워드로 검색 (대소문자 무시)
     */
    @GetMapping("/search")
    public String searchUsers(
            @RequestParam(required = false) String keyword,
            Model model
    ) {
        List<AdminUserDTO> users = (keyword != null && !keyword.isBlank())
                ? adminUserService.searchUsers(keyword)
                : adminUserService.getAllUsers();

        model.addAttribute("users", users);
        model.addAttribute("keyword", keyword);
        return "admin/users";
    }

    /*
     * 탈퇴 대기 회원 목록
     * - GET /admin/users/deleted
     * - deletedAt != null 인 회원만 반환
     */
    @GetMapping("/deleted")
    public String deletedUsers(Model model) {
        List<AdminUserDTO> users = adminUserService.getPendingDeleteUsers();
        model.addAttribute("users", users);
        model.addAttribute("tab", "pending");
        return "admin/users";
    }

    /*
     * 회원 수동 탈퇴 (즉시 hard delete)
     * - POST /admin/users/{userId}/forcedelete
     * - 탈퇴 상태(deletedAt != null)인 회원만 삭제 가능
     */
    @PostMapping("/{userId}/forcedelete")
    public String forceDelete(
            @PathVariable Long userId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminUserService.forceDeleteUser(userId);
            redirectAttributes.addFlashAttribute("successMsg", "회원이 즉시 삭제되었습니다.");
        } catch (Exception e) {
            log.error("[AdminUserController] 강제 삭제 실패 - userId={}", userId, e);
            redirectAttributes.addFlashAttribute("errorMsg", "삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
        return "redirect:/admin/users/deleted";
    }

    /*
     * 회원에게 이메일 전송
     * - POST /admin/users/{userId}/email
     * - subject, content 파라미터로 제목과 내용 전달
     */
    @PostMapping("/{userId}/email")
    public String sendEmail(
            @PathVariable Long userId,
            @RequestParam String subject,
            @RequestParam String content,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminUserService.sendEmailToUser(userId, subject, content);
            redirectAttributes.addFlashAttribute("successMsg", "이메일이 전송되었습니다.");
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("[AdminUserController] 이메일 전송 실패 - userId={}", userId, e);
            redirectAttributes.addFlashAttribute("errorMsg", "이메일 전송 중 오류가 발생했습니다: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
