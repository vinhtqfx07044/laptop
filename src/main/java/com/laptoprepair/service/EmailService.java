package com.laptoprepair.service;

import com.laptoprepair.entity.Request;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for sending various types of emails related to repair
 * requests.
 */
public interface EmailService {
    CompletableFuture<Void> sendConfirmationEmail(Request request);

    CompletableFuture<Void> sendUpdateEmail(Request request, String changes);

    CompletableFuture<Void> sendRecoverEmail(String email, List<Request> requests);
}