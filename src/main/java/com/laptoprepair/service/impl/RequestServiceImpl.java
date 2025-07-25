package com.laptoprepair.service.impl;

import com.laptoprepair.entity.Request;
import com.laptoprepair.enums.RequestStatus;
import com.laptoprepair.exception.NotFoundException;
import com.laptoprepair.exception.ValidationException;

import com.laptoprepair.repository.RequestRepository;
import com.laptoprepair.service.EmailService;
import com.laptoprepair.service.RequestService;
import com.laptoprepair.service.handler.RequestCreator;
import com.laptoprepair.service.handler.RequestUpdater;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestServiceImpl implements RequestService {

    private final RequestRepository reqRepo;
    private final EmailService emailService;
    private final RequestCreator requestCreator;
    private final RequestUpdater requestUpdater;

    @Transactional(readOnly = true)
    @Override
    public Request findById(UUID id) {
        return reqRepo.findByIdWithDetails(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy yêu cầu với ID: " + id));
    }

    @Override
    public Page<Request> list(String search, RequestStatus status, Pageable pageable) {
        return reqRepo.findWithFilters(search, status, pageable);
    }

    @Override
    public Request publicCreate(Request incomingRequest) throws ValidationException {
        return requestCreator.createPublic(incomingRequest);
    }

    @Override
    @Transactional
    public Request create(Request incomingRequest, MultipartFile[] newImages, String note) throws ValidationException {
        return requestCreator.createNew(incomingRequest, newImages, note);
    }

    @Override
    @Transactional
    public Request update(UUID id, Request incomingRequest, MultipartFile[] newImages, String[] imagesToDelete,
            String note) throws ValidationException {
        return requestUpdater.updateExisting(id, incomingRequest, newImages, imagesToDelete, note);
    }

    @Override
    public void recover(String email) {
        List<Request> requests = reqRepo.findByEmail(email);
        if (requests.isEmpty()) {
            return;
        }
        emailService.sendRecoverEmail(email, requests);
    }

}