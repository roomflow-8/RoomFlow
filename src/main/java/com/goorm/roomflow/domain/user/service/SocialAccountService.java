package com.goorm.roomflow.domain.user.service;

import com.goorm.roomflow.domain.user.dto.KakaoTokenRes;
import com.goorm.roomflow.domain.user.dto.NaverTokenRes;
import com.goorm.roomflow.domain.user.entity.SocialAccount;
import com.goorm.roomflow.domain.user.entity.User;
import com.goorm.roomflow.domain.user.repository.SocialAccountRepository;
import com.goorm.roomflow.global.code.ErrorCode;
import com.goorm.roomflow.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialAccountService {

    private final SocialAccountRepository socialAccountRepository;
    private final RestClient restClient;

    @Value("${spring.security.oauth2.client.registration.naver.client-id:}")
    private String naverClientId;

    @Value("${spring.security.oauth2.client.registration.naver.client-secret:}")
    private String naverClientSecret;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id:}")
    private String kakaoClientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret:}")
    private String kakaoClientSecret;

    @Transactional
    public void updateRefreshToken(Long userId, String provider, String refreshToken) {

        SocialAccount socialAccount = socialAccountRepository
                .findByUser_UserIdAndProvider(userId, provider)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (refreshToken != null && !refreshToken.isBlank()) {
            socialAccount.updateRefreshToken(refreshToken);
        }
    }

    @Transactional
    public void unlinkAll(User user) {
        List<SocialAccount> socialAccounts = user.getSocialAccounts();

        if (socialAccounts == null || socialAccounts.isEmpty()) {
            log.info("소셜 계정 없음 - userId={}", user.getUserId());
            return;
        }

        log.info("모든 소셜 계정 연동 해제 시작 - userId={}, accountCount={}",
                user.getUserId(), socialAccounts.size());

        for (SocialAccount socialAccount : socialAccounts) {
            unlink(socialAccount);
        }

        log.info("모든 소셜 계정 연동 해제 완료 - userId={}", user.getUserId());
    }

    @Transactional
    public void unlink(SocialAccount socialAccount) {
        Long userId = socialAccount.getUser().getUserId();
        String provider = socialAccount.getProvider();
        String refreshToken = socialAccount.getRefreshToken();

        log.info("소셜 계정 연동 해제 시도 - userId={}, provider={}, providerUserId={}",
                userId, provider, socialAccount.getProviderUserId());

        if (refreshToken == null || refreshToken.isBlank()) {
            log.warn("소셜 계정 연동 해제 실패 - refreshToken 없음 - userId={}, provider={}", userId, provider);
            throw new BusinessException(ErrorCode.SOCIAL_REFRESH_TOKEN_NOT_FOUND);
        }

        switch (provider.toLowerCase()) {
            case "kakao" -> unlinkKakao(refreshToken);
            case "naver" -> unlinkNaver(refreshToken);
            case "google" -> unlinkGoogle(refreshToken);
            default -> {
                log.warn("소셜 계정 연동 해제 실패 - 지원하지 않는 provider - userId={}, provider={}", userId, provider);
                throw new BusinessException(ErrorCode.UNSUPPORTED_SOCIAL_PROVIDER);
            }
        }

        log.info("소셜 계정 연동 해제 완료 - userId={}, provider={}", userId, provider);
    }

    private void unlinkKakao(String refreshToken) {
        String accessToken = reissueKakaoAccessToken(refreshToken);

        restClient.post()
                .uri("https://kapi.kakao.com/v1/user/unlink")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .toBodilessEntity();
    }

    private String reissueKakaoAccessToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", kakaoClientId);
        form.add("refresh_token", refreshToken);
        form.add("client_secret", kakaoClientSecret);

        KakaoTokenRes response = restClient.post()
                .uri("https://kauth.kakao.com/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(KakaoTokenRes.class);

        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new BusinessException(ErrorCode.SOCIAL_UNLINK_FAILED);
        }

        return response.accessToken();
    }

    private void unlinkNaver(String refreshToken) {
        String accessToken = reissueNaverAccessToken(refreshToken);

        restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("nid.naver.com")
                        .path("/oauth2.0/token")
                        .queryParam("grant_type", "delete")
                        .queryParam("client_id", naverClientId)
                        .queryParam("client_secret", naverClientSecret)
                        .queryParam("access_token", accessToken) // refreshToken 아님
                        .queryParam("service_provider", "NAVER")
                        .build())
                .retrieve()
                .toBodilessEntity();
    }

    private String reissueNaverAccessToken(String refreshToken) {
        NaverTokenRes response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("nid.naver.com")
                        .path("/oauth2.0/token")
                        .queryParam("grant_type", "refresh_token")
                        .queryParam("client_id", naverClientId)
                        .queryParam("client_secret", naverClientSecret)
                        .queryParam("refresh_token", refreshToken)
                        .build())
                .retrieve()
                .body(NaverTokenRes.class);

        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new BusinessException(ErrorCode.SOCIAL_UNLINK_FAILED);
        }

        return response.accessToken();
    }

    private void unlinkGoogle(String refreshToken) {
        restClient.post()
                .uri("https://oauth2.googleapis.com/revoke")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("token=" + refreshToken)
                .retrieve()
                .toBodilessEntity();
    }
}
