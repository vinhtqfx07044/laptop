package com.laptoprepair.controller;

import com.laptoprepair.entity.Request;
import com.laptoprepair.enums.RequestStatus;
import com.laptoprepair.service.RequestService;
import com.laptoprepair.web.RequestFormPopulator;
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
    private final RequestFormPopulator formPopulator;

    @Value("${pagination.default-page-size.requests}")
    private int defaultPageSize;

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
        formPopulator.populateForCreate(model, request);
        return "staff/request-form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable UUID id, Model model, HttpServletRequest request) {
        Request existingRequest = requestService.findById(id);

        // Redirect to detail view if request is completed or cancelled
        if (existingRequest.getStatus() == RequestStatus.COMPLETED
                || existingRequest.getStatus() == RequestStatus.CANCELLED) {
            return "redirect:/staff/requests/view/" + id;
        }

        formPopulator.populateForEdit(existingRequest, model, request);
        return "staff/request-form";
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable UUID id, Model model) {
        Request existingRequest = requestService.findById(id);
        model.addAttribute("request", existingRequest);
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
            formPopulator.populateForCreate(model, request);
            model.addAttribute("request", incomingRequest); // Override with form data
            return "staff/request-form";
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
            formPopulator.populateForEdit(existingRequest, model, request);
            model.addAttribute("request", incomingRequest); // Override with form data
            return "staff/request-form";
        }

        Request updated = requestService.update(id, incomingRequest, newImages, toDelete, note);
        redirectAttributes.addFlashAttribute("successMessage", "Yêu cầu đã được cập nhật thành công!");
        return "redirect:/staff/requests/edit/" + updated.getId();
    }


}