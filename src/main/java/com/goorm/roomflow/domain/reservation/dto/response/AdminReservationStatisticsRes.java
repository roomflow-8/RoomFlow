package com.goorm.roomflow.domain.reservation.dto.response;

/**
 * 예약 통계 Response
 */
public record AdminReservationStatisticsRes(
		Long total,        // 전체 예약 수
		Long confirmed,    // 확정된 예약 수
		Long cancelled,      // 취소된 예약 수
		Long today         // 금일 예약 수
) {

	public static AdminReservationStatisticsRes of(
			Long total,
			Long confirmed,
			Long cancelled,
			Long today
	) {
		return new AdminReservationStatisticsRes(
				total != null ? total : 0L,
				confirmed != null ? confirmed : 0L,
				cancelled != null ? cancelled : 0L,
				today != null ? today : 0L
		);
	}

	// Thymeleaf에서 사용하기 편하도록 포맷팅 메서드
	public String getFormattedTotal() {
		return String.format("%,d건", total);
	}

	public String getFormattedConfirmed() {
		return String.format("%,d건", confirmed);
	}

	public String getFormattedCancelled() {
		return String.format("%,d건", cancelled);
	}

	public String getFormattedToday() {
		return String.format("%,d건", today);
	}
}

