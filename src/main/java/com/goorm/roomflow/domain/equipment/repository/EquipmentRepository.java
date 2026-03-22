package com.goorm.roomflow.domain.equipment.repository;

import com.goorm.roomflow.domain.equipment.entity.Equipment;
import com.goorm.roomflow.domain.equipment.entity.EquipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long>, CustomEquipmentRepository {
	List<Equipment> findByStatus(EquipmentStatus status);
}
