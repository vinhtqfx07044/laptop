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
    public ResponseEntity<byte[]> export() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=service-items.csv")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(serviceItemService.exportCSV());
    }

    @PostMapping("/import")
    public String importCSV(@RequestParam("file") MultipartFile file)
            throws CSVImportException {
        serviceItemService.importCSV(file);
        return "redirect:/staff/service-items";
    }

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