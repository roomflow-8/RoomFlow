package com.goorm.roomflow.domain.reservation.service;

import com.goorm.roomflow.domain.equipment.dto.EquipmentAvailabilityDto;
import com.goorm.roomflow.domain.equipment.dto.request.EquipmentReservationReq;
import com.goorm.roomflow.domain.equipment.entity.Equipment;
import com.goorm.roomflow.domain.equipment.entity.EquipmentStatus;
import com.goorm.roomflow.domain.equipment.repository.EquipmentRepository;
import com.goorm.roomflow.domain.reservation.dto.request.AddEquipmentsReq;
import com.goorm.roomflow.domain.reservation.dto.request.CreateReservationRoomReq;
import com.goorm.roomflow.domain.reservation.dto.response.ReservationRoomRes;
import com.goorm.roomflow.domain.reservation.dto.response.ReservationTimeSlot;
import com.goorm.roomflow.domain.reservation.entity.Reservation;
import com.goorm.roomflow.domain.reservation.entity.ReservationEquipment;
import com.goorm.roomflow.domain.reservation.entity.ReservationRoom;
import com.goorm.roomflow.domain.reservation.entity.ReservationStatus;
import com.goorm.roomflow.domain.reservation.mapper.ReservationRoomMapper;
import com.goorm.roomflow.domain.reservation.repository.ReservationEquipmentRepository;
import com.goorm.roomflow.domain.reservation.repository.ReservationRepository;
import com.goorm.roomflow.domain.reservation.repository.ReservationRoomRepository;
import com.goorm.roomflow.domain.room.entity.MeetingRoom;
import com.goorm.roomflow.domain.room.entity.RoomSlot;
import com.goorm.roomflow.domain.room.entity.RoomStatus;
import com.goorm.roomflow.domain.room.repository.MeetingRoomRepository;
import com.goorm.roomflow.domain.room.repository.RoomSlotRepository;
import com.goorm.roomflow.global.code.ErrorCode;
import com.goorm.roomflow.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.util.Collections.min;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

	private final MeetingRoomRepository meetingRoomRepository;
	private final RoomSlotRepository roomSlotRepository;
	private final ReservationRepository reservationRepository;
	private final ReservationRoomRepository reservationRoomRepository;
	private final ReservationRoomMapper reservationRoomMapper;
	private final ReservationEquipmentRepository reservationEquipmentRepository;
	private final EquipmentRepository equipmentRepository;


	@Override
	@Transactional(readOnly = true)
	public ReservationRoomRes readReservationRoom(Long reservationId) {

		// 1. 예약 조회
		Reservation reservation = reservationRepository.findById(reservationId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

		// 2. 회의실 예약 조회
		List<ReservationRoom> reservationRooms = reservationRoomRepository.findByReservation(reservation);

		if (reservationRooms.isEmpty()) {
			throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
		}

		MeetingRoom meetingRoom = reservation.getMeetingRoom();

		// 3. 시간 조회
		List<RoomSlot> roomSlots = reservationRooms.stream()
				.map(ReservationRoom::getRoomSlot)
				.sorted(Comparator.comparing(RoomSlot::getSlotStartAt))
				.toList();

		LocalDate reservationDate = roomSlots.getFirst().getSlotStartAt().toLocalDate();
		List<ReservationTimeSlot> reservationTimeSlots = makeReservationTimeSlot(roomSlots);

		return reservationRoomMapper.toReservationRoomRes(
				reservation,
				meetingRoom,
				reservationTimeSlots,
				reservationDate
		);
	}

	@Override
	@Transactional
	public ReservationRoomRes createReservationRoom(CreateReservationRoomReq request) {

		// 1. 회의실 조회
		MeetingRoom meetingRoom = meetingRoomRepository.findById(request.roomId())
				.orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

		if (meetingRoom.getStatus() != RoomStatus.AVAILABLE) {
			throw new BusinessException(ErrorCode.ROOM_NOT_AVAILABLE);
		}

		// 2. 멱등키 중복 체크
		if (reservationRepository.existsByIdempotencyKey(request.idempotencyKey())) {
			throw new BusinessException(ErrorCode.DUPLICATE_RESERVATION_REQUEST);
		}

		// 3. 슬롯 조회
		List<RoomSlot> roomSlots = roomSlotRepository.findByMeetingRoom_RoomIdAndRoomSlotIdIn(
				request.roomId(), request.roomSlotIds());
		LocalDate reservationDate = roomSlots.getFirst().getSlotStartAt().toLocalDate();

		// 3-1. 슬롯 예외 처리
		if (roomSlots.size() != request.roomSlotIds().size()) {
			throw new BusinessException(ErrorCode.ROOM_SLOT_NOT_FOUND);
		}

		// 3-2. 슬롯 예약 확인
		boolean hasUnavailableSlot = roomSlots.stream()
				.anyMatch(slot -> !slot.isActive());

		if (hasUnavailableSlot) {
			throw new BusinessException(ErrorCode.RESERVATION_ALREADY_EXISTS);
		}

		// 4. 금액 계산
		BigDecimal totalAmount = calcTotalAmount(meetingRoom.getHourlyPrice(), roomSlots);

		// 5. 예약 생성
		Reservation reservation = new Reservation(
				1L,
				meetingRoom,
				request.idempotencyKey(),
				totalAmount);

		reservationRepository.save(reservation);

		// 6. 회의실 예약 생성
		List<ReservationRoom> reservationRooms = roomSlots.stream()
				.map(roomSlot -> new ReservationRoom(
						reservation,
						meetingRoom,
						roomSlot,
						meetingRoom.getHourlyPrice()
				)).toList();

		reservationRoomRepository.saveAll(reservationRooms);

		// 7. 슬롯 상태 변경
		roomSlots.forEach(roomSlot -> roomSlot.updateActiveStatus(false));

		//===============================
		/*
		260321 ES 추가
		 */
		// 비품 추가
		// 7과 8 사이. 비품 예약 추가 (있는 경우만)
		if (request.equipments() != null && !request.equipments().isEmpty()) {
			log.info("회의실 예약 생성과 함께 비품 예약 - reservationId: {}, equipmentCount: {}",
					reservation.getReservationId(), request.equipments().size());

			createEquipmentReservations(reservation, roomSlots, request.equipments());
		}

		//===============================

		// 8. 슬롯 정리
		List<ReservationTimeSlot> reservationTimeSlots = makeReservationTimeSlot(roomSlots);

		// 9. 결과 반환
		return reservationRoomMapper.toReservationRoomRes(
				reservation,
				meetingRoom,
				reservationTimeSlots,
				reservationDate
		);

	}

	// 슬롯 시간별 정리 함수
	private List<ReservationTimeSlot> makeReservationTimeSlot(List<RoomSlot> roomSlots) {
		List<RoomSlot> sortedSlots = roomSlots.stream()
				.sorted(Comparator.comparing(RoomSlot::getSlotStartAt))
				.toList();

		List<ReservationTimeSlot> reservationTimeSlots = new ArrayList<>();

		LocalDateTime start = sortedSlots.getFirst().getSlotStartAt();
		LocalDateTime end = sortedSlots.getFirst().getSlotEndAt();

		for (int i = 1; i < sortedSlots.size(); i++) {
			RoomSlot slot = sortedSlots.get(i);

			if (slot.getSlotStartAt().equals(end)) {
				end = slot.getSlotEndAt();
				continue;
			}

			reservationTimeSlots.add(new ReservationTimeSlot(start, end));

			start = slot.getSlotStartAt();
			end = slot.getSlotEndAt();
		}

		reservationTimeSlots.add(new ReservationTimeSlot(start, end));

		return reservationTimeSlots;
	}

	// 총 금액 계산
	private BigDecimal calcTotalAmount(BigDecimal houlyPrice, List<RoomSlot> roomSlots) {
		return houlyPrice.multiply(BigDecimal.valueOf(roomSlots.size()));
	}


	// ==================== 비품 예약 메서드 추가 ====================

	/**
	 * 비품 예약 추가
	 * - PENDING 상태: 회의실 예약과 함께 비품 추가
	 * - CONFIRMED 상태: 확정된 예약에 비품 추가
	 */
	@Override
	@Transactional
	public List<ReservationEquipment> addEquipmentsToReservation(
			Long reservationId,
			AddEquipmentsReq request) {

		log.info("기존 예약에 비품 추가 - reservationId: {}, count: {}",
				reservationId, request.equipments().size());

		// 1. 예약 조회
		Reservation reservation = reservationRepository.findById(reservationId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

		// 2. 취소/만료된 예약 체크
		if (reservation.getStatus() == ReservationStatus.CANCELLED
				|| reservation.getStatus() == ReservationStatus.EXPIRED) {
			throw new BusinessException(ErrorCode.RESERVATION_CANCELLED);
		}

		// 3. 예약 시간 정보 가져오기
		List<ReservationRoom> reservationRooms = reservationRoomRepository
				.findByReservation(reservation);

		if (reservationRooms.isEmpty()) {
			throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
		}

		List<RoomSlot> roomSlots = reservationRooms.stream()
				.map(ReservationRoom::getRoomSlot)
				.toList();

		return createEquipmentReservations(reservation, roomSlots, request.equipments()); //TODO:확인

	}

	private List<ReservationEquipment> createEquipmentReservations(Reservation reservation,
																   List<RoomSlot> roomSlots,
																   List<EquipmentReservationReq> equipmentRequests) {


		//1. 예약 시간 범위 계산
		LocalDateTime startTime = roomSlots.stream()
				.map(RoomSlot::getSlotStartAt)
				.min(LocalDateTime::compareTo)
				.orElseThrow();

		LocalDateTime endTime = roomSlots.stream()
				.map(RoomSlot::getSlotEndAt)
				.max(LocalDateTime::compareTo)
				.orElseThrow();

		log.debug("예약 시간 - start: {}, end: {}", startTime, endTime);

		// 2. 비품 예약 생성
		List<ReservationEquipment> reservationEquipments = new ArrayList<>();

		for (EquipmentReservationReq equipmentReq : equipmentRequests) {
			Equipment equipment = equipmentRepository.findById(equipmentReq.getEquipmentId())
					.orElseThrow(() -> new BusinessException(ErrorCode.EQUIPMENT_NOT_FOUND));

			// 5. 비품 상태 확인
			if (equipment.getStatus() != EquipmentStatus.AVAILABLE) {
				throw new BusinessException(ErrorCode.EQUIPMENT_NOT_AVAILABLE);
			}

			// 6. 재고 검증
			validateAvailableStock(
					equipment.getEquipmentId(),
					equipmentReq.getQuantity(),
					startTime,
					endTime
			);

			// 7. 단가와 총액 계산
			BigDecimal unitPrice = equipmentReq.getUnitPrice() != null
					? equipmentReq.getUnitPrice()
					: equipment.getPrice();

			BigDecimal totalAmount = unitPrice
					.multiply(BigDecimal.valueOf(equipmentReq.getQuantity()));

			// 8. ReservationEquipment 생성
			ReservationEquipment reservationEquipment = ReservationEquipment.builder()
					.reservation(reservation)
					.equipment(equipment)
					.quantity(equipmentReq.getQuantity())
					.status(ReservationStatus.PENDING) //TODO: status기본값 PENDING X -> 기본에서 PENDING으로 변경해야함.
					.unitPrice(unitPrice)
					.totalAmount(totalAmount)
					.build();

			reservationEquipments.add(reservationEquipment);

			log.debug("비품 예약 생성 - equipment: {}, quantity: {}, amount: {}",
					equipment.getEquipmentName(), equipmentReq.getQuantity(), totalAmount);
		}

		// 9. 저장
		List<ReservationEquipment> saved = reservationEquipmentRepository.saveAll(reservationEquipments);

		// 10. Reservation 총액 업데이트
		updateReservationTotalAmount(reservation);

		log.info("비품 예약 추가 완료 - count: {}", saved.size());

		return saved;
	}

	/**
	 * 사용 가능한 비품 목록 조회 (실시간 폴링용)
	 */
	@Override
	@Transactional(readOnly = true)
	public List<EquipmentAvailabilityDto> getAvailableEquipments(Long reservationId) {
		log.debug("사용 가능한 재고 조회 - reservationId: {}", reservationId);

		// 예약 존재 여부 확인
		if (!reservationRepository.existsById(reservationId)) {
			throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
		}

		return equipmentRepository.findAvailableEquipmentsByReservation(reservationId);
	}

	/**
	 * 재고 검증
	 */
	private void validateAvailableStock(
			Long equipmentId,
			int requestedQuantity,
			LocalDateTime startTime,
			LocalDateTime endTime) {

		Equipment equipment = equipmentRepository.findById(equipmentId)
				.orElseThrow(() -> new BusinessException(ErrorCode.EQUIPMENT_NOT_FOUND));

		int reservedQuantity = reservationEquipmentRepository
				.calculateReservedQuantity(equipmentId, startTime, endTime);

		int availableQuantity = equipment.getTotalStock() - reservedQuantity;

		log.debug("재고 검증 - equipment: {}, total: {}, reserved: {}, available: {}, requested: {}",
				equipment.getEquipmentName(),
				equipment.getTotalStock(),
				reservedQuantity,
				availableQuantity,
				requestedQuantity);

		if (availableQuantity < requestedQuantity) {
			throw new BusinessException(ErrorCode.EQUIPMENT_OUT_OF_STOCK);
		}
	}

	/**
	 * 예약 총액 업데이트
	 */
	private void updateReservationTotalAmount(Reservation reservation) {
		//1. 회의실 금액 계산(ReservationRoom의 amount합계)
		List<ReservationRoom> reservationRooms = reservationRoomRepository
				.findByReservation(reservation);

		log.info("---383--");
		log.info(reservation.toString());

		BigDecimal roomTotal = reservationRooms.stream()
				.map(ReservationRoom::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		//2. 비품 총액 계산 (취소된 것 제외)
		BigDecimal equipmentTotal = reservationEquipmentRepository
				.findByReservation_ReservationIdAndStatusNot(
						reservation.getReservationId(),
						ReservationStatus.CANCELLED
				)
				.stream()
				.map(ReservationEquipment::getTotalAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		//3. 총액 = 회의실 금액 + 비품 금액
		BigDecimal newTotal = roomTotal.add(equipmentTotal);
		reservation.updateTotalAmount(newTotal);

		log.debug("예약 총액 업데이트 - reservationId: {}, room: {}원, equipment: {}원, total: {}원",
				reservation.getReservationId(), roomTotal, equipmentTotal, newTotal);
	}

}
