package com.goorm.roomflow.domain.user.controller;

import com.goorm.roomflow.domain.user.dto.UserTO;
import com.goorm.roomflow.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "UserController", description = "유저 페이지 뷰 컨트롤러")
@Controller
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    // 로그인 페이지 렌더링
    @GetMapping("/login")
    public String loginPage(
            @RequestParam(required = false) String error,
            Model model) {

        if (error != null) {
            model.addAttribute("errorMsg", "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return "user/login";
    }

    // 일반 로그인 처리
    @PostMapping("/login")
    public String login(
            @RequestParam String email,
            @RequestParam String password,
            Model model) {

        UserTO to = userService.login(email, password);

        if (to == null) {
            model.addAttribute("errorMsg", "이메일 또는 비밀번호가 올바르지 않습니다.");
            return "user/login";
        }

        return "redirect:/rooms";
    }
}