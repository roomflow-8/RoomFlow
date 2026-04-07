package com.goorm.roomflow.domain.notice.mapper;

import com.goorm.roomflow.domain.notice.dto.response.NoticeAdminDetailRes;
import com.goorm.roomflow.domain.notice.dto.response.NoticeAdminRes;
import com.goorm.roomflow.domain.notice.dto.response.NoticeDetailRes;
import com.goorm.roomflow.domain.notice.dto.response.NoticeRes;
import com.goorm.roomflow.domain.notice.entity.Notice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NoticeMapper {

    NoticeAdminRes toNoticeAdminRes(Notice notice);

    @Mapping(target = "createdByName", expression = "java(getUserName(notice.getCreatedBy()))")
    @Mapping(target = "updatedByName", expression = "java(getUserName(notice.getUpdatedBy()))")
    NoticeAdminDetailRes toNoticeAdminDetailRes(Notice notice);

    NoticeRes toNoticeRes(Notice notice);
    NoticeDetailRes toNoticeDetailRes(Notice notice);

    default String getUserName(com.goorm.roomflow.domain.user.entity.User user) {
        return user != null ? user.getName() : null;
    }
}
