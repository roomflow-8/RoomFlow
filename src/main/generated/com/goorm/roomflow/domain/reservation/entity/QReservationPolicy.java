package com.goorm.roomflow.domain.reservation.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QReservationPolicy is a Querydsl query type for ReservationPolicy
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReservationPolicy extends EntityPathBase<ReservationPolicy> {

    private static final long serialVersionUID = -402131565L;

    public static final QReservationPolicy reservationPolicy = new QReservationPolicy("reservationPolicy");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath description = createString("description");

    public final NumberPath<Long> policyId = createNumber("policyId", Long.class);

    public final StringPath policyKey = createString("policyKey");

    public final StringPath policyValue = createString("policyValue");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QReservationPolicy(String variable) {
        super(ReservationPolicy.class, forVariable(variable));
    }

    public QReservationPolicy(Path<? extends ReservationPolicy> path) {
        super(path.getType(), path.getMetadata());
    }

    public QReservationPolicy(PathMetadata metadata) {
        super(ReservationPolicy.class, metadata);
    }

}

