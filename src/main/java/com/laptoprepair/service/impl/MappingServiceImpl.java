package com.laptoprepair.service.impl;

import com.laptoprepair.entity.Request;
import com.laptoprepair.entity.RequestItem;
import com.laptoprepair.entity.RequestHistory;
import com.laptoprepair.entity.RequestImage;
import com.laptoprepair.entity.ServiceItem;
import com.laptoprepair.exception.NotFoundException;
import com.laptoprepair.exception.ValidationException;
import com.laptoprepair.repository.ServiceItemRepository;
import com.laptoprepair.service.MappingService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MappingServiceImpl implements MappingService {

    private final ServiceItemRepository serviceItemRepository;

    @Override
    public void snapshotServiceItems(List<RequestItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        
        for (RequestItem item : items) {          
            ServiceItem serviceItem = serviceItemRepository.findByIdAndActive(item.getServiceItemId())
                    .orElseThrow(() -> new NotFoundException("Không tìm dịch vụ sửa chửa: " + item.getName()));
            
            item.setName(serviceItem.getName());
            item.setPrice(serviceItem.getPrice());
            item.setVatRate(serviceItem.getVatRate());
            
            if (item.getDiscount().compareTo(item.getPrice()) > 0) {
                throw new ValidationException("Giảm giá vượt quá giá gốc: " + item.getName());
            }
        }
    }

    @Override
    public Request copyRequestFields(Request target, Request source, boolean deepCopyCollections) {
        target.setName(source.getName());
        target.setPhone(source.getPhone());
        target.setEmail(source.getEmail());
        target.setAddress(source.getAddress());
        target.setBrandModel(source.getBrandModel());
        target.setAppointmentDate(source.getAppointmentDate());
        target.setDescription(source.getDescription());
        target.setStatus(source.getStatus());
        
        if (deepCopyCollections) {
            // For cloning - deep copy collections to avoid reference issues
            if (source.getItems() != null) {
                target.setItems(source.getItems().stream()
                    .map(item -> {
                        RequestItem newItem = new RequestItem();
                        newItem.setServiceItemId(item.getServiceItemId());
                        newItem.setName(item.getName());
                        newItem.setPrice(item.getPrice());
                        newItem.setVatRate(item.getVatRate());
                        newItem.setWarrantyDays(item.getWarrantyDays());
                        newItem.setQuantity(item.getQuantity());
                        newItem.setDiscount(item.getDiscount());
                        newItem.setRequest(target);
                        return newItem;
                    }).toList());
            }
            if (source.getImages() != null) {
                target.setImages(source.getImages().stream()
                    .map(image -> {
                        RequestImage newImage = new RequestImage();
                        newImage.setFilename(image.getFilename());
                        newImage.setRequest(target);
                        return newImage;
                    }).toList());
            }
            if (source.getHistory() != null) {
                target.setHistory(source.getHistory().stream()
                    .map(history -> {
                        RequestHistory newHistory = new RequestHistory();
                        newHistory.setChanges(history.getChanges());
                        newHistory.setCreatedAt(history.getCreatedAt());
                        newHistory.setCreatedBy(history.getCreatedBy());
                        newHistory.setRequest(target);
                        return newHistory;
                    }).toList());
            }
        } else {
            // For updating - proper collection management to avoid orphan removal issues
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
        
        return target;
    }
}