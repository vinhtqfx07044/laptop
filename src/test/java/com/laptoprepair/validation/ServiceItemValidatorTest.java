package com.laptoprepair.validation;

import com.laptoprepair.entity.ServiceItem;
import com.laptoprepair.exception.CSVImportException;
import com.laptoprepair.exception.ValidationException;
import com.laptoprepair.repository.ServiceItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceItemValidatorTest {

    @Mock
    private ServiceItemRepository repo;

    @InjectMocks
    private ServiceItemValidator validator;

    @Test
    void validateUniqueNameOnCreate_WithExistingName_ShouldThrowValidationException() {
        String existingName = "Thay màn hình";
        ServiceItem existingItem = new ServiceItem();
        existingItem.setName(existingName);
        when(repo.findByName(existingName)).thenReturn(Optional.of(existingItem));

        assertThatThrownBy(() -> validator.validateUniqueNameOnCreate(existingName))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Tên dịch vụ đã tồn tại. Vui lòng chọn tên khác");
    }

    @Test
    void validateUniqueNameOnUpdate_WithExistingNameAndDifferentId_ShouldThrowValidationException() {
        UUID id = UUID.randomUUID();
        String existingName = "Thay màn hình";
        when(repo.existsByNameAndIdNot(existingName, id)).thenReturn(true);

        assertThatThrownBy(() -> validator.validateUniqueNameOnUpdate(id, existingName))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Tên dịch vụ đã tồn tại. Vui lòng chọn tên khác");
    }

    @Test
    void validateCSVFile_WithEmptyFile_ShouldThrowCSVImportException() {
        MultipartFile emptyFile = new MockMultipartFile("file", "", "text/csv", new byte[0]);

        assertThatThrownBy(() -> validator.validateCSVFile(emptyFile))
                .isInstanceOf(CSVImportException.class)
                .hasMessage("File CSV trống hoặc không hợp lệ");
    }

    @Test
    void validateCSVName_WithEmptyName_ShouldThrowCSVImportException() {
        String emptyName = "   ";
        int rowNumber = 3;

        assertThatThrownBy(() -> validator.validateCSVName(emptyName, rowNumber))
                .isInstanceOf(CSVImportException.class)
                .hasMessage("Tên dịch vụ không được để trống");
    }

    @Test
    void validateCSVPrice_WithZeroPrice_ShouldThrowCSVImportException() {
        BigDecimal zeroPrice = BigDecimal.ZERO;
        int rowNumber = 2;

        assertThatThrownBy(() -> validator.validateCSVPrice(zeroPrice, rowNumber))
                .isInstanceOf(CSVImportException.class)
                .hasMessage("Giá dịch vụ phải lớn hơn 0");
    }

    @Test
    void validateCSVPrice_WithNegativePrice_ShouldThrowCSVImportException() {
        BigDecimal negativePrice = new BigDecimal("-100");
        int rowNumber = 3;

        assertThatThrownBy(() -> validator.validateCSVPrice(negativePrice, rowNumber))
                .isInstanceOf(CSVImportException.class)
                .hasMessage("Giá dịch vụ phải lớn hơn 0");
    }

    @Test
    void validateCSVVatRate_WithNegativeVatRate_ShouldThrowCSVImportException() {
        BigDecimal negativeVatRate = new BigDecimal("-0.1");
        int rowNumber = 2;

        assertThatThrownBy(() -> validator.validateCSVVatRate(negativeVatRate, rowNumber))
                .isInstanceOf(CSVImportException.class)
                .hasMessage("Thuế VAT không được là số âm");
    }

    @Test
    void validateCSVWarrantyDays_WithNegativeWarrantyDays_ShouldThrowCSVImportException() {
        int negativeWarrantyDays = -1;
        int rowNumber = 2;

        assertThatThrownBy(() -> validator.validateCSVWarrantyDays(negativeWarrantyDays, rowNumber))
                .isInstanceOf(CSVImportException.class)
                .hasMessage("Số ngày bảo hành không được là số âm");
    }
}