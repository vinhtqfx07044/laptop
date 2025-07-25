package com.laptoprepair.service;

import com.laptoprepair.entity.RequestItem;
import com.laptoprepair.entity.ServiceItem;
import com.laptoprepair.exception.NotFoundException;
import com.laptoprepair.repository.ServiceItemRepository;
import com.laptoprepair.service.impl.MappingServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MappingServiceImplTest {

    @Mock
    private ServiceItemRepository serviceItemRepository;

    @InjectMocks
    private MappingServiceImpl mappingService;

    @Test
    void snapshotServiceItems_WithExistingService_ShouldCopyFields() {
        UUID serviceItemId = UUID.randomUUID();
        RequestItem requestItem = new RequestItem();
        requestItem.setServiceItemId(serviceItemId);
        requestItem.setName("Old Name");
        requestItem.setPrice(BigDecimal.valueOf(50));
        requestItem.setDiscount(BigDecimal.valueOf(5));

        ServiceItem serviceItem = new ServiceItem();
        serviceItem.setName("New Service Name");
        serviceItem.setPrice(BigDecimal.valueOf(100));
        serviceItem.setVatRate(BigDecimal.valueOf(0.1));

        when(serviceItemRepository.findByIdAndActive(serviceItemId)).thenReturn(Optional.of(serviceItem));

        mappingService.snapshotServiceItems(List.of(requestItem));

        assertThat(requestItem.getName()).isEqualTo("New Service Name");
        assertThat(requestItem.getPrice()).isEqualTo(BigDecimal.valueOf(100));
        assertThat(requestItem.getVatRate()).isEqualTo(BigDecimal.valueOf(0.1));
    }

    @Test
    void snapshotServiceItems_WhenServiceNotFound_ShouldThrowNotFoundException() {
        UUID serviceItemId = UUID.randomUUID();
        RequestItem requestItem = new RequestItem();
        requestItem.setServiceItemId(serviceItemId);
        requestItem.setName("Test Service");

        when(serviceItemRepository.findByIdAndActive(serviceItemId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mappingService.snapshotServiceItems(List.of(requestItem)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Không tìm dịch vụ sửa chửa: Test Service");
    }
}