package com.risingbee.realestate.automation.service_hub.dto;

import com.risingbee.realestate.automation.tenancy.domain.MaintenanceTicket;
import com.risingbee.realestate.automation.tenancy.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketDispatchDTO {
    private MaintenanceTicket ticket;
    private Long ticketId;
    private String title;
    private String description;
    private TicketStatus status;
    private LocalDateTime createdAt;
    private String category;
    private String propertyAddress;
    private String localityCode;
    private String tenantPhone;
    private Long contractId;
    private Long propertyId;
}