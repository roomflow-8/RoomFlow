package com.goorm.roomflow.global.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Aspect
@Component
@Slf4j
public class ReservationPerformanceAspect {

	@Around("execution(* com.goorm.roomflow.domain.reservation.service.ReservationServiceImpl.addEquipmentsToReservation(..))")
	public Object measureReservationPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		int activeThreadsBefore = Thread.activeCount();

		log.info("🟢 예약 처리 시작 - activeThreads: {}", activeThreadsBefore);

		try {
			Object result = joinPoint.proceed(); // 실제 서비스 메서드 호출
			return result;
		} finally {
			stopWatch.stop();
			int activeThreadsAfter = Thread.activeCount();
			log.info("✅ 예약 처리 완료 - duration: {}ms, activeThreadsBefore: {}, activeThreadsAfter: {}",
					stopWatch.getTotalTimeMillis(), activeThreadsBefore, activeThreadsAfter);
		}
	}
}