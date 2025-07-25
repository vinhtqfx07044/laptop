package com.laptoprepair.service.handler;

import com.laptoprepair.entity.Request;
import com.laptoprepair.entity.RequestImage;
import com.laptoprepair.enums.RequestStatus;
import com.laptoprepair.exception.ValidationException;
import com.laptoprepair.repository.RequestRepository;
import com.laptoprepair.service.EmailService;
import com.laptoprepair.service.HistoryService;
import com.laptoprepair.service.ImageService;
import com.laptoprepair.service.MappingService;
import com.laptoprepair.validation.RequestValidator;
import com.laptoprepair.common.TimeProvider;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RequestUpdaterImpl implements RequestUpdater {

    private final RequestRepository reqRepo;
    private final RequestValidator requestValidator;
    private final MappingService mappingService;
    private final HistoryService historyService;
    private final ImageService imageService;
    private final EmailService emailService;
    private final TimeProvider timeProvider;

    @Override
    @Transactional
    public Request updateExisting(UUID id, Request incomingRequest, MultipartFile[] newImages, String[] imagesToDelete,
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
}