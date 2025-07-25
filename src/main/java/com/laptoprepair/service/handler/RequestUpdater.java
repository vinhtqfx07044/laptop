package com.laptoprepair.service.handler;

import com.laptoprepair.entity.Request;
import com.laptoprepair.exception.ValidationException;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface RequestUpdater {
    Request updateExisting(UUID id, Request incomingRequest, MultipartFile[] images, String[] toDelete, String note)
            throws ValidationException;
}