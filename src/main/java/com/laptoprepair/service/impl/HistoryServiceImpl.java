package com.laptoprepair.service.impl;

import com.laptoprepair.entity.Request;
import com.laptoprepair.entity.RequestHistory;
import com.laptoprepair.entity.RequestItem;
import com.laptoprepair.service.HistoryService;
import com.laptoprepair.utils.TimeProvider;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class HistoryServiceImpl implements HistoryService {

    private final TimeProvider timeProvider;

    @Override
    public void addRequestHistoryRecord(Request request, String changes, String user) {
        RequestHistory history = new RequestHistory();
        String finalChanges = (changes != null && changes.length() > 500) ? changes.substring(0, 500) + "..." : changes;

        history.setChanges(finalChanges);
        history.setCreatedAt(timeProvider.now());
        history.setCreatedBy(user);
        history.setRequest(request);

        request.getHistory().add(history);
    }

    @Override
    public String computeRequestChanges(Request oldRequest, Request newRequest) {
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
        if (!areRequestItemsEqual(oldRequest.getItems(), newRequest.getItems())) {
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
    public boolean areRequestItemsEqual(List<RequestItem> oldItems, List<RequestItem> newItems) {
        if (oldItems == null || newItems == null) {
            return false;
        }

        if (oldItems.size() != newItems.size()) {
            return false;
        }

        try {
            // Use Set comparison with custom equals() method
            return new HashSet<>(oldItems).equals(new HashSet<>(newItems));
        } catch (Exception e) {
            log.error("Error comparing items", e);
            return false;
        }
    }

}