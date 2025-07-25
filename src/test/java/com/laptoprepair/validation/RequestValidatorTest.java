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

import static org.assertj.core.api.Assertions.*;

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
    void validateItemsForStatus_InProgressWithoutItems_ShouldThrowValidationException() {
        Request request = new Request();
        request.setStatus(RequestStatus.IN_PROGRESS);
        request.setItems(new ArrayList<>());

        assertThatThrownBy(() -> requestValidator.validateItemsForStatus(request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Phiếu ở trạng thái \"Đang thực hiện\" phải có ít nhất một hạng mục dịch vụ");
    }
}