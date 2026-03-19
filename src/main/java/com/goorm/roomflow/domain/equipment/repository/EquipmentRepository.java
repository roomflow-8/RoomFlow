package com.goorm.roomflow.domain.equipment.repository;

import com.goorm.roomflow.domain.equipment.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long>, CustomEquipmentRepository {
}
