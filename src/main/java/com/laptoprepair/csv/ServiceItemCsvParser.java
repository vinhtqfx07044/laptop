package com.laptoprepair.csv;

import com.laptoprepair.entity.ServiceItem;
import com.laptoprepair.exception.CSVImportException;
import com.laptoprepair.validation.ServiceItemValidator;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class ServiceItemCsvParser {

    private final ServiceItemValidator serviceItemValidator;

    public ServiceItem parse(CSVRecord csvRecord, int rowNumber) throws CSVImportException {
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

    private boolean parseActiveField(String activeStr) {
        if (activeStr != null && !activeStr.trim().isEmpty()) {
            return Boolean.parseBoolean(activeStr.trim());
        }
        return true; // Default to active
    }
}