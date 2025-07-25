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

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RequestCreatorImpl implements RequestCreator {

    private final RequestRepository reqRepo;
    private final MappingService mappingService;
    private final HistoryService historyService;
    private final ImageService imageService;
    private final EmailService emailService;
    private final RequestValidator requestValidator;

    @Override
    @Transactional
    public Request createNew(Request incomingRequest, MultipartFile[] newImages, String note)
            throws ValidationException {
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
    public Request createPublic(Request incomingRequest) throws ValidationException {
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
}