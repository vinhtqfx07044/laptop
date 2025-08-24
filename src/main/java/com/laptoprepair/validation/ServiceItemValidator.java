package com.laptoprepair.validation;

import com.laptoprepair.exception.CSVImportException;
import com.laptoprepair.exception.ValidationException;
import com.laptoprepair.repository.ServiceItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Validator for ServiceItem entities.
 * Provides methods to validate service item properties, especially for uniqueness and CSV import data.
 */
@Component
@RequiredArgsConstructor
public class ServiceItemValidator {
    private final ServiceItemRepository repo;

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
        if (file.isEmpty()) {
            throw new CSVImportException("File CSV trống hoặc không hợp lệ");
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