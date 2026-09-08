package com.risingbee.realestate.automation.tenancy.dto;

import com.risingbee.realestate.automation.tenancy.enums.TicketCategory;
import lombok.Data;

@Data
public class MaintenanceTicketRequestDTO {
    private TicketCategory category;
    private String title;
    private String description;
}