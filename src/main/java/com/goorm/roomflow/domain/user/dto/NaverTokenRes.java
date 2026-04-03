package com.goorm.roomflow.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NaverTokenRes(
        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("refresh_token")
        String refreshToken,

        @JsonProperty("token_type")
        String tokenType,

        @JsonProperty("expires_in")
        String expiresIn,

        String result,
        String error,
        @JsonProperty("error_description")
        String errorDescription
) {
}