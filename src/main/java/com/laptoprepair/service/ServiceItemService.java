package com.laptoprepair.service;

import com.laptoprepair.entity.ServiceItem;
import com.laptoprepair.exception.CSVImportException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Service interface for managing service items.
 * Provides methods for creating, retrieving, updating, listing, importing, and
 * exporting service items.
 */
public interface ServiceItemService {
    ServiceItem create(ServiceItem serviceItem);

    ServiceItem findById(UUID id);

    Page<ServiceItem> list(String keyword, Boolean activeOnly, Pageable pageable);

    ServiceItem update(UUID id, ServiceItem serviceItem);

    void importCSV(MultipartFile file) throws CSVImportException;

    byte[] exportCSV();
}