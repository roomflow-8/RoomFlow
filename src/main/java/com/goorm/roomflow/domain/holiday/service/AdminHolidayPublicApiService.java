package com.goorm.roomflow.domain.holiday.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.goorm.roomflow.domain.holiday.dto.request.AdminHolidayReq;
import com.goorm.roomflow.domain.holiday.dto.response.HolidayApiResponse;
import com.goorm.roomflow.global.code.ErrorCode;
import com.goorm.roomflow.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminHolidayPublicApiService {

    private final RestClient restClient;
    private final XmlMapper xmlMapper;

    @Value("${public-api.holiday.service-key}")
    private String serviceKey;

    /**
     * 공공데이터포털 공휴일 API를 호출하여 특정 연도(및 월)의 공휴일 목록을 조회
     */
    public List<AdminHolidayReq> getPublicHolidays(int year, Integer month) {

        log.info("공휴일 API 조회 시작 year={}, month={}", year, month);

        try {

            // 공공데이터포털 공휴일 API 호출
            //  - serviceKey : 인증키, solYear : 조회연도, solMonth(선택) : 조회월 - 없으면 연도 전체 조회
            URI uri = UriComponentsBuilder.newInstance()
                    .scheme("https")
                    .host("apis.data.go.kr")
                    .path("/B090041/openapi/service/SpcdeInfoService/getRestDeInfo")
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("solYear", year)
                    .queryParamIfPresent("solMonth", Optional.ofNullable(month).map(m -> String.format("%02d", m)))
                    .build(true)
                    .toUri();

            // byte[]로 받아 글자 깨짐 방지
            byte[] responseBytes = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(byte[].class);

            String xml = new String(responseBytes, StandardCharsets.UTF_8);

            // xml 응답 데이터를 HolidayApiResponse로 변환
            HolidayApiResponse response =
                    xmlMapper.readValue(xml, HolidayApiResponse.class);

            // 응답 구조 검증
            if (response == null || response.getHeader() == null) {
                log.error("공휴일 API 응답 구조 오류 year={}, month={}", year, month);
                throw new BusinessException(ErrorCode.HOLIDAY_API_FAILED);
            }

            // result 검증 - 00 : 정상응답, 그 외 = 실패
            String resultCode = response.getHeader().getResultCode();

            if (!"00".equals(resultCode)) {

                log.error("공휴일 API resultCode 오류 code={}, year={}, month={}",
                        resultCode, year, month);

                throw new BusinessException(ErrorCode.HOLIDAY_API_FAILED);
            }

            // 응답데이터에서 공휴일 목록 추출
            List<HolidayApiResponse.Item> items =
                    response.getBody() != null
                            && response.getBody().getItems() != null
                            ? response.getBody().getItems().getItem()
                            : List.of();


            // 공휴일 데이터 추출
            List<AdminHolidayReq> result = new ArrayList<>();

            for (HolidayApiResponse.Item item : items) {

                if (!"Y".equals(item.getIsHoliday())) {
                    continue;
                }

                // Long 형태 -> yyyyMMdd 형태로 변환
                LocalDate holidayDate = LocalDate.parse(
                        String.valueOf(item.getLocdate()),
                        DateTimeFormatter.BASIC_ISO_DATE
                );

                result.add(new AdminHolidayReq(
                        item.getDateName(),
                        "공공데이터포털 공휴일 API 연동",
                        holidayDate,
                        true
                ));
            }

            log.info("공휴일 API 조회 완료 year={}, month={}, count={}",
                    year, month, result.size());

            return result;

        } catch (BusinessException e) {
            throw e;

        } catch (Exception e) {

            log.error("공휴일 API 조회 실패 year={}, month={}",
                    year, month, e);

            throw new BusinessException(ErrorCode.HOLIDAY_API_FAILED);
        }
    }
}