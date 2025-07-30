package com.laptoprepair.validation;

import com.laptoprepair.entity.Request;
import com.laptoprepair.enums.RequestStatus;
import com.laptoprepair.exception.ValidationException;
import com.laptoprepair.service.HistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Validator for Request entities.
 * Provides methods to validate various aspects of a request, such as appointment date, status transitions, and service items.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RequestValidator {
    private final HistoryService historyService;

    /**
     * Validates that the appointment date is in the future.
     * @param appointmentDate The LocalDateTime representing the appointment date.
     * @throws ValidationException if the appointment date is not in the future.
     */
    public void validateAppointmentDateInFuture(LocalDateTime appointmentDate) {
        if (appointmentDate != null && appointmentDate.isBefore(LocalDateTime.now())) {
            throw new ValidationException("Ngày hẹn phải sau thời điểm hiện tại");
        }
    }

    /**
     * Validates that a request is editable (i.e., its status is not COMPLETED or CANCELLED).
     * @param existingRequest The existing Request object to validate.
     * @throws ValidationException if the request is not editable.
     */
    public void validateEditable(Request existingRequest) {
        RequestStatus status = existingRequest.getStatus();
        if (status == RequestStatus.COMPLETED || status == RequestStatus.CANCELLED) {
            throw new ValidationException(
                    "Không thể chỉnh sửa phiếu ở trạng thái \"" + status.getDisplayName() + "\"");
        }
    }

    /**
     * Validates the transition of a request's status.
     * @param existing The existing Request object.
     * @param incomingRequest The incoming Request object with the new status.
     * @throws ValidationException if the status transition is not allowed.
     */
    public void validateStatusTransition(Request existing, Request incomingRequest) {
        if (!existing.getStatus().canTransitionTo(incomingRequest.getStatus())) {
            throw new ValidationException(
                    "Không thể chuyển đổi trạng thái phiếu từ " + existing.getStatus() +
                            " đến " + incomingRequest.getStatus());
        }
    }

    /**
     * Validates that a request has service items if its status requires them.
     * @param incomingRequest The incoming Request object to validate.
     * @throws ValidationException if the request status requires items but none are present.
     */
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

    /**
     * Validates that request items are not modified if the request status locks them.
     * @param existingRequest The existing Request object.
     * @param incomingRequest The incoming Request object with potentially modified items.
     * @throws ValidationException if items are modified when the status is locked.
     */
    public void validateNoItemModificationWhenLocked(Request existingRequest, Request incomingRequest) {
        if (existingRequest.getStatus().isRequestItemsLocked()
                && !historyService.areRequestItemsEqual(existingRequest.getItems(), incomingRequest.getItems())) {
            throw new ValidationException("Phiếu đã ở trạng thái \"" +
                    existingRequest.getStatus().getDisplayName() +
                    "\" và không thể thay đổi hạng mục.");
        }
    }
}