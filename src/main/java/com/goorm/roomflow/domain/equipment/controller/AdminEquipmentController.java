package com.goorm.roomflow.domain.equipment.controller;

import com.goorm.roomflow.domain.equipment.dto.request.AdminEquipmentReq;
import com.goorm.roomflow.domain.equipment.entity.EquipmentStatus;
import com.goorm.roomflow.domain.equipment.service.AdminEquipmentService;
import com.goorm.roomflow.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/equipments")
public class AdminEquipmentController {

    private final AdminEquipmentService adminEquipmentService;

    /**
     * 비품 목록 조회
     */
    @GetMapping
    public String equipmentList(Model model) {
        loadEquipmentList(model);
        initForms(model);
        return "admin/system/equipment-list";
    }

    /**
     * 비품 생성
     */
    @PostMapping("/create")
    public String createEquipment(
            @ModelAttribute("createForm") AdminEquipmentReq request,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminEquipmentService.createEquipment(request);

            redirectAttributes.addFlashAttribute("message", "비품이 등록되었습니다.");
            redirectAttributes.addFlashAttribute("alertType", "success");
            return "redirect:/admin/equipments";

        } catch (IllegalArgumentException | BusinessException e) {
            loadEquipmentList(model);
            initForms(model);
            model.addAttribute("openCreateModal", true);
            model.addAttribute("createErrorMessage", e.getMessage());
            return "admin/system/equipment-list";
        }
    }

    /**
     * 비품 수정
     */
    @PostMapping("/{equipmentId}/edit")
    public String modifyEquipment(
            @PathVariable Long equipmentId,
            @ModelAttribute("modifyForm") AdminEquipmentReq request,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        try {
            adminEquipmentService.modifyEquipment(equipmentId, request);

            redirectAttributes.addFlashAttribute("message", "비품 정보가 수정되었습니다.");
            redirectAttributes.addFlashAttribute("alertType", "success");
            return "redirect:/admin/equipments";

        } catch (IllegalArgumentException | BusinessException e) {
            loadEquipmentList(model);
            prepareCreateForm(model);

            model.addAttribute("openModifyModal", true);
            model.addAttribute("modifyTargetId", equipmentId);
            model.addAttribute("modifyErrorMessage", e.getMessage());

            return "admin/system/equipment-list";
        }
    }

    /**
     * 비품 상태 변경
     */
    @PostMapping("/{equipmentId}/status")
    public String changeEquipmentStatus(
            @PathVariable Long equipmentId,
            @RequestParam EquipmentStatus status,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        try {
            adminEquipmentService.changeEquipmentStatus(equipmentId, status);

            redirectAttributes.addFlashAttribute("message", "비품 상태가 변경되었습니다.");
            redirectAttributes.addFlashAttribute("alertType", "success");
            return "redirect:/admin/equipments";

        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("alertType", "error");
            return "redirect:/admin/equipments";
        }
    }

    private void loadEquipmentList(Model model) {
        model.addAttribute("equipments", adminEquipmentService.readEquipmentAdminList());
    }

    private void initForms(Model model) {
        prepareCreateForm(model);
        prepareModifyForm(model);
    }

    private void prepareCreateForm(Model model) {
        if (!model.containsAttribute("createForm")) {
            model.addAttribute("createForm",
                    new AdminEquipmentReq(null, 0, null, 0, null, null, null));
        }
    }

    private void prepareModifyForm(Model model) {
        if (!model.containsAttribute("modifyForm")) {
            model.addAttribute("modifyForm",
                    new AdminEquipmentReq(null, 0, null, 0, null, null, null));
        }
    }

}
