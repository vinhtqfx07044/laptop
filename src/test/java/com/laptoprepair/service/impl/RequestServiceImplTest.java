package com.laptoprepair.service.impl;

import com.laptoprepair.entity.Request;
import com.laptoprepair.entity.RequestItem;
import com.laptoprepair.entity.RequestImage;
import com.laptoprepair.entity.ServiceItem;
import com.laptoprepair.enums.RequestStatus;
import com.laptoprepair.exception.ValidationException;
import com.laptoprepair.repository.RequestRepository;
import com.laptoprepair.repository.ServiceItemRepository;
import com.laptoprepair.service.EmailService;
import com.laptoprepair.service.HistoryService;
import com.laptoprepair.service.ImageService;
import com.laptoprepair.config.VietnamTimeProvider;
import com.laptoprepair.validation.RequestValidator;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestServiceImplTest {

        @Mock
        private RequestRepository reqRepo;

        @Mock
        private ServiceItemRepository serviceItemRepository;

        @Mock
        private HistoryService historyService;

        @Mock
        private ImageService imageService;

        @Mock
        private EmailService emailService;

        @Mock
        private VietnamTimeProvider vietnamTimeProvider;

        private RequestValidator requestValidator;
        private RequestServiceImpl requestService;

        // Test data
        private Request testRequest;

        @BeforeEach
        void setUp() {
                // Create real validator with mock HistoryService dependency
                requestValidator = new RequestValidator(historyService);
                
                // Create service with all dependencies
                requestService = new RequestServiceImpl(reqRepo, serviceItemRepository,
                                                      historyService, imageService, emailService,
                                                      requestValidator, vietnamTimeProvider);
                
                testRequest = new Request();
                testRequest.setName("John Doe");
                testRequest.setPhone("0901234567");
                testRequest.setEmail("john@example.com");
                testRequest.setAppointmentDate(LocalDateTime.now().plusDays(1));
                testRequest.setDescription("Laptop won't turn on.");
                testRequest.setItems(new ArrayList<>());
                testRequest.setImages(new ArrayList<>());
        }

        // ===== PUBLIC CREATE METHOD TESTS =====

        @Test
        void publicCreate_UTC001_ValidRequest_ShouldReturnSavedRequestWithCorrectStatus() {
                // Arrange
                Request savedRequest = new Request();
                savedRequest.setName("John Doe");
                savedRequest.setPhone("0901234567");
                savedRequest.setEmail("john@example.com");
                savedRequest.setStatus(RequestStatus.SCHEDULED);
                savedRequest.setItems(List.of());
                savedRequest.setImages(List.of());

                // Mock repository and services - real validator will pass with future date
                when(reqRepo.save(any(Request.class))).thenReturn(savedRequest);
                doNothing().when(historyService).addRequestHistoryRecord(any(Request.class), eq("Tạo mới yêu cầu"),
                                eq("Khách"));
                when(emailService.sendConfirmationEmail(any(Request.class)))
                                .thenReturn(CompletableFuture.completedFuture(null));

                // Act
                Request result = requestService.publicCreate(testRequest);

                // Assert
                assertNotNull(result);
                assertEquals(RequestStatus.SCHEDULED, result.getStatus());
                assertTrue(result.getItems().isEmpty());
                assertTrue(result.getImages().isEmpty());
                
                // Verify interactions
                verify(reqRepo).save(any(Request.class));
                verify(historyService).addRequestHistoryRecord(any(Request.class), eq("Tạo mới yêu cầu"), eq("Khách"));
                verify(emailService).sendConfirmationEmail(any(Request.class));
        }

        @Test
        void publicCreate_UTC002_PastAppointmentDate_ShouldThrowValidationException() {
                // Arrange - Set past date so real validator will throw exception
                testRequest.setAppointmentDate(LocalDateTime.now().minusDays(1));

                // Act & Assert - Real validator will automatically throw exception for past date
                ValidationException exception = assertThrows(ValidationException.class,
                                () -> requestService.publicCreate(testRequest));

                assertEquals("Ngày hẹn phải sau thời điểm hiện tại", exception.getMessage());
                
                // Verify no save operations occurred due to early validation failure
                verify(reqRepo, never()).save(any(Request.class));
                verify(historyService, never()).addRequestHistoryRecord(any(), any(), any());
                verify(emailService, never()).sendConfirmationEmail(any());
        }

        @Test
        void publicCreate_UTC003_NoEmailProvided_ShouldSaveWithoutSendingEmail() {
                // Arrange
                testRequest.setEmail(null);
                Request savedRequest = new Request();
                savedRequest.setStatus(RequestStatus.SCHEDULED);
                savedRequest.setItems(List.of());
                savedRequest.setImages(List.of());

                // Mock repository and services - real validator will pass with future date
                when(reqRepo.save(any(Request.class))).thenReturn(savedRequest);
                doNothing().when(historyService).addRequestHistoryRecord(any(Request.class), eq("Tạo mới yêu cầu"),
                                eq("Khách"));
                when(emailService.sendConfirmationEmail(any(Request.class)))
                                .thenReturn(CompletableFuture.completedFuture(null));

                // Act
                Request result = requestService.publicCreate(testRequest);

                // Assert
                assertNotNull(result);
                assertEquals(RequestStatus.SCHEDULED, result.getStatus());
                
                // Verify interactions - email service is still called (handles null email internally)
                verify(reqRepo).save(any(Request.class));
                verify(historyService).addRequestHistoryRecord(any(Request.class), eq("Tạo mới yêu cầu"), eq("Khách"));
                verify(emailService).sendConfirmationEmail(any(Request.class));
        }

        // ===== UPDATE METHOD TESTS =====

        @Test
        void update_UTC001_FullUpdate_ShouldReturnUpdatedRequestWithCorrectStatusAndItems() {
                // Arrange
                UUID requestId = UUID.randomUUID();

                // Existing request
                Request existingRequest = new Request();
                existingRequest.setStatus(RequestStatus.SCHEDULED);
                existingRequest.setItems(new ArrayList<>());
                existingRequest.setImages(new ArrayList<>());

                // Incoming request with updates
                Request incomingRequest = new Request();
                incomingRequest.setName("John Doe");
                incomingRequest.setPhone("0901234567");
                incomingRequest.setStatus(RequestStatus.QUOTED);

                // Create request items
                RequestItem item1 = new RequestItem();
                item1.setServiceItemId(UUID.randomUUID());
                item1.setName("Service 1");
                item1.setPrice(BigDecimal.valueOf(100));
                item1.setVatRate(BigDecimal.valueOf(0.1));
                item1.setWarrantyDays(30);
                item1.setQuantity(1);
                item1.setDiscount(BigDecimal.ZERO);

                RequestItem item2 = new RequestItem();
                item2.setServiceItemId(UUID.randomUUID());
                item2.setName("Service 2");
                item2.setPrice(BigDecimal.valueOf(200));
                item2.setVatRate(BigDecimal.valueOf(0.1));
                item2.setWarrantyDays(60);
                item2.setQuantity(1);
                item2.setDiscount(BigDecimal.ZERO);

                incomingRequest.setItems(new ArrayList<>(List.of(item1, item2)));

                // Mock service items
                ServiceItem serviceItem1 = new ServiceItem();
                serviceItem1.setId(item1.getServiceItemId());
                serviceItem1.setName("Service 1");
                serviceItem1.setPrice(BigDecimal.valueOf(100));
                serviceItem1.setVatRate(BigDecimal.valueOf(0.1));
                serviceItem1.setWarrantyDays(30);

                ServiceItem serviceItem2 = new ServiceItem();
                serviceItem2.setId(item2.getServiceItemId());
                serviceItem2.setName("Service 2");
                serviceItem2.setPrice(BigDecimal.valueOf(200));
                serviceItem2.setVatRate(BigDecimal.valueOf(0.1));
                serviceItem2.setWarrantyDays(60);

                MultipartFile[] newImages = new MultipartFile[2];
                String[] toDelete = new String[0];
                String note = "Customer approved quote.";

                List<RequestImage> processedImages = new ArrayList<>();
                processedImages.add(new RequestImage());
                processedImages.add(new RequestImage());

                // Mock repository and service behaviors - real validator will handle validations
                when(reqRepo.findByIdWithItems(requestId)).thenReturn(Optional.of(existingRequest));
                when(serviceItemRepository.findAllByIdInAndActive(anyList()))
                                .thenReturn(List.of(serviceItem1, serviceItem2));
                when(imageService.updateRequestServiceImages(any(Request.class), any(MultipartFile[].class),
                                any(String[].class)))
                                .thenReturn(processedImages);
                when(historyService.computeRequestChanges(any(Request.class), any(Request.class)))
                                .thenReturn("Status changed");
                when(reqRepo.save(any(Request.class))).thenReturn(existingRequest);
                doNothing().when(historyService).addRequestHistoryRecord(any(Request.class), anyString(), anyString());
                when(emailService.sendUpdateEmail(any(Request.class), anyString()))
                                .thenReturn(CompletableFuture.completedFuture(null));

                // Act
                Request result = requestService.update(requestId, incomingRequest, newImages, toDelete, note);

                // Assert
                assertNotNull(result);
                assertEquals(RequestStatus.QUOTED, result.getStatus());
                assertEquals(2, result.getItems().size());
                assertEquals(2, result.getImages().size());
                
                // Verify interactions
                verify(reqRepo).findByIdWithItems(requestId);
                verify(serviceItemRepository).findAllByIdInAndActive(anyList());
                verify(reqRepo).save(any(Request.class));
        }

        @Test
        void update_UTC002_RequestNotFound_ShouldThrowValidationException() {
                // Arrange
                UUID nonExistentId = UUID.randomUUID();
                Request incomingRequest = new Request();

                when(reqRepo.findByIdWithItems(nonExistentId)).thenReturn(Optional.empty());

                // Act & Assert
                ValidationException exception = assertThrows(ValidationException.class,
                                () -> requestService.update(nonExistentId, incomingRequest, null, null, null));

                assertEquals("Không tìm thấy yêu cầu với ID: " + nonExistentId, exception.getMessage());
                
                // Verify repository was called but no further processing occurred
                verify(reqRepo).findByIdWithItems(nonExistentId);
                verify(reqRepo, never()).save(any(Request.class));
        }

        @Test
        void update_UTC003_StatusTransitionToCompleted_ShouldSetCompletedDate() {
                // Arrange
                UUID requestId = UUID.randomUUID();

                // Create a proper RequestItem with required fields
                RequestItem requestItem = new RequestItem();
                requestItem.setServiceItemId(UUID.randomUUID());
                requestItem.setName("Service Item");
                requestItem.setPrice(BigDecimal.valueOf(100));
                requestItem.setVatRate(BigDecimal.valueOf(0.1));
                requestItem.setWarrantyDays(30);
                requestItem.setQuantity(1);
                requestItem.setDiscount(BigDecimal.ZERO);

                ServiceItem serviceItem = new ServiceItem();
                serviceItem.setId(requestItem.getServiceItemId());
                serviceItem.setName("Service Item");
                serviceItem.setPrice(BigDecimal.valueOf(100));
                serviceItem.setVatRate(BigDecimal.valueOf(0.1));
                serviceItem.setWarrantyDays(30);

                Request existingRequest = new Request();
                existingRequest.setStatus(RequestStatus.IN_PROGRESS);
                existingRequest.setItems(new ArrayList<>(List.of(requestItem)));
                existingRequest.setImages(new ArrayList<>());

                Request incomingRequest = new Request();
                incomingRequest.setStatus(RequestStatus.COMPLETED);
                incomingRequest.setItems(new ArrayList<>(List.of(requestItem)));

                LocalDateTime completionTime = LocalDateTime.of(2025, 8, 27, 15, 33);
                
                // Mock repository and service behaviors - real validator will handle validations
                when(vietnamTimeProvider.now()).thenReturn(completionTime);
                when(reqRepo.findByIdWithItems(requestId)).thenReturn(Optional.of(existingRequest));
                when(serviceItemRepository.findAllByIdInAndActive(anyList())).thenReturn(List.of(serviceItem));
                when(historyService.computeRequestChanges(any(Request.class), any(Request.class)))
                                .thenReturn("Trạng thái: IN_PROGRESS → COMPLETED");
                when(reqRepo.save(any(Request.class))).thenReturn(existingRequest);
                when(imageService.updateRequestServiceImages(any(Request.class), isNull(), isNull()))
                                .thenReturn(new ArrayList<>());
                doNothing().when(historyService).addRequestHistoryRecord(any(Request.class), anyString(), anyString());
                when(emailService.sendUpdateEmail(any(Request.class), anyString()))
                                .thenReturn(CompletableFuture.completedFuture(null));

                // Act
                Request result = requestService.update(requestId, incomingRequest, null, null, null);

                // Assert
                assertNotNull(result);
                assertEquals(RequestStatus.COMPLETED, result.getStatus());
                assertEquals(completionTime, result.getCompletedAt());
                
                // Verify interactions
                verify(vietnamTimeProvider).now();
                verify(reqRepo).save(any(Request.class));
        }

        @Test
        void update_UTC004_InvalidStatusTransition_ShouldThrowValidationException() {
                // Arrange
                UUID requestId = UUID.randomUUID();

                Request existingRequest = new Request();
                existingRequest.setStatus(RequestStatus.CANCELLED);
                existingRequest.setItems(new ArrayList<>());
                existingRequest.setImages(new ArrayList<>());

                Request incomingRequest = new Request();
                incomingRequest.setStatus(RequestStatus.IN_PROGRESS);

                // Mock repository - real validator will automatically throw exception for CANCELLED transition
                when(reqRepo.findByIdWithItems(requestId)).thenReturn(Optional.of(existingRequest));

                // Act & Assert - Real validator will throw exception for cancelled status
                ValidationException exception = assertThrows(ValidationException.class,
                                () -> requestService.update(requestId, incomingRequest, null, null, null));

                // Real validator throws from validateEditable() for CANCELLED status
                assertEquals("Không thể chỉnh sửa phiếu ở trạng thái \"Đã hủy\"", exception.getMessage());
                
                // Verify repository was called but no save occurred due to validation failure
                verify(reqRepo).findByIdWithItems(requestId);
                verify(reqRepo, never()).save(any(Request.class));
        }

        @Test
        void update_UTC005_UpdateWithoutChanges_ShouldSaveWithOnlyNote() {
                // Arrange
                UUID requestId = UUID.randomUUID();
                String note = "Staff reviewed, no changes needed.";

                Request existingRequest = new Request();
                existingRequest.setStatus(RequestStatus.SCHEDULED);
                existingRequest.setItems(new ArrayList<>());
                existingRequest.setImages(new ArrayList<>());

                Request incomingRequest = new Request();
                incomingRequest.setStatus(RequestStatus.SCHEDULED);
                incomingRequest.setItems(new ArrayList<>());

                // Mock repository and service behaviors - real validator will pass for valid scenarios
                when(reqRepo.findByIdWithItems(requestId)).thenReturn(Optional.of(existingRequest));
                when(historyService.computeRequestChanges(any(Request.class), any(Request.class))).thenReturn("");
                when(reqRepo.save(any(Request.class))).thenReturn(existingRequest);
                when(imageService.updateRequestServiceImages(any(Request.class), isNull(), isNull()))
                                .thenReturn(new ArrayList<>());
                doNothing().when(historyService).addRequestHistoryRecord(any(Request.class), anyString(), anyString());
                when(emailService.sendUpdateEmail(any(Request.class), anyString()))
                                .thenReturn(CompletableFuture.completedFuture(null));

                // Act
                Request result = requestService.update(requestId, incomingRequest, null, null, note);

                // Assert
                assertNotNull(result);
                
                // Verify interactions
                verify(reqRepo).findByIdWithItems(requestId);
                verify(reqRepo).save(any(Request.class));
        }

        @Test
        void update_UTC006_ServiceItemDataInconsistency_ShouldThrowValidationException() {
                // Arrange
                UUID requestId = UUID.randomUUID();

                Request existingRequest = new Request();
                existingRequest.setStatus(RequestStatus.SCHEDULED);
                existingRequest.setItems(new ArrayList<>());
                existingRequest.setImages(new ArrayList<>());

                RequestItem item = new RequestItem();
                item.setServiceItemId(UUID.randomUUID());
                item.setName("Service Item");
                item.setPrice(BigDecimal.valueOf(100));
                item.setVatRate(BigDecimal.valueOf(0.1));
                item.setWarrantyDays(30);
                item.setQuantity(1);
                item.setDiscount(BigDecimal.ZERO);

                Request incomingRequest = new Request();
                incomingRequest.setStatus(RequestStatus.SCHEDULED);
                incomingRequest.setItems(new ArrayList<>(List.of(item)));

                ServiceItem serviceItem = new ServiceItem();
                serviceItem.setId(item.getServiceItemId());
                serviceItem.setName("Service Item");
                serviceItem.setPrice(BigDecimal.valueOf(200)); // Different price - will cause validation error
                serviceItem.setVatRate(BigDecimal.valueOf(0.1));
                serviceItem.setWarrantyDays(30);

                // Mock repository and services - service logic will detect inconsistency
                when(reqRepo.findByIdWithItems(requestId)).thenReturn(Optional.of(existingRequest));
                when(serviceItemRepository.findAllByIdInAndActive(anyList())).thenReturn(List.of(serviceItem));
                when(imageService.updateRequestServiceImages(any(Request.class), isNull(), isNull()))
                                .thenReturn(new ArrayList<>());

                // Act & Assert - Service will throw exception due to price inconsistency
                ValidationException exception = assertThrows(ValidationException.class,
                                () -> requestService.update(requestId, incomingRequest, null, null, null));

                assertTrue(exception.getMessage().contains("Giá dịch vụ"));
                assertTrue(exception.getMessage().contains("đã thay đổi"));
                
                // Verify repository was called but no save occurred due to validation failure
                verify(reqRepo).findByIdWithItems(requestId);
                verify(serviceItemRepository).findAllByIdInAndActive(anyList());
                verify(reqRepo, never()).save(any(Request.class));
        }

        @Test
        void update_UTC007_RequestItemsLockedForModification_ShouldThrowValidationException() {
                // Arrange
                UUID requestId = UUID.randomUUID();

                Request existingRequest = new Request();
                existingRequest.setStatus(RequestStatus.COMPLETED);
                existingRequest.setItems(new ArrayList<>(List.of(new RequestItem())));
                existingRequest.setImages(new ArrayList<>());

                Request incomingRequest = new Request();
                incomingRequest.setStatus(RequestStatus.COMPLETED);
                incomingRequest.setItems(new ArrayList<>(List.of(new RequestItem(), new RequestItem()))); // Different number of items

                // Mock repository and services - real validator + mock HistoryService will detect locked modification
                when(reqRepo.findByIdWithItems(requestId)).thenReturn(Optional.of(existingRequest));
                when(historyService.areRequestItemsEqual(any(), any())).thenReturn(false); // Items are different

                // Act & Assert - Real validator will throw exception for locked item modification
                ValidationException exception = assertThrows(ValidationException.class,
                                () -> requestService.update(requestId, incomingRequest, null, null, null));

                assertTrue(exception.getMessage().contains("Hoàn thành"));
                assertTrue(exception.getMessage().contains("không thể thay đổi hạng mục"));
                
                // Verify repository was called but no save occurred due to validation failure
                verify(reqRepo).findByIdWithItems(requestId);
                verify(historyService).areRequestItemsEqual(any(), any());
                verify(reqRepo, never()).save(any(Request.class));
        }
}