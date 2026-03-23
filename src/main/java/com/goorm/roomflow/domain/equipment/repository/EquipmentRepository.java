package com.goorm.roomflow.domain.equipment.repository;

import com.goorm.roomflow.domain.equipment.entity.Equipment;
import com.goorm.roomflow.domain.equipment.entity.EquipmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long>, CustomEquipmentRepository {
	List<Equipment> findByStatus(EquipmentStatus status);


	/*
	260322 ES 비관적 락으로 비품 조회 - reservationServiceImpl.validAvailableStock에 연결
	 */

	/**
	 * 비관적 락으로 비품 조회 (동시성 제어)
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Equipment> findByEquipmentId(Long equipmentId); //findByEquipmentIdWithLock(Long id) bug_report

	// 👍 이름으로 검색
	List<Equipment> findByEquipmentNameContaining(String keyword);

	// 👍 재고 있는 것만
	List<Equipment> findByStatusAndTotalStockGreaterThan(
			EquipmentStatus status,
			Integer minStock
	);
}
