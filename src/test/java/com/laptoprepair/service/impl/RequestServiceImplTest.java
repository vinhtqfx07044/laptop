package com.laptoprepair.service.impl;

import com.laptoprepair.entity.Request;
import com.laptoprepair.entity.RequestItem;
import com.laptoprepair.enums.RequestStatus;
import com.laptoprepair.exception.ValidationException;
import com.laptoprepair.repository.RequestRepository;
import com.laptoprepair.repository.ServiceItemRepository;
import com.laptoprepair.service.AuthService;
import com.laptoprepair.service.EmailService;
import com.laptoprepair.service.HistoryService;
import com.laptoprepair.service.ImageService;
import com.laptoprepair.utils.TimeProvider;
import com.laptoprepair.validation.RequestValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestServiceImplTest {

    @Mock
    private RequestRepository requestRepository;
    
    @Mock
    private ServiceItemRepository serviceItemRepository;
    
    @Mock
    private HistoryService historyService;
    
    @Mock
    private ImageService imageService;
    
    @Mock
    private EmailService emailService;
    
    @Mock
    private RequestValidator requestValidator;
    
    @Mock
    private TimeProvider timeProvider;
    
    @Mock
    private AuthService authService;

    @InjectMocks
    private RequestServiceImpl requestService;

    private Request testRequest;
    private LocalDateTime futureDate;
    private LocalDateTime pastDate;

    @BeforeEach
    void setUp() {
        futureDate = LocalDateTime.now().plusDays(1);
        pastDate = LocalDateTime.now().minusDays(1);
        
        testRequest = new Request();
        testRequest.setName("Test Customer");
        testRequest.setPhone("0123456789");
        testRequest.setEmail("test@example.com");
        testRequest.setDescription("Test description for repair");
        testRequest.setAppointmentDate(futureDate);
    }

    @DisplayName("publicCreateTest - UTCID01: Valid request with future appointment date")
    @Test
    void publicCreate_ValidRequest_ShouldCreateSuccessfully() {
        // Arrange
        Request expectedRequest = new Request();
        expectedRequest.setId(UUID.randomUUID());
        expectedRequest.setStatus(RequestStatus.SCHEDULED);
        expectedRequest.setItems(List.of());
        expectedRequest.setImages(List.of());
        
        when(requestRepository.save(any(Request.class))).thenReturn(expectedRequest);
        doNothing().when(requestValidator).validateAppointmentDateInFuture(any(LocalDateTime.class));
        doNothing().when(historyService).addRequestHistoryRecord(any(Request.class), anyString(), anyString());
        doNothing().when(emailService).sendConfirmationEmail(any(Request.class));

        // Act
        Request result = requestService.publicCreate(testRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(RequestStatus.SCHEDULED);
        assertThat(result.getItems()).isEmpty();
        assertThat(result.getImages()).isEmpty();
        
        verify(requestValidator).validateAppointmentDateInFuture(testRequest.getAppointmentDate());
        verify(historyService).addRequestHistoryRecord(testRequest, "Tạo mới yêu cầu", "Khách");
        verify(emailService).sendConfirmationEmail(expectedRequest);
        verify(requestRepository).save(testRequest);
    }

    @DisplayName("publicCreateTest - UTCID02: Request with past appointment date should throw ValidationException")
    @Test
    void publicCreate_PastAppointmentDate_ShouldThrowValidationException() {
        // Arrange
        testRequest.setAppointmentDate(pastDate);
        String expectedMessage = "Ngày hẹn phải sau thời điểm hiện tại";
        
        doThrow(new ValidationException(expectedMessage))
            .when(requestValidator).validateAppointmentDateInFuture(pastDate);

        // Act & Assert
        assertThatThrownBy(() -> requestService.publicCreate(testRequest))
            .isInstanceOf(ValidationException.class)
            .hasMessage(expectedMessage);
        
        verify(requestValidator).validateAppointmentDateInFuture(pastDate);
        verify(requestRepository, never()).save(any(Request.class));
        verify(historyService, never()).addRequestHistoryRecord(any(Request.class), anyString(), anyString());
        verify(emailService, never()).sendConfirmationEmail(any(Request.class));
    }

    @DisplayName("updateRequestTest - UTCID01: Valid status transition from SCHEDULED to QUOTED")
    @Test
    void update_ValidStatusTransition_ShouldUpdateSuccessfully() {
        // Arrange
        UUID requestId = UUID.randomUUID();
        
        Request existingRequest = new Request();
        existingRequest.setId(requestId);
        existingRequest.setStatus(RequestStatus.SCHEDULED);
        existingRequest.setItems(new ArrayList<>());
        existingRequest.setImages(new ArrayList<>());
        
        Request incomingRequest = new Request();
        incomingRequest.setStatus(RequestStatus.QUOTED);
        incomingRequest.setItems(new ArrayList<>());
        incomingRequest.setImages(new ArrayList<>());
        
        when(requestRepository.findByIdWithDetails(requestId)).thenReturn(Optional.of(existingRequest));
        when(requestRepository.save(any(Request.class))).thenReturn(existingRequest);
        when(imageService.updateRequestServiceImages(any(Request.class), any(), any())).thenReturn(new ArrayList<>());
        when(historyService.computeRequestChanges(any(Request.class), any(Request.class))).thenReturn("Trạng thái: SCHEDULED → QUOTED");
        when(authService.getCurrentUsername()).thenReturn("staff");
        
        doNothing().when(requestValidator).validateEditable(any(Request.class));
        doNothing().when(requestValidator).validateStatusTransition(any(Request.class), any(Request.class));
        doNothing().when(requestValidator).validateItemsForStatus(any(Request.class));
        doNothing().when(requestValidator).validateNoItemModificationWhenLocked(any(Request.class), any(Request.class));
        doNothing().when(historyService).addRequestHistoryRecord(any(Request.class), anyString(), anyString());
        doNothing().when(emailService).sendUpdateEmail(any(Request.class), anyString());

        // Act
        Request result = requestService.update(requestId, incomingRequest, null, null, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(RequestStatus.QUOTED);
        
        verify(requestValidator).validateEditable(existingRequest);
        verify(requestValidator).validateStatusTransition(existingRequest, incomingRequest);
        verify(historyService).addRequestHistoryRecord(eq(existingRequest), contains("Trạng thái: SCHEDULED → QUOTED"), eq("staff"));
        verify(emailService).sendUpdateEmail(eq(existingRequest), anyString());
    }

    @DisplayName("updateRequestTest - UTCID02: Invalid status transition should throw ValidationException")
    @Test
    void update_InvalidStatusTransition_ShouldThrowValidationException() {
        // Arrange
        UUID requestId = UUID.randomUUID();
        String expectedMessage = "Không thể chuyển đổi trạng thái từ COMPLETED sang IN_PROGRESS";
        
        Request existingRequest = new Request();
        existingRequest.setId(requestId);
        existingRequest.setStatus(RequestStatus.COMPLETED);
        existingRequest.setItems(new ArrayList<>());
        existingRequest.setImages(new ArrayList<>());
        
        Request incomingRequest = new Request();
        incomingRequest.setStatus(RequestStatus.IN_PROGRESS);
        
        when(requestRepository.findByIdWithDetails(requestId)).thenReturn(Optional.of(existingRequest));
        
        doNothing().when(requestValidator).validateEditable(any(Request.class));
        doThrow(new ValidationException(expectedMessage))
            .when(requestValidator).validateStatusTransition(existingRequest, incomingRequest);

        // Act & Assert
        assertThatThrownBy(() -> requestService.update(requestId, incomingRequest, null, null, null))
            .isInstanceOf(ValidationException.class)
            .hasMessage(expectedMessage);
        
        verify(requestValidator).validateStatusTransition(existingRequest, incomingRequest);
        verify(requestRepository, never()).save(any(Request.class));
    }

    @DisplayName("updateRequestTest - UTCID03: Attempt to modify items when locked should throw ValidationException")
    @Test
    void update_ModifyItemsWhenLocked_ShouldThrowValidationException() {
        // Arrange
        UUID requestId = UUID.randomUUID();
        String expectedMessage = "Phiếu đã ở trạng thái không cho phép sửa đổi hạng mục";
        
        Request existingRequest = new Request();
        existingRequest.setId(requestId);
        existingRequest.setStatus(RequestStatus.APPROVE_QUOTED);
        existingRequest.setItems(new ArrayList<>());
        existingRequest.setImages(new ArrayList<>());
        
        Request incomingRequest = new Request();
        incomingRequest.setStatus(RequestStatus.APPROVE_QUOTED);
        RequestItem newItem = new RequestItem();
        newItem.setName("New Item");
        incomingRequest.setItems(List.of(newItem));
        
        when(requestRepository.findByIdWithDetails(requestId)).thenReturn(Optional.of(existingRequest));
        
        doNothing().when(requestValidator).validateEditable(any(Request.class));
        doNothing().when(requestValidator).validateStatusTransition(any(Request.class), any(Request.class));
        doNothing().when(requestValidator).validateItemsForStatus(any(Request.class));
        doThrow(new ValidationException(expectedMessage))
            .when(requestValidator).validateNoItemModificationWhenLocked(existingRequest, incomingRequest);

        // Act & Assert
        assertThatThrownBy(() -> requestService.update(requestId, incomingRequest, null, null, null))
            .isInstanceOf(ValidationException.class)
            .hasMessage(expectedMessage);
        
        verify(requestValidator).validateNoItemModificationWhenLocked(existingRequest, incomingRequest);
        verify(requestRepository, never()).save(any(Request.class));
    }

    @DisplayName("updateRequestTest - UTCID04: Transition to COMPLETED should set completedAt")
    @Test
    void update_TransitionToCompleted_ShouldSetCompletedAt() {
        // Arrange
        UUID requestId = UUID.randomUUID();
        
        Request existingRequest = new Request();
        existingRequest.setId(requestId);
        existingRequest.setStatus(RequestStatus.IN_PROGRESS);
        existingRequest.setItems(new ArrayList<>());
        existingRequest.setImages(new ArrayList<>());
        
        Request incomingRequest = new Request();
        incomingRequest.setStatus(RequestStatus.COMPLETED);
        incomingRequest.setItems(new ArrayList<>());
        incomingRequest.setImages(new ArrayList<>());
        
        LocalDateTime completionTime = LocalDateTime.now();
        
        when(requestRepository.findByIdWithDetails(requestId)).thenReturn(Optional.of(existingRequest));
        when(requestRepository.save(any(Request.class))).thenReturn(existingRequest);
        when(timeProvider.now()).thenReturn(completionTime);
        when(imageService.updateRequestServiceImages(any(Request.class), any(), any())).thenReturn(new ArrayList<>());
        when(historyService.computeRequestChanges(any(Request.class), any(Request.class))).thenReturn("Trạng thái: IN_PROGRESS → COMPLETED");
        when(authService.getCurrentUsername()).thenReturn("staff");
        
        doNothing().when(requestValidator).validateEditable(any(Request.class));
        doNothing().when(requestValidator).validateStatusTransition(any(Request.class), any(Request.class));
        doNothing().when(requestValidator).validateItemsForStatus(any(Request.class));
        doNothing().when(requestValidator).validateNoItemModificationWhenLocked(any(Request.class), any(Request.class));
        doNothing().when(historyService).addRequestHistoryRecord(any(Request.class), anyString(), anyString());
        doNothing().when(emailService).sendUpdateEmail(any(Request.class), anyString());

        // Act
        Request result = requestService.update(requestId, incomingRequest, null, null, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(RequestStatus.COMPLETED);
        assertThat(result.getCompletedAt()).isEqualTo(completionTime);
        
        verify(timeProvider).now();
        verify(historyService).addRequestHistoryRecord(eq(existingRequest), anyString(), eq("staff"));
        verify(emailService).sendUpdateEmail(eq(existingRequest), anyString());
    }
}