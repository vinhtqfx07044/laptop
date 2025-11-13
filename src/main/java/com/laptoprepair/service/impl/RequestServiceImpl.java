package com.laptoprepair.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.laptoprepair.entity.Request;
import com.laptoprepair.entity.Request.RequestStatus;
import com.laptoprepair.entity.RequestImage;
import com.laptoprepair.entity.RequestItem;
import com.laptoprepair.entity.ServiceItem;
import com.laptoprepair.exception.NotFoundException;
import com.laptoprepair.exception.ValidationException;
import com.laptoprepair.repository.RequestRepository;
import com.laptoprepair.repository.ServiceItemRepository;
import com.laptoprepair.service.EmailService;
import com.laptoprepair.service.HistoryService;
import com.laptoprepair.service.ImageService;
import com.laptoprepair.service.RequestService;
import com.laptoprepair.service.SecurityService;
import com.laptoprepair.utils.TimeUtils;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Service implementation for managing laptop repair requests.
 * Provides comprehensive functionality for creating, updating, retrieving, and
 * managing repair requests
 * with support for service items, images, email notifications, and audit
 * history.
 *
 * @author Laptop Repair System
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private static final Logger log = LoggerFactory.getLogger(RequestServiceImpl.class);
    private static final String REQUEST_NOT_FOUND_MSG = "Không tìm thấy yêu cầu với ID: ";

    private final RequestRepository reqRepo;
    private final ServiceItemRepository serviceItemRepository;
    private final HistoryService historyService;
    private final ImageService imageService;
    private final EmailService emailService;
    private final SecurityService securityService;

    /**
     * Finds a request by its unique identifier.
     *
     * @param id the unique identifier of the request, must not be null
     * @return the found request
     * @throws NotFoundException if no request exists with the given ID
     */
    @Transactional(readOnly = true)
    @Override
    public Request findById(@NonNull UUID id) {
        return reqRepo.findById(id)
                .orElseThrow(() -> new NotFoundException(REQUEST_NOT_FOUND_MSG + id));
    }

    /**
     * Merges form data from an incoming request into an existing request.
     * This method is typically used during form validation failures to preserve
     * user input.
     *
     * @param existingRequest the existing request to merge into
     * @param incomingRequest the incoming request with form data to merge
     * @return the merged existing request
     */
    public Request mergeFormRequestWithExisting(Request existingRequest, Request incomingRequest) {
        copyRequest(existingRequest, incomingRequest, false, false);
        return existingRequest;
    }

    /**
     * Finds a request by ID with all associated items and images loaded.
     * Performs two separate queries to fetch items and images and merges the
     * results.
     *
     * @param id the unique identifier of the request, must not be null
     * @return the request with items and images populated
     * @throws NotFoundException if no request exists with the given ID
     */
    @Transactional(readOnly = true)
    @Override
    public Request findByIdWithItemsAndImages(@NonNull UUID id) {
        Request request = reqRepo.findByIdWithItems(id)
                .orElseThrow(() -> new NotFoundException(REQUEST_NOT_FOUND_MSG + id));
        Request requestWithImages = reqRepo.findByIdWithImages(id)
                .orElseThrow(() -> new NotFoundException(REQUEST_NOT_FOUND_MSG + id));
        request.setImages(requestWithImages.getImages());
        return request;
    }

    /**
     * Retrieves a paginated list of requests with optional filtering by search term
     * and status.
     *
     * @param search   the search term to filter requests (can be null for no search
     *                 filter)
     * @param status   the status to filter requests by (can be null for no status
     *                 filter)
     * @param pageable the pagination information
     * @return a page of filtered requests
     */
    @Override
    public Page<Request> list(String search, RequestStatus status, Pageable pageable) {
        String statusString = status != null ? status.name() : null;
        return reqRepo.findWithFilters(search, statusString, pageable);
    }

    /**
     * Creates a new request from public access (no authentication required).
     * Validates appointment date, sets initial status, and sends confirmation
     * email.
     *
     * @param incomingRequest the request data to create
     * @return the created request with generated ID and initial values set
     * @throws ValidationException if the appointment date is in the past
     */
    @Override
    public Request publicCreate(Request incomingRequest) throws ValidationException {
        LocalDateTime appointmentDate = incomingRequest.getAppointmentDate();
        if (appointmentDate != null && appointmentDate.isBefore(LocalDateTime.now())) {
            throw new ValidationException("Ngày hẹn phải sau thời điểm hiện tại");
        }

        incomingRequest.setStatus(RequestStatus.SCHEDULED);
        incomingRequest.setItems(List.of());
        incomingRequest.setImages(List.of());

        historyService.addRequestHistoryRecord(incomingRequest, "Tạo mới yêu cầu", "anonymous");

        Request savedRequest = reqRepo.save(incomingRequest);
        emailService.sendConfirmationEmail(savedRequest);
        return savedRequest;
    }

    /**
     * Creates a new request with full functionality including service items and
     * images.
     * Enriches request items with service data, processes image uploads, creates
     * history records,
     * and sends confirmation emails.
     *
     * @param incomingRequest the request data to create
     * @param newImages       array of image files to upload (can be null or empty)
     * @param note            optional note for the history record
     * @return the created request with generated ID, processed items, and uploaded
     *         images
     * @throws ValidationException if service item validation fails
     */
    @Override
    @Transactional
    public Request create(Request incomingRequest, MultipartFile[] newImages, String note)
            throws ValidationException {
        incomingRequest.setStatus(RequestStatus.SCHEDULED);

        if (incomingRequest.getItems() == null) {
            incomingRequest.setItems(new ArrayList<>());
        }
        for (RequestItem item : incomingRequest.getItems()) {
            item.setRequest(incomingRequest);
        }
        enrichRequestItems(incomingRequest.getItems());

        StringBuilder noteBuilder = new StringBuilder("Tạo mới yêu cầu");
        if (note != null && !note.trim().isEmpty()) {
            noteBuilder.append("\nGhi chú: ").append(note.trim());
        }

        historyService.addRequestHistoryRecord(incomingRequest, noteBuilder.toString(),
                securityService.getCurrentUsername());

        Request savedRequest = reqRepo.save(incomingRequest);

        List<RequestImage> images = imageService.uploadImages(savedRequest.getId(),
                new ArrayList<>(), newImages, savedRequest);
        savedRequest.setImages(new ArrayList<>(images));

        savedRequest = reqRepo.save(savedRequest);
        emailService.sendConfirmationEmail(savedRequest);
        return savedRequest;
    }

    /**
     * Updates an existing request with new data, images, and handles image
     * deletions.
     * Validates update permissions, manages status transitions, processes images,
     * records history, and sends notification emails.
     *
     * @param id              the unique identifier of the request to update
     * @param incomingRequest the updated request data (can be null for partial
     *                        updates)
     * @param newImages       array of new image files to upload (can be null or
     *                        empty)
     * @param toDelete        array of image IDs to delete (can be null or empty)
     * @param note            optional note for the history record
     * @return the updated request
     * @throws ValidationException if update validation fails or request is not
     *                             found
     */
    @Override
    @Transactional
    public Request update(UUID id, Request incomingRequest, MultipartFile[] newImages,
            String[] toDelete, String note) throws ValidationException {

        Request existingRequest = reqRepo.findByIdWithItems(id)
                .orElseThrow(() -> new ValidationException(REQUEST_NOT_FOUND_MSG + id));

        ensureRequestCanBeUpdated(existingRequest, incomingRequest);

        Request snapshot = snapshotRequest(existingRequest);

        if (incomingRequest != null
                && existingRequest.getStatus() != RequestStatus.COMPLETED
                && incomingRequest.getStatus() == RequestStatus.COMPLETED) {
            incomingRequest.setCompletedAt(TimeUtils.nowInVietnam());
        }

        List<RequestImage> currentImages = imageService.updateRequestServiceImages(existingRequest,
                newImages, toDelete);

        if (incomingRequest != null && !existingRequest.getStatus().isRequestItemsLocked()) {
            // Pass existing items to validate immutability of existing items
            enrichRequestItems(incomingRequest.getItems(), existingRequest.getItems());
        }

        copyRequest(existingRequest, incomingRequest, false, false);

        if (existingRequest.getImages() == null) {
            existingRequest.setImages(new ArrayList<>());
        } else {
            existingRequest.getImages().clear();
        }
        existingRequest.getImages().addAll(currentImages);

        persistUpdate(existingRequest, snapshot, note);
        return existingRequest;
    }

    /**
     * Sends a recovery email containing all requests associated with the given
     * email address.
     * If no requests are found for the email, no action is taken.
     *
     * @param email the email address to search requests for and send recovery
     *              information to
     */
    @Override
    public void recover(String email) {
        List<Request> requests = reqRepo.findByEmail(email);
        if (requests.isEmpty()) {
            return;
        }
        emailService.sendRecoverEmail(email, requests);
    }

    /**
     * Validates whether a request can be updated based on its current status and
     * incoming changes.
     * Prevents updates to cancelled requests, ensures service items are present for
     * certain statuses,
     * and prevents item modification when the request is in a locked state.
     *
     * @param existingRequest the current request state
     * @param incomingRequest the incoming request changes (can be null)
     * @throws ValidationException if the request cannot be updated due to status
     *                             constraints
     */
    private void ensureRequestCanBeUpdated(Request existingRequest, Request incomingRequest)
            throws ValidationException {
        if (existingRequest.getStatus() == RequestStatus.CANCELLED) {
            throw new ValidationException(
                    "Không thể chỉnh sửa phiếu ở trạng thái \"" + existingRequest.getStatus().getValue() + "\"");
        }

        if (incomingRequest != null && incomingRequest.getStatus() != null) {
            RequestStatus status = incomingRequest.getStatus();
            if (status != RequestStatus.SCHEDULED && status != RequestStatus.CANCELLED
                    && (incomingRequest.getItems() == null || incomingRequest.getItems().isEmpty())) {
                throw new ValidationException("Phiếu ở trạng thái \"" + status.getValue()
                        + "\" phải có ít nhất một hạng mục dịch vụ");
            }
        }

        if (existingRequest.getStatus().isRequestItemsLocked()
                && !historyService.areRequestItemsEqual(existingRequest.getItems(),
                        incomingRequest != null ? incomingRequest.getItems() : null)) {
            throw new ValidationException("Phiếu đã ở trạng thái \""
                    + existingRequest.getStatus().getValue()
                    + "\" và không thể thay đổi hạng mục.");
        }
    }

    /**
     * Creates a deep copy snapshot of a request for tracking changes.
     * Used for history tracking to compare before/after states.
     *
     * @param existingRequest the request to create a snapshot of
     * @return a deep copy of the request without ID-specific fields
     */
    private Request snapshotRequest(Request existingRequest) {
        Request snapshot = new Request();
        copyRequest(snapshot, existingRequest, true, false);
        return snapshot;
    }

    /**
     * Enriches request items with current service item data and validates
     * consistency.
     * For new items, copies latest service data and validates price/VAT/warranty
     * consistency.
     * For existing items, only validates discount amounts.
     *
     * @param items the list of request items to enrich and validate
     * @throws NotFoundException   if a service item is not found or inactive
     * @throws ValidationException if data consistency validation fails or discount
     *                             exceeds price
     */
    private void enrichRequestItems(List<RequestItem> items) {
        enrichRequestItems(items, null);
    }

    /**
     * Enriches request items with current service item data and validates
     * consistency.
     * For new items, copies latest service data and validates price/VAT/warranty
     * consistency.
     * For existing items, validates that no fields have been modified.
     *
     * @param items the list of request items to enrich and validate
     * @param existingItems the list of existing request items from DB (for validation)
     * @throws NotFoundException   if a service item is not found or inactive
     * @throws ValidationException if data consistency validation fails or discount
     *                             exceeds price
     */
    private void enrichRequestItems(List<RequestItem> items, List<RequestItem> existingItems) {
        if (items == null || items.isEmpty()) {
            return;
        }

        Map<UUID, ServiceItem> serviceItemMap = loadServiceItemsMap(items);
        Map<UUID, RequestItem> existingItemMap = buildExistingItemMap(existingItems);

        for (RequestItem item : items) {
            if (item == null) {
                continue;
            }
            processRequestItem(item, serviceItemMap, existingItemMap);
        }
    }

    private Map<UUID, ServiceItem> loadServiceItemsMap(List<RequestItem> items) {
        List<UUID> serviceItemIds = items.stream()
                .filter(Objects::nonNull)
                .map(RequestItem::getServiceItemId)
                .filter(Objects::nonNull)
                .toList();

        List<ServiceItem> serviceItems = serviceItemRepository.findAllByIdInAndActive(serviceItemIds);
        return serviceItems.stream()
                .collect(Collectors.toMap(ServiceItem::getId, serviceItem -> serviceItem));
    }

    private Map<UUID, RequestItem> buildExistingItemMap(List<RequestItem> existingItems) {
        if (existingItems == null || existingItems.isEmpty()) {
            return Map.of();
        }
        return existingItems.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(RequestItem::getId, item -> item));
    }

    private void processRequestItem(RequestItem item, Map<UUID, ServiceItem> serviceItemMap) {
        processRequestItem(item, serviceItemMap, Map.of());
    }

    private void processRequestItem(RequestItem item, Map<UUID, ServiceItem> serviceItemMap,
            Map<UUID, RequestItem> existingItemMap) {
        ServiceItem serviceItem = serviceItemMap.get(item.getServiceItemId());
        if (serviceItem == null) {
            throw new NotFoundException("Không tìm dịch vụ sửa chửa: " + item.getName());
        }

        boolean isNew = item.getId() == null;
        log.debug("Processing RequestItem: id={}, name='{}', serviceItemId={}, isNew={}",
                item.getId(), item.getName(), item.getServiceItemId(), isNew);

        if (isNew) {
            validateAndCopyServiceItemData(item, serviceItem);
        } else {
            // Existing item - validate immutability
            RequestItem existingItem = existingItemMap.get(item.getId());
            if (existingItem != null) {
                validateExistingItemImmutable(item, existingItem);
            } else {
                log.warn("Existing item not found in map for validation: id={}", item.getId());
            }
        }

        validateDiscount(item);
    }

    /**
     * Validates that an existing RequestItem remains immutable.
     * Ensures no fields can be modified for existing items (Phương án 1).
     * Only allows deletion of existing items or addition of new items.
     *
     * Uses RequestItem's equals() method to compare all fields.
     *
     * @param incomingItem the item from the frontend request
     * @param existingItem the item loaded from database
     * @throws ValidationException if any field has been modified
     */
    private void validateExistingItemImmutable(RequestItem incomingItem, RequestItem existingItem) {
        if (!incomingItem.equals(existingItem)) {
            String errorMsg = String.format(
                "Không thể thay đổi thông tin của item '%s'. Vui lòng xóa item cũ và thêm item mới nếu cần thay đổi.",
                existingItem.getName()
            );
            log.warn("Existing item immutability validation failed for item id={}", existingItem.getId());
            throw new ValidationException(errorMsg);
        }
        log.debug("Existing item immutability validation passed for item id={}", existingItem.getId());
    }

    private void validateAndCopyServiceItemData(RequestItem item, ServiceItem serviceItem) {
        StringBuilder errors = new StringBuilder();

        validatePrice(item, serviceItem, errors);
        validateVatRate(item, serviceItem, errors);
        validateWarrantyDays(item, serviceItem, errors);

        if (!errors.isEmpty()) {
            errors.append("Vui lòng làm mới trang và thử lại.");
            log.warn("Service item data consistency validation failed: {}", errors);
            throw new ValidationException(errors.toString());
        }

        BeanUtils.copyProperties(serviceItem, item, "id", "serviceItemId", "active",
                "createdAt", "updatedAt", "quantity", "discount");
    }

    private void validatePrice(RequestItem item, ServiceItem serviceItem, StringBuilder errors) {
        if (item.getPrice() != null && item.getPrice().compareTo(serviceItem.getPrice()) != 0) {
            String error = String.format("Giá dịch vụ '%s' đã thay đổi từ %s thành %s. ",
                    serviceItem.getName(), item.getPrice(), serviceItem.getPrice());
            log.debug("Price inconsistency detected: {}", error);
            errors.append(error);
        }
    }

    private void validateVatRate(RequestItem item, ServiceItem serviceItem, StringBuilder errors) {
        if (item.getVatRate() != null && item.getVatRate().compareTo(serviceItem.getVatRate()) != 0) {
            String error = String.format("VAT dịch vụ '%s' đã thay đổi từ %s%% thành %s%%. ",
                    serviceItem.getName(),
                    item.getVatRate().multiply(new BigDecimal("100")),
                    serviceItem.getVatRate().multiply(new BigDecimal("100")));
            log.debug("VAT rate inconsistency detected: {}", error);
            errors.append(error);
        }
    }

    private void validateWarrantyDays(RequestItem item, ServiceItem serviceItem, StringBuilder errors) {
        if (item.getWarrantyDays() != null
                && !item.getWarrantyDays().equals(serviceItem.getWarrantyDays())) {
            String error = String.format(
                    "Thời hạn bảo hành dịch vụ '%s' đã thay đổi từ %d ngày thành %d ngày. ",
                    serviceItem.getName(), item.getWarrantyDays(),
                    serviceItem.getWarrantyDays());
            log.debug("Warranty period inconsistency detected: {}", error);
            errors.append(error);
        }
    }

    private void validateDiscount(RequestItem item) {
        if (item.getDiscount() != null && item.getPrice() != null
                && item.getDiscount().compareTo(item.getPrice()) > 0) {
            throw new ValidationException("Giảm giá vượt quá giá gốc: " + item.getName());
        }
    }

    /**
     * Copies request data from source to target with configurable collection
     * handling.
     * Supports both shallow and deep copying of items and images collections.
     *
     * @param target              the request to copy data to
     * @param source              the request to copy data from
     * @param deepCopyCollections if true, creates new instances of collection
     *                            items; if false, reuses existing items
     * @param copyImages          if true, copies image collections; if false, skips
     *                            image processing
     */
    private void copyRequest(Request target, Request source, boolean deepCopyCollections,
            boolean copyImages) {
        if (target == null || source == null) {
            return;
        }

        BeanUtils.copyProperties(source, target, "id", "items", "images", "history", "createdAt",
                "updatedAt", "createdBy", "updatedBy");

        copyRequestItems(target, source, deepCopyCollections);

        if (copyImages) {
            copyRequestImages(target, source, deepCopyCollections);
        }
    }

    private void copyRequestItems(Request target, Request source, boolean deepCopyCollections) {
        if (source.getItems() == null) {
            if (deepCopyCollections) {
                target.setItems(null);
            }
            return;
        }

        if (deepCopyCollections) {
            target.setItems(deepCopyItems(source.getItems(), target));
        } else {
            shallowCopyItems(target, source);
        }
    }

    private List<RequestItem> deepCopyItems(List<RequestItem> sourceItems, Request targetRequest) {
        List<RequestItem> clonedItems = new ArrayList<>();
        for (RequestItem item : sourceItems) {
            if (item == null) {
                continue;
            }
            RequestItem clone = new RequestItem();
            BeanUtils.copyProperties(item, clone, "id", "request");
            clone.setRequest(targetRequest);
            clonedItems.add(clone);
        }
        return clonedItems;
    }

    private void shallowCopyItems(Request target, Request source) {
        if (target.getItems() == null) {
            target.setItems(new ArrayList<>());
        } else {
            target.getItems().clear();
        }
        for (RequestItem item : source.getItems()) {
            if (item == null) {
                continue;
            }
            item.setRequest(target);
            target.getItems().add(item);
        }
    }

    private void copyRequestImages(Request target, Request source, boolean deepCopyCollections) {
        if (source.getImages() == null) {
            if (deepCopyCollections) {
                target.setImages(null);
            }
            return;
        }

        if (target.getImages() == null) {
            target.setImages(new ArrayList<>());
        } else {
            target.getImages().clear();
        }

        for (RequestImage image : source.getImages()) {
            if (image == null) {
                continue;
            }
            image.setRequest(target);
            target.getImages().add(image);
        }
    }

    /**
     * Persists request updates, creates history records, and sends notification
     * emails.
     * Compares the snapshot with current request state to track changes and creates
     * comprehensive history records with notes and change details.
     *
     * @param request  the request to persist
     * @param snapshot the previous state of the request for comparison
     * @param note     optional note to include in the history record
     */
    private void persistUpdate(@NonNull Request request, Request snapshot, String note) {
        StringBuilder noteBuilder = new StringBuilder();

        if (note != null && !note.trim().isEmpty()) {
            noteBuilder.append("Ghi chú: ").append(note.trim()).append("\n");
        }

        String changes = historyService.computeRequestChanges(snapshot, request);
        if (changes != null && !changes.isBlank()) {
            noteBuilder.append(changes);
        }

        String historyNote = noteBuilder.toString();
        if (!historyNote.trim().isEmpty()) {
            historyService.addRequestHistoryRecord(request, historyNote,
                    securityService.getCurrentUsername());
        }

        Request saved = reqRepo.save(request);
        emailService.sendUpdateEmail(saved, historyNote);
    }
}
