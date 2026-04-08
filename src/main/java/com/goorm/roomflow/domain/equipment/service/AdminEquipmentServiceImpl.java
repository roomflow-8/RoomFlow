package com.goorm.roomflow.domain.equipment.service;

import com.goorm.roomflow.domain.equipment.dto.request.AdminEquipmentReq;
import com.goorm.roomflow.domain.equipment.dto.response.AdminEquipmentRes;
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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminEquipmentServiceImpl implements AdminEquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final ReservationEquipmentRepository reservationEquipmentRepository;
    private final S3ImageService s3ImageService;

    /**
     * 관리자 비품 목록 조회
     */
    @Transactional(readOnly = true)
    @Override
    public List<AdminEquipmentRes> readEquipmentAdminList() {

        log.info("비품 목록 조회 시작");
        List<Equipment> equipments = equipmentRepository.findAll();

        log.info("비품 목록 조회 완료: equipmentsCount={}", equipments.size());
        return equipments.stream()
                .map(equipment -> new AdminEquipmentRes(
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
                        Comparator.comparingInt((AdminEquipmentRes equipment) -> equipment.status().getPriority())
                                .thenComparing(Comparator.comparingInt(AdminEquipmentRes::totalReservations).reversed())
                )
                .toList();
    }

    /**
     * 비품 목록 생성
     * @param adminEquipmentReq 비품 정보
     */
    @Transactional
    @Override
    public void createEquipment(AdminEquipmentReq adminEquipmentReq) {

        log.info("비품 생성 시작 - equipmentName={}", adminEquipmentReq.equipmentName());

        String imageUrl = uploadImageIfExists(adminEquipmentReq.imageFile());

        Equipment equipment = Equipment.builder()
                .equipmentName(adminEquipmentReq.equipmentName())
                .totalStock(adminEquipmentReq.totalStock())
                .description(adminEquipmentReq.description())
                .maintenanceLimit(adminEquipmentReq.maintenanceLimit())
                .price(adminEquipmentReq.price())
                .status(adminEquipmentReq.status())
                .imageUrl(imageUrl)
                .build();

        Equipment savedEquipment = equipmentRepository.save(equipment);

        log.info("비품 생성 완료 - equipmentId={}", savedEquipment.getEquipmentId());
    }

    /**
     * 비품 수정
     * @param equipmentId
     * @param adminEquipmentReq
     */
    @Transactional
    @Override
    public void modifyEquipment(Long equipmentId, AdminEquipmentReq adminEquipmentReq) {

        log.info("비품 수정 시작 - equipmentId={}", equipmentId);

        Equipment equipment = loadEquipment(equipmentId);

        String imageUrl = equipment.getImageUrl();
        if (adminEquipmentReq.imageFile() != null && !adminEquipmentReq.imageFile().isEmpty()) {
            imageUrl = s3ImageService.upload(adminEquipmentReq.imageFile(), "equipment");
        }

        equipment.update(
                adminEquipmentReq.equipmentName(),
                adminEquipmentReq.totalStock(),
                adminEquipmentReq.description(),
                adminEquipmentReq.maintenanceLimit(),
                adminEquipmentReq.price(),
                adminEquipmentReq.status(),
                imageUrl
        );

        log.info("비품 수정 완료 - equipmentId={}", equipmentId);
    }
    /**
     * 비품 상태 변경
     */
    @Transactional
    @Override
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
