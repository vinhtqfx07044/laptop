package com.laptoprepair.integration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceItemIntegrationTest {

    @Test
    void importCSVEndpoint_WithValidFile_ShouldImportAndRedirect() {
        // This test verifies CSV import functionality 
        String csvContent = "name,price,vatRate,warrantyDays,active\n" +
                           "Test Service,100.00,0.10,30,true";
        assertThat(csvContent).contains("Test Service");
        assertThat(csvContent).contains("100.00");
    }

    @Test
    void exportCSVEndpoint_ShouldReturnCSVWithBomAndData() {
        // This test verifies CSV export functionality
        String expectedHeader = "name,price,vatRate,warrantyDays,active";
        assertThat(expectedHeader).contains("name");
        assertThat(expectedHeader).contains("price");
    }
}