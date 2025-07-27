package com.laptoprepair.controller;

import com.laptoprepair.entity.Request;
import com.laptoprepair.enums.RequestStatus;
import com.laptoprepair.service.RequestService;
import com.laptoprepair.utils.AppConstants;

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
import lombok.RequiredArgsConstructor;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

@Controller
@RequestMapping("/staff/requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;

    @Value("${app.pagination.default-page-size.requests}")
    private int defaultPageSize;

    @Value("${app.upload.max-images-per-request}")
    private int maxImagesPerRequest;

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

    @GetMapping("/create")
    public String createForm(Model model, HttpServletRequest request) {
        populateForCreate(model, request);
        return AppConstants.VIEW_STAFF_REQUEST_FORM;
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable UUID id, Model model, HttpServletRequest request) {
        Request existingRequest = requestService.findById(id);

        // Redirect to detail view if request is completed or cancelled
        if (existingRequest.getStatus() == RequestStatus.COMPLETED
                || existingRequest.getStatus() == RequestStatus.CANCELLED) {
            return "redirect:/staff/requests/view/" + id;
        }

        populateForEdit(existingRequest, model, request);
        return AppConstants.VIEW_STAFF_REQUEST_FORM;
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable UUID id, Model model) {
        Request existingRequest = requestService.findById(id);
        model.addAttribute(AppConstants.ATTR_REQUEST, existingRequest);
        model.addAttribute("isStaff", true);
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
            populateForCreate(model, request);
            model.addAttribute(AppConstants.ATTR_REQUEST, incomingRequest); // Override with form data
            return AppConstants.VIEW_STAFF_REQUEST_FORM;
        }

        Request saved = requestService.create(incomingRequest, newImages, note);
        redirectAttributes.addFlashAttribute("successMessage", "Yêu cầu đã được tạo thành công!");
        return "redirect:/staff/requests/edit/" + saved.getId();
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
            // Load full request from DB when validation fails
            Request existingRequest = requestService.findById(id);
            populateForEdit(existingRequest, model, request);
            model.addAttribute(AppConstants.ATTR_REQUEST, incomingRequest); // Override with form data
            return AppConstants.VIEW_STAFF_REQUEST_FORM;
        }

        Request updated = requestService.update(id, incomingRequest, newImages, toDelete, note);
        redirectAttributes.addFlashAttribute("successMessage", "Yêu cầu đã được cập nhật thành công!");
        return "redirect:/staff/requests/edit/" + updated.getId();
    }

    private void populateForCreate(Model model, HttpServletRequest request) {
        model.addAttribute(AppConstants.ATTR_REQUEST, new Request());
        addCommonAttributes(model, request);
    }

    private void populateForEdit(Request existing, Model model, HttpServletRequest request) {
        model.addAttribute(AppConstants.ATTR_REQUEST, existing);
        addCommonAttributes(model, request);

        // Add locked status for form controls
        boolean isRequestItemsLocked = existing.getStatus() != null && existing.getStatus().isRequestItemsLocked();
        model.addAttribute("isRequestItemsLocked", isRequestItemsLocked);
    }

    private void addCommonAttributes(Model model, HttpServletRequest request) {
        model.addAttribute("requestUri", request.getRequestURI());
        model.addAttribute("maxImages", maxImagesPerRequest);
    }

}