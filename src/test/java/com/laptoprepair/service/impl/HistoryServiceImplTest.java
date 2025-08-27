package com.laptoprepair.service.impl;

import com.laptoprepair.entity.Request;
import com.laptoprepair.entity.RequestHistory;
import com.laptoprepair.entity.RequestItem;
import com.laptoprepair.enums.RequestStatus;
import com.laptoprepair.utils.CurrencyUtils;
import com.laptoprepair.config.VietnamTimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistoryServiceImplTest {

    @Mock
    private VietnamTimeProvider vietnamTimeProvider;

    @InjectMocks
    private HistoryServiceImpl historyService;

    private Request request;

    @BeforeEach
    void setUp() {
        request = new Request();
        request.setHistory(new ArrayList<>());
    }

    @Test
    void addRequestHistoryRecord_UTC001_ValidHistoryRecord_ShouldCreateHistoryRecord() {
        LocalDateTime testTime = LocalDateTime.of(2024, 7, 1, 10, 0);
        when(vietnamTimeProvider.now()).thenReturn(testTime);
        
        String changes = "Status changed from SCHEDULED to QUOTED.";
        String user = "staff_user";

        historyService.addRequestHistoryRecord(request, changes, user);

        assertEquals(1, request.getHistory().size());
        RequestHistory history = request.getHistory().get(0);
        assertEquals(changes, history.getChanges());
        assertEquals(user, history.getCreatedBy());
        assertEquals(testTime, history.getCreatedAt());
        assertEquals(request, history.getRequest());
    }

    @Test
    void addRequestHistoryRecord_UTC002_LongChangeDescriptionTruncation_ShouldTruncateChanges() {
        LocalDateTime testTime = LocalDateTime.of(2024, 7, 1, 10, 0);
        when(vietnamTimeProvider.now()).thenReturn(testTime);
        
        String longChanges = "a".repeat(600);
        String user = "admin";

        historyService.addRequestHistoryRecord(request, longChanges, user);

        assertEquals(1, request.getHistory().size());
        RequestHistory history = request.getHistory().get(0);
        assertEquals(503, history.getChanges().length());
        assertTrue(history.getChanges().endsWith("..."));
        assertEquals(user, history.getCreatedBy());
        assertEquals(testTime, history.getCreatedAt());
    }

    @Test
    void addRequestHistoryRecord_UTC003_NullChangeDescription_ShouldAllowNullChanges() {
        LocalDateTime testTime = LocalDateTime.of(2024, 7, 1, 10, 0);
        when(vietnamTimeProvider.now()).thenReturn(testTime);
        
        String user = "system";

        historyService.addRequestHistoryRecord(request, null, user);

        assertEquals(1, request.getHistory().size());
        RequestHistory history = request.getHistory().get(0);
        assertNull(history.getChanges());
        assertEquals(user, history.getCreatedBy());
        assertEquals(testTime, history.getCreatedAt());
    }

    @Test
    void computeRequestChanges_UTC001_AllFieldsChanged_ShouldReturnAllChanges() {
        Request oldRequest = createRequest(RequestStatus.SCHEDULED,
                LocalDateTime.of(2024, 7, 1, 10, 0), createRequestItems(1));
        Request newRequest = createRequest(RequestStatus.QUOTED,
                LocalDateTime.of(2024, 7, 2, 11, 0), createRequestItems(2));

        try (MockedStatic<CurrencyUtils> currencyMock = mockStatic(CurrencyUtils.class)) {
            currencyMock.when(() -> CurrencyUtils.calculateRequestTotal(oldRequest.getItems()))
                    .thenReturn(new BigDecimal("100"));
            currencyMock.when(() -> CurrencyUtils.calculateRequestTotal(newRequest.getItems()))
                    .thenReturn(new BigDecimal("200"));

            String result = historyService.computeRequestChanges(oldRequest, newRequest);

            assertTrue(result.contains("Trạng thái: Đã lên lịch → Đã báo giá"));
            assertTrue(result.contains("Ngày hẹn: 2024-07-01T10:00 → 2024-07-02T11:00"));
            assertTrue(result.contains("Cập nhật hạng mục sửa chữa"));
            assertTrue(result.contains("Tổng tiền: 100 → 200 VND"));
        }
    }

    @Test
    void computeRequestChanges_UTC002_NoChanges_ShouldReturnEmptyString() {
        List<RequestItem> sameItems = createRequestItems(1);
        Request oldRequest = createRequest(RequestStatus.SCHEDULED,
                LocalDateTime.of(2024, 7, 1, 10, 0), sameItems);
        Request newRequest = createRequest(RequestStatus.SCHEDULED,
                LocalDateTime.of(2024, 7, 1, 10, 0), sameItems);

        try (MockedStatic<CurrencyUtils> currencyMock = mockStatic(CurrencyUtils.class)) {
            currencyMock.when(() -> CurrencyUtils.calculateRequestTotal(any()))
                    .thenReturn(new BigDecimal("100"));

            String result = historyService.computeRequestChanges(oldRequest, newRequest);

            assertEquals("", result);
        }
    }

    @Test
    void computeRequestChanges_UTC003_OnlyStatusChange_ShouldReturnStatusChange() {
        List<RequestItem> sameItems = createRequestItems(1);
        Request oldRequest = createRequest(RequestStatus.SCHEDULED,
                LocalDateTime.of(2024, 7, 1, 10, 0), sameItems);
        Request newRequest = createRequest(RequestStatus.IN_PROGRESS,
                LocalDateTime.of(2024, 7, 1, 10, 0), sameItems);

        try (MockedStatic<CurrencyUtils> currencyMock = mockStatic(CurrencyUtils.class)) {
            currencyMock.when(() -> CurrencyUtils.calculateRequestTotal(any()))
                    .thenReturn(new BigDecimal("100"));

            String result = historyService.computeRequestChanges(oldRequest, newRequest);

            assertEquals("Trạng thái: Đã lên lịch → Đang thực hiện", result);
        }
    }

    @Test
    void computeRequestChanges_UTC004_OnlyTotalPriceChange_ShouldReturnPriceAndItemChanges() {
        Request oldRequest = createRequest(RequestStatus.QUOTED,
                LocalDateTime.of(2024, 7, 1, 10, 0), createRequestItems(1));
        Request newRequest = createRequest(RequestStatus.QUOTED,
                LocalDateTime.of(2024, 7, 1, 10, 0), createRequestItems(2));

        try (MockedStatic<CurrencyUtils> currencyMock = mockStatic(CurrencyUtils.class)) {
            currencyMock.when(() -> CurrencyUtils.calculateRequestTotal(oldRequest.getItems()))
                    .thenReturn(new BigDecimal("100"));
            currencyMock.when(() -> CurrencyUtils.calculateRequestTotal(newRequest.getItems()))
                    .thenReturn(new BigDecimal("80"));

            String result = historyService.computeRequestChanges(oldRequest, newRequest);

            assertTrue(result.contains("Cập nhật hạng mục sửa chữa"));
            assertTrue(result.contains("Tổng tiền: 100 → 80 VND"));
        }
    }

    @Test
    void computeRequestChanges_UTC005_NullRequest_ShouldThrowNullPointerException() {
        Request validRequest = createRequest(RequestStatus.QUOTED,
                LocalDateTime.of(2024, 7, 1, 10, 0), createRequestItems(1));

        assertThrows(NullPointerException.class,
                () -> historyService.computeRequestChanges(null, validRequest));

        assertThrows(NullPointerException.class,
                () -> historyService.computeRequestChanges(validRequest, null));
    }

    private Request createRequest(RequestStatus status, LocalDateTime appointmentDate, List<RequestItem> items) {
        Request testRequest = new Request();
        testRequest.setStatus(status);
        testRequest.setAppointmentDate(appointmentDate);
        testRequest.setItems(items);
        return testRequest;
    }

    private List<RequestItem> createRequestItems(int id) {
        List<RequestItem> items = new ArrayList<>();
        RequestItem item = new RequestItem();
        // Using different serviceItemId to differentiate items
        item.setServiceItemId(java.util.UUID.randomUUID());
        item.setName("Item " + id);
        item.setPrice(new BigDecimal("100"));
        item.setQuantity(id);
        items.add(item);
        return items;
    }
}