package com.laptoprepair.controller;

import com.laptoprepair.entity.Request;
import com.laptoprepair.enums.RequestStatus;
import com.laptoprepair.exception.ValidationException;
import com.laptoprepair.exception.NotFoundException;
import com.laptoprepair.service.RequestService;

import org.springframework.beans.factory.annotation.Value;
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
 * Provides functionalities for listing, creating, editing, and viewing requests.
 */
@Controller
@RequestMapping("/staff/requests")
@RequiredArgsConstructor
@Slf4j
public class RequestController {
    
    private final RequestService requestService;

    @Value("${app.pagination.default-page-size.requests}")
    private int defaultPageSize;

    @Value("${app.upload.max-images-per-request}")
    private int maxImagesPerRequest;

    /**
     * Displays a paginated list of repair requests.
     * @param page The current page number (0-indexed).
     * @param size The number of requests per page. Defaults to app.pagination.default-page-size.requests.
     * @param search Optional search term to filter requests.
     * @param status Optional request status to filter requests.
     * @param model The Model object for view data.
     * @return The staff request list view.
     */
    @GetMapping("/list")
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) RequestStatus status,
            Model model) {

        if (size == null) {
            size = defaultPageSize;
        }

        Page<Request> requests = requestService.list(search, status, PageRequest.of(page, size));
        model.addAttribute("requests", requests);
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        return "staff/request-list";
    }

    /**
     * Displays the form for creating a new repair request.
     * @param model The Model object for view data.
     * @param request The HttpServletRequest.
     * @return The request creation form view.
     */
    @GetMapping("/create")
    public String createForm(Model model, HttpServletRequest request) {
        populateForCreate(model, request);
        return "staff/request-form";
    }

    /**
     * Displays the form for editing an existing repair request.
     * Redirects to the view page if the request status is COMPLETED or CANCELLED.
     * @param id The UUID of the request to edit.
     * @param model The Model object for view data.
     * @param request The HttpServletRequest.
     * @return The request edit form view or a redirect to the view page.
     */
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable UUID id, Model model, HttpServletRequest request) {
        Request existingRequest = requestService.findById(id);

        // Redirect to detail view if request is cancelled
        if (existingRequest.getStatus() == RequestStatus.CANCELLED) {
            return "redirect:/staff/requests/view/" + id;
        }

        populateForEdit(existingRequest, model, request);
        return "staff/request-form";
    }

    /**
     * Displays the detailed view of a specific repair request.
     * @param id The UUID of the request to view.
     * @param model The Model object for view data.
     * @return The request detail view.
     */
    @GetMapping("/view/{id}")
    public String view(@PathVariable UUID id, Model model) {
        Request existingRequest = requestService.findById(id);
        model.addAttribute("request", existingRequest);
        model.addAttribute("isStaff", true);
        log.info("Staff view accessed for request {}, isStaff set to true", id);
        return "staff/request-detail";
    }

    /**
     * Handles the submission for creating a new repair request.
     * @param incomingRequest The Request object from the form.
     * @param bindingResult The BindingResult for validation errors.
     * @param newImages Optional. New images to be uploaded with the request.
     * @param note Optional. A note for the request history.
     * @param model The Model object for view data.
     * @param request The HttpServletRequest.
     * @param redirectAttributes The RedirectAttributes for flash messages.
     * @return A redirect to the edit page of the newly created request or back to the form with errors.
     */
    @PostMapping("/create")
    public String create(@Valid @ModelAttribute Request incomingRequest,
            BindingResult bindingResult,
            @RequestParam(value = "newImages", required = false) MultipartFile[] newImages,
            @RequestParam(required = false) String note,
            Model model,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            populateForCreate(model, request);
            model.addAttribute("request", incomingRequest); // Override with form data
            return "staff/request-form";
        }

        try {
            Request saved = requestService.create(incomingRequest, newImages, note);
            redirectAttributes.addFlashAttribute("successMessage", "Yêu cầu đã được tạo thành công!");
            return "redirect:/staff/requests/edit/" + saved.getId();
        } catch (ValidationException | NotFoundException ex) {
            populateForCreate(model, request);
            model.addAttribute("request", incomingRequest);
            model.addAttribute("errorMessage", ex.getMessage());
            return "staff/request-form";
        }
    }

    /**
     * Handles the submission for updating an existing repair request.
     * @param id The UUID of the request to update.
     * @param incomingRequest The Request object from the form.
     * @param bindingResult The BindingResult for validation errors.
     * @param newImages Optional. New images to be uploaded.
     * @param toDelete Optional. Array of image IDs to be deleted.
     * @param note Optional. A note for the request history.
     * @param model The Model object for view data.
     * @param request The HttpServletRequest.
     * @param redirectAttributes The RedirectAttributes for flash messages.
     * @return A redirect to the edit page of the updated request or back to the form with errors.
     */
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
            // Load full request from DB when validation fails
            Request existingRequest = requestService.findById(id);
            populateForEdit(existingRequest, model, request);
            model.addAttribute("request", incomingRequest); // Override with form data
            return "staff/request-form";
        }

        try {
            Request updated = requestService.update(id, incomingRequest, newImages, toDelete, note);
            redirectAttributes.addFlashAttribute("successMessage", "Yêu cầu đã được cập nhật thành công!");
            return "redirect:/staff/requests/edit/" + updated.getId();
        } catch (ValidationException | NotFoundException ex) {
            Request existingRequest = requestService.findById(id);
            populateForEdit(existingRequest, model, request);
            model.addAttribute("request", incomingRequest);
            model.addAttribute("errorMessage", ex.getMessage());
            return "staff/request-form";
        }
    }

    /**
     * Populates the model with necessary attributes for the request creation form.
     * @param model The Model object.
     * @param request The HttpServletRequest.
     */
    private void populateForCreate(Model model, HttpServletRequest request) {
        model.addAttribute("request", new Request());
        model.addAttribute("requestUri", request.getRequestURI());
        model.addAttribute("maxImages", maxImagesPerRequest);
    }

    /**
     * Populates the model with necessary attributes for the request edit form.
     * @param existing The existing Request object.
     * @param model The Model object.
     * @param request The HttpServletRequest.
     */
    private void populateForEdit(Request existing, Model model, HttpServletRequest request) {
        model.addAttribute("request", existing);
        model.addAttribute("requestUri", request.getRequestURI());
        model.addAttribute("maxImages", maxImagesPerRequest);
        model.addAttribute("isRequestItemsLocked", existing.getStatus() != null && existing.getStatus().isRequestItemsLocked());
    }
}