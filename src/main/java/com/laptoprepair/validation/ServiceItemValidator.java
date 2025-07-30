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

    /**
     * Validates that the service item name is unique during creation.
     * @param name The name of the service item to validate.
     * @throws ValidationException if a service item with the same name already exists.
     */
    public void validateUniqueNameOnCreate(String name) {
        if (repo.findByName(name).isPresent()) {
            throw new ValidationException("Tên dịch vụ đã tồn tại. Vui lòng chọn tên khác");
        }
    }

    /**
     * Validates that the service item name is unique during update, excluding the current item's ID.
     * @param id The ID of the service item being updated.
     * @param name The new name of the service item to validate.
     * @throws ValidationException if another service item with the same name already exists.
     */
    public void validateUniqueNameOnUpdate(UUID id, String name) {
        if (repo.existsByNameAndIdNot(name, id)) {
            throw new ValidationException("Tên dịch vụ đã tồn tại. Vui lòng chọn tên khác");
        }
    }

    /**
     * Validates the uploaded CSV file for import.
     * @param file The MultipartFile representing the CSV file.
     * @throws CSVImportException if the file is empty or invalid.
     */
    public void validateCSVFile(MultipartFile file) throws CSVImportException {
        if (file.isEmpty()) {
            throw new CSVImportException("File CSV trống hoặc không hợp lệ");
        }
    }

    /**
     * Validates the name field from a CSV record.
     * @param name The name string from the CSV.
     * @param rowNumber The row number in the CSV file where the data is located.
     * @throws CSVImportException if the name is null or empty.
     */
    public void validateCSVName(String name, int rowNumber) throws CSVImportException {
        if (name == null || name.trim().isEmpty()) {
            throw new CSVImportException("Tên dịch vụ không được để trống", rowNumber, "name");
        }
    }

    /**
     * Validates the price field from a CSV record.
     * @param price The BigDecimal price value from the CSV.
     * @param rowNumber The row number in the CSV file.
     * @throws CSVImportException if the price is less than or equal to 0.
     */
    public void validateCSVPrice(BigDecimal price, int rowNumber) throws CSVImportException {
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CSVImportException("Giá dịch vụ phải lớn hơn 0", rowNumber, "price");
        }
    }

    /**
     * Validates the VAT rate field from a CSV record.
     * @param vatRate The BigDecimal VAT rate value from the CSV.
     * @param rowNumber The row number in the CSV file.
     * @throws CSVImportException if the VAT rate is negative.
     */
    public void validateCSVVatRate(BigDecimal vatRate, int rowNumber) throws CSVImportException {
        if (vatRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new CSVImportException("Thuế VAT không được là số âm", rowNumber, "vatRate");
        }
    }

    /**
     * Validates the warranty days field from a CSV record.
     * @param warrantyDays The integer value for warranty days from the CSV.
     * @param rowNumber The row number in the CSV file.
     * @throws CSVImportException if the warranty days value is negative.
     */
    public void validateCSVWarrantyDays(int warrantyDays, int rowNumber) throws CSVImportException {
        if (warrantyDays < 0) {
            throw new CSVImportException("Số ngày bảo hành không được là số âm", rowNumber, "warrantyDays");
        }
    }
}