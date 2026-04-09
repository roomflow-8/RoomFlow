package com.goorm.roomflow.global.config;

import com.goorm.roomflow.global.interceptor.LoggingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final LoggingInterceptor loggingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggingInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/favicon.ico",
                        "/error"
                );
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // JSON converter 우선 설정
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        jsonConverter.setSupportedMediaTypes(List.of(
                MediaType.APPLICATION_JSON,
                new MediaType("application", "*+json")
        ));
        converters.add(0, jsonConverter);

        // XML converter
        MappingJackson2XmlHttpMessageConverter xmlConverter =
                new MappingJackson2XmlHttpMessageConverter();
        xmlConverter.setSupportedMediaTypes(List.of(
                MediaType.APPLICATION_XML,
                MediaType.TEXT_XML,
                new MediaType("application", "*+xml")
        ));
        converters.add(xmlConverter);
    }

}
