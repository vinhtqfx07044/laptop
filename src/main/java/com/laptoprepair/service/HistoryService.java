package com.laptoprepair.service;

import java.util.List;

import com.laptoprepair.entity.Request;
import com.laptoprepair.entity.RequestItem;

public interface HistoryService {
    void addHistory(Request request, String note, String user);

    String computeChanges(Request oldRequest, Request newRequest);

    String getCurrentUser();

    boolean areItemsEqual(List<RequestItem> oldItems, List<RequestItem> newItems);
}