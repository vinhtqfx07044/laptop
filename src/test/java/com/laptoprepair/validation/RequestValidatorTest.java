package com.laptoprepair.validation;

import com.laptoprepair.entity.Request;
import com.laptoprepair.enums.RequestStatus;
import com.laptoprepair.exception.ValidationException;
import com.laptoprepair.service.HistoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestValidatorTest {

    @Mock
    private HistoryService historyService;

    @InjectMocks
    private RequestValidator requestValidator;

    @Test
    void validateAppointmentDateInFuture_WithPastDate_ShouldThrowValidationException() {
        LocalDateTime pastDate = LocalDateTime.now().minusDays(1);

        assertThatThrownBy(() -> requestValidator.validateAppointmentDateInFuture(pastDate))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Ngày hẹn phải sau thời điểm hiện tại");
    }

    @Test
    void validateEditable_WithCompletedStatus_ShouldThrowValidationException() {
        Request request = new Request();
        request.setStatus(RequestStatus.COMPLETED);

        assertThatThrownBy(() -> requestValidator.validateEditable(request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Không thể chỉnh sửa phiếu ở trạng thái \"Hoàn thành\"");
    }

    @Test
    void validateEditable_WithCancelledStatus_ShouldThrowValidationException() {
        Request request = new Request();
        request.setStatus(RequestStatus.CANCELLED);

        assertThatThrownBy(() -> requestValidator.validateEditable(request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Không thể chỉnh sửa phiếu ở trạng thái \"Đã hủy\"");
    }

    @Test
    void validateStatusTransition_WithInvalidTransition_ShouldThrowValidationException() {
        Request existing = new Request();
        existing.setStatus(RequestStatus.COMPLETED);

        Request incoming = new Request();
        incoming.setStatus(RequestStatus.IN_PROGRESS);

        assertThatThrownBy(() -> requestValidator.validateStatusTransition(existing, incoming))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Không thể chuyển đổi trạng thái phiếu từ");
    }

    @Test
    void validateItemsForStatus_WithQuotedStatusAndNoItems_ShouldThrowValidationException() {
        Request request = new Request();
        request.setStatus(RequestStatus.QUOTED);
        request.setItems(new ArrayList<>());

        assertThatThrownBy(() -> requestValidator.validateItemsForStatus(request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Phiếu ở trạng thái \"Đã báo giá\" phải có ít nhất một hạng mục dịch vụ");
    }

    @Test
    void validateNoItemModificationWhenLocked_WithLockedStatusAndDifferentItems_ShouldThrowValidationException() {
        Request existing = new Request();
        existing.setStatus(RequestStatus.COMPLETED);
        existing.setItems(List.of());

        Request incoming = new Request();
        incoming.setItems(List.of());

        when(historyService.areItemsEqual(existing.getItems(), incoming.getItems())).thenReturn(false);

        assertThatThrownBy(() -> requestValidator.validateNoItemModificationWhenLocked(existing, incoming))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Phiếu đã ở trạng thái \"Hoàn thành\" và không thể thay đổi hạng mục.");
    }
}