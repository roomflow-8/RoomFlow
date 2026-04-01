package com.goorm.roomflow.domain.user.service;

import com.goorm.roomflow.domain.reservation.entity.Reservation;
import com.goorm.roomflow.domain.reservation.entity.ReservationEquipment;
import com.goorm.roomflow.domain.reservation.entity.ReservationRoom;
import com.goorm.roomflow.domain.reservation.entity.ReservationStatus;
import com.goorm.roomflow.domain.reservation.repository.ReservationEquipmentRepository;
import com.goorm.roomflow.domain.reservation.repository.ReservationRepository;
import com.goorm.roomflow.domain.user.dto.SignupRequestDTO;
import com.goorm.roomflow.domain.user.dto.UserEquipmentItem;
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
    private final ReservationEquipmentRepository reservationEquipmentRepository;


    public void signup(SignupRequestDTO request) {

        String name = request.getName().trim();
        if (!UserValidator.isValidName(name)) {
            throw new IllegalArgumentException("이름은 한글 2~10자 또는 영문 2~20자로 입력해주세요.");
        }

        if (!UserValidator.isValidEmail(request.getEmail())) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
        }

        if (userJpaRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        if (!UserValidator.isValidPassword(request.getPassword())) {
            throw new IllegalArgumentException("비밀번호는 영문, 숫자, 특수문자를 포함하여 8자 이상이어야 합니다.");
        }

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

    // tab: upcoming(예정) / completed(완료) / cancelled(취소)
    // startDate, endDate: 날짜 필터 (없으면 전체 조회)
    @Transactional
    public List<UserReservationDTO> getReservationsByUserId(Long userId, String tab, String startDate, String endDate) {

        // meetingRoom + reservationRooms + roomSlot 한 번에 fetch join
        List<Reservation> reservations = reservationRepository.findReservationByUserId(userId);

        String currentTab = (tab != null && !tab.isBlank()) ? tab : "upcoming";

        LocalDate from = (startDate != null && !startDate.isBlank()) ? LocalDate.parse(startDate) : null;
        LocalDate to   = (endDate   != null && !endDate.isBlank())   ? LocalDate.parse(endDate)   : null;

        // 모든 예약의 비품을 한 번에 조회 후 reservationId 기준으로 그룹핑
        List<Long> reservationIds = reservations.stream()
                .map(Reservation::getReservationId)
                .toList();

        List<ReservationEquipment> allEquipments = reservationEquipmentRepository
                .findAllByReservationIdsWithEquipment(reservationIds);

        // key: reservationId, value: 해당 예약의 비품 목록
        Map<Long, List<ReservationEquipment>> equipmentMap = allEquipments.stream()
                .collect(Collectors.groupingBy(re -> re.getReservation().getReservationId()));

        List<UserReservationDTO> filtered = reservations.stream()
                .map(r -> toUserReservationDTO(
                        r,
                        equipmentMap.getOrDefault(r.getReservationId(), List.of())
                ))
                .filter(dto -> matchesTab(dto, currentTab))
                .filter(dto -> matchesDate(dto, from, to))
                .sorted(getComparator(currentTab))
                .toList();

        // return "cancelled".equals(currentTab) ? filtered : groupByRoomAndDate(filtered);
        return filtered;
    }

    // rawEquipments: equipmentMap에서 해당 예약의 비품만 분류해서 전달받음
    private UserReservationDTO toUserReservationDTO(Reservation r, List<ReservationEquipment> rawEquipments) {

        // join fetch로 이미 로딩된 데이터 사용
        List<ReservationRoom> rooms = r.getReservationRooms();

        List<LocalDateTime> startTimes = rooms.stream()
                .map(rr -> rr.getRoomSlot().getSlotStartAt())
                .sorted()
                .toList();

        List<LocalDateTime> endTimes = rooms.stream()
                .map(rr -> rr.getRoomSlot().getSlotEndAt())
                .sorted()
                .toList();

        List<LocalDateTime[]> merged = mergeConsecutiveSlots(startTimes, endTimes);
        List<LocalDateTime> mergedStarts = merged.stream().map(s -> s[0]).toList();
        List<LocalDateTime> mergedEnds   = merged.stream().map(s -> s[1]).toList();

        LocalDateTime slotStartAt = mergedStarts.isEmpty() ? null : mergedStarts.get(0);
        LocalDateTime slotEndAt   = mergedEnds.isEmpty()   ? null : mergedEnds.get(mergedEnds.size() - 1);

        // 전달받은 rawEquipments에서 취소된 비품 제외 후 DTO 변환
        List<UserEquipmentItem> equipmentItems = rawEquipments.stream()
                .filter(e -> e.getStatus() != ReservationStatus.CANCELLED)
                .map(e -> new UserEquipmentItem(
                        e.getReservationEquipmentId(),
                        e.getEquipment().getEquipmentName(),
                        e.getQuantity(),
                        e.getTotalAmount()
                ))
                .toList();

        BigDecimal equipmentTotalAmount = equipmentItems.stream()
                .map(UserEquipmentItem::totalAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal roomAmount = rooms.stream()
                .map(ReservationRoom::getAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 취소된 예약: reservation 테이블의 total_amount 사용
        // 예정/완료 예약: 회의실 금액 + 비품 금액 직접 계산
        BigDecimal grandTotal = r.getStatus() == ReservationStatus.CANCELLED
                ? r.getTotalAmount()
                : roomAmount.add(equipmentTotalAmount);
        BigDecimal hourlyPrice = r.getMeetingRoom().getHourlyPrice();
        int totalHours = rooms.size();

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

    // 같은 날짜 + 같은 회의실 예약을 하나의 카드로 그룹핑
    private List<UserReservationDTO> groupByRoomAndDate(List<UserReservationDTO> dtos) {
        Map<String, List<UserReservationDTO>> grouped = dtos.stream()
                .collect(Collectors.groupingBy(dto -> {
                    String date = dto.slotStartAt() != null
                            ? dto.slotStartAt().toLocalDate().toString()
                            : "";
                    return dto.roomName() + "|" + date;
                }));

        return grouped.values().stream()
                .map(this::mergeGroup)
                .sorted(Comparator.comparing(
                        dto -> dto.slotStartAt() != null ? dto.slotStartAt() : LocalDateTime.MIN
                ))
                .toList();
    }

    // 같은 그룹으로 묶인 예약들을 하나의 DTO로 합산
    private UserReservationDTO mergeGroup(List<UserReservationDTO> group) {
        UserReservationDTO first = group.get(0);

        List<LocalDateTime> allStartTimes = group.stream()
                .flatMap(d -> d.slotStartTimes().stream())
                .sorted()
                .toList();

        List<LocalDateTime> allEndTimes = group.stream()
                .flatMap(d -> d.slotEndTimes().stream())
                .sorted()
                .toList();

        BigDecimal totalAmount = group.stream()
                .map(UserReservationDTO::totalAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<LocalDateTime[]> merged = mergeConsecutiveSlots(allStartTimes, allEndTimes);
        List<LocalDateTime> mergedStarts = merged.stream().map(s -> s[0]).toList();
        List<LocalDateTime> mergedEnds   = merged.stream().map(s -> s[1]).toList();

        LocalDateTime slotStartAt = mergedStarts.isEmpty() ? null : mergedStarts.get(0);
        LocalDateTime slotEndAt   = mergedEnds.isEmpty()   ? null : mergedEnds.get(mergedEnds.size() - 1);

        List<UserEquipmentItem> allEquipments = group.stream()
                .flatMap(d -> d.equipments() != null ? d.equipments().stream() : Stream.empty())
                .toList();

        BigDecimal equipmentTotalAmount = group.stream()
                .map(UserReservationDTO::equipmentTotalAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalSlotHours = group.stream().mapToInt(UserReservationDTO::totalSlotHours).sum();

        BigDecimal grandTotal = totalAmount.add(equipmentTotalAmount);

        return UserReservationDTO.builder()
                .reservationId(first.reservationId())
                .roomName(first.roomName())
                .roomImageUrl(first.roomImageUrl())
                .status(first.status())
                .totalAmount(totalAmount)
                .memo(first.memo())
                .cancelledAt(first.cancelledAt())
                .cancelReason(first.cancelReason())
                .createdAt(first.createdAt())
                .slotStartAt(slotStartAt)
                .slotEndAt(slotEndAt)
                .slotStartTimes(mergedStarts)
                .slotEndTimes(mergedEnds)
                .equipments(allEquipments)
                .equipmentTotalAmount(equipmentTotalAmount)
                .roomHourlyPrice(first.roomHourlyPrice())
                .totalSlotHours(totalSlotHours)
                .grandTotalAmount(grandTotal)
                .build();
    }

    // EXPIRED / PENDING 상태는 어느 탭에서도 표시 안 함
    // CONFIRMED만 종료 시간 기준으로 예정 / 완료 구분
    private boolean matchesTab(UserReservationDTO dto, String tab) {
        LocalDateTime now = LocalDateTime.now();
        return switch (tab) {
            case "completed" -> dto.status() == ReservationStatus.CONFIRMED
                    && dto.slotEndAt() != null
                    && dto.slotEndAt().isBefore(now);
            case "cancelled" -> dto.status() == ReservationStatus.CANCELLED;
            default -> dto.status() == ReservationStatus.CONFIRMED
                    && (dto.slotEndAt() == null || !dto.slotEndAt().isBefore(now));
        };
    }

    // 연속된 슬롯을 하나의 시간 범위로 병합
    private List<LocalDateTime[]> mergeConsecutiveSlots(List<LocalDateTime> startTimes, List<LocalDateTime> endTimes) {
        if (startTimes.isEmpty()) return List.of();

        List<LocalDateTime[]> slots = new ArrayList<>();
        for (int i = 0; i < startTimes.size(); i++) {
            slots.add(new LocalDateTime[]{startTimes.get(i), endTimes.get(i)});
        }
        slots.sort(Comparator.comparing(s -> s[0]));

        List<LocalDateTime[]> mergedList = new ArrayList<>();
        LocalDateTime[] current = slots.get(0);

        for (int i = 1; i < slots.size(); i++) {
            LocalDateTime[] next = slots.get(i);
            if (!next[0].isAfter(current[1])) {
                current = new LocalDateTime[]{current[0], next[1].isAfter(current[1]) ? next[1] : current[1]};
            } else {
                mergedList.add(current);
                current = next;
            }
        }
        mergedList.add(current);

        return mergedList;
    }

    // 날짜 필터 없으면 전체 통과, 슬롯 시간 없으면 제외
    private boolean matchesDate(UserReservationDTO dto, LocalDate from, LocalDate to) {
        if (from == null && to == null) return true;
        if (dto.slotStartAt() == null) return false;
        LocalDate slotDate = dto.slotStartAt().toLocalDate();
        if (from != null && slotDate.isBefore(from)) return false;
        if (to   != null && slotDate.isAfter(to))    return false;
        return true;
    }

    // 예정: slotStartAt 오름차순 (가까운 날짜부터)
    // 완료: slotStartAt 내림차순 (최근 날짜부터)
    private Comparator<UserReservationDTO> getComparator(String tab) {
        if ("completed".equals(tab)) {
            return Comparator.comparing(
                    dto -> dto.slotStartAt() != null ? dto.slotStartAt() : LocalDateTime.MIN,
                    Comparator.reverseOrder()
            );
        }
        return Comparator.comparing(
                dto -> dto.slotStartAt() != null ? dto.slotStartAt() : LocalDateTime.MAX
        );
    }
}
