package com.goorm.roomflow.global.exception;

import com.goorm.roomflow.domain.equipment.controller.EquipmentController;
import com.goorm.roomflow.domain.equipment.controller.EquipmentReservationRestController;
import com.goorm.roomflow.domain.reservation.controller.ReservationController;
import com.goorm.roomflow.domain.reservation.controller.ReservationRestController;
import com.goorm.roomflow.domain.room.controller.MeetingRoomController;
import com.goorm.roomflow.domain.room.controller.MeetingRoomRestController;
import com.goorm.roomflow.domain.user.controller.UserController;
import com.goorm.roomflow.domain.user.controller.UserRestController;
import com.goorm.roomflow.global.code.ErrorCode;
import com.goorm.roomflow.global.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@ControllerAdvice(assignableTypes = {
		ReservationController.class,
		EquipmentController.class,
		MeetingRoomController.class,
		UserController.class,
})
public class GlobalViewExceptionHandler {

	/**
	 * 그 외 모든 예외 처리 (최종 fallback)
	 * - 예상하지 못한 예외를 처리하여 서버 에러로 응답
	 * - 내부 구현 정보는 노출하지 않음 (보안)
	 */
	@ExceptionHandler(Exception.class)
	public String handleException(Exception e, HttpServletRequest request, Model model) {

		log.error(e.getMessage(), e);

		model.addAttribute("status", ErrorCode.INTERNAL_ERROR.getStatus().value());
		model.addAttribute("errorCode", ErrorCode.INTERNAL_ERROR.getCode());
		model.addAttribute("message", ErrorCode.INTERNAL_ERROR.getMessage());
		model.addAttribute("path", request.getRequestURI());

		return "error/error";
	}
}

