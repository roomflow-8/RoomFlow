package com.goorm.roomflow.domain.reservation.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QReservationEquipment is a Querydsl query type for ReservationEquipment
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReservationEquipment extends EntityPathBase<ReservationEquipment> {

    private static final long serialVersionUID = 64466317L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QReservationEquipment reservationEquipment = new QReservationEquipment("reservationEquipment");

    public final DateTimePath<java.time.LocalDateTime> cancelledAt = createDateTime("cancelledAt", java.time.LocalDateTime.class);

    public final StringPath cancelReason = createString("cancelReason");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final com.goorm.roomflow.domain.equipment.entity.QEquipment equipment;

    public final NumberPath<Integer> quantity = createNumber("quantity", Integer.class);

    public final QReservation reservation;

    public final NumberPath<Long> reservationEquipmentId = createNumber("reservationEquipmentId", Long.class);

    public final EnumPath<ReservationStatus> status = createEnum("status", ReservationStatus.class);

    public final NumberPath<java.math.BigDecimal> totalAmount = createNumber("totalAmount", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> unitPrice = createNumber("unitPrice", java.math.BigDecimal.class);

    public QReservationEquipment(String variable) {
        this(ReservationEquipment.class, forVariable(variable), INITS);
    }

    public QReservationEquipment(Path<? extends ReservationEquipment> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QReservationEquipment(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QReservationEquipment(PathMetadata metadata, PathInits inits) {
        this(ReservationEquipment.class, metadata, inits);
    }

    public QReservationEquipment(Class<? extends ReservationEquipment> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.equipment = inits.isInitialized("equipment") ? new com.goorm.roomflow.domain.equipment.entity.QEquipment(forProperty("equipment")) : null;
        this.reservation = inits.isInitialized("reservation") ? new QReservation(forProperty("reservation"), inits.get("reservation")) : null;
    }

}

