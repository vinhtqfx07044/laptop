package com.laptoprepair.controller;

import com.laptoprepair.entity.ServiceItem;
import com.laptoprepair.exception.CSVImportException;
import com.laptoprepair.exception.ValidationException;
import com.laptoprepair.exception.NotFoundException;
import com.laptoprepair.service.ServiceItemService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * Controller for managing service items.
 * Provides endpoints for listing, exporting, importing, creating, updating, and searching service items.
 * Requires STAFF role for access.
 */
@Controller
@RequestMapping("/staff/service-items")
@PreAuthorize("hasRole('STAFF')")
@RequiredArgsConstructor
public class ServiceItemController {
    
    @Value("${app.pagination.default-page-size.service-items}")
    private int defaultPageSize;

    private final ServiceItemService serviceItemService;

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean activeOnly,
            Model model) {
        if (size == null) {
            size = defaultPageSize;
        }

        Page<ServiceItem> serviceItems = serviceItemService.list(search, activeOnly, PageRequest.of(page, size));
        model.addAttribute("serviceItems", serviceItems);
        model.addAttribute("currentPage", page);
        model.addAttribute("activeOnly", activeOnly);
        model.addAttribute("search", search);
        model.addAttribute("newServiceItem", new ServiceItem());
        return "staff/service-items";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCSV() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=service-items.csv")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(serviceItemService.exportCSV());
    }

    @PostMapping("/import")
    public String importCSV(@RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        // FIX: Local error handling thay vì GlobalExceptionHandler
        try {
            serviceItemService.importCSV(file);
            redirectAttributes.addFlashAttribute("successMessage", "Import CSV thành công!");
            return "redirect:/staff/service-items";
        } catch (CSVImportException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return list(0, null, null, null, model);
        }
    }

    @PostMapping("/create")
    public String create(@ModelAttribute ServiceItem serviceItem,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return list(0, null, null, null, model);
        }
        
        try {
            serviceItemService.create(serviceItem);
            redirectAttributes.addFlashAttribute("successMessage", "Dịch vụ đã được tạo thành công!");
            return "redirect:/staff/service-items";
        } catch (ValidationException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return list(0, null, null, null, model);
        }
    }

    @PostMapping("/update/{id}")
    public String update(@PathVariable UUID id,
            @ModelAttribute ServiceItem serviceItem,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return list(0, null, null, null, model);
        }

        try {
            serviceItemService.update(id, serviceItem);
            redirectAttributes.addFlashAttribute("successMessage", "Dịch vụ đã được cập nhật thành công!");
            return "redirect:/staff/service-items";
        } catch (ValidationException | NotFoundException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return list(0, null, null, null, model);
        }
    }

    @GetMapping("/search")
    @ResponseBody
    public Page<ServiceItem> search(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size) {
        if (size == null) {
            size = defaultPageSize;
        }
        // Only return active items for search
        return serviceItemService.list(query, true, PageRequest.of(page, size));
    }
}