package com.laptoprepair.validation;

import com.laptoprepair.entity.Request;
import com.laptoprepair.enums.RequestStatus;
import com.laptoprepair.exception.ValidationException;
import com.laptoprepair.service.HistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class RequestValidator {
    private final HistoryService historyService;

    public void validateAppointmentDateInFuture(LocalDateTime appointmentDate) {
        if (appointmentDate != null && appointmentDate.isBefore(LocalDateTime.now())) {
            throw new ValidationException("Ngày hẹn phải sau thời điểm hiện tại");
        }
    }

    public void validateEditable(Request existingRequest) {
        RequestStatus status = existingRequest.getStatus();
        if (status == RequestStatus.COMPLETED || status == RequestStatus.CANCELLED) {
            throw new ValidationException(
                    "Không thể chỉnh sửa phiếu ở trạng thái \"" + status.getDisplayName() + "\"");
        }
    }

    public void validateStatusTransition(Request existing, Request incomingRequest) {
        if (!existing.getStatus().canTransitionTo(incomingRequest.getStatus())) {
            throw new ValidationException(
                    "Không thể chuyển đổi trạng thái phiếu từ " + existing.getStatus() +
                            " đến " + incomingRequest.getStatus());
        }
    }

    public void validateItemsForStatus(Request incomingRequest) {
        if (incomingRequest == null || incomingRequest.getStatus() == null) {
            return;
        }

        RequestStatus status = incomingRequest.getStatus();
        if (status != RequestStatus.SCHEDULED && status != RequestStatus.CANCELLED
                && (incomingRequest.getItems() == null || incomingRequest.getItems().isEmpty())) {
            throw new ValidationException("Phiếu ở trạng thái \"" + status.getDisplayName() +
                    "\" phải có ít nhất một hạng mục dịch vụ");
        }
    }

    public void validateNoItemModificationWhenLocked(Request existingRequest, Request incomingRequest) {
        if (existingRequest.getStatus().isRequestItemsLocked()
                && !historyService.areItemsEqual(existingRequest.getItems(), incomingRequest.getItems())) {
            throw new ValidationException("Phiếu đã ở trạng thái \"" +
                    existingRequest.getStatus().getDisplayName() +
                    "\" và không thể thay đổi hạng mục.");
        }
    }
}