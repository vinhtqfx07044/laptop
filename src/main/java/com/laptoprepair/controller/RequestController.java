package com.laptoprepair.controller;

import com.laptoprepair.entity.Request;
import com.laptoprepair.entity.Request.RequestStatus;
import com.laptoprepair.exception.ValidationException;
import com.laptoprepair.exception.NotFoundException;
import com.laptoprepair.service.RequestService;
import com.laptoprepair.utils.ValidationErrorUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

/**
 * Controller for managing repair requests by staff members.
 * Provides functionalities for listing, creating, editing, and viewing
 * requests.
 */
@Controller
@RequestMapping("/staff/requests")
@RequiredArgsConstructor
@Slf4j
public class RequestController {

    private static final String REQUEST_ATTRIBUTE = "request";
    private static final String REQUEST_FORM_VIEW = "staff/request-form";

    private final RequestService requestService;
    private final ValidationErrorUtil validationErrorUtil;

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_IMAGES_PER_REQUEST = 5;

    @GetMapping("/list")
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) RequestStatus status,
            Model model) {

        if (size == null) {
            size = DEFAULT_PAGE_SIZE;
        }

        Page<Request> requests = requestService.list(search, status, PageRequest.of(page, size));
        model.addAttribute("requests", requests);
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        return "staff/request-list";
    }

    @GetMapping("/create")
    public String createForm(Model model, HttpServletRequest request) {
        populateForCreate(model, request);
        return REQUEST_FORM_VIEW;
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable UUID id, Model model, HttpServletRequest request) {
        log.info("Loading edit form for request ID: {}", id);
        Request existingRequest = requestService.findByIdWithItemsAndImages(id);
        log.info("Request loaded - ID: {}, Items count: {}, Images count: {}",
                existingRequest.getId(),
                existingRequest.getItems().size(),
                existingRequest.getImages().size());
        if (existingRequest.getStatus() == RequestStatus.CANCELLED) {
            return "redirect:/staff/requests/view/" + id;
        }
        populateForEdit(existingRequest, model, request);
        return REQUEST_FORM_VIEW;
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable UUID id, Model model) {
        Request existingRequest = requestService.findById(id);
        model.addAttribute(REQUEST_ATTRIBUTE, existingRequest);
        model.addAttribute("isStaff", true);
        log.info("Staff view accessed for request {}, isStaff set to true", id);
        return "staff/request-detail";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute Request incomingRequest,
            BindingResult bindingResult,
            @RequestParam(value = "newImages", required = false) MultipartFile[] newImages,
            @RequestParam(required = false) String note,
            Model model,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMessages", validationErrorUtil.extractErrorMessages(bindingResult));
            model.addAttribute("fieldHasErrors", validationErrorUtil.getFieldErrorStatus(bindingResult));
            populateForCreate(model, request);
            model.addAttribute(REQUEST_ATTRIBUTE, incomingRequest); // Override with form data
            return REQUEST_FORM_VIEW;
        }

        try {
            Request saved = requestService.create(incomingRequest, newImages, note);
            redirectAttributes.addFlashAttribute("successMessage", "Yêu cầu đã được tạo thành công!");
            return "redirect:/staff/requests/edit/" + saved.getId();
        } catch (ValidationException | NotFoundException ex) {
            populateForCreate(model, request);
            model.addAttribute(REQUEST_ATTRIBUTE, incomingRequest);
            model.addAttribute("errorMessage", ex.getMessage());
            return REQUEST_FORM_VIEW;
        }
    }

    @PostMapping("/edit/{id}")
    public String update(@PathVariable UUID id,
            @Valid @ModelAttribute Request incomingRequest,
            BindingResult bindingResult,
            @RequestParam(value = "newImages", required = false) MultipartFile[] newImages,
            @RequestParam(required = false) String[] toDelete,
            @RequestParam(required = false) String note,
            Model model,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            log.info("Validation failed for request ID: {}, reloading edit form", id);
            model.addAttribute("errorMessages", validationErrorUtil.extractErrorMessages(bindingResult));
            model.addAttribute("fieldHasErrors", validationErrorUtil.getFieldErrorStatus(bindingResult));

            // Load full request with items and images from DB when validation fails
            Request existingRequest = requestService.findByIdWithItemsAndImages(id);
            log.info("Reloaded request after validation failure - ID: {}, Items count: {}, Images count: {}",
                    existingRequest.getId(),
                    existingRequest.getItems().size(),
                    existingRequest.getImages().size());

            // Merge form data with existing request to preserve images
            Request mergedRequest = requestService.mergeFormRequestWithExisting(existingRequest, incomingRequest);
            populateForEdit(mergedRequest, model, request);
            return REQUEST_FORM_VIEW;
        }

        try {
            Request updated = requestService.update(id, incomingRequest, newImages, toDelete, note);
            redirectAttributes.addFlashAttribute("successMessage", "Yêu cầu đã được cập nhật thành công!");
            return "redirect:/staff/requests/edit/" + updated.getId();
        } catch (ValidationException | NotFoundException ex) {
            Request existingRequest = requestService.findByIdWithItemsAndImages(id);
            Request mergedRequest = requestService.mergeFormRequestWithExisting(existingRequest, incomingRequest);
            populateForEdit(mergedRequest, model, request);
            model.addAttribute("errorMessage", ex.getMessage());
            return REQUEST_FORM_VIEW;
        }
    }

    private void populateForCreate(Model model, HttpServletRequest request) {
        model.addAttribute(REQUEST_ATTRIBUTE, new Request());
        model.addAttribute("requestUri", request.getRequestURI());
        model.addAttribute("maxImages", MAX_IMAGES_PER_REQUEST);
    }

    private void populateForEdit(Request existing, Model model, HttpServletRequest request) {
        log.info("populateForEdit called - Request ID: {}, Items: {}, Images: {}",
                existing.getId(), existing.getItems().size(), existing.getImages().size());
        model.addAttribute(REQUEST_ATTRIBUTE, existing);
        model.addAttribute("requestUri", request.getRequestURI());
        model.addAttribute("maxImages", MAX_IMAGES_PER_REQUEST);
        model.addAttribute("isRequestItemsLocked",
                existing.getStatus() != null && existing.getStatus().isRequestItemsLocked());
        log.info("populateForEdit completed - Request added to model with {} images", existing.getImages().size());
    }
}