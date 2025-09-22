package com.laptoprepair.service.impl;

import com.laptoprepair.entity.Request;
import com.laptoprepair.service.EmailService;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * SMTP-based implementation of the {@link EmailService} interface for dev environment.
 * Uses Spring Boot Mail with SMTP (MailHog) for local development.
 */
@Service
@Profile("dev")
@RequiredArgsConstructor
public class EmailServiceImplSMTP implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImplSMTP.class);

    private final JavaMailSender mailSender;

    @Value("${app.public-request-base-url}")
    private String publicRequestBaseUrl;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.cc}")
    private String ccEmail;

    @Value("${app.shop.name}")
    private String shopName;

    private static final String THANK_YOU_MESSAGE = "Cảm ơn bạn đã sử dụng dịch vụ!";
    private static final String CONFIRMATION_SUBJECT = "Xác nhận yêu cầu sửa chữa tại %s";
    private static final String UPDATE_SUBJECT = "Cập nhật về yêu cầu sửa chữa của bạn tại %s";
    private static final String RECOVER_SUBJECT = "Khôi phục mã tra cứu tại %s";

    /**
     * Sends a confirmation email for a new repair request.
     */
    @Override
    @Async("emailTaskExecutor")
    public CompletableFuture<Void> sendConfirmationEmail(Request request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return CompletableFuture.completedFuture(null);
        }
        StringBuilder body = new StringBuilder("Yêu cầu của bạn đã được tiếp nhận.\n\n");
        body.append("Mã ID: " + request.getId() + "\n");
        body.append("Link tra cứu: " + publicRequestBaseUrl + request.getId() + "\n");
        body.append("\n").append(THANK_YOU_MESSAGE);
        String subject = String.format(CONFIRMATION_SUBJECT, shopName);
        return sendEmailAsync(request.getEmail(), subject, body.toString());
    }

    /**
     * Sends an update email for an existing repair request.
     */
    @Override
    @Async("emailTaskExecutor")
    public CompletableFuture<Void> sendUpdateEmail(Request request, String changes) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return CompletableFuture.completedFuture(null);
        }
        StringBuilder body = new StringBuilder("Yêu cầu sửa chữa của bạn đã được cập nhật:\n\n");
        body.append("Mã ID: " + request.getId() + "\n");
        body.append("Link tra cứu: ").append(publicRequestBaseUrl).append(request.getId()).append("\n");
        body.append(changes + "\n");
        body.append("\n").append(THANK_YOU_MESSAGE);
        String subject = String.format(UPDATE_SUBJECT, shopName);
        return sendEmailAsync(request.getEmail(), subject, body.toString());
    }

    /**
     * Sends a recovery email containing links to all requests associated with a given email address.
     */
    @Override
    @Async("emailTaskExecutor")
    public CompletableFuture<Void> sendRecoverEmail(String email, List<Request> requests) {
        StringBuilder body = new StringBuilder("Danh sách yêu cầu của bạn:\n\n");
        for (Request request : requests) {
            body.append("Mã ID: ").append(request.getId()).append("\n");
            body.append("Link tra cứu: ").append(publicRequestBaseUrl).append(request.getId()).append("\n");
            body.append("Ngày tạo: ").append(request.getCreatedAt().toLocalDate()).append("\n");
            body.append("Tình trạng: ").append(request.getStatus()).append("\n\n");
        }
        body.append(THANK_YOU_MESSAGE);
        String subject = String.format(RECOVER_SUBJECT, shopName);
        return sendEmailAsync(email, subject, body.toString());
    }

    private CompletableFuture<Void> sendEmailAsync(String toEmail, String subject, String body) {
        if ((toEmail == null || toEmail.trim().isEmpty())) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("Sending email via SMTP to: {} with subject: {}", toEmail, subject);

                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(toEmail);
                message.setSubject(subject);
                message.setText(body);

                // Add CC if configured
                if (ccEmail != null && !ccEmail.trim().isEmpty()) {
                    message.setCc(ccEmail);
                }

                mailSender.send(message);
                log.info("Email sent successfully via SMTP to: {}", toEmail);

            } catch (Exception e) {
                log.error("SMTP email sending failed to {}: {}", toEmail, e.getMessage(), e);
            }
        });
    }
}