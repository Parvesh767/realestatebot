package com.risingbee.realestate.automation.service_hub.web;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.ActorContext;
import com.risingbee.realestate.automation.service_hub.domain.ServiceBooking;
import com.risingbee.realestate.automation.service_hub.domain.ServiceCatalog;
import com.risingbee.realestate.automation.service_hub.dto.ServiceBookingRequestDTO;
import com.risingbee.realestate.automation.service_hub.repo.ServiceCatalogRepository;
import com.risingbee.realestate.automation.service_hub.service.ServiceHubService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceHubApiController {

    private final ServiceCatalogRepository catalogRepository;
    private final ServiceHubService serviceHubService;

    @GetMapping("/catalog")
    public ResponseEntity<List<ServiceCatalog>> getCatalog() {
        return ResponseEntity.ok(catalogRepository.findByActiveTrueOrderByCategoryAsc());
    }

    @PostMapping("/book")
    public ResponseEntity<?> bookJob(@RequestBody ServiceBookingRequestDTO req, HttpServletRequest request) {
        Actor actor = resolveActor(request);
        if (actor == null || actor.internalId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "UNAUTHORIZED", "message", "Authentication required"));
        }

        ServiceBooking booking = serviceHubService.createBooking(actor, req);
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "bookingId", booking.getId(),
                "completionOtp", booking.getCompletionOtp(),
                "paymentMode", booking.getPaymentMode()
        ));
    }

    @PostMapping("/complete")
    public ResponseEntity<?> completeJob(@RequestParam Long bookingId, @RequestParam String otp) {
        ServiceBooking booking = serviceHubService.completeJobWithOtp(bookingId, otp);
        return ResponseEntity.ok(Map.of(
                "status", "COMPLETED",
                "bookingId", booking.getId()
        ));
    }

    private Actor resolveActor(HttpServletRequest request) {
        Actor actor = ActorContext.get();
        if (actor != null && actor.internalId() != null) {
            return actor;
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object sessionActor = session.getAttribute("ACTOR");
            if (sessionActor instanceof Actor a) {
                return a;
            }
        }
        return null;
    }
}