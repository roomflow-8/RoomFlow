package com.goorm.roomflow.domain.reservation.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QReservationHistory is a Querydsl query type for ReservationHistory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReservationHistory extends EntityPathBase<ReservationHistory> {

    private static final long serialVersionUID = 1743757395L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QReservationHistory reservationHistory = new QReservationHistory("reservationHistory");

    public final com.goorm.roomflow.domain.user.entity.QUser changedBy;

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final EnumPath<ReservationStatus> fromStatus = createEnum("fromStatus", ReservationStatus.class);

    public final NumberPath<Long> historyId = createNumber("historyId", Long.class);

    public final StringPath reason = createString("reason");

    public final QReservation reservation;

    public final NumberPath<Long> targetId = createNumber("targetId", Long.class);

    public final EnumPath<TargetType> targetType = createEnum("targetType", TargetType.class);

    public final EnumPath<ReservationStatus> toStatus = createEnum("toStatus", ReservationStatus.class);

    public QReservationHistory(String variable) {
        this(ReservationHistory.class, forVariable(variable), INITS);
    }

    public QReservationHistory(Path<? extends ReservationHistory> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QReservationHistory(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QReservationHistory(PathMetadata metadata, PathInits inits) {
        this(ReservationHistory.class, metadata, inits);
    }

    public QReservationHistory(Class<? extends ReservationHistory> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.changedBy = inits.isInitialized("changedBy") ? new com.goorm.roomflow.domain.user.entity.QUser(forProperty("changedBy")) : null;
        this.reservation = inits.isInitialized("reservation") ? new QReservation(forProperty("reservation"), inits.get("reservation")) : null;
    }

}

