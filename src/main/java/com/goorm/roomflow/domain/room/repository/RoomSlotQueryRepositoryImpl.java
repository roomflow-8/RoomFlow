package com.goorm.roomflow.domain.room.repository;


import com.goorm.roomflow.domain.reservation.entity.QReservation;
import com.goorm.roomflow.domain.reservation.entity.QReservationRoom;
import com.goorm.roomflow.domain.reservation.entity.ReservationStatus;
import com.goorm.roomflow.domain.room.dto.response.RoomSlotAdminRes;
import com.goorm.roomflow.domain.room.dto.response.RoomSlotSummaryRes;
import com.goorm.roomflow.domain.room.entity.MeetingRoom;
import com.goorm.roomflow.domain.room.entity.QRoomSlot;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class RoomSlotQueryRepositoryImpl implements RoomSlotQueryRepository {

    private final JPAQueryFactory queryFactory;

    private final QRoomSlot roomSlot = QRoomSlot.roomSlot;
    private final QReservationRoom reservationRoom = QReservationRoom.reservationRoom;
    private final QReservation reservation = QReservation.reservation;

    @Override
    public List<RoomSlotAdminRes> findAdminRoomSlots(Long roomId, LocalDate date) {
        LocalDateTime startAt = date.atStartOfDay();
        LocalDateTime endAt = date.plusDays(1).atStartOfDay();

        return queryFactory
                .select(Projections.constructor(
                        RoomSlotAdminRes.class,
                        roomSlot.roomSlotId,
                        roomSlot.slotStartAt,
                        roomSlot.slotEndAt,
                        roomSlot.isActive,
                        new CaseBuilder()
                                .when(reservation.status.in(
                                        ReservationStatus.PENDING,
                                        ReservationStatus.CONFIRMED
                                ))
                                .then(true)
                                .otherwise(false)
                ))
                .from(roomSlot)
                .leftJoin(reservationRoom).on(reservationRoom.roomSlot.eq(roomSlot))
                .leftJoin(reservation).on(reservationRoom.reservation.eq(reservation))
                .where(
                        roomSlot.meetingRoom.roomId.eq(roomId),
                        roomSlot.slotStartAt.goe(startAt),
                        roomSlot.slotStartAt.lt(endAt)
                )
                .orderBy(roomSlot.slotStartAt.asc())
                .fetch();
    }

    @Override
    public RoomSlotSummaryRes findRoomSlotSummary(MeetingRoom meetingRoom, LocalDate selectedDate) {
        LocalDateTime now = LocalDateTime.now();

        List<RoomSlotAdminRes> futureSlots = queryFactory
                .select(Projections.constructor(
                        RoomSlotAdminRes.class,
                        roomSlot.roomSlotId,
                        roomSlot.slotStartAt,
                        roomSlot.slotEndAt,
                        roomSlot.isActive,
                        new CaseBuilder()
                                .when(reservation.status.in(
                                        ReservationStatus.PENDING,
                                        ReservationStatus.CONFIRMED
                                ))
                                .then(true)
                                .otherwise(false)
                ))
                .from(roomSlot)
                .leftJoin(reservationRoom).on(reservationRoom.roomSlot.eq(roomSlot))
                .leftJoin(reservation).on(reservationRoom.reservation.eq(reservation))
                .where(
                        roomSlot.meetingRoom.roomId.eq(meetingRoom.getRoomId()),
                        roomSlot.slotStartAt.goe(now)
                )
                .fetch();;

        int totalSlotCount = futureSlots.size();
        int reservedSlotCount = (int) futureSlots.stream()
                .filter(RoomSlotAdminRes::reserved)
                .count();
        int availableSlotCount = (int) futureSlots.stream()
                .filter(slot -> slot.active() && !slot.reserved())
                .count();
        int inactiveSlotCount = (int) futureSlots.stream()
                .filter(slot -> !slot.active() && !slot.reserved())
                .count();

        return new RoomSlotSummaryRes(
                meetingRoom.getRoomId(),
                meetingRoom.getRoomName(),
                meetingRoom.getStatus(),
                selectedDate,
                totalSlotCount,
                reservedSlotCount,
                availableSlotCount,
                inactiveSlotCount
        );
    }

    @Override
    public boolean existsValidReservationByRoomSlotId(Long roomSlotId) {
        Integer result = queryFactory
                .selectOne()
                .from(reservationRoom)
                .join(reservationRoom.reservation, reservation)
                .where(
                        reservationRoom.roomSlot.roomSlotId.eq(roomSlotId),
                        reservation.status.in(
                                ReservationStatus.PENDING,
                                ReservationStatus.CONFIRMED
                        )
                )
                .fetchFirst();

        return result != null;
    }

    @Override
    public boolean existsValidReservationByRoomIdAndDate(Long roomId, LocalDate date) {
        LocalDateTime startAt = date.atStartOfDay();
        LocalDateTime endAt = date.plusDays(1).atStartOfDay();

        Integer result = queryFactory
                .selectOne()
                .from(reservationRoom)
                .join(reservationRoom.reservation, reservation)
                .join(reservationRoom.roomSlot, roomSlot)
                .where(
                        roomSlot.meetingRoom.roomId.eq(roomId),
                        roomSlot.slotStartAt.goe(startAt),
                        roomSlot.slotStartAt.lt(endAt),
                        reservation.status.in(
                                ReservationStatus.PENDING,
                                ReservationStatus.CONFIRMED
                        )
                )
                .fetchFirst();

        return result != null;
    }
}
