package com.laptoprepair.service.tools;

import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import com.laptoprepair.entity.Request;
import com.laptoprepair.repository.RequestRepository;

/**
 * AI Tool service for staff-only request operations. Provides READ-ONLY capabilities for searching
 * repair requests. Leverages PostgreSQL native fuzzy search (pg_trgm) for effective information
 * retrieval. All search parameters are optional and can be combined for flexible querying.
 */
@Service
public class RequestTools {

    private static final Logger logger = LoggerFactory.getLogger(RequestTools.class);

    private final RequestRepository requestRepository;

    @Value("${laptoprepair.business.shop.public-request-base-url}")
    private String publicRequestBaseUrl;

    public RequestTools(RequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    @Tool(description = "Tìm kiếm phiếu sửa chữa với PostgreSQL native fuzzy search. "
            + "Tất cả tham số đều optional và có thể kết hợp với nhau. "
            + "Hỗ trợ tìm kiếm gần đúng (fuzzy) cho text fields và range search cho dates. "
            + "Trả về danh sách phiếu sửa chữa với đầy đủ thông tin và tổng tiền. "
            + "Valid statuses: SCHEDULED, QUOTED, APPROVE_QUOTED, IN_PROGRESS, COMPLETED, UNDER_WARRANTY, CANCELLED")
    @PreAuthorize("hasRole('STAFF')")
    public String searchRequests(
            @ToolParam(description = "ID phiếu sửa chữa (UUID) - exact match") String requestId,

            @ToolParam(description = "Tên khách hàng - fuzzy search") String customerName,

            @ToolParam(
                    description = "Số điện thoại khách hàng - fuzzy search") String customerPhone,

            @ToolParam(description = "Email khách hàng - fuzzy search") String customerEmail,

            @ToolParam(description = "Địa chỉ khách hàng - fuzzy search") String customerAddress,

            @ToolParam(description = "Brand và model laptop - fuzzy search") String brandModel,

            @ToolParam(description = "Serial number thiết bị - fuzzy search") String serialNumber,

            @ToolParam(description = "Mô tả tình trạng thiết bị - fuzzy search") String description,

            @ToolParam(
                    description = "Trạng thái: SCHEDULED, QUOTED, APPROVE_QUOTED, IN_PROGRESS, COMPLETED, UNDER_WARRANTY, CANCELLED") String status,

            @ToolParam(
                    description = "Ngày hẹn từ (ISO format: 2024-01-01T10:00:00)") String appointmentDateFrom,

            @ToolParam(
                    description = "Ngày hẹn đến (ISO format: 2024-01-01T10:00:00)") String appointmentDateTo,

            @ToolParam(
                    description = "Ngày hoàn thành từ (ISO format: 2024-01-01T10:00:00)") String completedDateFrom,

            @ToolParam(
                    description = "Ngày hoàn thành đến (ISO format: 2024-01-01T10:00:00)") String completedDateTo) {

        try {
            logger.info(
                    "Staff searching requests with fuzzy search - params: requestId={}, customerName={}, customerPhone={}, "
                            + "customerEmail={}, customerAddress={}, brandModel={}, serialNumber={}, description={}, status={}, "
                            + "appointmentDateFrom={}, appointmentDateTo={}, completedDateFrom={}, completedDateTo={}",
                    requestId, customerName, customerPhone, customerEmail, customerAddress,
                    brandModel, serialNumber, description, status, appointmentDateFrom,
                    appointmentDateTo, completedDateFrom, completedDateTo);

            List<Request> requests = requestRepository.fuzzySearchRequests(requestId, customerName,
                    customerPhone, customerEmail, customerAddress, brandModel, serialNumber,
                    description, status, appointmentDateFrom, appointmentDateTo, completedDateFrom,
                    completedDateTo);

            logger.info("Staff search completed - found {} requests", requests.size());

            if (requests.isEmpty()) {
                return "Không tìm thấy phiếu sửa chữa nào phù hợp với tiêu chí tìm kiếm.";
            }

            return formatRequests(requests);

        } catch (Exception e) {
            logger.error("Error searching requests with fuzzy search: {}", e.getMessage(), e);
            return "Không thể tìm kiếm phiếu sửa chữa. Vui lòng thử lại sau.";
        }
    }

    /**
     * Format list of requests with total amount and detail link.
     * toString() already contains all request details.
     */
    private String formatRequests(List<Request> requests) {
        return "Found " + requests.size() + " request(s):\n\n"
                + requests.stream()
                        .map(r -> r.toString() + "\nTotal: " + r.getTotal()
                                + "\nDetail Link: " + publicRequestBaseUrl + r.getId() + "\n")
                        .collect(Collectors.joining("\n---\n\n"));
    }
}
