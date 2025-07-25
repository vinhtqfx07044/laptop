package com.laptoprepair.service.impl;

import com.laptoprepair.entity.Request;
import com.laptoprepair.service.EmailService;
import com.laptoprepair.utils.AppConstants;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Value("${app.public-request-base-url}")
    private String publicRequestBaseUrl;

    @Value("${spring.mail.from}")
    private String fromEmail;

    @Value("${spring.mail.cc}")
    private String ccEmail;

    @Value("${app.shop.name}")
    private String shopName;

    private static final String THANK_YOU_MESSAGE = "Cảm ơn bạn đã sử dụng dịch vụ!";
    private static final String CONFIRMATION_SUBJECT = "Xác nhận yêu cầu sửa chữa tại %s";
    private static final String UPDATE_SUBJECT = "Cập nhật về yêu cầu sửa chữa của bạn tại %s";
    private static final String RECOVER_SUBJECT = "Khôi phục mã tra cứu tại %s";

    private final JavaMailSender mailSender;

    @Override
    public void sendConfirmationEmail(Request request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return;
        }
        StringBuilder body = new StringBuilder("Yêu cầu của bạn đã được tiếp nhận.\n\n");
        body.append("Mã ID: " + request.getId() + "\n");
        body.append(AppConstants.MESSAGE_LINK_PREFIX + publicRequestBaseUrl + request.getId() + "\n");
        body.append("\n").append(THANK_YOU_MESSAGE);
        String subject = String.format(CONFIRMATION_SUBJECT, shopName);
        sendEmail(request.getEmail(), subject, body.toString());
    }

    @Override
    public void sendUpdateEmail(Request request, String changes) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return;
        }
        StringBuilder body = new StringBuilder("Yêu cầu sửa chữa của bạn đã được cập nhật:\n\n");
        body.append("Mã ID: " + request.getId() + "\n");
        body.append(AppConstants.MESSAGE_LINK_PREFIX + publicRequestBaseUrl + request.getId() + "\n");
        body.append(changes + "\n");
        body.append("\n").append(THANK_YOU_MESSAGE);
        String subject = String.format(UPDATE_SUBJECT, shopName);
        sendEmail(request.getEmail(), subject, body.toString());
    }

    @Override
    public void sendRecoverEmail(String email, List<Request> requests) {
        StringBuilder body = new StringBuilder("Danh sách yêu cầu của bạn:\n\n");
        for (Request request : requests) {
            body.append(AppConstants.MESSAGE_LINK_PREFIX).append(publicRequestBaseUrl).append(request.getId()).append("\n");
        }
        body.append("\n").append(THANK_YOU_MESSAGE);
        String subject = String.format(RECOVER_SUBJECT, shopName);
        sendEmail(email, subject, body.toString());
    }

    private void sendEmail(String toEmail, String subject, String body) {
        if ((toEmail == null || toEmail.trim().isEmpty())) {
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            if (ccEmail != null && !ccEmail.trim().isEmpty()) {
                message.setCc(ccEmail);
            }
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Email sending failed: {}", e.getMessage(), e);
        }
    }
}