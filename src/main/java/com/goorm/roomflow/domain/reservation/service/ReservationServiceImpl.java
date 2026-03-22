package com.goorm.roomflow.domain.reservation.service;

import com.goorm.roomflow.domain.equipment.dto.EquipmentAvailabilityDto;
import com.goorm.roomflow.domain.equipment.dto.request.EquipmentReservationReq;
import com.goorm.roomflow.domain.equipment.dto.response.EquipmentReservationRes;
import com.goorm.roomflow.domain.equipment.entity.Equipment;
import com.goorm.roomflow.domain.equipment.entity.EquipmentStatus;
import com.goorm.roomflow.domain.equipment.repository.EquipmentRepository;
import com.goorm.roomflow.domain.reservation.dto.request.AddEquipmentsReq;
import com.goorm.roomflow.domain.reservation.dto.request.CancelReservationReq;
import com.goorm.roomflow.domain.reservation.dto.request.ConfirmReservationReq;
import com.goorm.roomflow.domain.reservation.dto.request.CreateReservationRoomReq;
import com.goorm.roomflow.domain.reservation.dto.response.ReservationRoomRes;
import com.goorm.roomflow.domain.reservation.dto.response.ReservationTimeSlot;
import com.goorm.roomflow.domain.reservation.entity.Reservation;
import com.goorm.roomflow.domain.reservation.entity.ReservationEquipment;
import com.goorm.roomflow.domain.reservation.entity.ReservationRoom;
import com.goorm.roomflow.domain.reservation.entity.ReservationStatus;
import com.goorm.roomflow.domain.reservation.entity.TargetType;
import com.goorm.roomflow.domain.reservation.event.ReservationStatusChangedEvent;
import com.goorm.roomflow.domain.reservation.mapper.ReservationRoomMapper;
import com.goorm.roomflow.domain.reservation.repository.ReservationEquipmentRepository;
import com.goorm.roomflow.domain.reservation.repository.ReservationRepository;
import com.goorm.roomflow.domain.reservation.repository.ReservationRoomRepository;
import com.goorm.roomflow.domain.room.entity.MeetingRoom;
import com.goorm.roomflow.domain.room.entity.RoomSlot;
import com.goorm.roomflow.domain.room.entity.RoomStatus;
import com.goorm.roomflow.domain.room.repository.MeetingRoomRepository;
import com.goorm.roomflow.domain.room.repository.RoomSlotRepository;
import com.goorm.roomflow.domain.user.entity.User;
import com.goorm.roomflow.domain.user.repository.UserJpaRepository;
import com.goorm.roomflow.global.code.ErrorCode;
import com.goorm.roomflow.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
	private final UserJpaRepository userRepository;
	private final ApplicationEventPublisher eventPublisher;

	/**
	 * 회의실 예약 정보를 조회한다.
	 * - reservationId를 기준으로 Reservation을 조회한 후,
	 * - 해당 예약에 연결된 ReservationRoom 및 RoomSlot 정보를 함께 조회하여 반환한다.
	 *
	 * @param reservationId 조회할 예약 ID
	 * @return 예약된 회의실 및 시간 슬롯 정보
	 * @throws BusinessException 예약을 찾을 수 없는 경우 발생
	 */
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

	/**
	 * 회의실 예약을 생성한다.
	 * - 회의실과 슬롯의 유효성을 확인한 뒤 예약을 생성하고, 선택한 슬롯을 예약 불가능 상태로 변경한다.
	 * 1. 회의실 존재 여부 확인
	 * 2. 멱등키 중복 여부 확인
	 * 3. 선택한 슬롯 존재 여부 및 예약 가능 여부 확인
	 * 4. 총 예약 금액 계산
	 * 5. Reservation 및 ReservationRoom 생성
	 * 6. 슬롯 비활성화 처리
	 *
	 * @param request 회의실 예약 생성 요청 정보
	 * @return 생성된 예약 정보 및 시간 슬롯 정보
	 * @throws BusinessException 회의실 또는 슬롯이 존재하지 않거나 이미 예약된 경우 발생
	 */
	@Override
	@Transactional
	public ReservationRoomRes createReservationRoom(CreateReservationRoomReq request) {

		// 임시 데이터
		User user = userRepository.findById(1L).get();

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
				user,
				meetingRoom,
				request.idempotencyKey(),
				totalAmount);

		reservationRepository.save(reservation);

		// 5-1. 예약 history 발행
		publishReservationStatusChangedEvent(
				reservation,
				null,
				reservation.getStatus(),
				user,
				"예약 생성"
		);

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

	/**
	 * 비품 예약 추가
	 * - PENDING 상태: 회의실 예약과 함께 비품 추가
	 * - CONFIRMED 상태: 확정된 예약에 비품 추가
	 */
	@Override
	@Transactional
	public EquipmentReservationRes addEquipmentsToReservation(
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


		List<ReservationEquipment> savedEquipments =
				createEquipmentReservations(reservation, roomSlots, request.equipments());

		return EquipmentReservationRes.from(reservation, savedEquipments);

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
	 * 회의실 예약 및 비품 예약을 확정한다.
	 * - reservationId를 기준으로 Reservation을 조회한 후, 예약과 비품 예약을 Confirmed 상태로 변경한다.
	 *
	 * @param reservationId 조회할 예약 ID, request 비품 예약 목록 및 예약 목적
	 */
	@Override
	@Transactional
	public void confirmReservation(Long reservationId, ConfirmReservationReq request) {

		// 회의실 예약 조회
		Reservation reservation = reservationRepository.findById(reservationId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

		ReservationStatus fromStatus = reservation.getStatus();

		// 회의실 예약 확정
		reservation.confirm(request.memo());

		// 회의실 예약 총 횟수 증가
		reservation.getMeetingRoom().incrementReservations();

		// 이벤트 발행
		publishReservationStatusChangedEvent(
				reservation,
				fromStatus,
				reservation.getStatus(),
				reservation.getUser(),
				request.memo()
		);

		// 비품 예약이 있으면 함께 확정
		if (request.reservationEquipmentIds() != null && !request.reservationEquipmentIds().isEmpty()) {
			confirmEquipment(reservationId, request.reservationEquipmentIds());
		}
	}

	/**
	 * 1. 비품만 예약 확정하는 서비스 메서드
	 * - 이미 확정된 회의실 예약에 비품을 추가하고 바로 확정
	 */
	@Override
	@Transactional
	public void confirmEquipmentsService(Long reservationId, List<Long> reservationEquipmentIds) {
		// 예약 조회
		Reservation reservation = reservationRepository.findById(reservationId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));


		confirmEquipment(reservationId, reservationEquipmentIds);
	}


	/* TODO: 2. 비품 예약 private 메서드 */
	private void confirmEquipment(Long reservationId, List<Long> longs) {
	}

	/**
	 * 회의실 예약을 취소한다.
	 * - 회의실 예약을 취소한 후, 회의실-예약 값을 삭제하고 슬롯을 다시 유효 상태로 변경한다.
	 *
	 * @param reservationId 예약 Key
	 * @throws BusinessException 예약이 존재하지 않거나 취소 가능한 상태가 아닌 경우 발생
	 */
	@Override
	@Transactional
	public void cancelReservation(Long reservationId, CancelReservationReq request) {

		// 예약 조회
		Reservation reservation = reservationRepository.findById(reservationId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

		// TODO: 예약자 검증

		ReservationStatus fromStatus = reservation.getStatus();

		String reason = request.reason();

		if (reason == null || reason.isBlank()) {
			reason = "사용자 취소";
		}

		// 회의실 취소 상태 검증 및 변경
		reservation.cancel(reason);

		// 회의실 목록 조회
		List<ReservationRoom> reservationRooms = reservationRoomRepository.findByReservation(reservation);

		// 예약 상태 변경 - 슬롯 복구
		reservationRooms.forEach(reservationRoom -> {
			RoomSlot roomSlot = reservationRoom.getRoomSlot();
			roomSlot.updateActiveStatus(true);
		});

		// 회의실 예약 슬롯 삭제
		reservationRoomRepository.deleteAll(reservationRooms);

		// TODO: 비품이 있는경우 함께 삭제

		// 상태 이벤트 발행
		publishReservationStatusChangedEvent(
				reservation,
				fromStatus,
				reservation.getStatus(),
				reservation.getUser(),
				reason
		);

	}

	// 연속된 RoomSlot을 하나의 예약 시간 구간으로 병합한다.
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
		return reservationTimeSlots;
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

	// 회의실 시간당 요금과 예약 슬롯 수를 기준으로 총 예약 금액을 계산한다.
	private BigDecimal calcTotalAmount(BigDecimal hourlyPrice, List<RoomSlot> roomSlots) {
		return hourlyPrice.multiply(BigDecimal.valueOf(roomSlots.size()));
	}

	// 예약 상태 변화 이벤트 발행
	private void publishReservationStatusChangedEvent(
			Reservation reservation,
			ReservationStatus fromStatus,
			ReservationStatus toStatus,
			User changedBy,
			String reason
	) {
		if (fromStatus == toStatus) {
			return;
		}

		eventPublisher.publishEvent(new ReservationStatusChangedEvent(
				reservation,
				TargetType.RESERVATION,
				reservation.getReservationId(),
				fromStatus,
				toStatus,
				changedBy,
				reason
		));
	}
}
