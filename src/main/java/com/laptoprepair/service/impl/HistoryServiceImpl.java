package com.laptoprepair.service.impl;

import com.laptoprepair.entity.Request;
import com.laptoprepair.entity.RequestHistory;
import com.laptoprepair.entity.RequestItem;
import com.laptoprepair.service.HistoryService;
import com.laptoprepair.config.VietnamTimeProvider;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of the {@link HistoryService} interface.
 * Provides methods for adding request history records and computing changes
 * between request states.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HistoryServiceImpl implements HistoryService {

    private final VietnamTimeProvider vietnamTimeProvider;

    /**
     * Adds a new history record to a request.
     * 
     * @param request The Request object to which the history record will be added.
     * @param changes A string describing the changes or notes for this history
     *                entry.
     * @param user    The user who made the changes.
     */
    @Override
    public void addRequestHistoryRecord(Request request, String changes, String user) {
        RequestHistory history = new RequestHistory();
        String finalChanges = (changes != null && changes.length() > 500) ? changes.substring(0, 500) + "..." : changes;

        history.setChanges(finalChanges);
        history.setCreatedAt(vietnamTimeProvider.now());
        history.setCreatedBy(user);
        history.setRequest(request);

        request.getHistory().add(history);
    }

    /**
     * Computes the differences between an old and a new Request object.
     * Generates a string summarizing changes in status, appointment date, request
     * items, and total price.
     * 
     * @param oldRequest The original Request object.
     * @param newRequest The updated Request object.
     * @return A string detailing the changes, or an empty string if no significant
     *         changes.
     */
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

    /**
     * Compares two lists of RequestItem objects for equality.
     * This comparison is based on the content of the items, not just their
     * references.
     * 
     * @param oldItems The list of old request items.
     * @param newItems The list of new request items.
     * @return true if the lists are equal in content, false otherwise.
     */
    @Override
    public boolean areRequestItemsEqual(List<RequestItem> oldItems, List<RequestItem> newItems) {
        if (oldItems == null || newItems == null) {
            return false;
        }

        if (oldItems.size() != newItems.size()) {
            return false;
        }

        try {
            return new HashSet<>(oldItems).equals(new HashSet<>(newItems));
        } catch (Exception e) {
            log.error("Error comparing items", e);
            return false;
        }
    }
}