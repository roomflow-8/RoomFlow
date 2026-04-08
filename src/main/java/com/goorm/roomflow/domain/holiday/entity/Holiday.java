package com.goorm.roomflow.domain.holiday.entity;

import com.goorm.roomflow.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "holiday",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_holiday_date", columnNames = "holiday_date")
        },
        indexes = {
                @Index(name = "idx_holiday_active_date", columnList = "is_active, holiday_date")
        }
)
public class Holiday extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long holidayId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private LocalDate holidayDate;

    @Column(nullable = false)
    private boolean isActive = true;

    @Builder
    public Holiday(String title, String description, LocalDate holidayDate, Boolean isActive) {
        validate(title, holidayDate);
        this.title = title.trim();
        this.description = description;
        this.holidayDate = holidayDate;
        this.isActive = isActive == null || isActive;
    }

    public void update(String title, String description, LocalDate holidayDate, Boolean isActive) {
        validate(title, holidayDate);
        this.title = title.trim();
        this.description = description;
        this.holidayDate = holidayDate;
        this.isActive = isActive == null || isActive;
    }

    public void changeActive(boolean isActive) {
        this.isActive = isActive;
    }

    private void validate(String title, LocalDate holidayDate) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("휴무일명을 입력해주세요.");
        }
        if (holidayDate == null) {
            throw new IllegalArgumentException("휴무일 날짜를 입력해주세요.");
        }
    }
}