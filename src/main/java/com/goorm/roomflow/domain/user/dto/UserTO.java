package com.goorm.roomflow.domain.user.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UserTO {
    private Long userId;
    private String name;
    private String email;
    private String password;
    private String role;
}
