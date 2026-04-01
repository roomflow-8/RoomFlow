package com.goorm.roomflow.global.config;

import com.goorm.roomflow.domain.user.service.CustomOAuth2UserService;
import com.goorm.roomflow.domain.user.service.FormLoginSuccessHandler;
import com.goorm.roomflow.domain.user.service.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final CustomOAuth2UserService customOAuth2UserService;
	private final OAuth2SuccessHandler oAuth2SuccessHandler;
	private final FormLoginSuccessHandler formLoginSuccessHandler;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
		httpSecurity
			.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session
				.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
			)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(
					"/", "/users/login", "/users/signup",
					"/users/email/send", "/users/email/verify",
					"/oauth2/**", "/login/oauth2/**",
					"/swagger-ui/**", "/v3/api-docs/**",
					"/css/**", "/js/**", "/images/**",
					"/rooms/**","/api/**"
				).permitAll()
				.anyRequest().authenticated()
			)
			.formLogin(form -> form
				.loginPage("/users/login")
				.loginProcessingUrl("/users/login")
				.usernameParameter("email")
				.passwordParameter("password")
					.successHandler(formLoginSuccessHandler)
				.failureUrl("/users/login?error=true")
			)
			.logout(logout -> logout
				.logoutUrl("/users/logout")
				.logoutSuccessUrl("/users/login")
				.invalidateHttpSession(true)
				.deleteCookies("JSESSIONID")
			)
			.oauth2Login(oauth2 -> oauth2
				.loginPage("/users/login")
				.userInfoEndpoint(userInfo -> userInfo
					.userService(customOAuth2UserService)
				)
				.successHandler(oAuth2SuccessHandler)
			);

		return httpSecurity.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
