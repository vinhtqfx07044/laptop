package com.laptoprepair.service.impl;

import com.laptoprepair.entity.Request;
import com.laptoprepair.entity.RequestItem;
import com.laptoprepair.entity.RequestHistory;
import com.laptoprepair.entity.RequestImage;
import com.laptoprepair.entity.ServiceItem;
import com.laptoprepair.exception.CSVImportException;
import com.laptoprepair.exception.NotFoundException;
import com.laptoprepair.exception.ValidationException;
import com.laptoprepair.repository.ServiceItemRepository;
import com.laptoprepair.service.MappingService;
import com.laptoprepair.utils.AppConstants;
import com.laptoprepair.validation.ServiceItemValidator;

import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MappingServiceImpl implements MappingService {

    private final ServiceItemRepository serviceItemRepository;
    private final ServiceItemValidator serviceItemValidator;

    @Override
    public void snapshotServiceItems(List<RequestItem> items) {
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
            // For cloning - deep copy collections to avoid reference issues
            copyCollectionsDeep(source, target);
        } else {
            // For updating - proper collection management to avoid orphan removal issues
            copyCollectionsShallow(source, target);
        }

        return target;
    }

    private void copyCollectionsDeep(Request source, Request target) {
        if (source.getItems() != null) {
            target.setItems(source.getItems().stream()
                    .map(item -> copyRequestItem(item, target))
                    .toList());
        }
        if (source.getImages() != null) {
            target.setImages(source.getImages().stream()
                    .map(image -> copyRequestImage(image, target))
                    .toList());
        }
        if (source.getHistory() != null) {
            target.setHistory(source.getHistory().stream()
                    .map(history -> copyRequestHistory(history, target))
                    .toList());
        }
    }

    private void copyCollectionsShallow(Request source, Request target) {
        if (source.getItems() != null) {
            target.getItems().clear();
            source.getItems().forEach(item -> {
                item.setRequest(target);
                target.getItems().add(item);
            });
        }
        if (source.getImages() != null) {
            target.getImages().clear();
            source.getImages().forEach(image -> {
                image.setRequest(target);
                target.getImages().add(image);
            });
        }
    }

    private RequestItem copyRequestItem(RequestItem source, Request target) {
        RequestItem newItem = new RequestItem();
        BeanUtils.copyProperties(source, newItem, "id", AppConstants.ATTR_REQUEST);
        newItem.setRequest(target);
        return newItem;
    }

    private RequestImage copyRequestImage(RequestImage source, Request target) {
        RequestImage newImage = new RequestImage();
        BeanUtils.copyProperties(source, newImage, "id", "request");
        newImage.setRequest(target);
        return newImage;
    }

    private RequestHistory copyRequestHistory(RequestHistory source, Request target) {
        RequestHistory newHistory = new RequestHistory();
        BeanUtils.copyProperties(source, newHistory, "id", "request");
        newHistory.setRequest(target);
        return newHistory;
    }

    public ServiceItem parseCSVRecord(CSVRecord csvRecord, int rowNumber) throws CSVImportException {
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