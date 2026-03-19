package com.goorm.roomflow.domain.room.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRoomSlot is a Querydsl query type for RoomSlot
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRoomSlot extends EntityPathBase<RoomSlot> {

    private static final long serialVersionUID = -1587779793L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRoomSlot roomSlot = new QRoomSlot("roomSlot");

    public final com.goorm.roomflow.domain.QBaseEntity _super = new com.goorm.roomflow.domain.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final BooleanPath isActive = createBoolean("isActive");

    public final QMeetingRoom meetingRoom;

    public final NumberPath<Long> roomSlotId = createNumber("roomSlotId", Long.class);

    public final DateTimePath<java.time.LocalDateTime> slotEndAt = createDateTime("slotEndAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> slotStartAt = createDateTime("slotStartAt", java.time.LocalDateTime.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QRoomSlot(String variable) {
        this(RoomSlot.class, forVariable(variable), INITS);
    }

    public QRoomSlot(Path<? extends RoomSlot> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRoomSlot(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRoomSlot(PathMetadata metadata, PathInits inits) {
        this(RoomSlot.class, metadata, inits);
    }

    public QRoomSlot(Class<? extends RoomSlot> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.meetingRoom = inits.isInitialized("meetingRoom") ? new QMeetingRoom(forProperty("meetingRoom")) : null;
    }

}

