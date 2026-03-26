package com.goorm.roomflow.domain.user.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String id;

    // 이메일 메시지 생성 (인증번호를 파라미터로 받아서 사용)
    public MimeMessage createMessage(String to, String code) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = javaMailSender.createMimeMessage();

        message.addRecipients(MimeMessage.RecipientType.TO, to);
        message.setSubject("RoomFlow 이메일 인증 코드");

        String msg = "";
        msg += "<div style=\"text-align:center; padding:20px;\">";
        msg += "<h2>RoomFlow 이메일 인증</h2>";
        msg += "<p>아래 인증 코드를 입력해주세요.</p>";
        msg += "<p style=\"font-size:24px; font-weight:bold; background-color:#f4f4f4; padding:10px 20px; border-radius:5px; display:inline-block;\">";
        msg += code;
        msg += "</p>";
        msg += "</div>";

        message.setText(msg, "utf-8", "html");
        message.setFrom(new InternetAddress(id, "RoomFlow"));

        return message;
    }

    // 6자리 인증번호 생성 (호출할 때마다 새로운 번호 생성)
    public static String createKey() {
        StringBuilder key = new StringBuilder();
        Random rnd = new Random();
        rnd.setSeed(System.currentTimeMillis());
        for (int i = 0; i < 6; i++) {
            key.append(rnd.nextInt(10));
        }
        return key.toString();
    }

    // 이메일 발송 후 인증번호 반환
    public String sendSimpleMessage(String to) throws Exception {
        String code = createKey(); // 매번 새로운 인증번호 생성
        MimeMessage message = createMessage(to, code);
        try {
            javaMailSender.send(message);
        } catch (MailException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("이메일 발송 실패: " + to);
        }
        return code; // 생성된 인증번호를 반환 (컨트롤러에서 세션에 저장)
    }
}
