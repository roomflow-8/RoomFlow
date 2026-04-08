package com.goorm.roomflow.domain.holiday.mapper;

import com.goorm.roomflow.domain.holiday.dto.request.AdminHolidayReq;
import com.goorm.roomflow.domain.holiday.dto.response.AdminHolidayRes;
import com.goorm.roomflow.domain.holiday.entity.Holiday;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface HolidayMapper {
    AdminHolidayRes toAdminHolidayRes(Holiday holiday);
}
