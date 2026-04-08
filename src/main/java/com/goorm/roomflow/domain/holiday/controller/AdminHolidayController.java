package com.goorm.roomflow.domain.holiday.controller;

import com.goorm.roomflow.domain.holiday.dto.request.AdminHolidayReq;
import com.goorm.roomflow.domain.holiday.service.AdminHolidayService;
import com.goorm.roomflow.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/holidays")
public class AdminHolidayController {

    private final AdminHolidayService adminHolidayService;

    /**
     * 휴무일 목록 조회
     */
    @GetMapping
    public String holidayList(Model model) {
        model.addAttribute("holidays", adminHolidayService.getHolidayList());
        return "admin/system/holiday-list";
    }

    /**
     * 휴무일 등록
     */
    @PostMapping("/create")
    public String createHoliday(@ModelAttribute("createForm") AdminHolidayReq request,
                                RedirectAttributes redirectAttributes) {
        try {
            adminHolidayService.createHoliday(request);
            redirectAttributes.addFlashAttribute("message", "휴무일이 등록되었습니다.");
            redirectAttributes.addFlashAttribute("alertType", "success");
        } catch (BusinessException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("alertType", "error");
        }

        return "redirect:/admin/holidays";
    }

    /**
     * 휴무일 수정
     */
    @PostMapping("/{holidayId}/edit")
    public String modifyHoliday(@PathVariable Long holidayId,
                                @ModelAttribute("modifyForm") AdminHolidayReq request,
                                RedirectAttributes redirectAttributes) {
        try {
            adminHolidayService.modifyHoliday(holidayId, request);
            redirectAttributes.addFlashAttribute("message", "휴무일이 수정되었습니다.");
            redirectAttributes.addFlashAttribute("alertType", "success");
        } catch (BusinessException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("alertType", "error");
        }

        return "redirect:/admin/holidays";
    }

    /**
     * 휴무일 삭제
     */
    @PostMapping("/{holidayId}/delete")
    public String deleteHoliday(@PathVariable Long holidayId,
                                RedirectAttributes redirectAttributes) {
        try {
            adminHolidayService.deleteHoliday(holidayId);
            redirectAttributes.addFlashAttribute("message", "휴무일이 삭제되었습니다.");
            redirectAttributes.addFlashAttribute("alertType", "success");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("alertType", "error");
        }

        return "redirect:/admin/holidays";
    }

    /**
     * 휴무일 활성/비활성 상태 변경
     */
    @PostMapping("/{holidayId}/status")
    public String changeHolidayStatus(@PathVariable Long holidayId,
                                      @RequestParam boolean active,
                                      RedirectAttributes redirectAttributes) {
        try {
            adminHolidayService.changeHolidayStatus(holidayId, active);
            redirectAttributes.addFlashAttribute(
                    "message",
                    active ? "휴무일이 활성화되었습니다." : "휴무일이 비활성화되었습니다."
            );
            redirectAttributes.addFlashAttribute("alertType", "success");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("alertType", "error");
        }

        return "redirect:/admin/holidays";
    }

    /**
     * 공공 API 휴무일 가져오기
     */
    @PostMapping("/import")
    public String importPublicHolidays(@RequestParam int year,
                                       @RequestParam(required = false) Integer month,
                                       RedirectAttributes redirectAttributes) {
        try {
            int savedCount = adminHolidayService.importPublicHolidays(year, month);
            redirectAttributes.addFlashAttribute(
                    "message",
                    savedCount + "건의 공휴일을 가져왔습니다."
            );
            redirectAttributes.addFlashAttribute("alertType", "success");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("alertType", "error");
        }

        return "redirect:/admin/holidays";
    }
}