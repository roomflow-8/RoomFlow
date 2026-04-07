package com.goorm.roomflow.global.config;

import com.goorm.roomflow.domain.user.service.CustomOAuth2UserService;
import com.goorm.roomflow.domain.user.service.FormLoginFailureHandler;
import com.goorm.roomflow.domain.user.service.FormLoginSuccessHandler;
import com.goorm.roomflow.domain.user.service.OAuth2SuccessHandler;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final CustomOAuth2UserService customOAuth2UserService;
	private final OAuth2SuccessHandler oAuth2SuccessHandler;
	private final FormLoginSuccessHandler formLoginSuccessHandler;
	private final FormLoginFailureHandler formLoginFailureHandler;

	/**
	 * 1.제거
	 * "/rooms/**","/api/**" //permitAll 제거
	 * <p>
	 * 2. 추가
	 * .requestMatchers(HttpMethod.GET, "/rooms/**").permitAll()
	 * .requestMatchers("/reservations/**", "/users/mypage/**", "/users/reservationlist").authenticated()
	 * <p>
	 * 3. you will gonna need it
	 * .requestMatchers("/rooms/admin/**").hasRole("ADMIN") //admin 있을시 추가
	 *
	 */

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity httpSecurity,
										   ClientRegistrationRepository clientRegistrationRepository) throws Exception {
		httpSecurity
				.csrf(csrf -> csrf
						.ignoringRequestMatchers("/api/v1/**", "/users/email/send", "/users/email/verify", "/api/orders"))
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
				)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/", "/users/login", "/users/signup","/users/restore",
								"/users/email/send", "/users/email/verify",
								"/oauth2/**", "/login/oauth2/**",
								"/swagger-ui/**", "/v3/api-docs/**",
								"/css/**", "/js/**", "/images/**", "/reservations/**", "/api/v1/**"
						).permitAll()
						.requestMatchers(HttpMethod.GET, "/rooms/**").permitAll()
						.requestMatchers("/users/mypage/**", "/users/reservationlist", "/admin/**").authenticated()
						.anyRequest().authenticated()
				)
				.formLogin(form -> form
						.loginPage("/users/login")
						.loginProcessingUrl("/users/login")
						.usernameParameter("email")
						.passwordParameter("password")
						.successHandler(formLoginSuccessHandler)
						.failureHandler(formLoginFailureHandler)
				)
				.logout(logout -> logout
						.logoutUrl("/users/logout")
						.logoutSuccessUrl("/users/login")
						.invalidateHttpSession(true)
						.deleteCookies("JSESSIONID")
				)
				.oauth2Login(oauth2 -> oauth2
						.loginPage("/users/login")
						.authorizationEndpoint(endpoint -> endpoint
								.authorizationRequestResolver(
										customAuthorizationRequestResolver(clientRegistrationRepository)
								)
						)
						.userInfoEndpoint(userInfo -> userInfo
								.userService(customOAuth2UserService)
						)
						.successHandler(oAuth2SuccessHandler)
						.failureHandler(formLoginFailureHandler)
				);

		return httpSecurity.build();
	}


	// 구글 최초 회원가입 시 - refreshToken을 받기 위한 설정
	@Bean
	public OAuth2AuthorizationRequestResolver customAuthorizationRequestResolver(
			ClientRegistrationRepository clientRegistrationRepository
	) {

		// Spring Security 기본 OAuth2 인가 요청 생성기
		// /oauth2/authorization/{registrationId} 요청을 처리해 AuthorizationRequest 생성
		DefaultOAuth2AuthorizationRequestResolver defaultResolver =
				new DefaultOAuth2AuthorizationRequestResolver(
						clientRegistrationRepository,
						"/oauth2/authorization"
				);

		// Authorization 요청을 커스터마이징하기 위한 Resolver 재정의
		return new OAuth2AuthorizationRequestResolver() {
			@Override
			public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
				OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request);
				return customize(authorizationRequest, request);
			}

			// registrationId를 포함한 resolve 메서드 -> 내부적으로 동일하게 호출
			@Override
			public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
				OAuth2AuthorizationRequest authorizationRequest =
						defaultResolver.resolve(request, clientRegistrationId);
				return customize(authorizationRequest, request);
			}

			// AuthorizationRequest에 추가 파라미터 설정
			private OAuth2AuthorizationRequest customize(OAuth2AuthorizationRequest authorizationRequest,
														 HttpServletRequest request) {
				if (authorizationRequest == null) {
					return null;
				}

				String requestUri = request.getRequestURI();

				// /oauth2/authorization/google 일 때만 추가
				if (!requestUri.endsWith("/google")) {
					return authorizationRequest;
				}

				// refresh token을 발급받기 위한 추가 설정
				Map<String, Object> additionalParameters = new HashMap<>(authorizationRequest.getAdditionalParameters());
				additionalParameters.put("access_type", "offline");

				return OAuth2AuthorizationRequest.from(authorizationRequest)
						.additionalParameters(additionalParameters)
						.build();
			}
		};
	}

}
