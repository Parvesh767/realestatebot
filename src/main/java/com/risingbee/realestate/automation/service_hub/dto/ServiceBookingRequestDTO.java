package com.risingbee.realestate.automation.service_hub.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ServiceBookingRequestDTO {
    private Long serviceCatalogId;
    private String address;
    private String landmark;
    private String localityCode;
    private String issueDescription;
    private LocalDate preferredDate;
    private String preferredTimeSlot;
    private String paymentMode; // "CREDIT" or "PAY_AFTER_SERVICE"

 // Optional Tenancy linkage
    private Long tenancyContractId;
    private Long maintenanceTicketId;

}