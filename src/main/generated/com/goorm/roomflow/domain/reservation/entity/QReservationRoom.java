package com.goorm.roomflow.domain.reservation.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QReservationRoom is a Querydsl query type for ReservationRoom
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReservationRoom extends EntityPathBase<ReservationRoom> {

    private static final long serialVersionUID = 272266620L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QReservationRoom reservationRoom = new QReservationRoom("reservationRoom");

    public final NumberPath<java.math.BigDecimal> amount = createNumber("amount", java.math.BigDecimal.class);

    public final com.goorm.roomflow.domain.room.entity.QMeetingRoom meetingRoom;

    public final QReservation reservation;

    public final NumberPath<Long> reservationRoomId = createNumber("reservationRoomId", Long.class);

    public final com.goorm.roomflow.domain.room.entity.QRoomSlot roomSlot;

    public QReservationRoom(String variable) {
        this(ReservationRoom.class, forVariable(variable), INITS);
    }

    public QReservationRoom(Path<? extends ReservationRoom> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QReservationRoom(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QReservationRoom(PathMetadata metadata, PathInits inits) {
        this(ReservationRoom.class, metadata, inits);
    }

    public QReservationRoom(Class<? extends ReservationRoom> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.meetingRoom = inits.isInitialized("meetingRoom") ? new com.goorm.roomflow.domain.room.entity.QMeetingRoom(forProperty("meetingRoom")) : null;
        this.reservation = inits.isInitialized("reservation") ? new QReservation(forProperty("reservation"), inits.get("reservation")) : null;
        this.roomSlot = inits.isInitialized("roomSlot") ? new com.goorm.roomflow.domain.room.entity.QRoomSlot(forProperty("roomSlot"), inits.get("roomSlot")) : null;
    }

}

