package com.goorm.roomflow.domain.equipment.dto;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.goorm.roomflow.domain.equipment.dto.QEquipmentAvailabilityDto is a Querydsl Projection type for EquipmentAvailabilityDto
 */
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QEquipmentAvailabilityDto extends ConstructorExpression<EquipmentAvailabilityDto> {

    private static final long serialVersionUID = 1446769731L;

    public QEquipmentAvailabilityDto(com.querydsl.core.types.Expression<Long> equipmentId, com.querydsl.core.types.Expression<String> equipmentName, com.querydsl.core.types.Expression<String> imageUrl, com.querydsl.core.types.Expression<Integer> totalStock, com.querydsl.core.types.Expression<Integer> availableStock, com.querydsl.core.types.Expression<com.goorm.roomflow.domain.equipment.entity.EquipmentStatus> status, com.querydsl.core.types.Expression<? extends java.math.BigDecimal> price) {
        super(EquipmentAvailabilityDto.class, new Class<?>[]{long.class, String.class, String.class, int.class, int.class, com.goorm.roomflow.domain.equipment.entity.EquipmentStatus.class, java.math.BigDecimal.class}, equipmentId, equipmentName, imageUrl, totalStock, availableStock, status, price);
    }

}

