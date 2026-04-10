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
import com.goorm.roomflow.global.code.ErrorCode;
import com.goorm.roomflow.global.exception.BusinessException;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

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
						reservation.status.in(ReservationStatus.PENDING, ReservationStatus.CONFIRMED)
				)
				.groupBy(reservation.reservationId,
						meetingRoom.roomId,
						meetingRoom.roomName)
				.fetchOne();

		if (reservationInfo == null) {
			throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
		}

		return reservationInfo;
	}

	private List<EquipmentAvailabilityDto> getAvailableEquipments(ReservationInfoDto reservationInfo) {

		QEquipment equipment = QEquipment.equipment;
		QReservationEquipment reservationEquipment = QReservationEquipment.reservationEquipment;
		QReservation reservation = QReservation.reservation;
		QReservationRoom reservationRoom = QReservationRoom.reservationRoom;
		QRoomSlot roomSlot = QRoomSlot.roomSlot;

		NumberExpression<Integer> availableStock =
				equipment.totalStock.subtract(
						JPAExpressions
								.select(reservationEquipment.quantity.sum().coalesce(0))
								.from(reservationEquipment)
								.join(reservationEquipment.reservation, reservation)
								.where(
										reservationEquipment.equipment.equipmentId.eq(equipment.equipmentId),
										reservationEquipment.status.in(ReservationStatus.PENDING, ReservationStatus.CONFIRMED),
										reservation.status.notIn(ReservationStatus.CANCELLED, ReservationStatus.EXPIRED),
										JPAExpressions
												.selectOne()
												.from(reservationRoom)
												.join(reservationRoom.roomSlot, roomSlot)
												.where(
														reservationRoom.reservation.eq(reservation),
														roomSlot.slotStartAt.lt(reservationInfo.endAt()),
														roomSlot.slotEndAt.gt(reservationInfo.startAt())
												)
												.exists()
								)
				);


		return jpaQueryFactory
				.select(new QEquipmentAvailabilityDto(
						equipment.equipmentId,
						equipment.equipmentName,
						equipment.imageUrl,
						equipment.totalStock,
						availableStock,
						equipment.status,
						equipment.price
				))
				.from(equipment)
				.where(
						equipment.status.eq(EquipmentStatus.AVAILABLE)
				)
				.orderBy(
						availableStock.desc(),       // 재고 많은 순
						equipment.equipmentName.asc()
				)
				.fetch();
	}

}


