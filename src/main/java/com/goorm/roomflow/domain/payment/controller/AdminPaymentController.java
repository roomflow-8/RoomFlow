package com.goorm.roomflow.domain.payment.controller;

import com.goorm.roomflow.domain.payment.dto.response.AdminPaymentSalesRes;
import com.goorm.roomflow.domain.payment.service.AdminPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/sales")
public class AdminPaymentController {

    private final AdminPaymentService adminPaymentService;

    @GetMapping
    public String getSalesPage(Model model) {
        AdminPaymentSalesRes salesPage = adminPaymentService.getSalesPage();

        model.addAttribute("summary", salesPage.summary());
        model.addAttribute("payments", salesPage.payments());
        return "admin/payment/sales-list";
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> downloadSalesExcel() {
        byte[] excelData = adminPaymentService.exportSalesExcel();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("roomflow-sales.xlsx")
                        .build()
        );

        headers.setContentType(
                MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                )
        );

        return ResponseEntity
                .ok()
                .headers(headers)
                .body(excelData);
    }
}
