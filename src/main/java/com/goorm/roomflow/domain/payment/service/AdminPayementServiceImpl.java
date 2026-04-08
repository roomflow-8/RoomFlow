package com.goorm.roomflow.domain.payment.service;

import com.goorm.roomflow.domain.payment.dto.response.AdminPaymentSalesItemRes;
import com.goorm.roomflow.domain.payment.dto.response.AdminPaymentSalesRes;
import com.goorm.roomflow.domain.payment.dto.response.AdminPaymentSalesSummaryRes;
import com.goorm.roomflow.domain.payment.entity.Payment;
import com.goorm.roomflow.domain.payment.entity.PaymentStatus;
import com.goorm.roomflow.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPayementServiceImpl implements AdminPaymentService {

    private final PaymentRepository paymentRepository;


    @Override
    public AdminPaymentSalesRes getSalesPage() {

        log.info("관리자 매출 정보 조회 시작");

        List<Payment> donePayments = paymentRepository.findAllByStatusOrderByApprovedAtDesc(PaymentStatus.DONE);

        AdminPaymentSalesSummaryRes summaryRes = createSummary(donePayments);

        List<AdminPaymentSalesItemRes> payments = donePayments.stream()
                        .map(payment ->
                                new AdminPaymentSalesItemRes(
                                        payment.getPaymentId(),
                                        payment.getOrderId(),
                                        payment.getOrderName(),
                                        payment.getUser().getName(),
                                        payment.getStatus(),
                                        payment.getRoomAmount(),
                                        payment.getEquipmentAmount(),
                                        payment.getTotalAmount(),
                                        payment.getMethod(),
                                        payment.getReceiptUrl(),
                                        payment.getApprovedAt()
                                ))
                                .toList();

        log.info("관리자 매출 정보 조회 완료");

        return new AdminPaymentSalesRes(summaryRes, payments);
    }

    private AdminPaymentSalesSummaryRes createSummary(List<Payment> payments) {

        BigDecimal totalAmount = payments.stream()
                .map(Payment::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal roomSales = payments.stream()
                .map(Payment::getRoomAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal equipmentSales = payments.stream()
                .map(Payment::getEquipmentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long doneCount = payments.size();

        return new AdminPaymentSalesSummaryRes(totalAmount, roomSales, equipmentSales, doneCount);
    }

    /**
     * 매출 정보 엑셀 다운로드
     */
    @Override
    public byte[] exportSalesExcel() {

        List<Payment> payments = paymentRepository.findAllByStatusOrderByApprovedAtDesc(PaymentStatus.DONE);

        // Excel 2007 버전 이상의 워크북을 메모리에 생성
        try(XSSFWorkbook workbook = new XSSFWorkbook()) {
            // 엑셀 시트 생성
            Sheet sheet = workbook.createSheet("매출 내역");

            // 헤더 행 생성
            Row header = sheet.createRow(0);

            String[] headers = {
                    "승인일시",
                    "주문번호",
                    "주문명",
                    "예약자",
                    "회의실 금액",
                    "비품 금액",
                    "총 금액",
                    "결제수단"
            };

            CellStyle headerStyle = makeCellStyle(workbook);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            int rowIdx = 1;

            // 결제 데이터를 행 단위로 엑셀에 작성
            for (Payment payment : payments) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(
                        payment.getApprovedAt() != null
                                ? payment.getApprovedAt().toString()
                                : "-"
                );
                row.createCell(1).setCellValue(payment.getOrderId());
                row.createCell(2).setCellValue(payment.getOrderName());
                row.createCell(3).setCellValue(payment.getUser().getName());
                row.createCell(4).setCellValue(payment.getRoomAmount().longValue());
                row.createCell(5).setCellValue(payment.getEquipmentAmount().longValue());
                row.createCell(6).setCellValue(payment.getTotalAmount().longValue());
                row.createCell(7).setCellValue(
                        payment.getMethod() != null ? payment.getMethod() : "-"
                );
            }

            // 각 컬럼 너비 자동 조정
            for (int i = 0; i < 8; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1024);
            }

            // 워크북을 바이트 배열로 변환
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);

            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("매출 엑셀 파일 생성에 실패했습니다.", e);
        }
    }

    private CellStyle makeCellStyle(Workbook workbook) {
        // 헤더 스타일 생성
        CellStyle headerStyle = workbook.createCellStyle();

        // 배경색 설정
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // 폰트 설정
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 11);
        headerStyle.setFont(headerFont);

        // 가운데 정렬
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // 테두리 설정
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        return headerStyle;
    }
}
