package com.goorm.roomflow.domain.equipment.repository;

import com.goorm.roomflow.domain.equipment.dto.EquipmentAvailabilityDto;
import com.goorm.roomflow.domain.equipment.dto.QEquipmentAvailabilityDto;
import com.goorm.roomflow.domain.equipment.entity.EquipmentStatus;
import com.goorm.roomflow.domain.equipment.entity.QEquipment;
import com.goorm.roomflow.domain.reservation.dto.ReservationInfoDto;
import com.goorm.roomflow.domain.reservation.entity.QReservation;
import com.goorm.roomflow.domain.reservation.entity.QReservationEquipment;
import com.goorm.roomflow.domain.reservation.entity.QReservationRoom;
import com.goorm.roomflow.domain.reservation.entity.ReservationStatus;
import com.goorm.roomflow.domain.room.entity.QMeetingRoom;
import com.goorm.roomflow.domain.room.entity.QRoomSlot;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CustomEquipmentRepositoryImpl implements CustomEquipmentRepository {

	private final JPAQueryFactory jpaQueryFactory;

	@Override
	public List<EquipmentAvailabilityDto> findAvailableEquipmentsByReservation(Long reservationId) {
		ReservationInfoDto reservationInfoDto = getReservationInfo(reservationId);
		return getAvailableEquipments(reservationInfoDto);
	}

	private ReservationInfoDto getReservationInfo(Long reservationId) {

		QReservation reservation = QReservation.reservation;
		QReservationRoom reservationRoom = QReservationRoom.reservationRoom;
		QRoomSlot roomSlot = QRoomSlot.roomSlot;
		QMeetingRoom meetingRoom = QMeetingRoom.meetingRoom;

		// 1. 예약 정보 조회
		ReservationInfoDto reservationInfo = jpaQueryFactory
				.select(Projections.constructor(ReservationInfoDto.class, reservation.reservationId, meetingRoom.roomId, meetingRoom.roomName,
						roomSlot.slotStartAt.min(), roomSlot.slotEndAt.max()
				))
				.from(reservation)
				.join(reservation.meetingRoom, meetingRoom)
				.join(reservationRoom).on(reservationRoom.reservation.eq(reservation))
				.join(reservationRoom.roomSlot, roomSlot)
				.where(
						reservation.reservationId.eq(reservationId),
						reservation.status.eq(ReservationStatus.PENDING)
				)
				.groupBy(reservation.reservationId,
						meetingRoom.roomId,
						meetingRoom.roomName)
				.fetchOne();

		if (reservationInfo == null) {
			throw new IllegalArgumentException("Invalid reservation or not in PENDING status");
		}

		return reservationInfo;
	}

	private List<EquipmentAvailabilityDto> getAvailableEquipments(ReservationInfoDto reservationInfo) {

		QEquipment equipment = QEquipment.equipment;
		QReservationEquipment reservationEquipment = QReservationEquipment.reservationEquipment;
		QReservation reservation = QReservation.reservation;
		QReservationRoom reservationRoom = QReservationRoom.reservationRoom;
		QRoomSlot roomSlot = QRoomSlot.roomSlot;

		LocalDate date = reservationInfo.startAt().toLocalDate();
		return jpaQueryFactory
				.select(new QEquipmentAvailabilityDto(
						equipment.equipmentId,
						equipment.equipmentName,
						equipment.imageUrl,
						equipment.totalStock,
						equipment.totalStock.subtract(
								JPAExpressions
										.select(reservationEquipment.quantity.sum().coalesce(0))
										.from(reservationEquipment)
										.join(reservationEquipment.reservation, reservation)
										.innerJoin(reservationRoom).on(reservationRoom.reservation.eq(reservation))
										.join(reservationRoom.roomSlot, roomSlot)
										.where(
												reservationEquipment.equipment.equipmentId.eq(equipment.equipmentId),
												reservation.status.ne(ReservationStatus.CANCELLED),
												roomSlot.slotStartAt.between(
														date.atStartOfDay(),
														date.plusDays(1).atStartOfDay()
												),
												roomSlot.slotStartAt.lt(reservationInfo.endAt()),
												roomSlot.slotEndAt.gt(reservationInfo.startAt())
										)
						),
						equipment.status,
						equipment.price
				))
				.from(equipment)
				.where(
						equipment.status.eq(EquipmentStatus.AVAILABLE)
				)
				.fetch();
	}


}


