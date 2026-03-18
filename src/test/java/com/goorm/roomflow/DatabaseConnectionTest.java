package com.goorm.roomflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * # 모든 테스트 실행
 * ./gradlew test
 * <p>
 * # 특정 테스트만 실행
 * ./gradlew test --tests DatabaseConnectionTest
 * <p>
 * # 테스트 결과 보기
 * ./gradlew test --info
 */

@SpringBootTest
public class DatabaseConnectionTest {
	@Autowired
	private DataSource dataSource;

	@Test
	void contextLoads() {
		// Spring Context 로드 확인
	}

	@Test
	void testDatabaseConnection() throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			assertThat(connection).isNotNull();
			assertThat(connection.isValid(10)).isTrue();

			System.out.println("DB 연결 성공!");
			System.out.println("URL: " + connection.getMetaData().getURL());
			System.out.println("DB: " + connection.getMetaData().getDatabaseProductName());
		}
	}

}
