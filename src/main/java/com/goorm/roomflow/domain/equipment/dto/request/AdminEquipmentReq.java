package com.goorm.roomflow.domain.equipment.dto.request;

import com.goorm.roomflow.domain.equipment.entity.EquipmentStatus;
import jakarta.validation.constraints.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

public record AdminEquipmentReq(

        @NotBlank(message = "비품명을 입력해주세요.")
        @Size(max = 100, message = "비품명은 100자 이하여야 합니다.")
        String equipmentName,

        @Min(value = 0, message = "총 수량은 0 이상이어야 합니다.")
        int totalStock,

        @Size(max = 1000, message = "설명은 1000자 이하여야 합니다.")
        String description,

        @Min(value = 0, message = "점검 기준 수량은 0 이상이어야 합니다.")
        int maintenanceLimit,

        @NotNull(message = "가격을 입력해주세요.")
        @DecimalMin(value = "0", inclusive = true, message = "가격은 0 이상이어야 합니다.")
        BigDecimal price,
        EquipmentStatus status,

        MultipartFile imageFile
) {
}
