package com.goorm.roomflow.domain.user.controller;

import com.goorm.roomflow.domain.user.dto.SignupRequestDTO;
import com.goorm.roomflow.domain.user.dto.UserTO;
import com.goorm.roomflow.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Tag(name = "UserController", description = "유저 페이지 뷰 컨트롤러")
@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 로그인 페이지 렌더링 (POST /users/login 은 Spring Security formLogin이 처리)
    @GetMapping("/login")
    public String loginPage(
            @RequestParam(required = false) String error,
            @ModelAttribute("successMsg") String successMsg,
            Model model) {

        if (error != null) {
            model.addAttribute("errorMsg", "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return "user/login";
    }

    // 내 예약 목록 페이지 렌더링
    @GetMapping("/reservationlist")
    public String reservationList(HttpSession session,
                                  @RequestParam(defaultValue = "upcoming") String tab,
                                  @RequestParam(required = false) String startDate,
                                  @RequestParam(required = false) String endDate,
                                  Model model) {
        UserTO loginUser = (UserTO) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/users/login";
        }
        model.addAttribute("reservations", userService.getReservationsByUserId(loginUser.getUserId(), tab, startDate, endDate));
        model.addAttribute("activeTab", tab);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("loginUser", loginUser);
        return "user/reservation-list";
    }

    // 회원가입 페이지 렌더링
    @GetMapping("/signup")
    public String signupPage() {
        return "user/signup";
    }

    // 마이페이지 렌더링
    @GetMapping("/mypage")
    public String myPage(HttpSession session, Model model) {
        UserTO loginUser = (UserTO) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/users/login";
        }
        model.addAttribute("loginUser", loginUser);
        return "user/mypage";
    }

    // 이름 변경
    @PostMapping("/mypage/name")
    public String updateName(@RequestParam String name,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        UserTO loginUser = (UserTO) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/users/login";
        }
        try {
            userService.updateName(loginUser.getEmail(), name);
            loginUser.setName(name);
            session.setAttribute("loginUser", loginUser);
            redirectAttributes.addFlashAttribute("successMsg", "이름이 변경되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/users/mypage";
    }

    // 비밀번호 변경
    @PostMapping("/mypage/password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String newPasswordConfirm,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        UserTO loginUser = (UserTO) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/users/login";
        }
        if (!newPassword.equals(newPasswordConfirm)) {
            redirectAttributes.addFlashAttribute("passwordError", "새 비밀번호가 일치하지 않습니다.");
            return "redirect:/users/mypage";
        }
        try {
            userService.changePassword(loginUser.getEmail(), currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("passwordSuccess", "비밀번호가 변경되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("passwordError", e.getMessage());
        }
        return "redirect:/users/mypage";
    }

    // 회원 탈퇴
    @PostMapping("/mypage/delete")
    public String deleteAccount(HttpSession session,
                                RedirectAttributes redirectAttributes) {
        UserTO loginUser = (UserTO) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/users/login";
        }
        try {
            userService.deleteAccount(loginUser.getEmail());
            session.invalidate();
            redirectAttributes.addFlashAttribute("successMsg", "회원탈퇴가 완료되었습니다.");
            return "redirect:/users/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/users/mypage";
        }
    }

    // 회원가입 처리
    @PostMapping("/signup")
    public String signup(@ModelAttribute SignupRequestDTO request,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        try {
            userService.signup(request);
            redirectAttributes.addFlashAttribute("successMsg", "회원가입이 완료되었습니다. 로그인해주세요.");
            return "redirect:/users/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("name", request.getName());
            model.addAttribute("email", request.getEmail());
            return "user/signup";
        }
    }
}
