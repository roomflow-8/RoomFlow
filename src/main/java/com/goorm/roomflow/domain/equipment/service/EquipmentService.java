package com.goorm.roomflow.domain.equipment.service;

import com.goorm.roomflow.domain.equipment.dto.EquipmentAvailabilityDto;
import com.goorm.roomflow.domain.equipment.dto.request.EquipmentReq;
import com.goorm.roomflow.domain.equipment.dto.response.EquipmentAdminRes;
import com.goorm.roomflow.domain.equipment.entity.Equipment;
import com.goorm.roomflow.domain.equipment.entity.EquipmentStatus;
import com.goorm.roomflow.domain.equipment.repository.EquipmentRepository;
import com.goorm.roomflow.domain.reservation.repository.ReservationEquipmentRepository;
import com.goorm.roomflow.global.code.ErrorCode;
import com.goorm.roomflow.global.exception.BusinessException;
import com.goorm.roomflow.global.s3.S3ImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EquipmentService {

	private final EquipmentRepository equipmentRepository;
	private final ReservationEquipmentRepository reservationEquipmentRepository;
	private final S3ImageService s3ImageService;

	/**
	 * 예약에 사용 가능한 장비 목록 조회
	 * @param reservationId 예약 ID
	 * @return 사용 가능한 장비 목록
	 */
	public List<EquipmentAvailabilityDto> getAvailableEquipments(Long reservationId) {
		log.info("예약 ID {}에 대한 사용 가능한 장비 조회 시작", reservationId);

		try {
			List<EquipmentAvailabilityDto> availableEquipments =
					equipmentRepository.findAvailableEquipmentsByReservation(reservationId);

			log.info("조회된 장비 개수: {}", availableEquipments.size());

			return availableEquipments;
		} catch (Exception e) {
			throw new BusinessException(ErrorCode.EQUIPMENT_NOT_FOUND);
		}
	}

	/**
	 * 관리자 기능 구현
	 */

	/**
	 * 관리자 비품 목록 조회
	 */
	@Transactional(readOnly = true)
	public List<EquipmentAdminRes> readEquipmentAdminList() {

		log.info("비품 목록 조회 시작");
		List<Equipment> equipments = equipmentRepository.findAll();

		log.info("비품 목록 조회 완료: equipmentsCount={}", equipments.size());
		return equipments.stream()
				.map(equipment -> new EquipmentAdminRes(
						equipment.getEquipmentId(),
						equipment.getEquipmentName(),
						equipment.getTotalStock(),
						equipment.getDescription(),
						equipment.getMaintenanceLimit(),
						equipment.getPrice(),
						equipment.getStatus(),
						equipment.getImageUrl(),
						equipment.getTotalReservations(),
						equipment.getCreatedAt(),
						equipment.getUpdatedAt()
				))
				.sorted(
						Comparator.comparingInt((EquipmentAdminRes equipment) -> equipment.status().getPriority())
								.thenComparing(Comparator.comparingInt(EquipmentAdminRes::totalReservations).reversed())
				)
				.toList();
	}

	/**
	 * 비품 목록 생성
	 * @param equipmentReq 비품 정보
	 */
	@Transactional
	public void createEquipment(EquipmentReq equipmentReq) {

		log.info("비품 생성 시작 - equipmentName={}", equipmentReq.equipmentName());

		String imageUrl = uploadImageIfExists(equipmentReq.imageFile());

		Equipment equipment = Equipment.builder()
				.equipmentName(equipmentReq.equipmentName())
				.totalStock(equipmentReq.totalStock())
				.description(equipmentReq.description())
				.maintenanceLimit(equipmentReq.maintenanceLimit())
				.price(equipmentReq.price())
				.status(equipmentReq.status())
				.imageUrl(imageUrl)
				.build();

		Equipment savedEquipment = equipmentRepository.save(equipment);

		log.info("비품 생성 완료 - equipmentId={}", savedEquipment.getEquipmentId());
	}

	/**
	 * 비품 수정
	 * @param equipmentId
	 * @param equipmentReq
	 */
	@Transactional
	public void modifyEquipment(Long equipmentId, EquipmentReq equipmentReq) {

		log.info("비품 수정 시작 - equipmentId={}", equipmentId);

		Equipment equipment = loadEquipment(equipmentId);

		String imageUrl = equipment.getImageUrl();
		if (equipmentReq.imageFile() != null && !equipmentReq.imageFile().isEmpty()) {
			imageUrl = s3ImageService.upload(equipmentReq.imageFile(), "equipment");
		}

		equipment.update(
				equipmentReq.equipmentName(),
				equipmentReq.totalStock(),
				equipmentReq.description(),
				equipmentReq.maintenanceLimit(),
				equipmentReq.price(),
				equipmentReq.status(),
				imageUrl
		);

		log.info("비품 수정 완료 - equipmentId={}", equipmentId);
	}
	/**
	 * 비품 상태 변경
	 */
	@Transactional
	public void changeEquipmentStatus(Long equipmentId, EquipmentStatus targetStatus) {
		log.info("비품 상태 변경 요청 시작 - equipmentId={}, targetStatus={}", equipmentId, targetStatus);

		Equipment equipment = loadEquipment(equipmentId);
		EquipmentStatus currentStatus = equipment.getStatus();

		log.info("현재 비품 상태 확인 - equipmentId={}, currentStatus={}", equipmentId, currentStatus);

		if (currentStatus == targetStatus) {
			log.info("비품 상태 변경 생략 - 동일한 상태 요청 - equipmentId={}, status={}", equipmentId, targetStatus);
			return;
		}

		if (targetStatus == EquipmentStatus.INACTIVE || targetStatus == EquipmentStatus.MAINTENANCE) {
			log.info("비품 사용 불가 상태 전환 검증 시작 - equipmentId={}, targetStatus={}", equipmentId, targetStatus);
			changeToUnavailable(equipment, targetStatus);
		}

		equipment.changeStatus(targetStatus);

		log.info("비품 상태 변경 완료 - equipmentId={}, from={}, to={}", equipmentId, currentStatus, targetStatus);
	}

	private void changeToUnavailable(Equipment equipment, EquipmentStatus targetStatus) {
		Long equipmentId = equipment.getEquipmentId();
		LocalDateTime now = LocalDateTime.now();

		log.info("비품 상태 변경 검증 시작 - equipmentId={}, targetStatus={}, 기준시간={}", equipmentId, targetStatus, now);

		boolean hasFutureReservation =
				reservationEquipmentRepository.existsFutureReservationByEquipmentId(equipmentId, now);

		log.info("미래 비품 예약 존재 여부 확인 - equipmentId={}, hasFutureReservation={}", equipmentId, hasFutureReservation);

		if (hasFutureReservation) {
			log.info("비품 상태 변경 실패 - 미래 예약 존재 - equipmentId={}, targetStatus={}", equipmentId, targetStatus);
			throw new BusinessException(ErrorCode.EQUIPMENT_STATUS_CHANGE_FORBIDDEN);
		}
	}

	private String uploadImageIfExists(MultipartFile image) {
		if (image == null || image.isEmpty()) {
			return null;
		}

		return s3ImageService.upload(image, "equipment");
	}

	private Equipment loadEquipment(Long equipmentId) {
		return equipmentRepository.findById(equipmentId)
				.orElseThrow(() -> new BusinessException(ErrorCode.EQUIPMENT_NOT_FOUND));
	}
}
