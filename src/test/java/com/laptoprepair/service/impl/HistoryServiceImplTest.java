package com.laptoprepair.service.impl;

import com.laptoprepair.entity.Request;
import com.laptoprepair.entity.RequestItem;
import com.laptoprepair.enums.RequestStatus;
import com.laptoprepair.utils.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class HistoryServiceImplTest {

    @Mock
    private TimeProvider timeProvider;

    @InjectMocks
    private HistoryServiceImpl historyService;

    private Request testRequest;

    @BeforeEach
    void setUp() {
        testRequest = new Request();
        testRequest.setHistory(new ArrayList<>());
    }

    @DisplayName("computeChangesTest - UTCID01: Changes in status and total should be detected")
    @Test
    void computeRequestChanges_StatusAndTotalChanged_ShouldReturnChangeSummary() {
        // Arrange
        Request oldRequest = new Request();
        oldRequest.setStatus(RequestStatus.SCHEDULED);
        oldRequest.setAppointmentDate(LocalDateTime.of(2024, 1, 1, 10, 0));
        oldRequest.setItems(new ArrayList<>());
        
        Request newRequest = new Request();
        newRequest.setStatus(RequestStatus.QUOTED);
        newRequest.setAppointmentDate(LocalDateTime.of(2024, 1, 1, 10, 0));
        
        RequestItem item = new RequestItem();
        item.setPrice(BigDecimal.valueOf(100000));
        item.setDiscount(BigDecimal.ZERO);
        newRequest.setItems(List.of(item));

        // Act
        String result = historyService.computeRequestChanges(oldRequest, newRequest);

        // Assert
        assertThat(result).isNotEmpty();
        assertThat(result).contains("Trạng thái: " + RequestStatus.SCHEDULED + " → " + RequestStatus.QUOTED);
        assertThat(result).contains("Tổng tiền: 0 → 100,000 VND");
    }

    @DisplayName("computeChangesTest - UTCID02: No changes should return empty string")
    @Test
    void computeRequestChanges_NoChanges_ShouldReturnEmptyString() {
        // Arrange
        Request oldRequest = new Request();
        oldRequest.setStatus(RequestStatus.QUOTED);
        oldRequest.setAppointmentDate(LocalDateTime.of(2024, 1, 1, 10, 0));
        oldRequest.setItems(new ArrayList<>());
        
        Request newRequest = new Request();
        newRequest.setStatus(RequestStatus.QUOTED);
        newRequest.setAppointmentDate(LocalDateTime.of(2024, 1, 1, 10, 0));
        newRequest.setItems(new ArrayList<>());

        // Act
        String result = historyService.computeRequestChanges(oldRequest, newRequest);

        // Assert
        assertThat(result).isEmpty();
    }

    @DisplayName("computeChangesTest - UTCID03: Different items should be detected")
    @Test
    void computeRequestChanges_DifferentItems_ShouldDetectItemChanges() {
        // Arrange
        RequestItem oldItem = new RequestItem();
        oldItem.setName("Item A");
        oldItem.setPrice(BigDecimal.valueOf(50000));
        oldItem.setDiscount(BigDecimal.ZERO);
        
        RequestItem newItem = new RequestItem();
        newItem.setName("Item B");
        newItem.setPrice(BigDecimal.valueOf(50000));
        newItem.setDiscount(BigDecimal.ZERO);
        
        Request oldRequest = new Request();
        oldRequest.setStatus(RequestStatus.SCHEDULED);
        oldRequest.setAppointmentDate(LocalDateTime.of(2024, 1, 1, 10, 0));
        oldRequest.setItems(List.of(oldItem));
        
        Request newRequest = new Request();
        newRequest.setStatus(RequestStatus.SCHEDULED);
        newRequest.setAppointmentDate(LocalDateTime.of(2024, 1, 1, 10, 0));
        newRequest.setItems(List.of(newItem));

        // Act
        String result = historyService.computeRequestChanges(oldRequest, newRequest);

        // Assert
        assertThat(result).contains("Cập nhật hạng mục sửa chữa");
    }

    @DisplayName("itemEqualityTest - UTCID01: Same items in different order should be equal")
    @Test
    void areRequestItemsEqual_SameItemsDifferentOrder_ShouldReturnTrue() {
        // Arrange
        RequestItem itemA = new RequestItem();
        itemA.setName("Item A");
        itemA.setPrice(BigDecimal.valueOf(50000));
        
        RequestItem itemB = new RequestItem();
        itemB.setName("Item B");
        itemB.setPrice(BigDecimal.valueOf(75000));
        
        List<RequestItem> oldItems = List.of(itemA, itemB);
        List<RequestItem> newItems = List.of(itemB, itemA);

        // Act
        boolean result = historyService.areRequestItemsEqual(oldItems, newItems);

        // Assert
        assertThat(result).isTrue();
    }

    @DisplayName("itemEqualityTest - UTCID02: Lists with different sizes should not be equal")
    @Test
    void areRequestItemsEqual_DifferentSizes_ShouldReturnFalse() {
        // Arrange
        RequestItem itemA = new RequestItem();
        itemA.setName("Item A");
        
        RequestItem itemB = new RequestItem();
        itemB.setName("Item B");
        
        RequestItem itemC = new RequestItem();
        itemC.setName("Item C");
        
        List<RequestItem> oldItems = List.of(itemA, itemB);
        List<RequestItem> newItems = List.of(itemA, itemB, itemC);

        // Act
        boolean result = historyService.areRequestItemsEqual(oldItems, newItems);

        // Assert
        assertThat(result).isFalse();
    }

    @DisplayName("itemEqualityTest - UTCID03: Lists with same size but different content should not be equal")
    @Test
    void areRequestItemsEqual_SameSizeDifferentContent_ShouldReturnFalse() {
        // Arrange
        RequestItem itemA = new RequestItem();
        itemA.setName("Item A");
        
        RequestItem itemB = new RequestItem();
        itemB.setName("Item B");
        
        RequestItem itemC = new RequestItem();
        itemC.setName("Item C");
        
        List<RequestItem> oldItems = List.of(itemA, itemB);
        List<RequestItem> newItems = List.of(itemA, itemC);

        // Act
        boolean result = historyService.areRequestItemsEqual(oldItems, newItems);

        // Assert
        assertThat(result).isFalse();
    }

    @DisplayName("itemEqualityTest - UTCID04: Empty lists should be equal")
    @Test
    void areRequestItemsEqual_EmptyLists_ShouldReturnTrue() {
        // Arrange
        List<RequestItem> oldItems = new ArrayList<>();
        List<RequestItem> newItems = new ArrayList<>();

        // Act
        boolean result = historyService.areRequestItemsEqual(oldItems, newItems);

        // Assert
        assertThat(result).isTrue();
    }
}