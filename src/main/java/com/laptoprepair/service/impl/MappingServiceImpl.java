package com.laptoprepair.service.impl;

import com.laptoprepair.entity.Request;
import com.laptoprepair.entity.RequestItem;
import com.laptoprepair.entity.ServiceItem;
import com.laptoprepair.exception.CSVImportException;
import com.laptoprepair.exception.NotFoundException;
import com.laptoprepair.exception.ValidationException;
import com.laptoprepair.repository.ServiceItemRepository;
import com.laptoprepair.service.MappingService;
import com.laptoprepair.validation.ServiceItemValidator;

import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MappingServiceImpl implements MappingService {

    private final ServiceItemRepository serviceItemRepository;
    private final ServiceItemValidator serviceItemValidator;

    @Override
    public void copyServiceItemsFields(List<RequestItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        for (RequestItem item : items) {
            ServiceItem serviceItem = serviceItemRepository.findByIdAndActive(item.getServiceItemId())
                    .orElseThrow(() -> new NotFoundException("Không tìm dịch vụ sửa chửa: " + item.getName()));

            // Sử dụng BeanUtils để copy các thuộc tính từ ServiceItem sang RequestItem
            BeanUtils.copyProperties(serviceItem, item, "id", "serviceItemId", "active", "createdAt", "updatedAt");

            if (item.getDiscount().compareTo(item.getPrice()) > 0) {
                throw new ValidationException("Giảm giá vượt quá giá gốc: " + item.getName());
            }
        }
    }

    @Override
    public Request copyRequestFields(Request target, Request source, boolean deepCopyCollections) {
        // Copy tất cả thuộc tính đơn giản, loại trừ collections và các thuộc tính không
        // cần thiết
        BeanUtils.copyProperties(source, target, "id", "items", "images", "history",
                "createdAt", "updatedAt", "createdBy", "updatedBy");

        if (deepCopyCollections) {
            copyItemsForCloning(source, target);
        } else {
            updateItemsReference(source, target);
        }

        return target;
    }

    private void copyItemsForCloning(Request source, Request target) {
        if (source.getItems() == null) {
            target.setItems(null);
            return;
        }

        List<RequestItem> copiedItems = new ArrayList<>();
        for (RequestItem item : source.getItems()) {
            RequestItem newItem = new RequestItem();
            BeanUtils.copyProperties(item, newItem, "id", "request");
            newItem.setRequest(target);
            copiedItems.add(newItem);
        }
        target.setItems(copiedItems);
    }

    private void updateItemsReference(Request source, Request target) {
        // For updating - only change reference
        if (source.getItems() != null) {
            target.getItems().clear();
            source.getItems().forEach(item -> item.setRequest(target));
            target.getItems().addAll(source.getItems());
        }
        if (source.getImages() != null) {
            target.getImages().clear();
            source.getImages().forEach(image -> image.setRequest(target));
            target.getImages().addAll(source.getImages());
        }
    }

    public ServiceItem copyCSVRecordFields(CSVRecord csvRecord, int rowNumber) throws CSVImportException {
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
        return activeStr == null || activeStr.trim().isEmpty() || Boolean.parseBoolean(activeStr.trim());
    }
}