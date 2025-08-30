package com.laptoprepair.service.impl;

import com.laptoprepair.entity.Request;
import com.laptoprepair.entity.RequestImage;
import com.laptoprepair.entity.RequestItem;
import com.laptoprepair.entity.ServiceItem;
import com.laptoprepair.enums.RequestStatus;
import com.laptoprepair.exception.NotFoundException;
import com.laptoprepair.exception.ValidationException;

import com.laptoprepair.repository.RequestRepository;
import com.laptoprepair.repository.ServiceItemRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.laptoprepair.service.EmailService;
import com.laptoprepair.service.HistoryService;
import com.laptoprepair.service.ImageService;
import com.laptoprepair.service.RequestService;
import com.laptoprepair.config.VietnamTimeProvider;
import com.laptoprepair.validation.RequestValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link RequestService} interface.
 * Provides business logic for managing repair requests, including CRUD
 * operations,
 * image handling, history tracking, and email notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RequestServiceImpl implements RequestService {

    private final RequestRepository reqRepo;
    private final ServiceItemRepository serviceItemRepository;
    private final HistoryService historyService;
    private final ImageService imageService;
    private final EmailService emailService;
    private final RequestValidator requestValidator;
    private final VietnamTimeProvider vietnamTimeProvider;

    /**
     * Finds a request by its ID.
     * 
     * @param id The UUID of the request to find.
     * @return The found Request entity.
     * @throws NotFoundException if the request with the given ID is not found.
     */
    @Transactional(readOnly = true)
    @Override
    public Request findById(UUID id) {
        return reqRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy yêu cầu với ID: " + id));
    }

    /**
     * Retrieves a paginated list of requests based on search criteria and status.
     * 
     * @param search   Optional search string to filter requests.
     * @param status   Optional RequestStatus to filter requests.
     * @param pageable Pagination information.
     * @return A Page of Request entities.
     */
    @Override
    public Page<Request> list(String search, RequestStatus status, Pageable pageable) {
        String statusString = status != null ? status.name() : null;
        return reqRepo.findWithFilters(search, statusString, pageable);
    }

    /**
     * Creates a new request submitted by a public user (non-staff).
     * Sets default status to SCHEDULED and sends a confirmation email.
     * 
     * @param incomingRequest The Request object submitted by the public user.
     * @return The saved Request entity.
     * @throws ValidationException if the appointment date is not in the future.
     */
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

    /**
     * Creates a new repair request by a staff member.
     * Handles setting default status, processing service items, adding history, and
     * uploading images.
     * 
     * @param incomingRequest The Request object containing details for the new
     *                        request.
     * @param newImages       An array of MultipartFile objects representing images
     *                        to be uploaded.
     * @param note            An optional note to be added to the request history.
     * @return The newly created and saved Request entity.
     * @throws ValidationException if there are validation errors during creation.
     */
    @Override
    @Transactional
    public Request create(Request incomingRequest, MultipartFile[] newImages, String note) throws ValidationException {
        incomingRequest.setStatus(RequestStatus.SCHEDULED);

        // Set request reference for items and snapshot service items BEFORE saving
        for (RequestItem item : incomingRequest.getItems()) {
            item.setRequest(incomingRequest);
        }
        copyServiceItemsFields(incomingRequest.getItems());

        // Build note after all data preparation
        StringBuilder noteBuilder = new StringBuilder("Tạo mới yêu cầu");
        if (note != null && !note.trim().isEmpty()) {
            noteBuilder.append("\nGhi chú: " + note.trim());
        }

        historyService.addRequestHistoryRecord(incomingRequest, noteBuilder.toString(),
                getCurrentUsername());

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

    /**
     * Updates an existing repair request.
     * Handles validation, status transitions, image updates, and history tracking.
     * 
     * @param id              The UUID of the request to update.
     * @param incomingRequest The Request object containing updated details.
     * @param newImages       An array of MultipartFile objects for new images to
     *                        add.
     * @param toDelete        An array of filenames of images to delete.
     * @param note            An optional note to add to the request history.
     * @return The updated and saved Request entity.
     * @throws ValidationException if there are validation errors or the request is
     *                             not found.
     */
    @Override
    @Transactional
    public Request update(UUID id, Request incomingRequest, MultipartFile[] newImages, String[] toDelete,
            String note) throws ValidationException {
        // Load existing request with items and images eagerly fetched
        Request existingRequest = reqRepo.findByIdWithItems(id)
                .orElseThrow(() -> new ValidationException("Không tìm thấy yêu cầu với ID: " + id));

        // Validate early to fail fast
        requestValidator.validateEditable(existingRequest);
        requestValidator.validateStatusTransition(existingRequest, incomingRequest);
        requestValidator.validateItemsForStatus(incomingRequest);
        requestValidator.validateNoItemModificationWhenLocked(existingRequest, incomingRequest);

        // Snapshot for history tracking
        Request archivedRequest = new Request();
        copyRequestFields(archivedRequest, existingRequest, true);

        // Set completion date if status is changing to COMPLETED
        if (existingRequest.getStatus() != RequestStatus.COMPLETED
                && incomingRequest.getStatus() == RequestStatus.COMPLETED) {
            incomingRequest.setCompletedAt(vietnamTimeProvider.now());
        }

        // Process images BEFORE copying fields (to preserve existing images)
        List<RequestImage> currentImages = imageService.updateRequestServiceImages(existingRequest, newImages,
                toDelete);

        // Snapshot service items, copy all fields and save
        if (!existingRequest.getStatus().isRequestItemsLocked()) {
            copyServiceItemsFields(incomingRequest.getItems());
        }

        copyRequestFields(existingRequest, incomingRequest, false);

        // Apply processed images after field copy
        existingRequest.getImages().clear();
        existingRequest.getImages().addAll(currentImages);

        // Build note with user input and computed changes
        StringBuilder noteBuilder = new StringBuilder(
                (note != null && !note.trim().isEmpty()) ? "Ghi chú: " + note.trim() + "\n" : "");
        noteBuilder.append(historyService.computeRequestChanges(archivedRequest, existingRequest));

        // Add history if there are actual changes OR if there's a modal note
        if (!noteBuilder.toString().trim().isEmpty()) {
            historyService.addRequestHistoryRecord(existingRequest, noteBuilder.toString(),
                    getCurrentUsername());
        }

        Request saved = reqRepo.save(existingRequest);
        emailService.sendUpdateEmail(saved, noteBuilder.toString());
        return saved;
    }

    /**
     * Recovers request information by sending an email with tracking links to the
     * provided email address.
     * 
     * @param email The email address for which to recover requests.
     */
    @Override
    public void recover(String email) {
        List<Request> requests = reqRepo.findByEmail(email);
        if (requests.isEmpty()) {
            return;
        }
        emailService.sendRecoverEmail(email, requests);
    }

    private void copyServiceItemsFields(List<RequestItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        // Batch fetching to avoid N+1 query problem
        // Step 1: Collect all service item IDs
        List<UUID> serviceItemIds = items.stream()
                .map(RequestItem::getServiceItemId)
                .collect(Collectors.toList());

        // Step 2: Fetch all service items in a single query
        List<ServiceItem> serviceItems = serviceItemRepository.findAllByIdInAndActive(serviceItemIds);

        // Step 3: Convert to Map for O(1) lookup
        Map<UUID, ServiceItem> serviceItemMap = serviceItems.stream()
                .collect(Collectors.toMap(ServiceItem::getId, serviceItem -> serviceItem));

        // Step 4: Process each RequestItem using the pre-loaded Map
        for (RequestItem item : items) {
            ServiceItem serviceItem = serviceItemMap.get(item.getServiceItemId());
            if (serviceItem == null) {
                throw new NotFoundException("Không tìm dịch vụ sửa chửa: " + item.getName());
            }

            // Validate critical data consistency between frontend and database
            validateServiceItemDataConsistency(item, serviceItem);

            // Copy latest data from ServiceItem, preserving user-customizable fields
            BeanUtils.copyProperties(serviceItem, item, "id", "serviceItemId", "active", "createdAt", "updatedAt",
                    "quantity", "discount");

            if (item.getDiscount().compareTo(item.getPrice()) > 0) {
                throw new ValidationException("Giảm giá vượt quá giá gốc: " + item.getName());
            }
        }
    }

    private void validateServiceItemDataConsistency(RequestItem requestItem, ServiceItem serviceItem) {
        StringBuilder errors = new StringBuilder();

        // Check price consistency
        if (requestItem.getPrice() != null &&
                requestItem.getPrice().compareTo(serviceItem.getPrice()) != 0) {
            errors.append(String.format("Giá dịch vụ '%s' đã thay đổi từ %s thành %s. ",
                    serviceItem.getName(),
                    requestItem.getPrice(),
                    serviceItem.getPrice()));
        }

        // Check VAT rate consistency
        if (requestItem.getVatRate() != null &&
                requestItem.getVatRate().compareTo(serviceItem.getVatRate()) != 0) {
            errors.append(String.format("VAT dịch vụ '%s' đã thay đổi từ %s%% thành %s%%. ",
                    serviceItem.getName(),
                    requestItem.getVatRate().multiply(new BigDecimal("100")),
                    serviceItem.getVatRate().multiply(new BigDecimal("100"))));
        }

        // Check warranty period consistency
        if (requestItem.getWarrantyDays() != null &&
                !requestItem.getWarrantyDays().equals(serviceItem.getWarrantyDays())) {
            errors.append(String.format("Thời hạn bảo hành dịch vụ '%s' đã thay đổi từ %d ngày thành %d ngày. ",
                    serviceItem.getName(),
                    requestItem.getWarrantyDays(),
                    serviceItem.getWarrantyDays()));
        }

        // If there are any inconsistencies, throw validation error
        if (errors.length() > 0) {
            errors.append("Vui lòng làm mới trang và thử lại.");
            throw new ValidationException(errors.toString());
        }
    }

    private Request copyRequestFields(Request target, Request source, boolean deepCopyCollections) {
        BeanUtils.copyProperties(source, target, "id", "items", "images", "history",
                "createdAt", "updatedAt", "createdBy", "updatedBy");

        if (deepCopyCollections) {
            if (source.getItems() == null) {
                target.setItems(null);
                return target;
            }

            List<RequestItem> copiedItems = new ArrayList<>();
            for (RequestItem item : source.getItems()) {
                RequestItem newItem = new RequestItem();
                BeanUtils.copyProperties(item, newItem, "id", "request");
                newItem.setRequest(target);
                copiedItems.add(newItem);
            }
            target.setItems(copiedItems);
        } else {
            // For updating - only change reference
            if (source.getItems() != null) {
                target.getItems().clear();
                source.getItems().forEach(item -> item.setRequest(target));
                target.getItems().addAll(source.getItems());
            }
            if (source.getImages() != null) {
                target.getImages().clear();
                source.getImages().forEach(image -> image.setRequest(target));
                target.getImages().addAll(source.getImages());
            }
        }

        return target;
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()))
                ? auth.getName()
                : "Public";
    }

}