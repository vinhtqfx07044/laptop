package com.laptoprepair.service.impl;

import com.laptoprepair.entity.ServiceItem;
import com.laptoprepair.exception.CSVImportException;
import com.laptoprepair.exception.NotFoundException;
import com.laptoprepair.exception.ValidationException;
import com.laptoprepair.repository.ServiceItemRepository;
import com.laptoprepair.validation.ServiceItemValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceItemServiceImplTest {

        @Mock
        private ServiceItemRepository serviceItemRepository;

        private ServiceItemValidator serviceItemValidator;
        private ServiceItemServiceImpl serviceItemService;

        private ServiceItem testServiceItem;
        private UUID testServiceItemId;

        @BeforeEach
        void setUp() {
                // Create real validator with mock repository
                serviceItemValidator = new ServiceItemValidator(serviceItemRepository);
                
                // Create service with mock repository and real validator
                serviceItemService = new ServiceItemServiceImpl(serviceItemRepository, serviceItemValidator);
                
                testServiceItemId = UUID.randomUUID();
                testServiceItem = new ServiceItem();
                testServiceItem.setId(testServiceItemId);
                testServiceItem.setName("Laptop Cleaning");
                testServiceItem.setPrice(new BigDecimal("250000"));
                testServiceItem.setVatRate(new BigDecimal("0.10"));
                testServiceItem.setWarrantyDays(30);
                testServiceItem.setActive(true);
        }

        @Test
        void create_UTC001_ValidServiceItem_ShouldReturnSavedServiceItem() {
                // Arrange
                ServiceItem inputServiceItem = new ServiceItem();
                inputServiceItem.setName("Laptop Cleaning");
                inputServiceItem.setPrice(new BigDecimal("250000"));
                inputServiceItem.setVatRate(new BigDecimal("0.10"));
                inputServiceItem.setWarrantyDays(30);
                inputServiceItem.setActive(true);

                // Mock repository to simulate name doesn't exist (allowing creation)
                when(serviceItemRepository.findByName("Laptop Cleaning")).thenReturn(Optional.empty());
                when(serviceItemRepository.save(eq(inputServiceItem))).thenReturn(inputServiceItem);

                // Act
                ServiceItem result = serviceItemService.create(inputServiceItem);

                // Assert
                assertNotNull(result);
                assertEquals("Laptop Cleaning", result.getName());
                assertEquals(new BigDecimal("250000"), result.getPrice());
                assertEquals(new BigDecimal("0.10"), result.getVatRate());
                assertEquals(30, result.getWarrantyDays());
                assertTrue(result.isActive());
                
                // Verify validator called repository to check uniqueness
                verify(serviceItemRepository).findByName("Laptop Cleaning");
                verify(serviceItemRepository).save(inputServiceItem);
        }

        @Test
        void create_UTC002_DuplicateName_ShouldThrowValidationException() {
                // Arrange
                ServiceItem inputServiceItem = new ServiceItem();
                inputServiceItem.setName("Existing Service Name");

                ServiceItem existingItem = new ServiceItem();
                existingItem.setId(UUID.randomUUID());
                existingItem.setName("Existing Service Name");

                // Mock repository to simulate name already exists
                when(serviceItemRepository.findByName("Existing Service Name")).thenReturn(Optional.of(existingItem));

                // Act & Assert
                ValidationException exception = assertThrows(ValidationException.class,
                                () -> serviceItemService.create(inputServiceItem));

                assertEquals("Tên dịch vụ đã tồn tại. Vui lòng chọn tên khác", exception.getMessage());
                
                // Verify validator called repository to check uniqueness but didn't call save
                verify(serviceItemRepository).findByName("Existing Service Name");
                verify(serviceItemRepository, never()).save(any(ServiceItem.class));
        }

        @Test
        void update_UTC001_ValidUpdate_ShouldReturnUpdatedServiceItem() {
                // Arrange
                ServiceItem existingServiceItem = new ServiceItem();
                existingServiceItem.setId(testServiceItemId);
                existingServiceItem.setName("Original Service Name");
                existingServiceItem.setPrice(new BigDecimal("200000"));
                existingServiceItem.setVatRate(new BigDecimal("0.08"));
                existingServiceItem.setWarrantyDays(15);
                existingServiceItem.setActive(true);

                ServiceItem incomingServiceItem = new ServiceItem();
                incomingServiceItem.setName("Updated Cleaning Service");
                incomingServiceItem.setPrice(new BigDecimal("300000"));
                incomingServiceItem.setVatRate(new BigDecimal("0.12"));
                incomingServiceItem.setWarrantyDays(60);
                incomingServiceItem.setActive(false);

                // Mock repository behaviors for successful update
                when(serviceItemRepository.findById(eq(testServiceItemId)))
                                .thenReturn(Optional.of(existingServiceItem));
                // Mock uniqueness check to return false (no other item has this name)
                when(serviceItemRepository.existsByNameAndIdNot("Updated Cleaning Service", testServiceItemId))
                                .thenReturn(false);
                when(serviceItemRepository.save(any(ServiceItem.class))).thenReturn(existingServiceItem);

                // Act
                ServiceItem result = serviceItemService.update(testServiceItemId, incomingServiceItem);

                // Assert
                assertNotNull(result);
                assertEquals(testServiceItemId, result.getId());
                assertEquals("Updated Cleaning Service", result.getName());
                assertEquals(new BigDecimal("300000"), result.getPrice());
                assertEquals(new BigDecimal("0.12"), result.getVatRate());
                assertEquals(60, result.getWarrantyDays());
                assertFalse(result.isActive());
                
                // Verify validator called repository to check uniqueness
                verify(serviceItemRepository).existsByNameAndIdNot("Updated Cleaning Service", testServiceItemId);
                verify(serviceItemRepository).save(any(ServiceItem.class));
        }

        @Test
        void update_UTC002_ServiceItemNotFound_ShouldThrowNotFoundException() {
                // Arrange
                UUID nonExistentId = UUID.randomUUID();
                ServiceItem incomingServiceItem = new ServiceItem();

                when(serviceItemRepository.findById(eq(nonExistentId))).thenReturn(Optional.empty());

                // Act & Assert
                NotFoundException exception = assertThrows(NotFoundException.class,
                                () -> serviceItemService.update(nonExistentId, incomingServiceItem));

                assertEquals("Không tìm thấy dịch vụ với ID: " + nonExistentId, exception.getMessage());
                
                // Verify repository was called to find the item
                verify(serviceItemRepository).findById(nonExistentId);
        }

        @Test
        void update_UTC003_DuplicateNameAnotherItem_ShouldThrowValidationException() {
                // Arrange
                ServiceItem existingServiceItem = new ServiceItem();
                existingServiceItem.setId(testServiceItemId);
                existingServiceItem.setName("Original Service Name");

                ServiceItem incomingServiceItem = new ServiceItem();
                incomingServiceItem.setName("Name of another existing service");

                // Mock repository behaviors for duplicate name scenario
                when(serviceItemRepository.findById(eq(testServiceItemId)))
                                .thenReturn(Optional.of(existingServiceItem));
                // Mock uniqueness check to return true (another item has this name)
                when(serviceItemRepository.existsByNameAndIdNot("Name of another existing service", testServiceItemId))
                                .thenReturn(true);

                // Act & Assert
                ValidationException exception = assertThrows(ValidationException.class,
                                () -> serviceItemService.update(testServiceItemId, incomingServiceItem));

                assertEquals("Tên dịch vụ đã tồn tại. Vui lòng chọn tên khác", exception.getMessage());
                
                // Verify validator called repository to check uniqueness but didn't save
                verify(serviceItemRepository).existsByNameAndIdNot("Name of another existing service", testServiceItemId);
                verify(serviceItemRepository, never()).save(any(ServiceItem.class));
        }

        @Test
        void update_UTC004_UpdateWithSameExistingName_ShouldReturnUpdatedServiceItem() {
                // Arrange
                ServiceItem existingServiceItem = new ServiceItem();
                existingServiceItem.setId(testServiceItemId);
                existingServiceItem.setName("Original Service Name");
                existingServiceItem.setPrice(new BigDecimal("200000"));
                existingServiceItem.setActive(true);

                ServiceItem incomingServiceItem = new ServiceItem();
                incomingServiceItem.setName("Original Service Name"); // Same name
                incomingServiceItem.setPrice(new BigDecimal("250000")); // Updated price
                incomingServiceItem.setActive(false); // Updated active status

                // Mock repository behaviors for same name update
                when(serviceItemRepository.findById(eq(testServiceItemId)))
                                .thenReturn(Optional.of(existingServiceItem));
                // Mock uniqueness check to return false (same item name, different ID excluded)
                when(serviceItemRepository.existsByNameAndIdNot("Original Service Name", testServiceItemId))
                                .thenReturn(false);
                when(serviceItemRepository.save(any(ServiceItem.class))).thenReturn(existingServiceItem);

                // Act
                ServiceItem result = serviceItemService.update(testServiceItemId, incomingServiceItem);

                // Assert
                assertNotNull(result);
                assertEquals(testServiceItemId, result.getId());
                assertEquals("Original Service Name", result.getName());
                assertEquals(new BigDecimal("250000"), result.getPrice());
                assertFalse(result.isActive());
                
                // Verify validator called repository to check uniqueness
                verify(serviceItemRepository).existsByNameAndIdNot("Original Service Name", testServiceItemId);
                verify(serviceItemRepository).save(any(ServiceItem.class));
        }

        @Test
        void importCSV_UTC001_ValidCSVNewItems_ShouldCompleteSuccessfully() throws Exception {
                // Arrange
                String csvContent = "Name,Price,VatRate,WarrantyDays,Active\n" +
                                "New Service A,100000,0.1,7,true\n" +
                                "New Service B,200000,0.08,30,true";
                MockMultipartFile file = new MockMultipartFile("test.csv", "test.csv", "text/csv",
                                csvContent.getBytes());

                // Mock repository to simulate all items are new (don't exist yet)
                when(serviceItemRepository.findByName(eq("New Service A"))).thenReturn(Optional.empty());
                when(serviceItemRepository.findByName(eq("New Service B"))).thenReturn(Optional.empty());
                when(serviceItemRepository.saveAll(anyList())).thenReturn(List.of());

                // Act & Assert
                assertDoesNotThrow(() -> serviceItemService.importCSV(file));
                
                // Verify repository interactions
                verify(serviceItemRepository).findByName("New Service A");
                verify(serviceItemRepository).findByName("New Service B");
                verify(serviceItemRepository).saveAll(anyList());
        }

        @Test
        void importCSV_UTC002_ValidCSVMixedNewAndUpdate_ShouldCompleteSuccessfully() throws Exception {
                // Arrange
                String csvContent = "Name,Price,VatRate,WarrantyDays,Active\n" +
                                "Existing Service C,150000,0.1,15,false\n" +
                                "New Service D,300000,0.08,60,true";
                MockMultipartFile file = new MockMultipartFile("test.csv", "test.csv", "text/csv",
                                csvContent.getBytes());

                ServiceItem existingServiceC = new ServiceItem();
                existingServiceC.setId(UUID.randomUUID());
                existingServiceC.setName("Existing Service C");

                // Mock repository to simulate mixed scenario: one existing, one new
                when(serviceItemRepository.findByName(eq("Existing Service C")))
                                .thenReturn(Optional.of(existingServiceC));
                when(serviceItemRepository.findByName(eq("New Service D"))).thenReturn(Optional.empty());
                when(serviceItemRepository.saveAll(anyList())).thenReturn(List.of());

                // Act & Assert
                assertDoesNotThrow(() -> serviceItemService.importCSV(file));
                
                // Verify repository interactions
                verify(serviceItemRepository).findByName("Existing Service C");
                verify(serviceItemRepository).findByName("New Service D");
                verify(serviceItemRepository).saveAll(anyList());
        }

        @Test
        void importCSV_UTC003_EmptyCSVFile_ShouldThrowCSVImportException() throws Exception {
                // Arrange
                MockMultipartFile file = new MockMultipartFile("test.csv", "test.csv", "text/csv", "".getBytes());

                // Act & Assert
                // Real validator will detect empty file and throw exception
                CSVImportException exception = assertThrows(CSVImportException.class,
                                () -> serviceItemService.importCSV(file));

                assertEquals("File CSV trống hoặc không hợp lệ", exception.getMessage());
        }

        @Test
        void importCSV_UTC004_InvalidPriceInCSV_ShouldThrowCSVImportException() throws Exception {
                // Arrange
                String csvContent = "Name,Price,VatRate,WarrantyDays,Active\n" +
                                "Service 1,invalid_price,0.1,7,true";
                MockMultipartFile file = new MockMultipartFile("test.csv", "test.csv", "text/csv",
                                csvContent.getBytes());

                // Act & Assert
                // NumberFormatException will be caught and converted to CSVImportException
                CSVImportException exception = assertThrows(CSVImportException.class,
                                () -> serviceItemService.importCSV(file));

                assertEquals("Dữ liệu số không hợp lệ", exception.getMessage());
        }

        @Test
        void importCSV_UTC005_PriceIsZero_ShouldThrowCSVImportException() throws Exception {
                // Arrange
                String csvContent = "Name,Price,VatRate,WarrantyDays,Active\n" +
                                "Service 1,0,0.1,7,true";
                MockMultipartFile file = new MockMultipartFile("test.csv", "test.csv", "text/csv",
                                csvContent.getBytes());

                // Act & Assert
                // Real validator will detect price = 0 and throw exception
                CSVImportException exception = assertThrows(CSVImportException.class,
                                () -> serviceItemService.importCSV(file));

                assertEquals("Giá dịch vụ phải lớn hơn 0", exception.getMessage());
        }
}