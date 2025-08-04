package com.laptoprepair.service.impl;

import com.laptoprepair.entity.ServiceItem;
import com.laptoprepair.exception.CSVImportException;
import com.laptoprepair.exception.ValidationException;
import com.laptoprepair.repository.ServiceItemRepository;
import com.laptoprepair.validation.ServiceItemValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceItemServiceImplTest {

    @Mock
    private ServiceItemRepository serviceItemRepository;
    
    @Mock
    private ServiceItemValidator serviceItemValidator;
    
    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private ServiceItemServiceImpl serviceItemService;

    private ServiceItem testServiceItem;

    @BeforeEach
    void setUp() {
        testServiceItem = new ServiceItem();
        testServiceItem.setName("Test Service");
        testServiceItem.setPrice(BigDecimal.valueOf(100000));
        testServiceItem.setVatRate(BigDecimal.valueOf(0.1));
        testServiceItem.setWarrantyDays(30);
        testServiceItem.setActive(true);
    }

    @DisplayName("serviceItemCRUDTest - UTCID01: Create service item with unique name")
    @Test
    void create_UniqueServiceItemName_ShouldCreateSuccessfully() {
        // Arrange
        ServiceItem expectedServiceItem = new ServiceItem();
        expectedServiceItem.setId(UUID.randomUUID());
        expectedServiceItem.setName("Test Service");
        expectedServiceItem.setPrice(BigDecimal.valueOf(100000));
        
        when(serviceItemRepository.save(testServiceItem)).thenReturn(expectedServiceItem);
        doNothing().when(serviceItemValidator).validateUniqueNameOnCreate("Test Service");

        // Act
        ServiceItem result = serviceItemService.create(testServiceItem);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Service");
        assertThat(result.getPrice()).isEqualTo(BigDecimal.valueOf(100000));
        
        verify(serviceItemValidator).validateUniqueNameOnCreate("Test Service");
        verify(serviceItemRepository).save(testServiceItem);
    }

    @DisplayName("serviceItemCRUDTest - UTCID02: Create service item with duplicate name should throw ValidationException")
    @Test
    void create_DuplicateServiceItemName_ShouldThrowValidationException() {
        // Arrange
        String expectedMessage = "Tên dịch vụ đã tồn tại";
        
        doThrow(new ValidationException(expectedMessage))
            .when(serviceItemValidator).validateUniqueNameOnCreate("Test Service");

        // Act & Assert
        assertThatThrownBy(() -> serviceItemService.create(testServiceItem))
            .isInstanceOf(ValidationException.class)
            .hasMessage(expectedMessage);
        
        verify(serviceItemValidator).validateUniqueNameOnCreate("Test Service");
        verify(serviceItemRepository, never()).save(any(ServiceItem.class));
    }

    @DisplayName("serviceItemCRUDTest - UTCID03: Update service item with conflicting name should throw ValidationException")
    @Test
    void update_ConflictingServiceItemName_ShouldThrowValidationException() {
        // Arrange
        UUID serviceItemId = UUID.randomUUID();
        String expectedMessage = "Tên dịch vụ đã được sử dụng bởi dịch vụ khác";
        
        ServiceItem existingServiceItem = new ServiceItem();
        existingServiceItem.setId(serviceItemId);
        existingServiceItem.setName("Existing Service");
        
        ServiceItem incomingServiceItem = new ServiceItem();
        incomingServiceItem.setName("Conflicting Service");
        
        when(serviceItemRepository.findById(serviceItemId)).thenReturn(Optional.of(existingServiceItem));
        doThrow(new ValidationException(expectedMessage))
            .when(serviceItemValidator).validateUniqueNameOnUpdate(serviceItemId, "Conflicting Service");

        // Act & Assert
        assertThatThrownBy(() -> serviceItemService.update(serviceItemId, incomingServiceItem))
            .isInstanceOf(ValidationException.class)
            .hasMessage(expectedMessage);
        
        verify(serviceItemValidator).validateUniqueNameOnUpdate(serviceItemId, "Conflicting Service");
        verify(serviceItemRepository, never()).save(any(ServiceItem.class));
    }

    @DisplayName("importCSVTest - UTCID01: Import valid CSV content successfully")
    @Test
    void importCSV_ValidCSVContent_ShouldImportSuccessfully() throws IOException, CSVImportException {
        // Arrange
        String csvContent = "name,price,vatRate,warrantyDays,active\n" +
                           "Service A,50000,0.1,30,true\n" +
                           "Service B,75000,0.1,60,false";
        
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes()));
        when(serviceItemRepository.findByName("Service A")).thenReturn(Optional.empty());
        when(serviceItemRepository.findByName("Service B")).thenReturn(Optional.empty());
        
        doNothing().when(serviceItemValidator).validateCSVFile(multipartFile);
        doNothing().when(serviceItemValidator).validateCSVName(anyString(), anyInt());
        doNothing().when(serviceItemValidator).validateCSVPrice(any(BigDecimal.class), anyInt());
        doNothing().when(serviceItemValidator).validateCSVVatRate(any(BigDecimal.class), anyInt());
        doNothing().when(serviceItemValidator).validateCSVWarrantyDays(anyInt(), anyInt());

        // Act
        assertThatCode(() -> serviceItemService.importCSV(multipartFile))
            .doesNotThrowAnyException();

        // Assert
        verify(serviceItemValidator).validateCSVFile(multipartFile);
        verify(serviceItemRepository).saveAll(argThat(list -> {
            if (!(list instanceof List)) return false;
            List<ServiceItem> serviceList = (List<ServiceItem>) list;
            return serviceList.size() == 2 && 
                   serviceList.get(0).getName().equals("Service A") && 
                   serviceList.get(1).getName().equals("Service B");
        }));
    }

    @DisplayName("importCSVTest - UTCID02: Import CSV with invalid price should throw CSVImportException")
    @Test
    void importCSV_InvalidPriceInCSV_ShouldThrowCSVImportException() throws IOException, CSVImportException {
        // Arrange
        String csvContent = "name,price,vatRate,warrantyDays,active\n" +
                           "Service A,invalid_price,0.1,30,true";
        
        String expectedMessage = "Dữ liệu số không hợp lệ";
        
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes()));
        doNothing().when(serviceItemValidator).validateCSVFile(multipartFile);
        doNothing().when(serviceItemValidator).validateCSVName(anyString(), anyInt());

        // Act & Assert
        assertThatThrownBy(() -> serviceItemService.importCSV(multipartFile))
            .isInstanceOf(CSVImportException.class)
            .hasMessageContaining(expectedMessage);
        
        verify(serviceItemValidator).validateCSVFile(multipartFile);
        verify(serviceItemRepository, never()).saveAll(any());
    }

    @DisplayName("importCSVTest - UTCID03: Import empty CSV file should throw CSVImportException")
    @Test
    void importCSV_EmptyFile_ShouldThrowCSVImportException() throws CSVImportException {
        // Arrange
        String expectedMessage = "File CSV trống";
        
        doThrow(new CSVImportException(expectedMessage))
            .when(serviceItemValidator).validateCSVFile(multipartFile);

        // Act & Assert
        assertThatThrownBy(() -> serviceItemService.importCSV(multipartFile))
            .isInstanceOf(CSVImportException.class)
            .hasMessage(expectedMessage);
        
        verify(serviceItemValidator).validateCSVFile(multipartFile);
        verify(serviceItemRepository, never()).saveAll(any());
    }
}