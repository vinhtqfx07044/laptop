package com.laptoprepair.service.impl;

import com.laptoprepair.entity.Request;
import com.laptoprepair.entity.RequestImage;
import com.laptoprepair.entity.RequestItem;
import com.laptoprepair.enums.RequestStatus;
import com.laptoprepair.exception.NotFoundException;
import com.laptoprepair.exception.ValidationException;

import com.laptoprepair.repository.RequestRepository;
import com.laptoprepair.service.AuthService;
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
    private final AuthService authService;

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
        historyService.addRequestHistoryRecord(incomingRequest, "Tạo mới yêu cầu", "Khách");

        // Save request
        Request savedRequest = reqRepo.save(incomingRequest);

        // Send confirmation email
        emailService.sendConfirmationEmail(savedRequest);

        return savedRequest;
    }

    @Override
    @Transactional
    public Request create(Request incomingRequest, MultipartFile[] newImages, String note) throws ValidationException {
        incomingRequest.setStatus(RequestStatus.SCHEDULED);

        // Set request reference for items and snapshot service items BEFORE saving
        for (RequestItem item : incomingRequest.getItems()) {
            item.setRequest(incomingRequest);
        }
        mappingService.copyServiceItemsFields(incomingRequest.getItems());

        // Build note after all data preparation
        StringBuilder noteBuilder = new StringBuilder("Tạo mới yêu cầu");
        if (note != null && !note.trim().isEmpty()) {
            noteBuilder.append("\nGhi chú: " + note.trim());
        }

        historyService.addRequestHistoryRecord(incomingRequest, noteBuilder.toString(), authService.currentUser());

        // Create request with properly linked items
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
    public Request update(UUID id, Request incomingRequest, MultipartFile[] newImages, String[] toDelete,
            String note) throws ValidationException {
        // Load existing request
        Request existingRequest = reqRepo.findByIdWithDetails(id)
                .orElseThrow(() -> new ValidationException("Không tìm thấy yêu cầu với ID: " + id));
        
        // Trigger lazy loading for collections
        existingRequest.getItems().size();
        existingRequest.getImages().size();

        // Validate early to fail fast
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

        // Process images BEFORE copying fields (to preserve existing images)
        List<RequestImage> currentImages = imageService.updateRequestServiceImages(existingRequest, newImages,
                toDelete);

        // Snapshot service items, copy mọi trường và lưu
        mappingService.copyServiceItemsFields(incomingRequest.getItems());
        mappingService.copyRequestFields(existingRequest, incomingRequest, false);
        
        // Apply processed images after field copy
        existingRequest.getImages().clear();
        existingRequest.getImages().addAll(currentImages);

        // Build note with user input and computed changes
        StringBuilder noteBuilder = new StringBuilder(
                (note != null && !note.trim().isEmpty()) ? "Ghi chú: " + note.trim() + "\n" : "");
        noteBuilder.append(historyService.computeRequestChanges(archivedRequest, existingRequest));

        // Add history if there are actual changes OR if there's a modal note
        if (!noteBuilder.toString().trim().isEmpty()) {
            historyService.addRequestHistoryRecord(existingRequest, noteBuilder.toString(), authService.currentUser());
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