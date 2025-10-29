package com.laptoprepair.service.tools;

import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import com.laptoprepair.entity.ServiceItem;
import com.laptoprepair.repository.ServiceItemRepository;

/**
 * AI Tool service for searching service item pricing information. Provides
 * READ-ONLY capabilities
 * with PostgreSQL native fuzzy search. Simple fuzzy search by service name
 * only. Accessible to all
 * users (no authentication required).
 */
@Service
public class ServiceItemTools {

    private static final Logger logger = LoggerFactory.getLogger(ServiceItemTools.class);

    private final ServiceItemRepository serviceItemRepository;

    public ServiceItemTools(ServiceItemRepository serviceItemRepository) {
        this.serviceItemRepository = serviceItemRepository;
    }

    /**
     * Search active service items with PostgreSQL native fuzzy search by name.
     * Simple search by service name only.
     * Returns raw entity data for LLM to format.
     * READ-ONLY - no modifications allowed.
     *
     * @param serviceName Service name (fuzzy search)
     * @return Raw entity data as string
     */
    @Tool(description = "Tìm kiếm dịch vụ sửa chữa với PostgreSQL native fuzzy search. " +
            "Chỉ tìm trong các dịch vụ đang hoạt động (active). " +
            "Hỗ trợ tìm kiếm gần đúng (fuzzy) cho tên dịch vụ. " +
            "Trả về danh sách dịch vụ với giá, VAT, và thời gian bảo hành.")
    public String searchServicePrices(
            @ToolParam(description = "Tên dịch vụ - fuzzy search") String serviceName) {

        try {
            logger.info("Searching service items with fuzzy search - serviceName={}", serviceName);

            List<ServiceItem> serviceItems = serviceItemRepository.fuzzySearchServiceItems(
                    serviceName);

            logger.info("Service search completed - found {} service items", serviceItems.size());

            if (serviceItems.isEmpty()) {
                return "Không tìm thấy dịch vụ phù hợp.";
            }

            return formatServiceItems(serviceItems);

        } catch (Exception e) {
            logger.error("Error searching service items with fuzzy search: {}", e.getMessage(), e);
            return "Không thể tìm kiếm dịch vụ. Vui lòng thử lại sau.";
        }
    }

    /**
     * Format list of service items with toString().
     */
    private String formatServiceItems(List<ServiceItem> serviceItems) {
        return "Found " + serviceItems.size() + " service item(s):\n\n" + serviceItems.stream()
                .map(ServiceItem::toString).collect(Collectors.joining("\n---\n\n"));
    }
}
