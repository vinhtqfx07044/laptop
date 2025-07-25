package com.laptoprepair.service;

import com.laptoprepair.entity.Request;
import java.util.List;

public interface EmailService {
    void sendConfirmationEmail(Request request);

    void sendUpdateEmail(Request request, String changes);

    void sendRecoverEmail(String email, List<Request> requests);
}