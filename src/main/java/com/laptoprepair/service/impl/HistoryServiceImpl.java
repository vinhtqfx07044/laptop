package com.laptoprepair.service.impl;

import com.laptoprepair.entity.Request;
import com.laptoprepair.entity.RequestHistory;
import com.laptoprepair.entity.RequestItem;
import com.laptoprepair.service.HistoryService;
import com.laptoprepair.common.TimeProvider;
import com.laptoprepair.web.AuthService;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class HistoryServiceImpl implements HistoryService {

    private final TimeProvider timeProvider;
    private final AuthService authService;

    @Override
    public void addHistory(Request request, String changes, String user) {
        RequestHistory history = new RequestHistory();
        String finalChanges = (changes != null && changes.length() > 500) ? changes.substring(0, 500) + "..." : changes;

        history.setChanges(finalChanges);
        history.setCreatedAt(timeProvider.now());
        history.setCreatedBy(user);
        history.setRequest(request);

        request.getHistory().add(history);
    }

    @Override
    public String computeChanges(Request oldRequest, Request newRequest) {
        StringBuilder changes = new StringBuilder();

        // Track status changes
        if (!oldRequest.getStatus().equals(newRequest.getStatus())) {
            changes.append("Trạng thái: " + oldRequest.getStatus() + " → " + newRequest.getStatus() + "\n");
        }

        // Track appointment date changes
        if (!oldRequest.getAppointmentDate().equals(newRequest.getAppointmentDate())) {
            changes.append(
                    "Ngày hẹn: " + oldRequest.getAppointmentDate() + " → " + newRequest.getAppointmentDate() + "\n");
        }

        // Track request items changes
        if (!areItemsEqual(oldRequest.getItems(), newRequest.getItems())) {
            changes.append("Cập nhật hạng mục sửa chữa\n");
        }

        // Track total price changes
        if (oldRequest.getTotal().compareTo(newRequest.getTotal()) != 0) {
            String priceChange = "Tổng tiền: " + String.format("%,.0f", oldRequest.getTotal())
                    + " → " + String.format("%,.0f", newRequest.getTotal()) + " VND\n";
            changes.append(priceChange);
        }

        return changes.toString().trim();
    }

    @Override
    public String getCurrentUser() {
        return authService.currentUser();
    }

    @Override
    public boolean areItemsEqual(List<RequestItem> oldItems, List<RequestItem> newItems) {
        if (oldItems == null || newItems == null)
            return false;

        try {
            // Sort both lists by serviceItemId and compare using toString()
            String oldItemsString = oldItems.stream()
                    .sorted((item1, item2) -> item1.getServiceItemId().compareTo(item2.getServiceItemId()))
                    .map(RequestItem::toString)
                    .collect(Collectors.joining(","));

            String newItemsString = newItems.stream()
                    .sorted((item1, item2) -> item1.getServiceItemId().compareTo(item2.getServiceItemId()))
                    .map(RequestItem::toString)
                    .collect(Collectors.joining(","));

            return oldItemsString.equals(newItemsString);
        } catch (Exception e) {
            log.error("Error comparing items", e);
            return false;
        }
    }

}