package com.laptoprepair.validation;

import com.laptoprepair.exception.CSVImportException;
import com.laptoprepair.exception.ValidationException;
import com.laptoprepair.repository.ServiceItemRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Validator for ServiceItem entities.
 * Provides methods to validate service item properties, especially for
 * uniqueness and CSV import data.
 */
@Component
@RequiredArgsConstructor
public class ServiceItemValidator {
    private final ServiceItemRepository repo;
    
    // Expected CSV headers
    private static final List<String> EXPECTED_CSV_HEADERS = Arrays.asList(
        "name", "price", "vatRate", "warrantyDays", "active"
    );
    
    // Accepted CSV MIME types
    private static final List<String> ACCEPTED_CSV_MIME_TYPES = Arrays.asList(
        "text/csv", "application/csv", "text/plain"
    );
    
    // Maximum CSV file size (5MB)
    private static final long MAX_CSV_FILE_SIZE = 5_000_000L;

    public void validateUniqueNameOnCreate(String name) {
        if (repo.findByName(name).isPresent()) {
            throw new ValidationException("Tên dịch vụ đã tồn tại. Vui lòng chọn tên khác");
        }
    }

    public void validateUniqueNameOnUpdate(UUID id, String name) {
        if (repo.existsByNameAndIdNot(name, id)) {
            throw new ValidationException("Tên dịch vụ đã tồn tại. Vui lòng chọn tên khác");
        }
    }

    public void validateCSVFile(MultipartFile file) throws CSVImportException {
        if (file == null || file.isEmpty()) {
            throw new CSVImportException("File CSV trống hoặc không hợp lệ");
        }
        
        validateFileExtension(file);
        validateFileMimeType(file);
        validateFileSize(file);
        validateCSVStructure(file);
    }
    
    /**
     * Validates that the file has a .csv extension
     */
    private void validateFileExtension(MultipartFile file) throws CSVImportException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new CSVImportException("Tên file không hợp lệ");
        }
        
        String fileExtension = originalFilename.toLowerCase();
        if (!fileExtension.endsWith(".csv")) {
            throw new CSVImportException("Chỉ chấp nhận file có định dạng .csv. File được chọn: " + originalFilename);
        }
    }
    
    /**
     * Validates the MIME content type of the file
     */
    private void validateFileMimeType(MultipartFile file) throws CSVImportException {
        String contentType = file.getContentType();
        if (contentType == null || !ACCEPTED_CSV_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new CSVImportException("Loại file không được hỗ trợ. Chỉ chấp nhận file CSV. Loại file hiện tại: " +
                (contentType != null ? contentType : "không xác định"));
        }
    }
    
    /**
     * Validates the file size
     */
    private void validateFileSize(MultipartFile file) throws CSVImportException {
        if (file.getSize() > MAX_CSV_FILE_SIZE) {
            throw new CSVImportException("File CSV quá lớn. Kích thước tối đa cho phép: " +
                (MAX_CSV_FILE_SIZE / 1_000_000) + "MB");
        }
    }
    
    /**
     * Validates the CSV file structure and headers using the same format as the actual import
     */
    private void validateCSVStructure(MultipartFile file) throws CSVImportException {
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, CSVFormat.Builder.create()
                     .setHeader("name", "price", "vatRate", "warrantyDays", "active")
                     .setSkipHeaderRecord(true)
                     .build())) {
            
            // Try to read first data record to ensure file has data after header
            boolean hasDataRows = parser.iterator().hasNext();
            if (!hasDataRows) {
                throw new CSVImportException("File CSV không có dữ liệu. Vui lòng thêm ít nhất một dòng dữ liệu sau header.");
            }
            
            // Try to access each expected field from first record to validate header structure
            CSVRecord firstRecord = parser.iterator().next();
            try {
                firstRecord.get("name");
                firstRecord.get("price");
                firstRecord.get("vatRate");
                firstRecord.get("warrantyDays");
                firstRecord.get("active");
            } catch (IllegalArgumentException e) {
                throw new CSVImportException("File CSV thiếu hoặc sai tên cột. " +
                    "Các cột bắt buộc phải có tên chính xác: name, price, vatRate, warrantyDays, active");
            }
            
        } catch (IOException e) {
            throw new CSVImportException("Lỗi đọc file CSV: " + e.getMessage());
        } catch (CSVImportException e) {
            // Re-throw our custom exceptions
            throw e;
        } catch (Exception e) {
            throw new CSVImportException("Định dạng file CSV không hợp lệ: " + e.getMessage());
        }
    }

    public void validateCSVName(String name, int rowNumber) throws CSVImportException {
        if (name == null || name.trim().isEmpty()) {
            throw new CSVImportException("Tên dịch vụ không được để trống", rowNumber, "name");
        }
    }

    public void validateCSVPrice(BigDecimal price, int rowNumber) throws CSVImportException {
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CSVImportException("Giá dịch vụ phải lớn hơn 0", rowNumber, "price");
        }
    }

    public void validateCSVVatRate(BigDecimal vatRate, int rowNumber) throws CSVImportException {
        if (vatRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new CSVImportException("Thuế VAT không được là số âm", rowNumber, "vatRate");
        }
    }

    public void validateCSVWarrantyDays(int warrantyDays, int rowNumber) throws CSVImportException {
        if (warrantyDays < 0) {
            throw new CSVImportException("Số ngày bảo hành không được là số âm", rowNumber, "warrantyDays");
        }
    }
}