package com.goorm.roomflow.domain.user.mapper;

import com.goorm.roomflow.domain.user.dto.UserTO;
import com.goorm.roomflow.domain.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    public UserTO toUserTO(User user);
    public User toUser(UserTO userTO);
}