package com.laptoprepair.service.impl;

import com.laptoprepair.entity.Request;
import com.laptoprepair.entity.RequestImage;
import com.laptoprepair.enums.RequestStatus;
import com.laptoprepair.exception.NotFoundException;
import com.laptoprepair.exception.ValidationException;

import com.laptoprepair.repository.RequestRepository;
import com.laptoprepair.service.EmailService;
import com.laptoprepair.service.HistoryService;
import com.laptoprepair.service.ImageService;
import com.laptoprepair.service.MappingService;
import com.laptoprepair.service.RequestService;
import com.laptoprepair.utils.TimeProvider;
import com.laptoprepair.validation.RequestValidator;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestServiceImpl implements RequestService {

    private final RequestRepository reqRepo;
    private final MappingService mappingService;
    private final HistoryService historyService;
    private final ImageService imageService;
    private final EmailService emailService;
    private final RequestValidator requestValidator;
    private final TimeProvider timeProvider;


    @Transactional(readOnly = true)
    @Override
    public Request findById(UUID id) {
        return reqRepo.findByIdWithDetails(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy yêu cầu với ID: " + id));
    }

    @Override
    public Page<Request> list(String search, RequestStatus status, Pageable pageable) {
        String statusString = status != null ? status.name() : null;
        return reqRepo.findWithFilters(search, statusString, pageable);
    }

    @Override
    public Request publicCreate(Request incomingRequest) throws ValidationException {
        requestValidator.validateAppointmentDateInFuture(incomingRequest.getAppointmentDate());

        // Set minimal defaults for public submission
        incomingRequest.setStatus(RequestStatus.SCHEDULED);
        incomingRequest.setItems(List.of());
        incomingRequest.setImages(List.of());

        // Add history entry
        historyService.addHistory(incomingRequest, "Tạo mới yêu cầu", "Khách");

        // Save request
        Request savedRequest = reqRepo.save(incomingRequest);

        // Send confirmation email
        emailService.sendConfirmationEmail(savedRequest);

        return savedRequest;
    }

    @Override
    @Transactional
    public Request create(Request incomingRequest, MultipartFile[] newImages, String note) throws ValidationException {
                StringBuilder noteBuilder = new StringBuilder("Tạo mới yêu cầu");

        if (note != null && !note.trim().isEmpty()) {
            noteBuilder.append("\nGhi chú: " + note.trim());
        }

        incomingRequest.setStatus(RequestStatus.SCHEDULED);
        mappingService.snapshotServiceItems(incomingRequest.getItems());
        historyService.addHistory(incomingRequest, noteBuilder.toString(), historyService.getCurrentUser());

        // Create request first to get ID
        Request savedRequest = reqRepo.save(incomingRequest);

        // Process images if provided
        List<RequestImage> images = imageService.uploadImages(savedRequest.getId(), new ArrayList<>(), newImages,
                savedRequest);
        savedRequest.setImages(new ArrayList<>(images));
        savedRequest = reqRepo.save(savedRequest);

        emailService.sendConfirmationEmail(savedRequest);

        return savedRequest;
    }

    @Override
    @Transactional
    public Request update(UUID id, Request incomingRequest, MultipartFile[] newImages, String[] imagesToDelete,
            String note) throws ValidationException {
                StringBuilder noteBuilder = new StringBuilder(
                (note != null && !note.trim().isEmpty()) ? "Ghi chú: " + note.trim() + "\n" : "");

        // Load existing request to get current images
        Request existingRequest = reqRepo.findByIdWithDetails(id)
                .orElseThrow(() -> new ValidationException("Không tìm thấy yêu cầu với ID: " + id));
        incomingRequest.setImages(new ArrayList<>(existingRequest.getImages()));

        // Process images (uploads and deletions)
        List<RequestImage> currentImages = imageService.updateRequestServiceImages(incomingRequest, newImages,
                imagesToDelete);
        incomingRequest.setImages(new ArrayList<>(currentImages));

        requestValidator.validateEditable(existingRequest);
        requestValidator.validateStatusTransition(existingRequest, incomingRequest);
        requestValidator.validateItemsForStatus(incomingRequest);
        requestValidator.validateNoItemModificationWhenLocked(existingRequest, incomingRequest);

        // Snapshot để làm history
        Request archivedRequest = new Request();
        mappingService.copyRequestFields(archivedRequest, existingRequest, true);

        // Set completion date if status is changing to COMPLETED
        if (existingRequest.getStatus() != RequestStatus.COMPLETED
                && incomingRequest.getStatus() == RequestStatus.COMPLETED) {
            incomingRequest.setCompletedAt(timeProvider.now());
        }

        // Snapshot service items, copy mọi trường và lưu
        mappingService.snapshotServiceItems(incomingRequest.getItems());
        mappingService.copyRequestFields(existingRequest, incomingRequest, false);

        noteBuilder.append(historyService.computeChanges(archivedRequest, existingRequest));

        // Add history if there are actual changes OR if there's a modal note
        if (!noteBuilder.toString().trim().isEmpty()) {
            historyService.addHistory(existingRequest, noteBuilder.toString(), historyService.getCurrentUser());
        }

        Request saved = reqRepo.save(existingRequest);
        emailService.sendUpdateEmail(saved, noteBuilder.toString());
        return saved;
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