package com.goorm.roomflow.domain.equipment.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QEquipment is a Querydsl query type for Equipment
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEquipment extends EntityPathBase<Equipment> {

    private static final long serialVersionUID = -290957823L;

    public static final QEquipment equipment = new QEquipment("equipment");

    public final com.goorm.roomflow.domain.QBaseEntity _super = new com.goorm.roomflow.domain.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath description = createString("description");

    public final NumberPath<Long> equipmentId = createNumber("equipmentId", Long.class);

    public final StringPath equipmentName = createString("equipmentName");

    public final StringPath imageUrl = createString("imageUrl");

    public final NumberPath<Integer> maintenanceLimit = createNumber("maintenanceLimit", Integer.class);

    public final NumberPath<java.math.BigDecimal> price = createNumber("price", java.math.BigDecimal.class);

    public final EnumPath<EquipmentStatus> status = createEnum("status", EquipmentStatus.class);

    public final NumberPath<Integer> totalReservations = createNumber("totalReservations", Integer.class);

    public final NumberPath<Integer> totalStock = createNumber("totalStock", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QEquipment(String variable) {
        super(Equipment.class, forVariable(variable));
    }

    public QEquipment(Path<? extends Equipment> path) {
        super(path.getType(), path.getMetadata());
    }

    public QEquipment(PathMetadata metadata) {
        super(Equipment.class, metadata);
    }

}

