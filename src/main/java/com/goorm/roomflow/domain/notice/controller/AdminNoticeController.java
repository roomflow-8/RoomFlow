package com.goorm.roomflow.domain.notice.controller;

import com.goorm.roomflow.domain.notice.dto.request.NoticeReq;
import com.goorm.roomflow.domain.notice.dto.response.NoticeAdminDetailRes;
import com.goorm.roomflow.domain.notice.dto.response.NoticeAdminRes;
import com.goorm.roomflow.domain.notice.service.AdminNoticeService;
import com.goorm.roomflow.domain.user.service.CustomUser;
import com.goorm.roomflow.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/notices")
public class AdminNoticeController {

    private final AdminNoticeService adminNoticeService;

    /**
     * 관리자 공지 목록 조회
     */
    @GetMapping
    public String noticeList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model
    ) {

        Page<NoticeAdminRes> noticePage = adminNoticeService.readNoticeList(page - 1, size);

        model.addAttribute("noticePage", noticePage);
        model.addAttribute("notices", noticePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("size", size);

        return "admin/system/notice/notice-list.html";
    }

    /**
     * 관리자 공지 상세 조회
     */
    @GetMapping("/{noticeId}")
    public String noticeDetail(
            @PathVariable Long noticeId,
            Model model
    ) {

        NoticeAdminDetailRes notice = adminNoticeService.readNoticeDetail(noticeId);
        model.addAttribute("notice", notice);

        return "admin/system/notice/notice-detail";
    }

    /**
     * 관리자 공지 생성 페이지
     */
    @GetMapping("/create")
    public String createNoticePage(Model model) {
        if (!model.containsAttribute("createForm")) {
            model.addAttribute("createForm", new NoticeReq("", "", false, true));
        }

        return "admin/system/notice/notice-create";
    }

    /**
     * 공지 생성
     */
    @PostMapping("/create")
    public String createNotice(
            @AuthenticationPrincipal CustomUser currentUser,
            @ModelAttribute("createForm") NoticeReq request,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminNoticeService.createNotice(currentUser.getUserId(), request);

            redirectAttributes.addFlashAttribute("message", "공지사항이 등록되었습니다.");
            redirectAttributes.addFlashAttribute("alertType", "success");

            return "redirect:/admin/notices";

        } catch (BusinessException | IllegalArgumentException e) {
            model.addAttribute("createErrorMessage", e.getMessage());
            return "admin/system/notice/notice-create";
        }
    }

    /**
     * 관리자 공지 수정 페이지
     */
    @GetMapping("/{noticeId}/edit")
    public String modifyNoticePage(
            @PathVariable Long noticeId,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            NoticeAdminDetailRes notice = adminNoticeService.readNoticeDetail(noticeId);
            model.addAttribute("notice", notice);

            if (!model.containsAttribute("modifyForm")) {
                model.addAttribute("modifyForm", new NoticeReq(
                        notice.title(),
                        notice.content(),
                        notice.pinned(),
                        notice.visible()
                ));
            }

            return "admin/system/notice/notice-edit";

        } catch (BusinessException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("alertType", "error");
            return "redirect:/admin/notices";
        }
    }

    /**
     * 공지 수정
     */
    @PostMapping("/{noticeId}/edit")
    public String modifyNotice(
            @AuthenticationPrincipal CustomUser currentUser,
            @PathVariable Long noticeId,
            @ModelAttribute("modifyForm") NoticeReq request,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        try {
            adminNoticeService.modifyNotice(currentUser.getUserId(), noticeId, request);

            redirectAttributes.addFlashAttribute("message", "공지사항이 수정되었습니다.");
            redirectAttributes.addFlashAttribute("alertType", "success");

            return "redirect:/admin/notices/" + noticeId;

        } catch (BusinessException | IllegalArgumentException e) {
            NoticeAdminDetailRes notice = adminNoticeService.readNoticeDetail(noticeId);
            model.addAttribute("notice", notice);
            model.addAttribute("modifyErrorMessage", e.getMessage());

            return "admin/system/notice/notice-edit";
        }
    }

    /**
     * 공지 삭제
     */
    @PostMapping("/{noticeId}/delete")
    public String deleteNotice(
            @PathVariable Long noticeId,
            RedirectAttributes redirectAttributes
    ) {

        try {
            adminNoticeService.deleteNotice(noticeId);

            redirectAttributes.addFlashAttribute("message", "공지사항이 삭제되었습니다.");
            redirectAttributes.addFlashAttribute("alertType", "success");

        } catch (BusinessException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("alertType", "error");
        }

        return "redirect:/admin/notices";
    }
}