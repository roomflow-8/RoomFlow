package com.goorm.roomflow.domain.equipment.service;

import com.goorm.roomflow.domain.equipment.dto.request.AdminEquipmentReq;
import com.goorm.roomflow.domain.equipment.dto.response.AdminEquipmentRes;
import com.goorm.roomflow.domain.equipment.entity.EquipmentStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AdminEquipmentService {
    @Transactional(readOnly = true)
    List<AdminEquipmentRes> readEquipmentAdminList();

    @Transactional
    void createEquipment(AdminEquipmentReq adminEquipmentReq);

    @Transactional
    void modifyEquipment(Long equipmentId, AdminEquipmentReq adminEquipmentReq);

    @Transactional
    void changeEquipmentStatus(Long equipmentId, EquipmentStatus targetStatus);
}
