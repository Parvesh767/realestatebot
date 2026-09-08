package com.risingbee.realestate.automation.web;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.risingbee.realestate.automation.domain.Lead;
import com.risingbee.realestate.automation.domain.Property;
import com.risingbee.realestate.automation.repo.LeadRepository;
import com.risingbee.realestate.automation.service.PropertyService;
import com.risingbee.realestate.leads.enums.LeadSource;
import com.risingbee.realestate.leads.enums.LeadStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/properties")
@RequiredArgsConstructor
@Slf4j
public class PropertyDetailsController {

    private final PropertyService propertyService;
    private final LeadRepository leadRepository;

    @GetMapping("/{id}")
    public String showPropertyDetails(
            @PathVariable Long id,
            @RequestParam(required = false) String searchContext,
            Model model) {

        Property property = propertyService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Property not found with id: " + id));

        // Fetch up to 4 similar alternate properties in the same city / locality
        List<Property> similarProperties = propertyService.findMatchesForSearchers(
                property.getBhk(),
                property.getCityCode(),
                property.getLocalityCode(),
                null,
                null
        ).stream()
         .filter(p -> !p.getId().equals(id)) // Exclude current property
         .limit(4)
         .toList();

        model.addAttribute("property", property);
        model.addAttribute("similarProperties", similarProperties);
        model.addAttribute("searchContext", searchContext != null ? searchContext : "");
        return "property-details";
    }

    @PostMapping("/inquire")
    public String submitInquiry(
            @RequestParam Long propertyId,
            @RequestParam String phone,
            @RequestParam(required = false, defaultValue = "7") Integer moveInDays,
            @RequestParam(required = false) String visitDate,
            @RequestParam(required = false) String notes,
            Model model) {

        Property property = propertyService.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        String rawRequirement = String.format("Interested in %s (%s). Need in %d days. Preferred Visit: %s. %s",
                property.getTitle(), property.getBhk(), moveInDays,
                (visitDate != null && !visitDate.isBlank() ? visitDate : "ASAP"),
                (notes != null ? notes : ""));

        
        
        
        
        
        
        Optional<Lead> existingLeadOpt = leadRepository
        	    .findTopByOwnerAccountIdAndPhoneNumberOrderByCreatedAtDesc(
        	        property.getOwnerAccountId(), phone.replaceAll("[^0-9]", ""));

        	Lead lead;
        	if (existingLeadOpt.isPresent()) {
        	    lead = existingLeadOpt.get();
        	    lead.setUpdatedAt(Instant.now());
        	    lead.setPhoneNumber(phone.replaceAll("[^0-9]", ""));
                lead.setOwnerAccountId(property.getOwnerAccountId());
                lead.setBhk(property.getBhk());
                lead.setMinBudget(property.getPrice());
                lead.setMaxBudget(property.getPrice());
                lead.setCityCode(property.getCityCode());
                lead.setLocalityCode(property.getLocalityCode());
                lead.setRawMessage(rawRequirement);
        	    
        	    // Keeps existing 'unlocked' or 'unlockedAt' state intact!
        	} else {
        	    lead = new Lead();
        	    lead.setPhoneNumber(phone.replaceAll("[^0-9]", ""));
        	    lead.setOwnerAccountId(property.getOwnerAccountId());
        	    lead.setBhk(property.getBhk());
        	    lead.setMinBudget(property.getPrice());
        	    lead.setMaxBudget(property.getPrice());
        	    lead.setCityCode(property.getCityCode());
        	    lead.setLocalityCode(property.getLocalityCode());
        	    lead.setRawMessage(rawRequirement);
        	    lead.setStatus(LeadStatus.NEW);
        	    lead.setSource(LeadSource.WEB);
        	    lead.setUnlocked(false);
        	}

        	leadRepository.save(lead);
        
        
        
    
        log.info("Captured Tenant Inquiry for Property #{} -> Lead #{}", propertyId, lead.getId());

        List<Property> similarProperties = propertyService.findMatchesForSearchers(
                property.getBhk(), property.getCityCode(), property.getLocalityCode(), null, null
        ).stream().filter(p -> !p.getId().equals(propertyId)).limit(4).toList();

        model.addAttribute("property", property);
        model.addAttribute("similarProperties", similarProperties);
        model.addAttribute("inquirySuccess", true);
        return "property-details";
    }
}