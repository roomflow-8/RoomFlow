package com.goorm.roomflow.domain.equipment.service;

import com.goorm.roomflow.domain.equipment.repository.EquipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EquipmentService {
	private final EquipmentRepository equipmentRepository;
}
