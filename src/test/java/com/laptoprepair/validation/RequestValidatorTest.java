package com.laptoprepair.validation;

import com.laptoprepair.entity.Request;
import com.laptoprepair.entity.RequestItem;
import com.laptoprepair.enums.RequestStatus;
import com.laptoprepair.exception.ValidationException;
import com.laptoprepair.service.HistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestValidatorTest {

    @Mock
    private HistoryService historyService;

    @InjectMocks
    private RequestValidator requestValidator;

    private Request testRequest;

    @BeforeEach
    void setUp() {
        testRequest = new Request();
        testRequest.setItems(new ArrayList<>());
    }

    @DisplayName("statusValidationTest - UTCID01: Valid status transition should pass")
    @Test
    void validateStatusTransition_ValidTransition_ShouldNotThrowException() {
        // Arrange
        Request existingRequest = new Request();
        existingRequest.setStatus(RequestStatus.SCHEDULED);
        
        Request incomingRequest = new Request();
        incomingRequest.setStatus(RequestStatus.QUOTED);

        // Act & Assert
        assertThatCode(() -> requestValidator.validateStatusTransition(existingRequest, incomingRequest))
            .doesNotThrowAnyException();
    }

    @DisplayName("statusValidationTest - UTCID02: Invalid status transition should throw ValidationException")
    @Test
    void validateStatusTransition_InvalidTransition_ShouldThrowValidationException() {
        // Arrange
        Request existingRequest = new Request();
        existingRequest.setStatus(RequestStatus.COMPLETED);
        
        Request incomingRequest = new Request();
        incomingRequest.setStatus(RequestStatus.IN_PROGRESS);

        // Act & Assert
        assertThatThrownBy(() -> requestValidator.validateStatusTransition(existingRequest, incomingRequest))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Không thể chuyển đổi trạng thái phiếu từ")
            .hasMessageContaining(RequestStatus.COMPLETED.toString())
            .hasMessageContaining(RequestStatus.IN_PROGRESS.toString());
    }

    @DisplayName("statusValidationTest - UTCID03: Transition to same status should pass")
    @Test
    void validateStatusTransition_SameStatus_ShouldNotThrowException() {
        // Arrange
        Request existingRequest = new Request();
        existingRequest.setStatus(RequestStatus.QUOTED);
        
        Request incomingRequest = new Request();
        incomingRequest.setStatus(RequestStatus.QUOTED);

        // Act & Assert
        assertThatCode(() -> requestValidator.validateStatusTransition(existingRequest, incomingRequest))
            .doesNotThrowAnyException();
    }

    @DisplayName("itemLockValidationTest - UTCID01: Modifying items when status is unlocked should pass")
    @Test
    void validateNoItemModificationWhenLocked_UnlockedStatus_ShouldNotThrowException() {
        // Arrange
        Request existingRequest = new Request();
        existingRequest.setStatus(RequestStatus.SCHEDULED); // Unlocked status
        existingRequest.setItems(new ArrayList<>());
        
        RequestItem item = new RequestItem();
        item.setName("New Item");
        
        Request incomingRequest = new Request();
        incomingRequest.setItems(List.of(item));

        // Act & Assert - No mocking needed since the method won't call areRequestItemsEqual for unlocked status
        assertThatCode(() -> requestValidator.validateNoItemModificationWhenLocked(existingRequest, incomingRequest))
            .doesNotThrowAnyException();
    }

    @DisplayName("itemLockValidationTest - UTCID02: Modifying items when status is locked should throw ValidationException")
    @Test
    void validateNoItemModificationWhenLocked_LockedStatusWithChanges_ShouldThrowValidationException() {
        // Arrange
        Request existingRequest = new Request();
        existingRequest.setStatus(RequestStatus.APPROVE_QUOTED); // Locked status
        existingRequest.setItems(new ArrayList<>());
        
        RequestItem item = new RequestItem();
        item.setName("New Item");
        
        Request incomingRequest = new Request();
        incomingRequest.setItems(List.of(item));
        
        when(historyService.areRequestItemsEqual(existingRequest.getItems(), incomingRequest.getItems()))
            .thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> requestValidator.validateNoItemModificationWhenLocked(existingRequest, incomingRequest))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Phiếu đã ở trạng thái")
            .hasMessageContaining("không thể thay đổi hạng mục");
    }

    @DisplayName("itemLockValidationTest - UTCID03: No item modification when status is locked should pass")
    @Test
    void validateNoItemModificationWhenLocked_LockedStatusNoChanges_ShouldNotThrowException() {
        // Arrange
        Request existingRequest = new Request();
        existingRequest.setStatus(RequestStatus.APPROVE_QUOTED); // Locked status
        existingRequest.setItems(new ArrayList<>());
        
        Request incomingRequest = new Request();
        incomingRequest.setItems(new ArrayList<>());
        
        when(historyService.areRequestItemsEqual(existingRequest.getItems(), incomingRequest.getItems()))
            .thenReturn(true);

        // Act & Assert
        assertThatCode(() -> requestValidator.validateNoItemModificationWhenLocked(existingRequest, incomingRequest))
            .doesNotThrowAnyException();
    }
}