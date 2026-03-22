package com.goorm.roomflow.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "reservationHistoryExecutor")
    public Executor threadPoolTaskExecutor(){
        return new VirtualThreadTaskExecutor("reservation-history-");
    }
}