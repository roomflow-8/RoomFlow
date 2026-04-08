package com.goorm.roomflow.domain.reservation.controller;

import com.goorm.roomflow.domain.reservation.dto.request.ReservationPolicyUpdateReq;
import com.goorm.roomflow.domain.reservation.service.AdminReservationPolicyService;
import com.goorm.roomflow.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/policies")
public class AdminReservationPolicyController {

    private final AdminReservationPolicyService adminReservationPolicyService;

    /**
     * 정책 목록 조회
     */
    @GetMapping
    public String policyList(Model model) {
        model.addAttribute("policies", adminReservationPolicyService.getPolicyList());
        return "admin/system/policy-list";
    }

    /**
     * 정책 값 수정
     */
    @PostMapping("/{policyId}/edit")
    public String updatePolicy(
            @PathVariable Long policyId,
            @ModelAttribute ReservationPolicyUpdateReq policyUpdateReq,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminReservationPolicyService.updatePolicy(policyId, policyUpdateReq);

            redirectAttributes.addFlashAttribute("message", "정책 값이 수정되었습니다.");
            redirectAttributes.addFlashAttribute("alertType", "success");
        } catch (IllegalArgumentException | BusinessException e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("alertType", "error");
        }

        return "redirect:/admin/policies";
    }

}
