package com.laptoprepair.service;

import com.laptoprepair.entity.Request;
import java.util.List;

/**
 * Service interface for sending various types of emails related to repair
 * requests.
 */
public interface EmailService {
    void sendConfirmationEmail(Request request);

    void sendUpdateEmail(Request request, String changes);

    void sendRecoverEmail(String email, List<Request> requests);
}