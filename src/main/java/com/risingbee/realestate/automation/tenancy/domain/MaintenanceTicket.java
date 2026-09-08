package com.risingbee.realestate.automation.tenancy.domain;

import com.risingbee.realestate.automation.tenancy.enums.TicketCategory;
import com.risingbee.realestate.automation.tenancy.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "maintenance_tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenancy_contract_id", nullable = false)
    private Long tenancyContractId;

    @Column(name = "tenant_account_id", nullable = false)
    private Long tenantAccountId;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 50, nullable = false)
    private TicketCategory category;

    @Column(name = "title", length = 150, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "photo_url")
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private TicketStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = TicketStatus.OPEN;
    }
}