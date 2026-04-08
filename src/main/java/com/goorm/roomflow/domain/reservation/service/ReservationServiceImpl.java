package com.goorm.roomflow.domain.reservation.service;

import com.goorm.roomflow.domain.equipment.dto.EquipmentAvailabilityDto;
import com.goorm.roomflow.domain.equipment.entity.Equipment;
import com.goorm.roomflow.domain.equipment.entity.EquipmentStatus;
import com.goorm.roomflow.domain.equipment.repository.EquipmentRepository;
import com.goorm.roomflow.domain.holiday.service.AdminHolidayService;
import com.goorm.roomflow.domain.reservation.dto.request.*;
import com.goorm.roomflow.domain.reservation.dto.response.EquipmentItem;
import com.goorm.roomflow.domain.reservation.dto.response.EquipmentReservationRes;
import com.goorm.roomflow.domain.reservation.dto.response.ReservationRoomRes;
import com.goorm.roomflow.domain.reservation.dto.response.ReservationTimeSlot;
import com.goorm.roomflow.domain.reservation.entity.*;
import com.goorm.roomflow.domain.reservation.event.ReservationStatusChangedEvent;
import com.goorm.roomflow.domain.reservation.mapper.ReservationRoomMapper;
import com.goorm.roomflow.domain.reservation.repository.ReservationEquipmentRepository;
import com.goorm.roomflow.domain.reservation.repository.ReservationPolicyRepository;
import com.goorm.roomflow.domain.reservation.repository.ReservationRepository;
import com.goorm.roomflow.domain.reservation.repository.ReservationRoomRepository;
import com.goorm.roomflow.domain.room.entity.MeetingRoom;
import com.goorm.roomflow.domain.room.entity.RoomSlot;
import com.goorm.roomflow.domain.room.entity.RoomStatus;
import com.goorm.roomflow.domain.room.repository.MeetingRoomRepository;
import com.goorm.roomflow.domain.room.repository.RoomSlotRepository;
import com.goorm.roomflow.domain.user.entity.User;
import com.goorm.roomflow.domain.user.entity.UserRole;
import com.goorm.roomflow.domain.user.repository.UserJpaRepository;
import com.goorm.roomflow.domain.user.service.CustomUser;
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
	private final ReservationPolicyRepository reservationPolicyRepository;
	private final AdminHolidayService adminHolidayService;

	@Override
	public Reservation getReservation(Long userId, Long reservationId) {
		log.debug("예약 조회 - reservationId: {}", reservationId);


		Reservation reservation = reservationRepository.findById(reservationId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));


		// ✅ 권한 체크
		if (!reservation.getUser().getUserId().equals(userId)) {
			throw new BusinessException(ErrorCode.RESERVATION_FORBIDDEN);
		}

		return reservation;
	}

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
	public ReservationRoomRes readReservationRoom(CustomUser user, Long reservationId) {
		long start = System.currentTimeMillis();
		Long userId = user.getUserId();

		log.info("회의실 예약 조회 시작 - userId={}, reservationId={}", userId, reservationId);

		// 1. 예약 조회
		Reservation reservation = reservationRepository.findByIdWithUserAndMeetingRoom(reservationId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

		// 2. 권한 검증
		if (!user.isAdmin() && !userId.equals(reservation.getUser().getUserId())) {
			log.warn("예약 조회 권한 없음 - userId={}, reservationId={}, reservationOwnerId={}",
					userId, reservationId, reservation.getUser().getUserId());

			throw new BusinessException(ErrorCode.RESERVATION_FORBIDDEN);
		}

		// 3. 회의실 예약 조회
		List<ReservationRoom> reservationRooms = reservationRoomRepository.findByReservation(reservation);

		if (reservationRooms.isEmpty()) {
			log.warn("예약된 회의실 정보 없음 - reservationId={}", reservationId);

			throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
		}

		MeetingRoom meetingRoom = reservation.getMeetingRoom();

		// 4. 시간 조회
		List<RoomSlot> roomSlots = reservationRooms.stream()
				.map(ReservationRoom::getRoomSlot)
				.sorted(Comparator.comparing(RoomSlot::getSlotStartAt))
				.toList();

		LocalDate reservationDate = roomSlots.getFirst().getSlotStartAt().toLocalDate();
		List<ReservationTimeSlot> reservationTimeSlots = makeReservationTimeSlot(roomSlots);

		// 5. 비품 예약 조회
		List<EquipmentItem> equipmentItems = reservationEquipmentRepository
				.findByReservation_ReservationIdAndStatus(reservationId, ReservationStatus.PENDING)
				.stream()
				.map(EquipmentItem::from)
				.toList();

		// 금액 계산
		BigDecimal roomAmount = calcTotalAmount(meetingRoom.getHourlyPrice(), roomSlots);

		BigDecimal equipmentAmount = equipmentItems.stream()
				.map(EquipmentItem::totalAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal totalAmount = equipmentAmount;

		if (reservation.getStatus() == ReservationStatus.PENDING) {
			totalAmount = totalAmount.add(roomAmount);
		}

		long elapsed = System.currentTimeMillis() - start;
		log.info("회의실 예약 조회 완료 - reservationId={}, slotCount={}, equipmentCount={}, elapsedMs={}",
				reservationId,
				roomSlots.size(),
				equipmentItems.size(),
				elapsed);

		return reservationRoomMapper.toReservationRoomRes(
				reservation,
				meetingRoom,
				reservationTimeSlots,
				equipmentItems,
				reservationDate,
				roomSlots.size(),
				roomAmount,
				equipmentAmount,
				totalAmount
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
	@Transactional
	public ReservationRoomRes createReservationRoomTransactional(Long userId, CreateReservationRoomReq request) {
		long start = System.currentTimeMillis();

		log.info("회의실 예약 생성 시작 - userId={}, roomId={}, slotIds={}, idempotencyKey={}",
				userId, request.roomId(), request.roomSlotIds(), request.idempotencyKey());

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));


		// 1. 회의실 조회
		MeetingRoom meetingRoom = meetingRoomRepository.findById(request.roomId())
				.orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

		if (meetingRoom.getStatus() != RoomStatus.AVAILABLE) {
			log.warn("예약 불가 회의실 - roomId={}, status={}", meetingRoom.getRoomId(), meetingRoom.getStatus());
			throw new BusinessException(ErrorCode.ROOM_NOT_AVAILABLE);
		}

		// 2. 멱등키 중복 체크
		if (reservationRepository.existsByIdempotencyKey(request.idempotencyKey())) {
			log.warn("중복 예약 요청 감지 - userId={}, slotIds={}, idempotencyKey={}",
					userId, request.roomSlotIds(), request.idempotencyKey());
			throw new BusinessException(ErrorCode.DUPLICATE_RESERVATION_REQUEST);
		}

		// 3. 슬롯 조회
		List<RoomSlot> roomSlots = roomSlotRepository.findActiveSlotsByRoomIdAndRoomSlotIds(
				request.roomId(), request.roomSlotIds());

		// 3-1. 슬롯 예외 처리
		if (roomSlots.size() != request.roomSlotIds().size()) {
			log.warn("존재하지 않거나 이미 비활성화된 슬롯 포함 - roomId={}, requestedSlotIds={}, foundSlotCount={}",
					request.roomId(), request.roomSlotIds(), roomSlots.size());

			throw new BusinessException(ErrorCode.RESERVATION_ALREADY_EXISTS);
		}

		LocalDate reservationDate = roomSlots.getFirst().getSlotStartAt().toLocalDate();

		adminHolidayService.validateHoliday(reservationDate);

		// 4. 금액 계산
		BigDecimal totalAmount = calcTotalAmount(meetingRoom.getHourlyPrice(), roomSlots);

		// 5. 예약 생성
		Reservation reservation = new Reservation(
				user,
				meetingRoom,
				request.idempotencyKey(),
				totalAmount);

		reservationRepository.save(reservation);

		log.info("예약 엔티티 저장 완료 - reservationId={}, roomId={}",
				reservation.getReservationId(), meetingRoom.getRoomId());

		// 5-1. 예약 history 발행
		publishReservationStatusChangedEvent(
				reservation,
				ReservationStatus.NONE,
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

		log.info("회의실 예약 엔티티 저장 완료 - reservationId={}, reservationRoomCount={}",
				reservation.getReservationId(), reservationRooms.size());

		int totalHours = roomSlots.size();

		// 7. 슬롯 상태 변경
		for (RoomSlot roomSlot : roomSlots) {
			roomSlot.updateActiveStatus(false);
		}

		log.info("슬롯 비활성화 완료 - reservationId={}, slotIds={}, count={}",
				reservation.getReservationId(), request.roomSlotIds(), roomSlots.size());

		// 8. 슬롯 정리
		List<ReservationTimeSlot> reservationTimeSlots = makeReservationTimeSlot(roomSlots);

		long elapsed = System.currentTimeMillis() - start;
		log.info("회의실 예약 생성 완료 - reservationId={}, userId={}, elapsedMs={}",
				reservation.getReservationId(), userId, elapsed);

		// 9. 결과 반환
		return reservationRoomMapper.toReservationRoomRes(
				reservation,
				meetingRoom,
				reservationTimeSlots,
				null,
				reservationDate,
				totalHours,
				reservation.getTotalAmount(),
				BigDecimal.valueOf(0),
				reservation.getTotalAmount()
		);

	}
	/**
	 * 비품 예약 추가
	 * 예약 상태를 PENDING으로 변경
	 */
	@Override
	@Transactional
	public EquipmentReservationRes addEquipmentsToReservation(Long userId,
															  Long reservationId,
															  AddEquipmentsReq request) {


		// 1. 예약 조회
		Reservation reservation = reservationRepository.findById(reservationId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

		log.info("addEquipmentsToReservation, {}", reservation.getReservationId());

		if (!reservation.getUser().getUserId().equals(userId)) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		// 2. 취소/만료된 예약 체크
		if (reservation.getStatus() == ReservationStatus.CANCELLED
				|| reservation.getStatus() == ReservationStatus.EXPIRED) {
			throw new BusinessException(ErrorCode.RESERVATION_CANCELLED);
		}

		// 3. 예약 시간 정보 가져오기
		List<RoomSlot> roomSlots = reservationRoomRepository.findByReservation(reservation)
				.stream()
				.map(ReservationRoom::getRoomSlot)
				.toList();

		// 4. 비품 예약 처리
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

		// 2. ID 순서로 재정렬
		List<EquipmentReservationReq> sortedRequests = equipmentRequests.stream()
				.sorted(Comparator.comparing(EquipmentReservationReq::equipmentId))
				.toList();

		// 시간 측정 시작
		long startTimeMillis = System.currentTimeMillis();
		log.info("비품 예약 시작 - count: {}", sortedRequests.size());

		List<ReservationEquipment> reservationEquipments = new ArrayList<>();

		for ( EquipmentReservationReq equipmentReq : sortedRequests) {

			Equipment equipment = equipmentRepository.findById(equipmentReq.equipmentId())
					.orElseThrow(() -> new BusinessException(ErrorCode.EQUIPMENT_NOT_FOUND));

			// 3. 비품 상태 확인
			if (equipment.getStatus() != EquipmentStatus.AVAILABLE) {
				throw new BusinessException(ErrorCode.EQUIPMENT_NOT_AVAILABLE);
			}

			// 4. 재고 검증
			validateAvailableStock(
					reservation.getReservationId(),
					equipment,
					equipmentReq.quantity(),
					startTime,
					endTime
			);

			// 5. 단가와 총액 계산
			BigDecimal unitPrice = equipmentReq.unitPrice() != null
					? equipmentReq.unitPrice()
					: equipment.getPrice();

			BigDecimal totalAmount = unitPrice
					.multiply(BigDecimal.valueOf(equipmentReq.quantity()));

			// 6. ReservationEquipment 생성
			ReservationEquipment reservationEquipment = ReservationEquipment.builder()
					.reservation(reservation)
					.equipment(equipment)
					.quantity(equipmentReq.quantity())
					.status(ReservationStatus.PENDING)
					.unitPrice(unitPrice)
					.totalAmount(totalAmount)
					.build();


			//7. 즉시 저장
			ReservationEquipment saved = reservationEquipmentRepository.saveAndFlush(reservationEquipment);
			reservationEquipments.add(saved);


			log.debug("비품 예약 생성 - equipment: {}, quantity: {}, amount: {}",
					equipment.getEquipmentName(), equipmentReq.quantity(), totalAmount);

		}

		// ⏱️ 시간 측정 종료
		long endTimeMillis = System.currentTimeMillis();
		log.debug("⏱️ 비품 예약 완료 - count: {}, 처리시간: {}ms",
				equipmentRequests.size(), endTimeMillis - startTimeMillis);


		// 8. Reservation 총액 업데이트
		updateReservationTotalAmount(reservation);

		// 9. 상태 변경 이벤트 발행
		reservationEquipments.forEach(re ->
				publishEquipmentsReservationStatusChangedEvent(
						reservation,
						re.getReservationEquipmentId(),
						ReservationStatus.NONE,
						re.getStatus(),
						reservation.getUser(),
						"비품 예약 생성"
				)
		);

		log.info("비품 예약 추가 완료 - count: {}", reservationEquipments.size());

		return reservationEquipments;
	}


	/**
	 * 회의실 예약 및 비품 예약을 확정한다.
	 * - reservationId를 기준으로 Reservation을 조회한 후, 예약과 비품 예약을 Confirmed 상태로 변경한다.
	 *
	 * @param reservationId 조회할 예약 ID, request 비품 예약 목록 및 예약 목적
	 */
	@Override
	@Transactional
	public void confirmReservation(Long userId, Long reservationId, ConfirmReservationReq request) {

		long startTime = System.currentTimeMillis();
		log.info("예약 확정 요청 - reservationId={}, userId={}", reservationId, userId);

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		// 회의실 예약 조회
		Reservation reservation = reservationRepository.findByReservationId(reservationId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

		ReservationStatus fromStatus = reservation.getStatus();

		if (user.getRole().equals(UserRole.USER)
				&& !user.getUserId().equals(reservation.getUser().getUserId())) {

			log.warn("예약 확정 권한 없음 - reservationId={}, requestUserId={}, ownerUserId={}",
					reservationId,
					userId,
					reservation.getUser().getUserId());

			throw new BusinessException(ErrorCode.RESERVATION_FORBIDDEN);
		}

		if (reservation.getStatus() == ReservationStatus.PENDING) {
			// 회의실 예약 확정
			reservation.confirm(request.memo());

			// 회의실 예약 총 횟수 증가
			reservation.getMeetingRoom().incrementReservations();

			// 이벤트 발행
			publishReservationStatusChangedEvent(
					reservation,
					fromStatus,
					reservation.getStatus(),
					user,
					request.memo()
			);

			log.info("예약 확정 완료 - reservationId={}, userId={}", reservationId, userId);
		} else {
			log.warn("이미 처리된 예약 확정 요청 - reservationId={}, currentStatus={}", reservationId, reservation.getStatus());
		}

		// 비품 예약이 있으면 함께 확정
		if (request.reservationEquipmentIds() != null && !request.reservationEquipmentIds().isEmpty()) {
			confirmEquipment(reservationId, request.reservationEquipmentIds());
		}
		log.info("예약 확정 처리 완료 - reservationId={}, duration={}ms", reservationId, System.currentTimeMillis() - startTime);
	}

	/**
	 * 비품 예약 확정 서비스 메서드
	 * - 이미 확정된 회의실 예약에 비품을 추가하고 바로 확정
	 */
	@Override
	@Transactional
	public void confirmEquipmentsService(Long userId, Long reservationId, List<Long> reservationEquipmentIds) {
		// 예약 조회
		Reservation reservation = reservationRepository.findById(reservationId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));


		// 권한 체크
		if (!reservation.getUser().getUserId().equals(userId)) {
			log.debug("confirmEquipmentsService - reservationUserId()={}, userId={}", reservation.getUser().getUserId(), userId);
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}

		// 회의실 예약이 확정 상태인지 확인
		if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
			throw new BusinessException(ErrorCode.RESERVATION_NOT_CONFIRMED);
		}

		confirmEquipment(reservationId, reservationEquipmentIds);
	}


	/**
	 * 비품 예약 확정 private 메서드
	 * - reservationEquipmentIds에 해당하는 비품 예약들을 CONFIRMED로 변경
	 */
	private void confirmEquipment(Long reservationId, List<Long> reservationEquipmentIds) {
		log.info("비품 예약 확정 시작 - reservationId: {}, equipmentIds: {}",
				reservationId, reservationEquipmentIds);

		Reservation reservation = reservationRepository.findById(reservationId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

		// 비품 예약 조회
		List<ReservationEquipment> reservationEquipments =
				reservationEquipmentRepository.findAllById(reservationEquipmentIds);

		// 요청한 ID 개수와 조회된 개수 비교
		if (reservationEquipments.size() != reservationEquipmentIds.size()) {
			throw new BusinessException(ErrorCode.RESERVATION_EQUIPMENT_NOT_FOUND);
		}

		List<ReservationStatusChangedEvent> events = new ArrayList<>();


		for (ReservationEquipment reservationEquipment : reservationEquipments) {

			//  예약 ID 일치 확인 (전달받은 reservationId와 reservationEquipment의 reservationId의 일치여부 확인)
			if (!reservationEquipment.getReservation().getReservationId().equals(reservationId)) {
				throw new BusinessException(ErrorCode.INVALID_RESERVATION_EQUIPMENT);
			}

			// 이미 취소된 비품 예약인지 확인
			if (reservationEquipment.getStatus() == ReservationStatus.CANCELLED
					|| reservationEquipment.getStatus() == ReservationStatus.EXPIRED) {
				throw new BusinessException(ErrorCode.RESERVATION_EQUIPMENT_CANCELLED);
			}

			// 이미 확정된 경우 스킵
			if (reservationEquipment.getStatus() == ReservationStatus.CONFIRMED) {
				log.debug("이미 확정된 비품 예약 - id: {}", reservationEquipment.getReservationEquipmentId());
				continue;
			}

			//  현재 상태 저장 (fromStatus)
			ReservationStatus fromStatus = reservationEquipment.getStatus();

			// 상태 변경 (PENDING -> CONFIRMED)
			reservationEquipment.confirm();

			// 비품 총 예약 건수++ 추가
			Equipment equipment = reservationEquipment.getEquipment();
			equipment.incrementReservations();


			events.add(new ReservationStatusChangedEvent(
					reservation,
					TargetType.EQUIPMENT,
					reservationEquipment.getReservationEquipmentId(),  // 비품 예약 ID
					fromStatus,
					reservationEquipment.getStatus(),
					reservation.getUser(),
					"비품 예약 확정"
			));


			log.info("비품 예약 확정 완료 - equipmentId: {}, quantity: {}",
					reservationEquipment.getEquipment().getEquipmentId(),
					reservationEquipment.getQuantity());
		}

		// 총액 업데이트
		updateReservationTotalAmount(reservation);

		events.forEach(eventPublisher::publishEvent);
		log.info("비품 예약 확정 완료 - count: {}", reservationEquipments.size());
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
	public void cancelReservation(Long userId, Long reservationId, CancelReservationReq request) {
		long start = System.currentTimeMillis();
		log.info("예약 취소 시작 - userId={}, reservationId={}", userId, reservationId);

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		// 예약 조회
		Reservation reservation = reservationRepository.findByIdWithAll(reservationId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

		if (user.getRole().equals(UserRole.USER)
				&& !user.getUserId().equals(reservation.getUser().getUserId())) {
			log.warn("예약 취소 권한 없음 - userId={}, reservationId={}, reservationUserId={}",
					userId, reservationId, reservation.getUser().getUserId());

			throw new BusinessException(ErrorCode.RESERVATION_FORBIDDEN);
		}

		validateCancelDeadline(reservation);

		ReservationStatus fromStatus = reservation.getStatus();

		log.info("예약 취소 대상 조회 완료 - reservationId={}", reservationId);

		String reason = request.reason();

		if (reason == null || reason.isBlank()) {
			reason = "사용자 취소";
		}

		// 회의실 취소 상태 검증 및 변경
		reservation.cancel(reason);
		// 회의실 예약 총 횟수 감소
		reservation.getMeetingRoom().decrementReservations();

		// 회의실 목록 조회
		List<ReservationRoom> reservationRooms = reservationRoomRepository.findByReservation(reservation);

		// 예약 상태 변경 - 슬롯 복구
		reservationRooms.forEach(reservationRoom -> {
			RoomSlot roomSlot = reservationRoom.getRoomSlot();
			roomSlot.updateActiveStatus(true);
		});

		log.info("예약 상태 변경 완료 - reservationId={}, reason={}, reservationRoomCount={}",
				reservationId, reason, reservationRooms.size());

		// 비품이 있는경우 함께 삭제
		if (request.reservationEquipmentIds() != null && !request.reservationEquipmentIds().isEmpty()) {
			cancelEquipments(reservation, request.reservationEquipmentIds(), reason);
		}

		// 예약 금액 - 0원 변환
		reservation.updateTotalAmount(BigDecimal.ZERO);

		// 상태 이벤트 발행
		publishReservationStatusChangedEvent(
				reservation,//reservation
				fromStatus, //confirmed
				reservation.getStatus(),//cancelled
				user, //User
				reason //"cancel"
		);

		log.info("예약 취소 완료 - reservationId={}, userId={}, elapsed={}ms",
				reservationId, userId, System.currentTimeMillis() - start);
	}

	@Override
	@Transactional
	public void cancelReservationEquipments(Long userId, Long reservationId, CancelReservationEquipmentsReq request) {
		try {

			// 1. 예약 조회
			Reservation reservation = reservationRepository.findById(reservationId)
					.orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

			// 2. 권한 체크
			if (!reservation.getUser().getUserId().equals(userId)) {
				throw new BusinessException(ErrorCode.FORBIDDEN);
			}

			// 취소/만료된 예약은 비품 취소 불가
			if (reservation.getStatus() == ReservationStatus.CANCELLED || reservation.getStatus() == ReservationStatus.EXPIRED) {
				throw new BusinessException(ErrorCode.RESERVATION_CANCELLED);
			}
			log.debug("cancelEquipments 호출 전");
			cancelEquipments(reservation, request.reservationEquipmentIds(), request.reason());
			log.debug("cancelEquipments 호출 후");
		} catch (Exception e) {
			log.error("비품 예약 취소 실패 - reservationId: {}, error: {}",
					reservationId, e.getMessage(), e);
			throw new BusinessException(ErrorCode.RESERVATION_EQUIPMENT_CANCEL_FAILED);

		}
	}

	/**
	 * 회의실 예약을 만료시킨다.
	 * - 이전 단계로 돌아갈 때, 회의실 예약 상태를 만료상태로 변경하고 슬롯을 유효상태로 변경한다.
	 *
	 * @param reservationId 예약 Key
	 * @throws BusinessException 예약이 존재하지 않거나 만료 가능한 상태가 아닌 경우 발생
	 */
	@Override
	@Transactional
	public void expireReservation(Long userId, Long reservationId) {
		long start = System.currentTimeMillis();
		log.info("[이전 단계] 예약 만료 시작 - userId={}, reservationId={}", userId, reservationId);

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		// 예약 조회
		Reservation reservation = reservationRepository.findByReservationId(reservationId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

		if (user.getRole().equals(UserRole.USER)
				&& !user.getUserId().equals(reservation.getUser().getUserId())) {
			log.warn("[이전 단계] 예약 만료 권한 없음 - userId={}, reservationId={}, reservationUserId={}",
					userId, reservationId, reservation.getUser().getUserId());

			throw new BusinessException(ErrorCode.RESERVATION_FORBIDDEN);
		}
		ReservationStatus fromStatus = reservation.getStatus();

		if (fromStatus == ReservationStatus.PENDING) {
			String reason = "이전 상태로 이동";

			// 회의실 취소 상태 검증 및 변경
			reservation.expire(reason);

			log.info("[이전 단계] 예약 상태 변경 완료 - reservationId={}, {} -> {}", reservationId, fromStatus, reservation.getStatus());

			// 회의실 목록 조회
			List<ReservationRoom> reservationRooms = reservationRoomRepository.findByReservation(reservation);

			// 예약 상태 변경 - 슬롯 복구
			reservationRooms.forEach(reservationRoom -> {
				RoomSlot roomSlot = reservationRoom.getRoomSlot();
				roomSlot.updateActiveStatus(true);
			});

			log.info("[이전 단계] 예약 슬롯 복구 완료 - reservationId={}, slotCount={}",
					reservationId, reservationRooms.size());

			// 회의실 예약 슬롯 삭제
			reservationRoomRepository.deleteAll(reservationRooms);

			log.info("[이전 단계] 예약 슬롯 삭제 완료 - reservationId={}, deletedCount={}",
					reservationId, reservationRooms.size());

			// 상태 이벤트 발행
			publishReservationStatusChangedEvent(
					reservation,
					fromStatus,
					reservation.getStatus(),
					user,
					reason
			);

			log.info("예약 만료 완료 - reservationId={}, userId={}, elapsed={}ms", reservationId, userId, System.currentTimeMillis() - start);
		}

	}

	// 연속된 RoomSlot을 하나의 예약 시간 구간으로 병합한다.
	private List<ReservationTimeSlot> makeReservationTimeSlot(List<RoomSlot> sortedSlots) {

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

	/**
	 * 사용 가능한 비품 목록 조회 (실시간 폴링용)
	 */
	@Override
	@Transactional(readOnly = true)
	public List<EquipmentAvailabilityDto> getAvailableEquipments(Long userId, Long reservationId) {
		log.debug("사용 가능한 재고 조회 - reservationId: {}", reservationId);

		// 1. 예약 조회
		Reservation reservation = reservationRepository.findById(reservationId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));


		// 권한 체크
		if (!reservation.getUser().getUserId().equals(userId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}

		return equipmentRepository.findAvailableEquipmentsByReservation(reservationId);
	}

	/**
	 * 재고 검증
	 */
	private void validateAvailableStock(
			Long reservationId,
			Equipment equipment,
			int requestedQuantity,
			LocalDateTime startTime,
			LocalDateTime endTime) {

		List<Integer> quantities = reservationEquipmentRepository.findReservedQuantities(reservationId, equipment.getEquipmentId(), startTime, endTime );

		int reservedQuantity = quantities.stream().mapToInt(Integer::intValue).sum();

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

		log.info("회의실 예약 상태 변경 이벤트 발행 - reservationId={}, {} -> {}", reservation.getReservationId(), fromStatus, toStatus);
	}


	/**
	 * 비품 예약 상태 변경 이벤트 발행
	 *
	 * @param reservation            예약
	 * @param equipmentReservationId 비품 예약 ID
	 * @param fromStatus             이전 상태
	 * @param toStatus               변경된 상태
	 * @param changedBy              변경자
	 * @param reason                 변경 사유
	 */
	private void publishEquipmentsReservationStatusChangedEvent(
			Reservation reservation,
			Long equipmentReservationId,
			ReservationStatus fromStatus,
			ReservationStatus toStatus,
			User changedBy,
			String reason
	) {
		if (fromStatus == toStatus) {
			return;
		}

		log.info("비품 예약 상태 변경 이벤트 - equipmentReservationId: {}, {} -> {}",
				equipmentReservationId, fromStatus, toStatus);

		eventPublisher.publishEvent(new ReservationStatusChangedEvent(
				reservation,
				TargetType.EQUIPMENT,
				equipmentReservationId,
				fromStatus,
				toStatus,
				changedBy,
				reason
		));
	}

	/**
	 * 비품 취소 private 메서드
	 */
	private void cancelEquipments(
			Reservation reservation,
			List<Long> reservationEquipmentIds,
			String reason
	) {
		//예약ID 조회
		Long reservationId = reservation.getReservationId();

		log.info("비품 예약 취소 시작 - reservationId: {}, equipmentIds: {}, count: {}",
				reservationId, reservationEquipmentIds, reservationEquipmentIds.size());

		//비품 예약 조회
		List<ReservationEquipment> reservationEquipments = reservationEquipmentRepository.findAllById(reservationEquipmentIds);
		log.debug("비품 예약 조회 완료 - count: {}", reservationEquipments.size());

		if (reservationEquipments.size() != reservationEquipmentIds.size()) {
			throw new BusinessException(ErrorCode.RESERVATION_EQUIPMENT_NOT_FOUND);
		}

		List<ReservationStatusChangedEvent> events = new ArrayList<>();

		for (ReservationEquipment reservationEquipment : reservationEquipments) {
			log.debug("처리 중 - id: {}", reservationEquipment.getReservationEquipmentId());

			// 예약 ID 일치 확인
			if (!reservationEquipment.getReservation().getReservationId().equals(reservationId)) {
				throw new BusinessException(ErrorCode.INVALID_RESERVATION_EQUIPMENT);
			}

			// 이미 취소된 비품 예약인지 확인
			if (reservationEquipment.getStatus() == ReservationStatus.CANCELLED) {
				log.debug("이미 취소된 비품 예약 - id: {}",
						reservationEquipment.getReservationEquipmentId());
				continue;
			}

			// 현재 상태 저장 (Confirmed)
			ReservationStatus fromStatus = reservationEquipment.getStatus();

			// reason 처리
			String finalReason = (reason != null && !reason.isBlank())
					? reason
					: "사용자 취소";

			//  상태 변경 (cancelledAt 자동 설정)
			log.debug("상태 변경 - cancel() 호출");
			reservationEquipment.cancel(finalReason);

			// 확정된 비품이었다면 예약 건수 감소
			if (fromStatus == ReservationStatus.CONFIRMED) {
				Equipment equipment = reservationEquipment.getEquipment();
				equipment.decrementReservations();
			}
			events.add(new ReservationStatusChangedEvent(
					reservation,
					TargetType.EQUIPMENT,
					reservationEquipment.getReservationEquipmentId(),
					fromStatus,
					reservationEquipment.getStatus(),
					reservation.getUser(),
					finalReason
			));

		}

		// 총액 업데이트
		updateReservationTotalAmount(reservation);


		// 이벤트 발행
		log.debug("이벤트 발행 전");
		events.forEach(eventPublisher::publishEvent);

		log.info("비품 예약 취소 완료 - count: {}", reservationEquipments.size());

	}


	@Override
	@Transactional
	public void expirePendingEquipments(Long changedByUserId, List<Long> reservationEquipmentIds) {

		if (reservationEquipmentIds == null || reservationEquipmentIds.isEmpty()) {
			log.info("expire 대상 없음");
			return;
		}

		log.info("비품 예약 expire 시작 - ids: {}, count: {}, changedBy: {}",
				reservationEquipmentIds,
				reservationEquipmentIds.size(),
				changedByUserId);


		// user 조회
		User changedBy = userRepository.findById(changedByUserId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		String reason = "이전 단계 이동";

		List<ReservationEquipment> reservationEquipments =
				reservationEquipmentRepository.findAllByIdWithReservation(reservationEquipmentIds);


		List<ReservationEquipment> pendingEquipments = reservationEquipments.stream()
				.filter(equipments -> equipments.getStatus() == ReservationStatus.PENDING)
				.toList();

		pendingEquipments.forEach(equipment -> {
			Reservation reservation = equipment.getReservation();
			ReservationStatus fromStatus = equipment.getStatus();

			equipment.expire(reason);

			publishEquipmentsReservationStatusChangedEvent(
					reservation,
					equipment.getReservationEquipmentId(),
					fromStatus,
					equipment.getStatus(),
					changedBy,
					reason
			);
		});

		log.info("비품 예약 expire 완료 - 처리: {}/{}",
				pendingEquipments.size(),
				reservationEquipments.size());


	}

	/**
	 * 결제용 예약 정보 조회 (확정된 비품 포함)
	 */
	@Override
	@Transactional
	public ReservationRoomRes readConfirmedReservationRoom(CustomUser user, Long reservationId) {

			long start = System.currentTimeMillis();
			Long userId = user.getUserId();

			log.info("결제용 예약 조회 시작 - userId={}, reservationId={}", userId, reservationId);

			// 1. 예약 조회
			Reservation reservation = reservationRepository.findByIdWithUserAndMeetingRoom(reservationId)
					.orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

			// 2. 권한 검증
			if (!user.isAdmin() && !userId.equals(reservation.getUser().getUserId())) {
				throw new BusinessException(ErrorCode.RESERVATION_FORBIDDEN);
			}

			// 3. 회의실 예약 조회
			List<ReservationRoom> reservationRooms = reservationRoomRepository.findByReservation(reservation);

			if (reservationRooms.isEmpty()) {
				throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
			}

			MeetingRoom meetingRoom = reservation.getMeetingRoom();

			// 4. 시간 조회
			List<RoomSlot> roomSlots = reservationRooms.stream()
					.map(ReservationRoom::getRoomSlot)
					.sorted(Comparator.comparing(RoomSlot::getSlotStartAt))
					.toList();

			LocalDate reservationDate = roomSlots.getFirst().getSlotStartAt().toLocalDate();
			List<ReservationTimeSlot> reservationTimeSlots = makeReservationTimeSlot(roomSlots);

			// 5. 비품 예약 조회 - CONFIRMED 상태로 조회
			List<EquipmentItem> equipmentItems = reservationEquipmentRepository
					.findByReservation_ReservationIdAndStatus(reservationId, ReservationStatus.CONFIRMED)
					.stream()
					.map(EquipmentItem::from)
					.toList();

			// 6. 금액 계산 - 회의실 + 비품 모두 포함
			BigDecimal roomAmount = calcTotalAmount(meetingRoom.getHourlyPrice(), roomSlots);

			BigDecimal equipmentAmount = equipmentItems.stream()
					.map(EquipmentItem::totalAmount)
					.reduce(BigDecimal.ZERO, BigDecimal::add);

			BigDecimal totalAmount = roomAmount.add(equipmentAmount);

			long elapsed = System.currentTimeMillis() - start;
			log.info("결제용 예약 조회 완료 - reservationId={}, totalAmount={}, elapsedMs={}",
					reservationId, totalAmount, elapsed);

			return reservationRoomMapper.toReservationRoomRes(
					reservation,
					meetingRoom,
					reservationTimeSlots,
					equipmentItems,
					reservationDate,
					roomSlots.size(),
					roomAmount,
					equipmentAmount,
					totalAmount
			);

	}

	private void validateCancelDeadline(Reservation reservation) {
		int cancelDeadlineMinutes = getCancelDeadlineMinutes();

		LocalDateTime reservationStartAt = reservation.getReservationRooms().stream()
				.map(reservationRoom -> reservationRoom.getRoomSlot().getSlotStartAt())
				.min(LocalDateTime::compareTo)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_START_TIME_NOT_FOUND));

		LocalDateTime deadline = reservationStartAt.minusMinutes(cancelDeadlineMinutes);

		if (LocalDateTime.now().isAfter(deadline)) {
			throw new BusinessException(ErrorCode.RESERVATION_CANCEL_DEADLINE_EXPIRED);
		}
	}

	private int getCancelDeadlineMinutes() {
		return Integer.parseInt(
				reservationPolicyRepository.findByPolicyKey("CANCEL_DEADLINE_MINUTES")
						.orElseThrow(() -> new BusinessException(ErrorCode.POLICY_NOT_FOUND))
						.getPolicyValue()
		);
	}
}


