package com.goorm.roomflow.domain.user.service;

import com.goorm.roomflow.domain.reservation.entity.Reservation;
import com.goorm.roomflow.domain.reservation.entity.ReservationEquipment;
import com.goorm.roomflow.domain.reservation.entity.ReservationRoom;
import com.goorm.roomflow.domain.reservation.entity.ReservationStatus;
import com.goorm.roomflow.domain.reservation.repository.ReservationEquipmentRepository;
import com.goorm.roomflow.domain.reservation.repository.ReservationRepository;
import com.goorm.roomflow.domain.reservation.repository.ReservationRoomRepository;
import com.goorm.roomflow.domain.user.dto.SignupRequestDTO;
import com.goorm.roomflow.domain.user.dto.UserReservationDTO;
import com.goorm.roomflow.domain.user.dto.UserTO;
import com.goorm.roomflow.domain.user.entity.SocialAccount;
import com.goorm.roomflow.domain.user.entity.User;
import com.goorm.roomflow.domain.user.entity.UserRole;
import com.goorm.roomflow.domain.user.mapper.UserMapper;
import com.goorm.roomflow.domain.user.repository.SocialAccountRepository;
import com.goorm.roomflow.domain.user.repository.UserJpaRepository;
import com.goorm.roomflow.domain.user.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserJpaRepository userJpaRepository;
    private final UserMapper userMapper;
    private final SocialAccountRepository socialAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final ReservationRepository reservationRepository;
    private final ReservationRoomRepository reservationRoomRepository;
    private final ReservationEquipmentRepository reservationEquipmentRepository;


    public void signup(SignupRequestDTO request) {

        // 이름 유효성 검사
        String name = request.getName().trim();
        if (!UserValidator.isValidName(name)) {
            throw new IllegalArgumentException("이름은 한글 2~10자 또는 영문 2~20자로 입력해주세요.");
        }

        // 이메일 형식 검사
        if (!UserValidator.isValidEmail(request.getEmail())) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
        }

        // 이메일 중복 확인
        if (userJpaRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 비밀번호 유효성 검사
        if (!UserValidator.isValidPassword(request.getPassword())) {
            throw new IllegalArgumentException("비밀번호는 영문, 숫자, 특수문자를 포함하여 8자 이상이어야 합니다.");
        }

        // 비밀번호 확인 일치 여부
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        User user = User.builder()
                .name(name)
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.USER)
                .build();

        userJpaRepository.save(user);
    }

    public void updateName(String email, String newName) {
        String trimmed = newName.trim();
        if (!UserValidator.isValidName(trimmed)) {
            throw new IllegalArgumentException("이름은 한글 2~10자 또는 영문 2~20자로 입력해주세요.");
        }
        User user = userJpaRepository.findByEmail(email);
        if (user == null || user.isDeleted()) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }
        user.updateName(trimmed);
        userJpaRepository.save(user);
    }

    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = userJpaRepository.findByEmail(email);
        if (user == null || user.isDeleted()) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }
        if (!UserValidator.isValidPassword(newPassword)) {
            throw new IllegalArgumentException("비밀번호는 영문, 숫자, 특수문자를 포함하여 8자 이상이어야 합니다.");
        }
        user.updatePassword(passwordEncoder.encode(newPassword));
        userJpaRepository.save(user);
    }

    public void deleteAccount(String email) {
        User user = userJpaRepository.findByEmail(email);
        if (user == null || user.isDeleted()) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }
        user.delete();
        userJpaRepository.save(user);
    }

    public UserTO findByEmail(String email) {
        User user = userJpaRepository.findByEmail(email);
        if (user == null || user.isDeleted()) {
            return null;
        }
        return userMapper.toUserTO(user);
    }

    public List<SocialAccount> findSocialAccountsByUserId(Long userId) {
        return socialAccountRepository.findAll().stream()
                .filter(sa -> sa.getUser().getUserId().equals(userId))
                .toList();
    }

    // 로그인한 사용자의 예약 목록을 조회하는 메서드
    // tab 값에 따라 예정 / 완료 / 취소 예약을 구분해서 보여줌
    // startDate, endDate가 있으면 해당 기간의 예약만 보여줌
    @Transactional
    public List<UserReservationDTO> getReservationsByUserId(Long userId, String tab, String startDate, String endDate) {

        // 해당 유저의 전체 예약 목록을 한 번에 조회
        // (예약마다 회의실 정보를 따로 조회하면 쿼리가 너무 많이 나가서 JOIN FETCH로 한 번에 가져옴)
        List<Reservation> reservations = reservationRepository.findByUserUserIdWithRoom(userId);

        // tab 값이 없으면 예정된 예약(upcoming)을 기본으로 보여줌
        String currentTab = (tab != null && !tab.isBlank()) ? tab : "upcoming";

        // 화면에서 넘어온 날짜 문자열을 날짜 타입으로 변환, 없으면 null
        LocalDate from = (startDate != null && !startDate.isBlank()) ? LocalDate.parse(startDate) : null;
        LocalDate to   = (endDate   != null && !endDate.isBlank())   ? LocalDate.parse(endDate)   : null;

        List<UserReservationDTO> filtered = reservations.stream()
                .map(this::toUserReservationDTO)             // STEP 1: 화면에 필요한 데이터만 담은 DTO로 변환
                .filter(dto -> matchesTab(dto, currentTab))  // STEP 2: 선택한 탭에 맞는 예약만 필터링
                .filter(dto -> matchesDate(dto, from, to))   // STEP 3: 날짜 범위에 맞는 예약만 필터링
                .toList();

        // 취소 탭은 슬롯 데이터가 없어서 그룹핑 생략
        return "cancelled".equals(currentTab) ? filtered : groupByRoomAndDate(filtered);
    }

    // Reservation 엔티티를 화면용 DTO로 변환하는 메서드
    // 예약에 연결된 슬롯(날짜/시간) 정보는 reservationRoomRepository로 별도 조회
    // (Reservation에서 직접 꺼내면 DB 연결이 끊긴 상태라 슬롯 데이터를 못 가져오는 경우가 있음)
    private UserReservationDTO toUserReservationDTO(Reservation r) {
        List<ReservationRoom> rooms = reservationRoomRepository.findByReservation(r);

        // 시작/종료 시간 목록을 오름차순으로 정렬해서 저장
        List<LocalDateTime> startTimes = rooms.stream()
                .map(rr -> rr.getRoomSlot().getSlotStartAt())
                .sorted()
                .toList();

        List<LocalDateTime> endTimes = rooms.stream()
                .map(rr -> rr.getRoomSlot().getSlotEndAt())
                .sorted()
                .toList();

        // 연속된 슬롯 병합
        List<LocalDateTime[]> merged = mergeConsecutiveSlots(startTimes, endTimes);
        List<LocalDateTime> mergedStarts = merged.stream().map(s -> s[0]).toList();
        List<LocalDateTime> mergedEnds   = merged.stream().map(s -> s[1]).toList();

        LocalDateTime slotStartAt = mergedStarts.isEmpty() ? null : mergedStarts.get(0);
        LocalDateTime slotEndAt   = mergedEnds.isEmpty()   ? null : mergedEnds.get(mergedEnds.size() - 1);

        // 비품 조회 (취소된 비품 제외)
        List<ReservationEquipment> rawEquipments = reservationEquipmentRepository.findByReservation_ReservationId(r.getReservationId());
        List<UserReservationDTO.EquipmentItem> equipmentItems = rawEquipments.stream()
                .filter(e -> e.getStatus() != ReservationStatus.CANCELLED)
                .map(e -> UserReservationDTO.EquipmentItem.builder()
                        .equipmentName(e.getEquipment().getEquipmentName())
                        .quantity(e.getQuantity())
                        .totalAmount(e.getTotalAmount())
                        .build())
                .toList();

        BigDecimal equipmentTotalAmount = equipmentItems.stream()
                .map(UserReservationDTO.EquipmentItem::getTotalAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 회의실 금액은 예약된 슬롯(ReservationRoom)의 amount 합계로 계산
        // 총 금액(totalAmount)은 이후 비교용
        // 화면 표시용 grandTotalAmount 는 회의실 금액 + 비품 금액으로 계산
        BigDecimal roomAmount = rooms.stream()
                .map(ReservationRoom::getAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grandTotal = roomAmount.add(equipmentTotalAmount);
        BigDecimal hourlyPrice = r.getMeetingRoom().getHourlyPrice();
        int totalHours = rooms.size(); // 슬롯 1개 = 1시간

        return UserReservationDTO.builder()
                .reservationId(r.getReservationId())
                .roomName(r.getMeetingRoom().getRoomName())
                .roomImageUrl(r.getMeetingRoom().getImageUrl())
                .status(r.getStatus())
                .totalAmount(roomAmount)
                .memo(r.getMemo())
                .cancelledAt(r.getCancelledAt())
                .cancelReason(r.getCancelReason())
                .createdAt(r.getCreatedAt())
                .slotStartAt(slotStartAt)
                .slotEndAt(slotEndAt)
                .slotStartTimes(mergedStarts)
                .slotEndTimes(mergedEnds)
                .equipments(equipmentItems)
                .equipmentTotalAmount(equipmentTotalAmount)
                .roomHourlyPrice(hourlyPrice)
                .totalSlotHours(totalHours)
                .grandTotalAmount(grandTotal)
                .build();
    }

    // 같은 날짜 + 같은 회의실 예약을 하나의 카드로 묶는 메서드
    private List<UserReservationDTO> groupByRoomAndDate(List<UserReservationDTO> dtos) {
        Map<String, List<UserReservationDTO>> grouped = dtos.stream()
                .collect(Collectors.groupingBy(dto -> {
                    String date = dto.getSlotStartAt() != null
                            ? dto.getSlotStartAt().toLocalDate().toString()
                            : "";
                    return dto.getRoomName() + "|" + date;
                }));

        return grouped.values().stream()
                .map(this::mergeGroup)
                .sorted(Comparator.comparing(
                        dto -> dto.getSlotStartAt() != null ? dto.getSlotStartAt() : LocalDateTime.MIN
                ))
                .toList();
    }

    // 같은 그룹(날짜 + 회의실)으로 묶인 예약들을 하나의 DTO로 합치는 메서드
    // 시간 목록 전부 모아서 병합, 금액은 합산
    private UserReservationDTO mergeGroup(List<UserReservationDTO> group) {
        // 그룹의 대표값으로 첫 번째 예약 DTO를 사용한다.
        UserReservationDTO first = group.get(0);

        // 그룹에 포함된 모든 시작 시간을 모아 오름차순으로 정렬한다.
        List<LocalDateTime> allStartTimes = group.stream()
                .flatMap(d -> d.getSlotStartTimes().stream())
                .sorted()
                .toList();

        // 그룹에 포함된 모든 종료 시간을 모아 오름차순으로 정렬한다.
        List<LocalDateTime> allEndTimes = group.stream()
                .flatMap(d -> d.getSlotEndTimes().stream())
                .sorted()
                .toList();

        // 그룹에 포함된 각 예약의 회의실 금액을 모두 합산한다.
        // totalAmount 는 비품 금액을 제외한 회의실 금액 합계이다.
        BigDecimal totalAmount = group.stream()
                .map(UserReservationDTO::getTotalAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 연속된 시간 슬롯은 하나의 구간으로 병합
        List<LocalDateTime[]> merged = mergeConsecutiveSlots(allStartTimes, allEndTimes);
        List<LocalDateTime> mergedStarts = merged.stream().map(s -> s[0]).toList();
        List<LocalDateTime> mergedEnds   = merged.stream().map(s -> s[1]).toList();

        // 병합된 시간 구간의 첫 시작 시간과 마지막 종료 시간을 대표 시간으로 사용
        LocalDateTime slotStartAt = mergedStarts.isEmpty() ? null : mergedStarts.get(0);
        LocalDateTime slotEndAt   = mergedEnds.isEmpty()   ? null : mergedEnds.get(mergedEnds.size() - 1);

        // 그룹 내 모든 비품 목록을 하나로 합친다.
        List<UserReservationDTO.EquipmentItem> allEquipments = group.stream()
                .flatMap(d -> d.getEquipments() != null ? d.getEquipments().stream() : Stream.empty())
                .toList();

        // 그룹에 포함된 비품 금액을 모두 합산
        BigDecimal equipmentTotalAmount = group.stream()
                .map(UserReservationDTO::getEquipmentTotalAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 그룹에 포함된 전체 슬롯 수를 합산
        int totalSlotHours = group.stream().mapToInt(UserReservationDTO::getTotalSlotHours).sum();

        // 화면 표시용 총 금액은 회의실 금액과 비품 금액을 더해서 계산
        BigDecimal grandTotal = totalAmount.add(equipmentTotalAmount);

        // 병합된 예약 정보를 하나의 DTO로 만들어 반환
        return UserReservationDTO.builder()
                .reservationId(first.getReservationId())
                .roomName(first.getRoomName())
                .roomImageUrl(first.getRoomImageUrl())
                .status(first.getStatus())
                .totalAmount(totalAmount)
                .memo(first.getMemo())
                .cancelledAt(first.getCancelledAt())
                .cancelReason(first.getCancelReason())
                .createdAt(first.getCreatedAt())
                .slotStartAt(slotStartAt)
                .slotEndAt(slotEndAt)
                .slotStartTimes(mergedStarts)
                .slotEndTimes(mergedEnds)
                .equipments(allEquipments)
                .equipmentTotalAmount(equipmentTotalAmount)
                .roomHourlyPrice(first.getRoomHourlyPrice())
                .totalSlotHours(totalSlotHours)
                .grandTotalAmount(grandTotal)
                .build();
    }

    // 선택한 탭에 맞는 예약인지 확인하는 메서드
    // EXPIRED, PENDING 상태는 어느 탭에서도 표시하지 않음
    // CONFIRMED 상태만 종료 시간 기준으로 예정 / 완료로 구분
    private boolean matchesTab(UserReservationDTO dto, String tab) {
        LocalDateTime now = LocalDateTime.now();
        return switch (tab) {
            // 완료: CONFIRMED이면서 종료 시간이 지난 예약
            case "completed" -> dto.getStatus() == ReservationStatus.CONFIRMED
                    && dto.getSlotEndAt() != null
                    && dto.getSlotEndAt().isBefore(now);

            // 취소: CANCELLED 상태인 예약만
            case "cancelled" -> dto.getStatus() == ReservationStatus.CANCELLED;

            // 예정(기본값): CONFIRMED이면서 아직 종료 시간이 안 지난 예약
            default -> dto.getStatus() == ReservationStatus.CONFIRMED
                    && (dto.getSlotEndAt() == null || !dto.getSlotEndAt().isBefore(now));
        };
    }

    // 연속된 슬롯 시간을 하나의 범위로 병합하는 메서드
    private List<LocalDateTime[]> mergeConsecutiveSlots(List<LocalDateTime> startTimes, List<LocalDateTime> endTimes) {
        if (startTimes.isEmpty()) return List.of();

        // 시작 시간 기준으로 쌍을 만들고 정렬
        List<LocalDateTime[]> slots = new ArrayList<>();
        for (int i = 0; i < startTimes.size(); i++) {
            slots.add(new LocalDateTime[]{startTimes.get(i), endTimes.get(i)});
        }
        slots.sort(Comparator.comparing(s -> s[0]));

        List<LocalDateTime[]> merged = new ArrayList<>();
        LocalDateTime[] current = slots.get(0);

        for (int i = 1; i < slots.size(); i++) {
            LocalDateTime[] next = slots.get(i);
            // 다음 슬롯 시작이 현재 슬롯 종료와 같거나 이전이면 연속으로 보고 병합
            if (!next[0].isAfter(current[1])) {
                current = new LocalDateTime[]{current[0], next[1].isAfter(current[1]) ? next[1] : current[1]};
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);

        return merged;
    }

    // 날짜 범위에 맞는 예약인지 확인하는 메서드
    // 날짜 필터가 없으면 전체 통과
    // 날짜 필터가 있는데 슬롯 시간 정보가 없는 예약은 제외 (날짜를 알 수 없으니까...)
    private boolean matchesDate(UserReservationDTO dto, LocalDate from, LocalDate to) {
        if (from == null && to == null) return true;
        if (dto.getSlotStartAt() == null) return false;
        LocalDate slotDate = dto.getSlotStartAt().toLocalDate();
        if (from != null && slotDate.isBefore(from)) return false;
        if (to   != null && slotDate.isAfter(to))    return false;
        return true;
    }
}
