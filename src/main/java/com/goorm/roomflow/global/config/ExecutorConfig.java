package com.goorm.roomflow.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ExecutorConfig {

	@Bean(name = "equipmentReservationExecutor")
	public ExecutorService equipmentReservationExecutor() {
		return Executors.newVirtualThreadPerTaskExecutor();
	}


	/**
	 * 애플리케이션 종료 시 Executor 정리
	 */
	@Bean
	public ExecutorShutdownHook executorShutdownHook(ExecutorService equipmentReservationExecutor) {
		return new ExecutorShutdownHook(equipmentReservationExecutor);
	}

	static class ExecutorShutdownHook {
		private final ExecutorService executorService;

		public ExecutorShutdownHook(ExecutorService executorService) {
			this.executorService = executorService;
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				executorService.shutdown();
			}));
		}
	}

}