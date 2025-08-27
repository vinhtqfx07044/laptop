package com.laptoprepair.service.impl;

import com.laptoprepair.entity.Request;
import com.laptoprepair.service.EmailService;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of the {@link EmailService} interface.
 * Handles sending various email notifications related to repair requests.
 */
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

    /**
     * Sends a confirmation email for a new repair request.
     * The email includes the request ID and a link to track the request.
     * 
     * @param request The Request object for which to send the confirmation.
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
     * The email includes the request ID, a tracking link, and a summary of changes.
     * 
     * @param request The Request object that was updated.
     * @param changes A string describing the changes made to the request.
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
     * Sends a recovery email containing links to all requests associated with a
     * given email address.
     * 
     * @param email    The email address to send the recovery information to.
     * @param requests A list of Request objects associated with the email.
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
                log.debug("Sending email to: {} with subject: {}", toEmail, subject);
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(toEmail);
                if (ccEmail != null && !ccEmail.trim().isEmpty()) {
                    message.setCc(ccEmail);
                }
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
                log.info("Email sent successfully to: {}", toEmail);
            } catch (MailException e) {
                log.error("Email sending failed to {}: {}", toEmail, e.getMessage(), e);
                // throw new RuntimeException("Failed to send email", e);
            }
        });
    }
}