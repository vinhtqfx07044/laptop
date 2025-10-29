package com.laptoprepair.service.impl;

import com.laptoprepair.entity.ServiceItem;
import com.laptoprepair.exception.NotFoundException;
import com.laptoprepair.exception.ValidationException;
import com.laptoprepair.repository.ServiceItemRepository;

import com.laptoprepair.service.ServiceItemService;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ServiceItemServiceImpl implements ServiceItemService {

    private static final String NAME_FIELD = "name";
    private static final String PRICE_FIELD = "price";
    private static final String VAT_RATE_FIELD = "vatRate";
    private static final String WARRANTY_DAYS_FIELD = "warrantyDays";
    private static final String ACTIVE_FIELD = "active";

    // Accepted CSV MIME types
    private static final List<String> ACCEPTED_CSV_MIME_TYPES = Arrays.asList(
            "text/csv", "application/csv", "text/plain");

    // Maximum CSV file size (5MB)
    private static final long MAX_CSV_FILE_SIZE = 5_000_000L;

    private final ServiceItemRepository serviceItemRepository;

    /**
     * Creates a new service item.
     * 
     * @param serviceItem The ServiceItem object to create.
     * @return The created ServiceItem.
     */
    @Override
    public ServiceItem create(ServiceItem serviceItem) {
        // Validate unique name on create
        if (serviceItemRepository.findByName(serviceItem.getName()).isPresent()) {
            throw new ValidationException("Tên dịch vụ đã tồn tại. Vui lòng chọn tên khác");
        }
        return serviceItemRepository.save(serviceItem);
    }

    /**
     * Finds a service item by its ID.
     * 
     * @param id The UUID of the service item to find.
     * @return The found ServiceItem.
     * @throws NotFoundException if the service item with the given ID is not found.
     */
    @Override
    @Transactional(readOnly = true)
    public ServiceItem findById(@NonNull UUID id) {
        return serviceItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dịch vụ với ID: " + id));
    }

    /**
     * Updates an existing service item.
     * 
     * @param id                  The UUID of the service item to update.
     * @param incomingServiceItem The ServiceItem object with updated details.
     * @return The updated ServiceItem.
     */
    @Override
    public ServiceItem update(@NonNull UUID id, ServiceItem incomingServiceItem) {
        ServiceItem existingServiceItem = loadAndValidateServiceItem(id);

        // Validate unique name on update
        if (serviceItemRepository.existsByNameAndIdNot(incomingServiceItem.getName(), id)) {
            throw new ValidationException("Tên dịch vụ đã tồn tại. Vui lòng chọn tên khác");
        }

        // Update fields directly
        existingServiceItem.setName(incomingServiceItem.getName());
        existingServiceItem.setPrice(incomingServiceItem.getPrice());
        existingServiceItem.setVatRate(incomingServiceItem.getVatRate());
        existingServiceItem.setWarrantyDays(incomingServiceItem.getWarrantyDays());
        existingServiceItem.setActive(incomingServiceItem.isActive());

        return serviceItemRepository.save(existingServiceItem);
    }

    /**
     * Retrieves a paginated list of service items based on a keyword and active
     * status.
     * 
     * @param keyword    Optional keyword to filter service items by name.
     * @param activeOnly Optional boolean to filter for active service items only.
     * @param pageable   Pagination information.
     * @return A Page of ServiceItem entities.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ServiceItem> list(String keyword, Boolean activeOnly, Pageable pageable) {
        String trimmedKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        return serviceItemRepository.findWithFilters(trimmedKeyword, activeOnly, pageable);
    }

    /**
     * Imports service items from a CSV file.
     * 
     * @param file The MultipartFile representing the CSV file to import.
     * @throws ValidationException if there is an error during CSV parsing or data
     *                             validation.
     */
    @Override
    public void importCSV(MultipartFile file) throws ValidationException {
        // Validate CSV file
        if (file == null || file.isEmpty()) {
            throw new ValidationException("File CSV trống hoặc không hợp lệ");
        }

        validateFileExtension(file);
        validateFileMimeType(file);
        validateFileSize(file);
        validateCSVStructure(file);

        List<ServiceItem> serviceItemsToBeSaved = new ArrayList<>();

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
                CSVParser parser = CSVParser.parse(reader, CSVFormat.DEFAULT
                        .builder()
                        .setHeader(NAME_FIELD, PRICE_FIELD, VAT_RATE_FIELD, WARRANTY_DAYS_FIELD, ACTIVE_FIELD)
                        .setSkipHeaderRecord(true)
                        .get())) {

            int rowNumber = 1;
            for (CSVRecord csvRecord : parser) {
                rowNumber++;
                ServiceItem serviceItem = copyCSVRecordFields(csvRecord, rowNumber);
                serviceItemRepository.findByName(serviceItem.getName()).ifPresent(existing -> {
                    serviceItem.setId(existing.getId());
                    serviceItem.setCreatedAt(existing.getCreatedAt());
                });
                serviceItemsToBeSaved.add(serviceItem);
            }

            serviceItemRepository.saveAll(serviceItemsToBeSaved);

        } catch (ValidationException e) {
            throw e;
        } catch (IOException e) {
            throw new ValidationException("Lỗi đọc file CSV: " + e.getMessage());
        } catch (Exception e) {
            throw new ValidationException("Lỗi không xác định khi import CSV: " + e.getMessage());
        }
    }

    /**
     * Exports all service items to a CSV file.
     * 
     * @return A byte array representing the CSV file content.
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] exportCSV() {
        try {
            StringWriter stringWriter = new StringWriter();
            try (CSVPrinter csvPrinter = new CSVPrinter(stringWriter,
                    CSVFormat.DEFAULT
                            .builder()
                            .setHeader(NAME_FIELD, PRICE_FIELD, VAT_RATE_FIELD, WARRANTY_DAYS_FIELD, ACTIVE_FIELD)
                            .get())) {
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

    private byte[] addUtf8Bom(String content) {
        byte[] bom = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF }; // UTF-8 BOM
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[bom.length + contentBytes.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(contentBytes, 0, result, bom.length, contentBytes.length);
        return result;
    }

    private ServiceItem copyCSVRecordFields(CSVRecord csvRecord, int rowNumber) throws ValidationException {
        ServiceItem serviceItem = new ServiceItem();

        try {
            // Parse name
            String name = csvRecord.get(NAME_FIELD);
            if (name == null || name.trim().isEmpty()) {
                throw new ValidationException("Tên dịch vụ không được để trống (dòng " + rowNumber + ")");
            }
            serviceItem.setName(name.trim());

            // Parse price
            BigDecimal price = new BigDecimal(csvRecord.get(PRICE_FIELD));
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Giá dịch vụ phải lớn hơn 0 (dòng " + rowNumber + ")");
            }
            serviceItem.setPrice(price);

            // Parse vatRate
            BigDecimal vatRate = new BigDecimal(csvRecord.get(VAT_RATE_FIELD));
            if (vatRate.compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("Thuế VAT không được là số âm (dòng " + rowNumber + ")");
            }
            serviceItem.setVatRate(vatRate);

            // Parse warrantyDays
            int warrantyDays = Integer.parseInt(csvRecord.get(WARRANTY_DAYS_FIELD));
            if (warrantyDays < 0) {
                throw new ValidationException("Số ngày bảo hành không được là số âm (dòng " + rowNumber + ")");
            }
            serviceItem.setWarrantyDays(warrantyDays);

            // Parse active (default to true if not specified or invalid)
            String activeStr = csvRecord.get(ACTIVE_FIELD);
            serviceItem.setActive(
                    activeStr == null || activeStr.trim().isEmpty() || Boolean.parseBoolean(activeStr.trim()));

            return serviceItem;
        } catch (NumberFormatException e) {
            throw new ValidationException("Dữ liệu số không hợp lệ (dòng " + rowNumber + ")");
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Cột không tồn tại trong file CSV (dòng " + rowNumber + ")");
        }
    }

    /**
     * Validates that the file has a .csv extension
     */
    private void validateFileExtension(MultipartFile file) throws ValidationException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new ValidationException("Tên file không hợp lệ");
        }

        String fileExtension = originalFilename.toLowerCase();
        if (!fileExtension.endsWith(".csv")) {
            throw new ValidationException("Chỉ chấp nhận file có định dạng .csv. File được chọn: " + originalFilename);
        }
    }

    /**
     * Validates the MIME content type of the file
     */
    private void validateFileMimeType(MultipartFile file) throws ValidationException {
        String contentType = file.getContentType();
        if (contentType == null || !ACCEPTED_CSV_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new ValidationException("Loại file không được hỗ trợ. Chỉ chấp nhận file CSV. Loại file hiện tại: " +
                    (contentType != null ? contentType : "không xác định"));
        }
    }

    /**
     * Validates the file size
     */
    private void validateFileSize(MultipartFile file) throws ValidationException {
        if (file.getSize() > MAX_CSV_FILE_SIZE) {
            throw new ValidationException("File CSV quá lớn. Kích thước tối đa cho phép: " +
                    (MAX_CSV_FILE_SIZE / 1_000_000) + "MB");
        }
    }

    /**
     * Validates the CSV file structure and headers using the same format as the
     * actual import
     */
    private void validateCSVStructure(MultipartFile file) throws ValidationException {
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
                CSVParser parser = CSVParser.parse(reader, CSVFormat.DEFAULT
                        .builder()
                        .setHeader(NAME_FIELD, PRICE_FIELD, VAT_RATE_FIELD, WARRANTY_DAYS_FIELD, ACTIVE_FIELD)
                        .setSkipHeaderRecord(true)
                        .get())) {

            // Try to read first data record to ensure file has data after header
            boolean hasDataRows = parser.iterator().hasNext();
            if (!hasDataRows) {
                throw new ValidationException(
                        "File CSV không có dữ liệu. Vui lòng thêm ít nhất một dòng dữ liệu sau header.");
            }

            // Try to access each expected field from first record to validate header
            // structure
            CSVRecord firstRecord = parser.iterator().next();
            validateCSVHeaders(firstRecord);

        } catch (IOException e) {
            throw new ValidationException("Lỗi đọc file CSV: " + e.getMessage());
        } catch (ValidationException e) {
            // Re-throw our custom exceptions
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Định dạng file CSV không hợp lệ: " + e.getMessage());
        }
    }

    private ServiceItem loadAndValidateServiceItem(@NonNull UUID id) {
        return serviceItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dịch vụ với ID: " + id));
    }

    private void validateCSVHeaders(CSVRecord firstRecord) throws ValidationException {
        try {
            firstRecord.get(NAME_FIELD);
            firstRecord.get(PRICE_FIELD);
            firstRecord.get(VAT_RATE_FIELD);
            firstRecord.get(WARRANTY_DAYS_FIELD);
            firstRecord.get(ACTIVE_FIELD);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("File CSV thiếu hoặc sai tên cột. " +
                    "Các cột bắt buộc phải có tên chính xác: " +
                    String.join(", ", NAME_FIELD, PRICE_FIELD, VAT_RATE_FIELD, WARRANTY_DAYS_FIELD, ACTIVE_FIELD));
        }
    }
}