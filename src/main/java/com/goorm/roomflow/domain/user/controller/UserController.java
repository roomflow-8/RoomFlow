package com.goorm.roomflow.domain.user.controller;

import com.goorm.roomflow.domain.user.dto.SignupRequestDTO;
import com.goorm.roomflow.domain.user.dto.UserDto;
import com.goorm.roomflow.domain.user.service.CustomUser;
import com.goorm.roomflow.domain.user.service.EmailService;
import com.goorm.roomflow.domain.user.service.UserService;
import com.goorm.roomflow.global.exception.BusinessException;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Tag(name = "UserController", description = "유저 페이지 뷰 컨트롤러")
@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;
	private final EmailService emailService;

	// 로그인 페이지 렌더링 (POST /users/login 은 Spring Security formLogin이 처리)
	@GetMapping("/login")
	public String loginPage(
			@RequestParam(required = false) String error,
			@RequestParam(required = false) String email,
			HttpSession session,
			Model model) {

		if ("LOGIN_FAILED".equals(error)) {
			model.addAttribute("errorMsg", "이메일 또는 비밀번호가 올바르지 않습니다.");
		}

		if ("USER_DELETED".equals(error)) {
			model.addAttribute("showRestoreAlert", true);
			model.addAttribute("restoreEmail", email);
		}
		return "user/login";
	}

	// 계정 복구
	@PostMapping("/restore")
	public String restoreUser(
			@RequestParam String email,
			HttpSession session,
			RedirectAttributes redirectAttributes
	) {

		userService.restoreUserByEmail(email);

		session.removeAttribute("restoreEmail");

		redirectAttributes.addFlashAttribute("message", "계정이 복구되었습니다.\n다시 로그인해 주세요.");
		redirectAttributes.addFlashAttribute("alertType", "success");

		return "redirect:/users/login";
	}

	// 내 예약 목록 페이지 렌더링
	@GetMapping("/reservationlist")
	public String reservationList(@AuthenticationPrincipal CustomUser currentUser,
								  @RequestParam(defaultValue = "upcoming") String tab,
								  @RequestParam(required = false) String startDate,
								  @RequestParam(required = false) String endDate,
								  Model model) {
		if (currentUser == null) {
			return "redirect:/users/login";
		}
		UserDto loginUser =
				userService.getUserByEmail(currentUser.getEmail());

		model.addAttribute("reservations", userService.getReservationsByUserId(currentUser.getUserId(), tab, startDate, endDate));
		model.addAttribute("activeTab", tab);
		model.addAttribute("startDate", startDate);
		model.addAttribute("endDate", endDate);
		model.addAttribute("loginUser", loginUser);
		return "user/reservation-list";
	}

	// 회원가입 페이지 렌더링
	@GetMapping("/signup")
	public String signupPage(HttpSession session, Model model) {
		Boolean emailVerified = (Boolean) session.getAttribute("emailVerified");
		model.addAttribute("emailVerified", emailVerified != null && emailVerified);
		return "user/signup";
	}

	// 마이페이지 렌더링
	@GetMapping("/mypage")
	public String myPage(@AuthenticationPrincipal CustomUser currentUser,
						 Model model) {
		if (currentUser == null) {
			return "redirect:/users/login";
		}
		UserDto loginUser =
				userService.getUserByEmail(currentUser.getEmail());

		model.addAttribute("loginUser", loginUser);
		return "user/mypage";
	}

	// 이름 변경
	@PostMapping("/mypage/name")
	public String updateName(@RequestParam String name,
							 @AuthenticationPrincipal CustomUser currentUser,
							 RedirectAttributes redirectAttributes) {

		if (currentUser == null) {
			return "redirect:/users/login";
		}
		try {
			userService.updateName(currentUser.getEmail(), name);

			redirectAttributes.addFlashAttribute("successMsg", "이름이 변경되었습니다.");
		} catch (IllegalArgumentException e) {
			redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
		}
		return "redirect:/users/mypage";
	}

	// 비밀번호 변경
	@PostMapping("/mypage/password")
	public String changePassword(
			@AuthenticationPrincipal CustomUser currentUser,
			@RequestParam String currentPassword,
			@RequestParam String newPassword,
			@RequestParam String newPasswordConfirm,
			RedirectAttributes redirectAttributes) {

		if (currentUser == null) {
			return "redirect:/users/login";
		}
		if (!newPassword.equals(newPasswordConfirm)) {
			redirectAttributes.addFlashAttribute("passwordError", "새 비밀번호가 일치하지 않습니다.");
			return "redirect:/users/mypage";
		}
		try {
			userService.changePassword(currentUser.getEmail(), currentPassword, newPassword);
			redirectAttributes.addFlashAttribute("passwordSuccess", "비밀번호가 변경되었습니다.");
		} catch (IllegalArgumentException e) {
			redirectAttributes.addFlashAttribute("passwordError", e.getMessage());
		}
		return "redirect:/users/mypage";
	}

	// 회원 탈퇴
	@PostMapping("/mypage/delete")
	public String deleteAccount(
			@AuthenticationPrincipal CustomUser currentUser,
			HttpSession session,
			RedirectAttributes redirectAttributes
	) {

		if (currentUser == null) {
			return "redirect:/users/login";
		}
		try {
			userService.deleteAccount(currentUser.getUserId());
			session.invalidate();
			redirectAttributes.addFlashAttribute("alertType", "success");
			redirectAttributes.addFlashAttribute(
					"message",
					"회원탈퇴가 완료되었습니다.<br>7일 이내 로그인하면 복구가 가능합니다."
			);

			return "redirect:/users/login";
		} catch (BusinessException e) {

			redirectAttributes.addFlashAttribute("alertType", "error");
			redirectAttributes.addFlashAttribute("message", e.getMessage());
			return "redirect:/users/mypage";
		}
	}

	// 인증번호 발송
	@PostMapping("/email/send")
	@ResponseBody
	public ResponseEntity<String> sendEmail(@RequestParam String email,
											HttpSession session) {
		try {
			String code = emailService.sendSimpleMessage(email);
			session.setAttribute("emailCode", code);          // 인증번호를 세션에 저장
			session.setAttribute("emailTarget", email);       // 인증 요청한 이메일 저장
			session.setAttribute("emailVerified", false);     // 아직 인증 안 됨
			return ResponseEntity.ok("인증번호가 발송되었습니다.");
		} catch (Exception e) {
			return ResponseEntity.badRequest().body("이메일 발송에 실패했습니다.");
		}
	}

	// 인증번호 확인
	@PostMapping("/email/verify")
	@ResponseBody
	public ResponseEntity<String> verifyEmail(@RequestParam String code,
											  HttpSession session) {
		String savedCode = (String) session.getAttribute("emailCode");

		if (savedCode == null) {
			return ResponseEntity.badRequest().body("인증번호를 먼저 발송해주세요.");
		}
		if (!savedCode.equals(code)) {
			return ResponseEntity.badRequest().body("인증번호가 올바르지 않습니다.");
		}

		session.setAttribute("emailVerified", true);                              // 인증 완료 표시
		session.setAttribute("emailVerifiedFor", session.getAttribute("emailTarget")); // 어떤 이메일로 인증했는지 저장
		return ResponseEntity.ok("이메일 인증이 완료되었습니다.");
	}

	// 회원가입 처리
	@PostMapping("/signup")
	public String signup(@ModelAttribute SignupRequestDTO request,
						 HttpSession session,
						 Model model,
						 RedirectAttributes redirectAttributes) {

		// 이메일 인증 여부 확인
		Boolean emailVerified = (Boolean) session.getAttribute("emailVerified");
		String emailVerifiedFor = (String) session.getAttribute("emailVerifiedFor");
		if (emailVerified == null || !emailVerified || !request.getEmail().equals(emailVerifiedFor)) {
			model.addAttribute("errorMsg", "이메일 인증을 완료해주세요.");
			model.addAttribute("name", request.getName());
			model.addAttribute("email", request.getEmail());
			model.addAttribute("emailVerified", false);
			return "user/signup";
		}

		try {
			userService.signup(request);
			session.removeAttribute("emailCode");
			session.removeAttribute("emailTarget");
			session.removeAttribute("emailVerified");
			session.removeAttribute("emailVerifiedFor");
			redirectAttributes.addFlashAttribute("alertType", "success");
			redirectAttributes.addFlashAttribute("message", "회원가입이 완료되었습니다.\n로그인해주세요.");
			return "redirect:/users/login";
		} catch (IllegalArgumentException e) {
			model.addAttribute("errorMsg", e.getMessage());
			model.addAttribute("name", request.getName());
			model.addAttribute("email", request.getEmail());
			model.addAttribute("emailVerified", true);  // 인증은 완료됐으므로 유지
			return "user/signup";
		}
	}
}
