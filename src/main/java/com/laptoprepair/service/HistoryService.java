package com.laptoprepair.service;

import java.util.List;

import com.laptoprepair.entity.Request;
import com.laptoprepair.entity.RequestItem;

public interface HistoryService {
    void addRequestHistoryRecord(Request request, String note, String user);

    String computeRequestChanges(Request oldRequest, Request newRequest);

    boolean areRequestItemsEqual(List<RequestItem> oldItems, List<RequestItem> newItems);
}