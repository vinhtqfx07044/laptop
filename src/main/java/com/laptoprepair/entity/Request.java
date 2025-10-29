package com.laptoprepair.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonValue;
import com.laptoprepair.utils.CurrencyUtils;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents a repair request made by a customer. Contains details about the customer, the device,
 * the requested service, and the status of the request.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class Request extends BaseEntity {

    @NotBlank(message = "Tên khách hàng là bắt buộc")
    @Size(min = 3, max = 100, message = "Tên khách hàng phải có từ 3-100 ký tự")
    private String name;

    @NotBlank(message = "Số điện thoại là bắt buộc")
    @Pattern(regexp = "0\\d{9}", message = "Số điện thoại phải có 10 chữ số và bắt đầu bằng 0")
    private String phone;

    @Email(message = "Email không đúng định dạng")
    private String email;

    private String address;
    private String brandModel;

    @Column(name = "serial_number")
    private String serialNumber;

    @NotNull(message = "Ngày hẹn là bắt buộc")
    private LocalDateTime appointmentDate;

    @NotBlank(message = "Mô tả tình trạng thiết bị là bắt buộc")
    @Size(min = 10, max = 1000, message = "Mô tả phải có từ 10-1000 ký tự")
    private String description;

    @Enumerated(EnumType.STRING)
    private RequestStatus status = RequestStatus.SCHEDULED;

    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequestItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequestHistory> history = new ArrayList<>();

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequestImage> images = new ArrayList<>();

    public BigDecimal getTotal() {
        return CurrencyUtils.calculateRequestTotal(items);
    }

    @Override
    public String toString() {
        return "Request{" +
                "id=" + getId() +
                ", createdAt=" + getCreatedAt() +
                ", createdBy='" + getCreatedBy() + '\'' +
                ", updatedAt=" + getUpdatedAt() +
                ", updatedBy='" + getUpdatedBy() + '\'' +
                ", name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", address='" + address + '\'' +
                ", brandModel='" + brandModel + '\'' +
                ", serialNumber='" + serialNumber + '\'' +
                ", appointmentDate=" + appointmentDate +
                ", description='" + description + '\'' +
                ", status=" + status +
                ", completedAt=" + completedAt +
                ", items=" + items +
                ", images=" + images +
                ", history=" + history +
                '}';
    }

    /**
     * Defines the possible statuses for a repair request.
     */
    public enum RequestStatus {
        SCHEDULED("Đã lên lịch"),
        QUOTED("Đã báo giá"),
        APPROVE_QUOTED("Đã duyệt báo giá"),
        IN_PROGRESS("Đang thực hiện"),
        COMPLETED("Hoàn thành"),
        UNDER_WARRANTY("Đang bảo hành"),
        CANCELLED("Đã hủy");

        private final String value;

        RequestStatus(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        // Check if request items can be edited based on status
        public boolean isRequestItemsLocked() {
            return this == COMPLETED || this == UNDER_WARRANTY || this == CANCELLED;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
