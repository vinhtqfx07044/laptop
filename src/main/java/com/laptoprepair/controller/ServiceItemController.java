package com.laptoprepair.controller;

import com.laptoprepair.entity.ServiceItem;
import com.laptoprepair.exception.CSVImportException;
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
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Controller
@RequestMapping("/staff/service-items")
@PreAuthorize("hasRole('STAFF')")
/**
 * Controller for managing service items.
 * Provides endpoints for listing, exporting, importing, creating, updating, and searching service items.
 * Requires STAFF role for access.
 */
@RequiredArgsConstructor
public class ServiceItemController {
    
    @Value("${app.pagination.default-page-size.service-items}")
    private int defaultPageSize;

    private final ServiceItemService serviceItemService;

    /**
     * Displays a paginated list of service items.
     *
     * @param page The current page number (0-indexed).
     * @param size The number of items per page. If null, uses the default page size.
     * @param search An optional search term to filter service items by name or description.
     * @param activeOnly An optional flag to display only active service items.
     * @param model The Spring UI model to add attributes for the view.
     * @return The name of the Thymeleaf template for displaying service items.
     */
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

    /**
     * Exports all service items to a CSV file.
     *
     * @return A ResponseEntity containing the CSV file as a byte array, with appropriate headers for download.
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=service-items.csv")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(serviceItemService.exportCSV());
    }

    /**
     * Imports service items from a CSV file.
     *
     * @param file The MultipartFile representing the uploaded CSV file.
     * @return A redirect string to the service items listing page.
     * @throws CSVImportException If an error occurs during CSV import.
     */
    @PostMapping("/import")
    public String importCSV(@RequestParam("file") MultipartFile file)
            throws CSVImportException {
        serviceItemService.importCSV(file);
        return "redirect:/staff/service-items";
    }

    /**
     * Creates a new service item or updates an existing one.
     *
     * @param serviceItem The ServiceItem object containing the data to be created or updated.
     * @param actionType A string indicating whether the action is 'create' or 'update'.
     * @param bindingResult The BindingResult object for validation errors.
     * @param model The Spring UI model.
     * @return A redirect string to the service items listing page if successful, or back to the form with errors.
     */
    @PostMapping("/create")
    public String createOrUpdate(@ModelAttribute ServiceItem serviceItem,
            @RequestParam(defaultValue = "create") String actionType,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            return list(0, null, null, null, model);
        }

        if ("update".equals(actionType) && serviceItem.getId() != null) {
            serviceItemService.update(serviceItem.getId(), serviceItem);
        } else {
            serviceItemService.create(serviceItem);
        }
        return "redirect:/staff/service-items";
    }

    /**
     * Updates an existing service item.
     *
     * @param id The UUID of the service item to update.
     * @param serviceItem The ServiceItem object containing the updated data.
     * @param bindingResult The BindingResult object for validation errors.
     * @param model The Spring UI model.
     * @return A redirect string to the service items listing page if successful, or back to the form with errors.
     */
    @PostMapping("/update/{id}")
    public String update(@PathVariable UUID id,
            @ModelAttribute ServiceItem serviceItem,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            return list(0, null, null, null, model);
        }
        serviceItemService.update(id, serviceItem);
        return "redirect:/staff/service-items";
    }

    /**
     * Searches for service items based on a query and returns a paginated result.
     * This endpoint is typically used for AJAX requests.
     *
     * @param query An optional search query to filter service items.
     * @param page The current page number (0-indexed).
     * @param size The number of items per page. If null, uses the default page size.
     * @return A Page object containing the matching ServiceItem entities.
     */
    @GetMapping("/search")
    @ResponseBody
    public Page<ServiceItem> searchServiceItems(
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