package com.laptoprepair.service;

import com.laptoprepair.entity.Request;
import com.laptoprepair.entity.RequestItem;
import com.laptoprepair.entity.ServiceItem;
import com.laptoprepair.exception.CSVImportException;
import org.apache.commons.csv.CSVRecord;
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
    void copyServiceItemsFields(List<RequestItem> items);

    /**
     * Parses a CSV record into a ServiceItem entity
     */
    ServiceItem copyCSVRecordFields(CSVRecord csvRecord, int rowNumber) throws CSVImportException;
}