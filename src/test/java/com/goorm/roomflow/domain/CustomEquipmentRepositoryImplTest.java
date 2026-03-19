package com.goorm.roomflow.domain;

import com.goorm.roomflow.domain.reservation.dto.ReservationInfoDto;
import com.goorm.roomflow.domain.reservation.entity.*;
import com.goorm.roomflow.domain.room.entity.MeetingRoom;
import com.goorm.roomflow.domain.room.entity.QMeetingRoom;
import com.goorm.roomflow.domain.room.entity.QRoomSlot;
import com.goorm.roomflow.domain.room.entity.RoomSlot;
import com.goorm.roomflow.domain.user.entity.User;
import com.goorm.roomflow.domain.user.entity.UserRole;
import com.goorm.roomflow.global.config.JpaConfig;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@DataJpaTest
@Import(JpaConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@RequiredArgsConstructor
public class CustomEquipmentRepositoryImplTest {

	// 1. 테스트 클래스 내부에 직접 설정 (가장 안전)
	@TestConfiguration
	static class TestConfig {
		@PersistenceContext
		private EntityManager em;

		@Bean
		public JPAQueryFactory jpaQueryFactory() {
			return new JPAQueryFactory(em);
		}
	}

	private final JPAQueryFactory jpaQueryFactory;

	@Autowired
	private EntityManager em;

	@Test
	@DisplayName("예약 정보 DTO 조회 테스트")
	void getReservationInfoTest() {

		// Given
		log.info("--- 테스트 데이터 준비 시작 ---");

		// 1. 사용자(User) 생성
		User user = User.builder()
				.name("chloe")
				.email("test@example.com")
				.role(UserRole.USER)
				.password("password")
				.build();
		em.persist(user);

		// 1. 회의실 생성
		MeetingRoom room = MeetingRoom.builder()
				.roomName("A회의실")
				.capacity(30)
				.hourlyPrice(BigDecimal.valueOf(3000)) // 가격 설정
				.build();
		em.persist(room);

// 2. 시간 슬롯(RoomSlot) 생성
		// DTO에서 min/max를 구하므로 최소 2개 이상의 슬롯을 넣어보는 게 좋습니다.
		RoomSlot slot1 = RoomSlot.builder()
				.slotStartAt(LocalDateTime.of(2026, 3, 19, 14, 0))
				.slotEndAt(LocalDateTime.of(2026, 3, 19, 15, 0))
				.meetingRoom(room)
				.build();
		RoomSlot slot2 = RoomSlot.builder()
				.slotStartAt(LocalDateTime.of(2026, 3, 19, 15, 0))
				.slotEndAt(LocalDateTime.of(2026, 3, 19, 16, 0))
				.meetingRoom(room)
				.build();
		em.persist(slot1);
		em.persist(slot2);

		// 3. 예약(Reservation) 생성
		// 필수 필드인 idempotencyKey를 꼭 넣어주세요.
		Reservation reservation = Reservation.builder()
				//.status(ReservationStatus.PENDING)
				.meetingRoom(room)
				.user(user)
				.idempotencyKey(UUID.randomUUID().toString()) // 필수값!
				.build();
		em.persist(reservation);

		// 4. 예약과 슬롯 연결 (ReservationRoom)
		// 쿼리에서 join(reservationRoom).on(...) 부분을 만족시키기 위함입니다.
		ReservationRoom resRoom1 = ReservationRoom.builder()
				.reservation(reservation)
				.roomSlot(slot1)
				.meetingRoom(room)
				.build();
		ReservationRoom resRoom2 = ReservationRoom.builder()
				.reservation(reservation)
				.roomSlot(slot2)
				.meetingRoom(room)
				.build();
		em.persist(resRoom1);
		em.persist(resRoom2);

		em.flush();
		em.clear();
		log.info("--- 테스트 데이터 준비 완료 (영속성 컨텍스트 초기화) ---");

		// When
		Long targetId = reservation.getReservationId();
		log.info("조회 요청 ID: {}", targetId);

		QReservation qReservation = QReservation.reservation;
		QReservationRoom reservationRoom = QReservationRoom.reservationRoom;
		QRoomSlot roomSlot = QRoomSlot.roomSlot;
		QMeetingRoom meetingRoom = QMeetingRoom.meetingRoom;

		ReservationInfoDto result = jpaQueryFactory
				.select(Projections.constructor(ReservationInfoDto.class,
						qReservation.reservationId,
						meetingRoom.roomId,
						meetingRoom.roomName,
						roomSlot.slotStartAt.min(),
						roomSlot.slotEndAt.max()
				))
				.from(qReservation)
				.join(qReservation.meetingRoom, meetingRoom)
				.join(reservationRoom).on(reservationRoom.reservation.eq(qReservation))
				.join(reservationRoom.roomSlot, roomSlot)
				.where(
						qReservation.reservationId.eq(targetId),
						qReservation.status.eq(ReservationStatus.PENDING)
				)
				.groupBy(qReservation.reservationId, meetingRoom.roomId, meetingRoom.roomName)
				.fetchOne();

		// Then
		if (result != null) {
			log.info("조회 성공! 결과 데이터: {}", result);
			assertThat(result)
					.isNotNull()
					.extracting("reservationId")
					.isEqualTo(targetId);
		} else {
			log.error("데이터 조회 실패: 결과가 null입니다. ID: {}", targetId);
		}

		assertThat(result).isNotNull();
	}


}
