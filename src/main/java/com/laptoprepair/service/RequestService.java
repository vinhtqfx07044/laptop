package com.laptoprepair.service;

import com.laptoprepair.entity.Request;
import com.laptoprepair.enums.RequestStatus;
import com.laptoprepair.exception.ValidationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

public interface RequestService {
    Request create(Request request, MultipartFile[] images, String note) throws ValidationException;

    Request update(UUID id, Request request, MultipartFile[] newImages, String[] toDelete, String note)
            throws ValidationException;

    Request findById(UUID id);

    Page<Request> list(String search, RequestStatus status, Pageable pageable);

    Request publicCreate(Request request) throws ValidationException;

    void recover(String email);
}