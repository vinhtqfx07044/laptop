package com.laptoprepair.csv;

import com.laptoprepair.entity.ServiceItem;
import com.laptoprepair.exception.CSVImportException;
import com.laptoprepair.validation.ServiceItemValidator;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceItemCsvParserTest {

    @Mock
    private ServiceItemValidator serviceItemValidator;

    @Mock
    private CSVRecord csvRecord;

    @InjectMocks
    private ServiceItemCsvParser csvParser;

    @Test
    void parse_WithValidRecord_ShouldReturnServiceItem() throws CSVImportException {
        when(csvRecord.get("name")).thenReturn("Test Service");
        when(csvRecord.get("price")).thenReturn("100.00");
        when(csvRecord.get("vatRate")).thenReturn("0.10");
        when(csvRecord.get("warrantyDays")).thenReturn("30");
        when(csvRecord.get("active")).thenReturn("true");

        ServiceItem result = csvParser.parse(csvRecord, 1);

        assertThat(result.getName()).isEqualTo("Test Service");
        assertThat(result.getPrice()).isEqualTo(new BigDecimal("100.00"));
        assertThat(result.getVatRate()).isEqualTo(new BigDecimal("0.10"));
        assertThat(result.getWarrantyDays()).isEqualTo(30);
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void parse_WithInvalidNumberFormat_ShouldThrowCSVImportException() {
        when(csvRecord.get("name")).thenReturn("Test Service");
        when(csvRecord.get("price")).thenReturn("invalid-price");

        assertThatThrownBy(() -> csvParser.parse(csvRecord, 1))
                .isInstanceOf(CSVImportException.class)
                .hasMessage("Dữ liệu số không hợp lệ");
    }
}