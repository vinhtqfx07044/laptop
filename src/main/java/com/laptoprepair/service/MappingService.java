package com.laptoprepair.service;

import com.laptoprepair.entity.Request;
import com.laptoprepair.entity.RequestItem;
import java.util.List;

public interface MappingService {
    
    /**
     * Creates a deep copy of Request for change tracking
     */
    Request copyRequestFields(Request target, Request source, boolean deepCopyCollections);
    
    /**
     * Snapshots current service item data into request items
     * This ensures price/name consistency even if master data changes
     */
    void snapshotServiceItems(List<RequestItem> items);
}