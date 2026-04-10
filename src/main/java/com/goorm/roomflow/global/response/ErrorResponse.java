package com.goorm.roomflow.global.response;

import com.goorm.roomflow.global.code.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

	private boolean success;
	private String code;
	private String message;
	private List<ValidationError> errors;
	private LocalDateTime timestamp;

	public static ErrorResponse of(ErrorCode errorCode) {
		return ErrorResponse.builder()
				.success(false)
				.code(errorCode.getCode())
				.message(errorCode.getMessage())
				.errors(Collections.emptyList())
				.timestamp(LocalDateTime.now())
				.build();
	}

	public static ErrorResponse of(ErrorCode errorCode, List<ValidationError> errors) {
		return ErrorResponse.builder()
				.success(false)
				.code(errorCode.getCode())
				.message(errorCode.getMessage())
				.errors(errors)
				.timestamp(LocalDateTime.now())
				.build();
	}

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ValidationError {
		private String field;
		private Object value;
		private String reason;

		public static ValidationError of(String field, Object value, String reason) {
			return ValidationError.builder()
					.field(field)
					.value(value)
					.reason(reason)
					.build();
		}
	}
}