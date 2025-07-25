package com.laptoprepair.service.impl;

import com.laptoprepair.entity.ServiceItem;
import com.laptoprepair.exception.CSVImportException;
import com.laptoprepair.exception.NotFoundException;
import com.laptoprepair.repository.ServiceItemRepository;
import com.laptoprepair.service.ServiceItemService;
import com.laptoprepair.validation.ServiceItemValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ServiceItemServiceImpl implements ServiceItemService {

    private final ServiceItemRepository serviceItemRepository;
    private final ServiceItemValidator serviceItemValidator;

    @Override
    public ServiceItem create(ServiceItem serviceItem) {
        serviceItemValidator.validateUniqueNameOnCreate(serviceItem.getName());
        return serviceItemRepository.save(serviceItem);
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceItem findById(UUID id) {
        return serviceItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dịch vụ với ID: " + id));
    }

    @Override
    public ServiceItem update(UUID id, ServiceItem incomingServiceItem) {
        ServiceItem existingServiceItem = findById(id);

        serviceItemValidator.validateUniqueNameOnUpdate(id, incomingServiceItem.getName());

        // Update fields directly
        existingServiceItem.setName(incomingServiceItem.getName());
        existingServiceItem.setPrice(incomingServiceItem.getPrice());
        existingServiceItem.setVatRate(incomingServiceItem.getVatRate());
        existingServiceItem.setWarrantyDays(incomingServiceItem.getWarrantyDays());
        existingServiceItem.setActive(incomingServiceItem.isActive());

        return serviceItemRepository.save(existingServiceItem);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceItem> list(String keyword, Boolean activeOnly, Pageable pageable) {
        String trimmedKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        return serviceItemRepository.findWithFilters(trimmedKeyword, activeOnly, pageable);
    }

    @Override
    public void importCSV(MultipartFile file) throws CSVImportException {
        serviceItemValidator.validateCSVFile(file);

        List<ServiceItem> serviceItemsToBeSaved = new ArrayList<>();

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
                CSVParser parser = new CSVParser(reader, CSVFormat.Builder.create()
                        .setHeader("name", "price", "vatRate", "warrantyDays", "active")
                        .setSkipHeaderRecord(true).build())) {

            int rowNumber = 1;
            for (CSVRecord csvRecord : parser) {
                rowNumber++;
                ServiceItem serviceItem = parseCSVRecord(csvRecord, rowNumber);
                serviceItemRepository.findByName(serviceItem.getName()).ifPresent(existing -> {
                    serviceItem.setId(existing.getId());
                    serviceItem.setCreatedAt(existing.getCreatedAt());
                });
                serviceItemsToBeSaved.add(serviceItem);
            }

            serviceItemRepository.saveAll(serviceItemsToBeSaved);

        } catch (CSVImportException e) {
            throw e;
        } catch (IOException e) {
            throw new CSVImportException("Lỗi đọc file CSV: " + e.getMessage());
        } catch (Exception e) {
            throw new CSVImportException("Lỗi không xác định khi import CSV: " + e.getMessage());
        }
    }

    private ServiceItem parseCSVRecord(CSVRecord csvRecord, int rowNumber) throws CSVImportException {
        ServiceItem serviceItem = new ServiceItem();

        try {
            // Parse name
            String name = csvRecord.get("name");
            serviceItemValidator.validateCSVName(name, rowNumber);
            serviceItem.setName(name.trim());

            // Parse price
            BigDecimal price = new BigDecimal(csvRecord.get("price"));
            serviceItemValidator.validateCSVPrice(price, rowNumber);
            serviceItem.setPrice(price);

            // Parse vatRate
            BigDecimal vatRate = new BigDecimal(csvRecord.get("vatRate"));
            serviceItemValidator.validateCSVVatRate(vatRate, rowNumber);
            serviceItem.setVatRate(vatRate);

            // Parse warrantyDays
            int warrantyDays = Integer.parseInt(csvRecord.get("warrantyDays"));
            serviceItemValidator.validateCSVWarrantyDays(warrantyDays, rowNumber);
            serviceItem.setWarrantyDays(warrantyDays);

            // Parse active (default to true if not specified or invalid)
            String activeStr = csvRecord.get("active");
            serviceItem.setActive(parseActiveField(activeStr));

            return serviceItem;
        } catch (NumberFormatException e) {
            throw new CSVImportException("Dữ liệu số không hợp lệ", rowNumber);
        } catch (IllegalArgumentException e) {
            throw new CSVImportException("Cột không tồn tại trong file CSV", rowNumber);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportCSV() {
        try {
            StringWriter stringWriter = new StringWriter();
            try (CSVPrinter csvPrinter = new CSVPrinter(stringWriter,
                    CSVFormat.Builder.create()
                            .setHeader("name", "price", "vatRate", "warrantyDays", "active")
                            .build())) {
                for (ServiceItem serviceItem : serviceItemRepository.findAll()) {
                    csvPrinter.printRecord(
                            serviceItem.getName(),
                            serviceItem.getPrice(),
                            serviceItem.getVatRate(),
                            serviceItem.getWarrantyDays(),
                            serviceItem.isActive());
                }
            }

            // Convert to UTF-8 bytes with BOM for proper Excel display
            return addUtf8Bom(stringWriter.toString());
        } catch (IOException e) {
            return "Error exporting".getBytes(StandardCharsets.UTF_8);
        }
    }

    private boolean parseActiveField(String activeStr) {
        if (activeStr != null && !activeStr.trim().isEmpty()) {
            return Boolean.parseBoolean(activeStr.trim());
        }
        return true; // Default to active
    }

    private byte[] addUtf8Bom(String content) {
        byte[] bom = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF }; // UTF-8 BOM
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[bom.length + contentBytes.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(contentBytes, 0, result, bom.length, contentBytes.length);
        return result;
    }

}