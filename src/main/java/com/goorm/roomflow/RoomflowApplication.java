package com.goorm.roomflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class RoomflowApplication {

	public static void main(String[] args) {
		SpringApplication.run(RoomflowApplication.class, args);
	}

}
