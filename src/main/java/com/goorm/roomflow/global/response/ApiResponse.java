package com.goorm.roomflow.global.response;

import com.goorm.roomflow.global.code.SuccessCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {

	private boolean success;
	private String code;
	private String message;
	private T data;

	public static <T> ResponseEntity<ApiResponse<T>> success(SuccessCode successCode, T data) {
		return ResponseEntity
				.status(successCode.getStatus())
				.body(ApiResponse.<T>builder()
						.success(true)
						.code(successCode.getCode())
						.message(successCode.getMessage())
						.data(data)
						.build());
	}

	public static ResponseEntity<ApiResponse<Void>> success(SuccessCode successCode) {
		return ResponseEntity
				.status(successCode.getStatus())
				.body(ApiResponse.<Void>builder()
						.success(true)
						.code(successCode.getCode())
						.message(successCode.getMessage())
						.data(null)
						.build());
	}
}