package com.laptoprepair.integration;

import com.laptoprepair.entity.ServiceItem;
import com.laptoprepair.repository.ServiceItemRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.hamcrest.Matchers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(roles = "STAFF")
class ServiceItemIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ServiceItemRepository serviceItemRepository;

        @Test
        void importCSVEndpoint_WithValidFile_ShouldImportServiceItems() throws Exception {
                String csvContent = "name,price,vatRate,warrantyDays,active\n" +
                                "Thay màn hình laptop,500000,0.10,30,true\n" +
                                "Sửa lỗi phần mềm,200000,0.10,7,true";

                MockMultipartFile csvFile = new MockMultipartFile(
                                "file", "services.csv", "text/csv", csvContent.getBytes());

                mockMvc.perform(multipart("/staff/service-items/import")
                                .file(csvFile))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/staff/service-items"));
        }

        @Test
        void exportCSVEndpoint_ShouldReturnCSVWithData() throws Exception {
                ServiceItem item1 = createTestServiceItem("Service 1", new BigDecimal("100000"));
                ServiceItem item2 = createTestServiceItem("Service 2", new BigDecimal("200000"));
                serviceItemRepository.save(item1);
                serviceItemRepository.save(item2);

                mockMvc.perform(get("/staff/service-items/export"))
                                .andExpect(status().isOk())
                                .andExpect(header().string("Content-Type", "text/csv; charset=UTF-8"))
                                .andExpect(header().string("Content-Disposition",
                                                "attachment; filename=service-items.csv"))
                                .andExpect(content().string(Matchers.containsString("Service 1")))
                                .andExpect(content().string(Matchers.containsString("Service 2")));
        }

        @Test
        void createServiceItem_WithValidData_ShouldPersist() throws Exception {
                mockMvc.perform(post("/staff/service-items/create")
                                .param("name", "Test Service Item")
                                .param("price", "150000")
                                .param("vatRate", "0.10")
                                .param("warrantyDays", "30")
                                .param("active", "true"))
                                .andExpect(status().is3xxRedirection());
        }

        @Test
        void importCSVEndpoint_WithInvalidData_ShouldReturnError() throws Exception {
                String invalidCsvContent = "name,price,vatRate,warrantyDays,active\n" +
                                "Invalid Service,invalid_price,0.10,30,true";

                MockMultipartFile csvFile = new MockMultipartFile(
                                "file", "invalid.csv", "text/csv", invalidCsvContent.getBytes());

                mockMvc.perform(multipart("/staff/service-items/import")
                                .file(csvFile))
                                .andExpect(status().is3xxRedirection());
        }

        private ServiceItem createTestServiceItem(String name, BigDecimal price) {
                ServiceItem item = new ServiceItem();
                item.setName(name);
                item.setPrice(price);
                item.setVatRate(new BigDecimal("0.10"));
                item.setWarrantyDays(30);
                item.setActive(true);
                return item;
        }
}