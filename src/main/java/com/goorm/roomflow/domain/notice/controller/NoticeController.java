package com.goorm.roomflow.domain.notice.controller;

import com.goorm.roomflow.domain.notice.dto.response.NoticeDetailRes;
import com.goorm.roomflow.domain.notice.dto.response.NoticeRes;
import com.goorm.roomflow.domain.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/notices")
public class NoticeController {
    private final NoticeService noticeService;

    @GetMapping
    public String noticeList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model
    ) {
        Page<NoticeRes> noticePage = noticeService.readNoticeList(page - 1, size);

        model.addAttribute("noticePage", noticePage);
        model.addAttribute("notices", noticePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("size", size);

        return "notice/notice-list.html";
    }

    @GetMapping("/{noticeId}")
    public String noticeDetail(
            @PathVariable Long noticeId,
            Model model
    ) {

        NoticeDetailRes notice = noticeService.readNotice(noticeId);
        model.addAttribute("notice", notice);

        return "notice/notice-detail";
    }
}
