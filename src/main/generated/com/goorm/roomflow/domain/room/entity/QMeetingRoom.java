package com.goorm.roomflow.domain.room.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QMeetingRoom is a Querydsl query type for MeetingRoom
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMeetingRoom extends EntityPathBase<MeetingRoom> {

    private static final long serialVersionUID = -404367296L;

    public static final QMeetingRoom meetingRoom = new QMeetingRoom("meetingRoom");

    public final com.goorm.roomflow.domain.QBaseEntity _super = new com.goorm.roomflow.domain.QBaseEntity(this);

    public final NumberPath<Integer> capacity = createNumber("capacity", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath description = createString("description");

    public final NumberPath<java.math.BigDecimal> hourlyPrice = createNumber("hourlyPrice", java.math.BigDecimal.class);

    public final StringPath imageUrl = createString("imageUrl");

    public final NumberPath<Long> roomId = createNumber("roomId", Long.class);

    public final StringPath roomName = createString("roomName");

    public final EnumPath<RoomStatus> status = createEnum("status", RoomStatus.class);

    public final NumberPath<Integer> totalReservations = createNumber("totalReservations", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QMeetingRoom(String variable) {
        super(MeetingRoom.class, forVariable(variable));
    }

    public QMeetingRoom(Path<? extends MeetingRoom> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMeetingRoom(PathMetadata metadata) {
        super(MeetingRoom.class, metadata);
    }

}

