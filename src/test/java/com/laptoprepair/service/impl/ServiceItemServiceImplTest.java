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

    @Mock
    private ServiceItemValidator serviceItemValidator;

    @InjectMocks
    private ServiceItemServiceImpl serviceItemService;

    private ServiceItem testServiceItem;
    private UUID testServiceItemId;

    @BeforeEach
    void setUp() {
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

        doNothing().when(serviceItemValidator).validateUniqueNameOnCreate(eq("Laptop Cleaning"));
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
    }

    @Test
    void create_UTC002_DuplicateName_ShouldThrowValidationException() {
        // Arrange
        ServiceItem inputServiceItem = new ServiceItem();
        inputServiceItem.setName("Existing Service Name");

        doThrow(new ValidationException("Tên dịch vụ đã tồn tại. Vui lòng chọn tên khác"))
                .when(serviceItemValidator).validateUniqueNameOnCreate(eq("Existing Service Name"));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
                () -> serviceItemService.create(inputServiceItem));

        assertEquals("Tên dịch vụ đã tồn tại. Vui lòng chọn tên khác", exception.getMessage());
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

        when(serviceItemRepository.findById(eq(testServiceItemId))).thenReturn(Optional.of(existingServiceItem));
        doNothing().when(serviceItemValidator).validateUniqueNameOnUpdate(eq(testServiceItemId),
                eq("Updated Cleaning Service"));
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
    }

    @Test
    void update_UTC003_DuplicateNameAnotherItem_ShouldThrowValidationException() {
        // Arrange
        ServiceItem existingServiceItem = new ServiceItem();
        existingServiceItem.setId(testServiceItemId);
        existingServiceItem.setName("Original Service Name");

        ServiceItem incomingServiceItem = new ServiceItem();
        incomingServiceItem.setName("Name of another existing service");

        when(serviceItemRepository.findById(eq(testServiceItemId))).thenReturn(Optional.of(existingServiceItem));
        doThrow(new ValidationException("Tên dịch vụ đã tồn tại. Vui lòng chọn tên khác"))
                .when(serviceItemValidator)
                .validateUniqueNameOnUpdate(eq(testServiceItemId), eq("Name of another existing service"));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
                () -> serviceItemService.update(testServiceItemId, incomingServiceItem));

        assertEquals("Tên dịch vụ đã tồn tại. Vui lòng chọn tên khác", exception.getMessage());
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

        when(serviceItemRepository.findById(eq(testServiceItemId))).thenReturn(Optional.of(existingServiceItem));
        doNothing().when(serviceItemValidator).validateUniqueNameOnUpdate(eq(testServiceItemId),
                eq("Original Service Name"));
        when(serviceItemRepository.save(any(ServiceItem.class))).thenReturn(existingServiceItem);

        // Act
        ServiceItem result = serviceItemService.update(testServiceItemId, incomingServiceItem);

        // Assert
        assertNotNull(result);
        assertEquals(testServiceItemId, result.getId());
        assertEquals("Original Service Name", result.getName());
        assertEquals(new BigDecimal("250000"), result.getPrice());
        assertFalse(result.isActive());
    }

    @Test
    void importCSV_UTC001_ValidCSVNewItems_ShouldCompleteSuccessfully() throws Exception {
        // Arrange
        String csvContent = "Name,Price,VatRate,WarrantyDays,Active\n" +
                "New Service A,100000,0.1,7,true\n" +
                "New Service B,200000,0.08,30,true";
        MockMultipartFile file = new MockMultipartFile("test.csv", "test.csv", "text/csv", csvContent.getBytes());

        doNothing().when(serviceItemValidator).validateCSVFile(eq(file));
        when(serviceItemRepository.findByName(eq("New Service A"))).thenReturn(Optional.empty());
        when(serviceItemRepository.findByName(eq("New Service B"))).thenReturn(Optional.empty());
        doNothing().when(serviceItemValidator).validateCSVName(anyString(), anyInt());
        doNothing().when(serviceItemValidator).validateCSVPrice(any(BigDecimal.class), anyInt());
        doNothing().when(serviceItemValidator).validateCSVVatRate(any(BigDecimal.class), anyInt());
        doNothing().when(serviceItemValidator).validateCSVWarrantyDays(anyInt(), anyInt());
        when(serviceItemRepository.saveAll(anyList())).thenReturn(List.of());

        // Act & Assert
        assertDoesNotThrow(() -> serviceItemService.importCSV(file));
    }

    @Test
    void importCSV_UTC002_ValidCSVMixedNewAndUpdate_ShouldCompleteSuccessfully() throws Exception {
        // Arrange
        String csvContent = "Name,Price,VatRate,WarrantyDays,Active\n" +
                "Existing Service C,150000,0.1,15,false\n" +
                "New Service D,300000,0.08,60,true";
        MockMultipartFile file = new MockMultipartFile("test.csv", "test.csv", "text/csv", csvContent.getBytes());

        ServiceItem existingServiceC = new ServiceItem();
        existingServiceC.setId(UUID.randomUUID());
        existingServiceC.setName("Existing Service C");

        doNothing().when(serviceItemValidator).validateCSVFile(eq(file));
        when(serviceItemRepository.findByName(eq("Existing Service C"))).thenReturn(Optional.of(existingServiceC));
        when(serviceItemRepository.findByName(eq("New Service D"))).thenReturn(Optional.empty());
        doNothing().when(serviceItemValidator).validateCSVName(anyString(), anyInt());
        doNothing().when(serviceItemValidator).validateCSVPrice(any(BigDecimal.class), anyInt());
        doNothing().when(serviceItemValidator).validateCSVVatRate(any(BigDecimal.class), anyInt());
        doNothing().when(serviceItemValidator).validateCSVWarrantyDays(anyInt(), anyInt());
        when(serviceItemRepository.saveAll(anyList())).thenReturn(List.of());

        // Act & Assert
        assertDoesNotThrow(() -> serviceItemService.importCSV(file));
    }

    @Test
    void importCSV_UTC003_EmptyCSVFile_ShouldThrowCSVImportException() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile("test.csv", "test.csv", "text/csv", "".getBytes());

        doThrow(new CSVImportException("File CSV trống hoặc không hợp lệ"))
                .when(serviceItemValidator).validateCSVFile(eq(file));

        // Act & Assert
        CSVImportException exception = assertThrows(CSVImportException.class,
                () -> serviceItemService.importCSV(file));

        assertEquals("File CSV trống hoặc không hợp lệ", exception.getMessage());
    }

    @Test
    void importCSV_UTC004_InvalidPriceInCSV_ShouldThrowCSVImportException() throws Exception {
        // Arrange
        String csvContent = "Name,Price,VatRate,WarrantyDays,Active\n" +
                "Service 1,invalid_price,0.1,7,true";
        MockMultipartFile file = new MockMultipartFile("test.csv", "test.csv", "text/csv", csvContent.getBytes());

        doNothing().when(serviceItemValidator).validateCSVFile(eq(file));

        // Act & Assert
        CSVImportException exception = assertThrows(CSVImportException.class,
                () -> serviceItemService.importCSV(file));

        assertEquals("Dữ liệu số không hợp lệ", exception.getMessage());
    }

    @Test
    void importCSV_UTC005_PriceIsZero_ShouldThrowCSVImportException() throws Exception {
        // Arrange
        String csvContent = "Name,Price,VatRate,WarrantyDays,Active\n" +
                "Service 1,0,0.1,7,true";
        MockMultipartFile file = new MockMultipartFile("test.csv", "test.csv", "text/csv", csvContent.getBytes());

        doNothing().when(serviceItemValidator).validateCSVFile(eq(file));
        doNothing().when(serviceItemValidator).validateCSVName(anyString(), anyInt());
        doThrow(new CSVImportException("Giá dịch vụ phải lớn hơn 0", 2))
                .when(serviceItemValidator).validateCSVPrice(eq(BigDecimal.ZERO), eq(2));

        // Act & Assert
        CSVImportException exception = assertThrows(CSVImportException.class,
                () -> serviceItemService.importCSV(file));

        assertEquals("Giá dịch vụ phải lớn hơn 0", exception.getMessage());
    }
}