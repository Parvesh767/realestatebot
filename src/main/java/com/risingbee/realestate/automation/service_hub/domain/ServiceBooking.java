package com.risingbee.realestate.automation.service_hub.domain;

import com.risingbee.realestate.automation.actor.enums.ActorType;
import com.risingbee.realestate.automation.service_hub.enums.BookingStatus;
import com.risingbee.realestate.automation.service_hub.enums.ServiceCategory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "service_bookings")
@Getter
@Setter
@NoArgsConstructor
public class ServiceBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long accountId; // The account initiating/paying (e.g. Broker or Tenant)

    @Enumerated(EnumType.STRING)
    private ActorType bookedByRole;

    // --- Added fields for Tenancy & On-Site Recipient ---
    @Column(name = "tenancy_contract_id")
    private Long tenancyContractId;

    @Column(name = "property_id")
    private Long propertyId;

    @Column(name = "target_recipient_phone")
    private String targetRecipientPhone; // The tenant phone who will actually receive the tech and hold the OTP
    // ----------------------------------------------------

    private String userPhone;
    private String userAddress;
    private String landmark;
    private String localityCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_catalog_id")
    private ServiceCatalog serviceItem;

    @Enumerated(EnumType.STRING)
    private ServiceCategory category;

    @Column(columnDefinition = "text")
    private String issueDescription;

    private LocalDate preferredDate;
    private String preferredTimeSlot;

    private Integer creditsDeducted = 0;
    private Long amountPaidInr = 0L;
    private String paymentMode; // "CREDIT", "PAY_AFTER_SERVICE"

    @Enumerated(EnumType.STRING)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(nullable = false)
    private String completionOtp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_vendor_id")
    private ServiceVendor assignedVendor;

    private Instant createdAt = Instant.now();
    private Instant completedAt;

    private Long vendorPayoutAmount = 0L;
    private Long platformGrossMargin = 0L;
    private boolean payoutSettled = false;
    private Instant payoutSettledAt;
    
 // Inside ServiceBooking.java:

    @Column(name = "maintenance_ticket_id")
    private Long maintenanceTicketId;
    
    
}