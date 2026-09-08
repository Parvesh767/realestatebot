package com.risingbee.realestate.automation.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.ActorContext;
import com.risingbee.realestate.automation.actor.domain.Account;
import com.risingbee.realestate.automation.actor.enums.ActorType;
import com.risingbee.realestate.automation.actor.repo.AccountRepository;
import com.risingbee.realestate.automation.domain.Property;
import com.risingbee.realestate.automation.domain.PropertyUnit;
import com.risingbee.realestate.automation.dto.PropertyRequestDTO;
import com.risingbee.realestate.automation.dto.PropertyResponseDTO;
import com.risingbee.realestate.automation.exception.AccessDeniedException;
import com.risingbee.realestate.automation.mapper.PropertyMapper;
import com.risingbee.realestate.automation.repo.PropertyRepository;
import com.risingbee.realestate.automation.repo.PropertyUnitRepository;
import com.risingbee.realestate.automation.service.PropertyService;
import com.risingbee.realestate.automation.service.PropertyUnitService;
import com.risingbee.realestate.automation.service.WhatsAppSender;
import com.risingbee.realestate.automation.tenancy.domain.TenancyContract;
import com.risingbee.realestate.automation.tenancy.enums.TenancyStatus;
import com.risingbee.realestate.automation.tenancy.repo.TenancyContractRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/properties")
public class AdminPropertyController {

    private final PropertyService service;
    private final PropertyRepository propertyRepository;
    private final TenancyContractRepository contractRepository;
    private final AccountRepository accountRepository;
    private final WhatsAppSender whatsAppSender;
    private final PropertyUnitService propertyUnitService;
    private final PropertyUnitRepository propertyUnitRepository;

    @GetMapping
    public String listProperties(Model model, HttpServletRequest request) {
        Actor actor = resolveActor(request);
        if (actor == null) {
            return "redirect:/web/auth/login";
        }

        // 1. Fetch all properties for this broker
        List<Property> properties = propertyRepository.findByOwnerAccountIdOrderByCreatedAtDesc(actor.internalId());

        // 2. Fetch occupancy data
        List<TenancyContract> contracts = contractRepository.findAll();
        Set<Long> rentedPropertyIds = contracts.stream()
                .filter(c -> c.getStatus() != TenancyStatus.SETTLED)
                .map(TenancyContract::getPropertyId)
                .collect(Collectors.toSet());

        long rentedCount = properties.stream()
                .filter(p -> rentedPropertyIds.contains(p.getId()))
                .count();

        
     // Find properties that act as parents (they have child units in the DB)
        Set<Long> matrixPropertyIds = propertyUnitRepository.findAll().stream()
                .map(PropertyUnit::getPropertyId)
                .collect(Collectors.toSet());

        model.addAttribute("matrixPropertyIds", matrixPropertyIds);
        
        
        // 3. NEW: Detect which properties are "Parent Buildings" (have child units)
        // Fetch all units owned by this broker (or all units and filter by property ID)
        List<PropertyUnit> allUnits = propertyUnitRepository.findAll();
        Set<Long> multiUnitPropertyIds = allUnits.stream()
                .map(PropertyUnit::getPropertyId)
                .collect(Collectors.toSet());

        model.addAttribute("properties", properties);
        model.addAttribute("rentedPropertyIds", rentedPropertyIds);
        model.addAttribute("rentedCount", rentedCount);
        model.addAttribute("multiUnitPropertyIds", multiUnitPropertyIds); // Pass to frontend

        return "admin/property/list";
    }
    
    
    @GetMapping("/{propertyId}/units")
    @ResponseBody
    public ResponseEntity<List<PropertyUnit>> getPropertyUnits(
            @PathVariable Long propertyId,
            HttpServletRequest request
    ) {
        Actor actor = resolveActor(request);
        if (actor == null) return ResponseEntity.status(401).build();

        List<PropertyUnit> units = propertyUnitRepository.findByPropertyId(propertyId);
        return ResponseEntity.ok(units);
    }
    
    
    @PostMapping("/{id}/duplicate")
    public String duplicateProperty(@PathVariable Long id, HttpServletRequest request) {
        Actor actor = resolveActor(request);
        if (actor == null) return "redirect:/web/auth/login";

        Property source = propertyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        // Verify ownership
        if (!source.getOwnerAccountId().equals(actor.internalId())) {
            throw new AccessDeniedException("Unauthorized");
        }

        // Create a clone
        Property clone = new Property();
        clone.setTitle(source.getTitle() + " (Copy)");
        clone.setBhk(source.getBhk());
        clone.setPrice(source.getPrice());
        clone.setSecurityDeposit(source.getSecurityDeposit());
        clone.setFurnishing(source.getFurnishing());
        clone.setArea(source.getArea());
        clone.setMapLink(source.getMapLink());
        clone.setAmenities(source.getAmenities());
        clone.setVideoTourUrl(source.getVideoTourUrl());
        clone.setPhotos(source.getPhotos());
        clone.setOwnerAccountId(actor.internalId());
        clone.setFloorNumber(2); // Default placeholder for the duplicated floor
        clone.setCityCode("GUR");
        Property saved = propertyRepository.save(clone);
        log.info("Duplicated Property #{} into new Property #{}", id, saved.getId());

        // Redirect to edit page so the broker can adjust the floor number and title
        return "redirect:/admin/properties/" + saved.getId() + "/edit";
    }
    
    
    @PostMapping("/units/{unitId}/update")
    @ResponseBody
    public ResponseEntity<?> updatePropertyUnit(
            @PathVariable Long unitId,
            @RequestParam(required = false) Long rentInr,
            @RequestParam(required = false) String status,
            HttpServletRequest request
    ) {
        Actor actor = resolveActor(request);
        if (actor == null) return ResponseEntity.status(401).build();

        PropertyUnit unit = propertyUnitRepository.findById(unitId)
                .orElseThrow(() -> new IllegalArgumentException("Unit not found"));

        // Verify ownership via parent property
        Property parent = propertyRepository.findById(unit.getPropertyId()).orElseThrow();
        if (!parent.getOwnerAccountId().equals(actor.internalId())) {
             return ResponseEntity.status(403).body("Unauthorized");
        }

        if (rentInr != null) unit.setRentInr(rentInr);
        if (status != null) unit.setStatus(status); // VACANT, RENTED_OUT, MAINTENANCE

        propertyUnitRepository.save(unit);
        return ResponseEntity.ok(unit);
    }
    
    
    
    @GetMapping("/units/{unitId}")
    public String showUnitDetails(@PathVariable Long unitId, Model model) {
        // 1. Fetch the specific child unit
        PropertyUnit unit = propertyUnitRepository.findById(unitId)
                .orElseThrow(() -> new IllegalArgumentException("Unit not found"));

        // 2. Fetch the parent building to inherit location, BHK, and map data
        Property parent = propertyRepository.findById(unit.getPropertyId())
                .orElseThrow(() -> new IllegalArgumentException("Parent property not found"));

        // 3. Create a merged "Composite" object to seamlessly feed your existing public template
        Property composite = new Property();
        
        // Core Overrides (Unit takes priority)
        composite.setId(parent.getId()); // Keep parent ID so inquiries route to the correct broker
        composite.setTitle(unit.getUnitName()); // e.g., "Sohna Homes — 1st Floor — Unit 101"
        composite.setPrice(unit.getRentInr());
        composite.setSecurityDeposit(unit.getSecurityDepositInr());
        
        // Inherited from Parent
        composite.setBhk(parent.getBhk());
        composite.setCityCode(parent.getCityCode());
        composite.setLocalityCode(parent.getLocalityCode());
        composite.setMapLink(parent.getMapLink());
        composite.setVideoTourUrl(parent.getVideoTourUrl());
        composite.setPropertyType(parent.getPropertyType());

        // Smart Fallbacks (Use Unit data if it exists, otherwise use Parent data)
        composite.setFurnishing(unit.getFurnishing() != null ? unit.getFurnishing() : parent.getFurnishing());
        
        String desc = (unit.getDescription() != null && !unit.getDescription().isBlank()) 
                      ? unit.getDescription() : parent.getDescription();
        composite.setDescription(desc);
        
        List<String> photos = (unit.getPhotos() != null && !unit.getPhotos().isEmpty()) 
                              ? unit.getPhotos() : parent.getPhotos();
        composite.setPhotos(photos);
        
        List<String> amenities = (unit.getAmenities() != null && !unit.getAmenities().isEmpty()) 
                                 ? unit.getAmenities() : parent.getAmenities();
        composite.setAmenities(amenities);

        // 4. Pass the merged object to the existing template
        model.addAttribute("property", composite);

        return "/property-details"; // Reuses your exact standalone HTML view
    }
    
    @GetMapping("/units/{unitId}/edit")
    public String showEditUnitForm(@PathVariable Long unitId, Model model, HttpServletRequest request) {
        Actor actor = resolveActor(request);
        if (actor == null) return "redirect:/web/auth/login";

        PropertyUnit unit = propertyUnitRepository.findById(unitId)
                .orElseThrow(() -> new IllegalArgumentException("Unit not found"));

        // Verify ownership via parent
        Property parent = propertyRepository.findById(unit.getPropertyId()).orElseThrow();
        if (!parent.getOwnerAccountId().equals(actor.internalId())) {
            throw new AccessDeniedException("Unauthorized");
        }

        model.addAttribute("unit", unit);
        model.addAttribute("parentPhotos", parent.getPhotos()); // Pass parent photos for UI fallback
        return "admin/property/form-unit";
    }

    @PostMapping("/units/{unitId}/edit")
    public String updateUnitDetails(
            @PathVariable Long unitId,
            @ModelAttribute PropertyUnit updatedUnit,
            HttpServletRequest request
    ) {
        Actor actor = resolveActor(request);
        if (actor == null) return "redirect:/web/auth/login";

        PropertyUnit unit = propertyUnitRepository.findById(unitId).orElseThrow();
        
        // Update specific fields
        unit.setRentInr(updatedUnit.getRentInr());
        unit.setSecurityDepositInr(updatedUnit.getSecurityDepositInr());
        unit.setFurnishing(updatedUnit.getFurnishing());
        unit.setAmenities(updatedUnit.getAmenities());
        unit.setDescription(updatedUnit.getDescription());
        
        propertyUnitRepository.save(unit);
        
        // Redirect back to parent property view
        return "redirect:/admin/properties";
    }
    
    
    
    
    // Quick Toggle Active State for Inventory Auditing
    @PostMapping("/{id}/toggle-active")
    public String togglePropertyStatus(@PathVariable Long id, HttpServletRequest request) {
        Actor actor = resolveActor(request);
        if (actor == null) return "redirect:/web/auth/login";

        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Property ID: " + id));

        if (!property.getOwnerAccountId().equals(actor.internalId())) {
            throw new RuntimeException("Unauthorized action on foreign property listing.");
        }

        // Toggle active status flag
        if (property.isActive()) {
            property.deactivate();
        } else {
            property.setActive(true);
        }
        propertyRepository.save(property);
        log.info("Toggled active status for Property #{} to {}", id, property.isActive());

        return "redirect:/admin/properties";
    }

    @GetMapping("/{propertyId}/contract")
    public String showCreateContractForm(@PathVariable Long propertyId, Model model, HttpServletRequest request) {
        Actor actor = resolveActor(request);
        if (actor == null) return "redirect:/web/auth/login";

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Property ID: " + propertyId));

        model.addAttribute("property", property);
        return "tenancy/create-contract";
    }

    @PostMapping("/{propertyId}/contract")
    public String createTenancyContract(
            @PathVariable Long propertyId,
            @RequestParam String tenantName,
            @RequestParam String tenantPhone,
            @RequestParam Long monthlyRent,
            @RequestParam Long securityDeposit,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(defaultValue = "30") Integer noticePeriodDays,
            HttpServletRequest request
    ) {
        Actor actor = resolveActor(request);
        if (actor == null) return "redirect:/web/auth/login";

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Property ID: " + propertyId));

        String cleanPhone = formatWhatsAppNumber(tenantPhone);

        Account tenantAccount = accountRepository.findByExternalId(cleanPhone)
                .orElseGet(() -> {
                    Account newAcc = new Account();
                    newAcc.setExternalId(cleanPhone);
                    newAcc.setPhone(cleanPhone);
                    newAcc.setName(tenantName != null && !tenantName.isBlank() ? tenantName : "Tenant " + cleanPhone);
                    newAcc.setType(ActorType.TENANT);
                    newAcc.setActive(true);
                    newAcc.setVerified(false);
                    newAcc.setProfileCompleted(false);
                    newAcc.setProfileStage(false);
                    newAcc.setCreditsBalance(0);
                    newAcc.setCreatedAt(LocalDateTime.now());
                    return accountRepository.save(newAcc);
                });

        TenancyContract contract = new TenancyContract();
        contract.setPropertyId(propertyId);
        contract.setOwnerAccountId(actor.internalId());
        contract.setOwnerPhone(actor.externalId());
        contract.setTenantAccountId(tenantAccount.getId());
        contract.setTenantName(tenantName);
        contract.setTenantPhone(cleanPhone);
        contract.setMonthlyRent(monthlyRent);
        contract.setSecurityDeposit(securityDeposit);
        contract.setStartDate(startDate);
        contract.setEndDate(endDate);
        contract.setNoticePeriodDays(noticePeriodDays);
        contract.setStatus(TenancyStatus.ACTIVE);

        TenancyContract savedContract = contractRepository.save(contract);
        log.info("Created Tenancy Contract #{} for Property #{} with Tenant Account #{}",
                savedContract.getId(), propertyId, tenantAccount.getId());

        if (!cleanPhone.isBlank()) {
            String welcomeMsg = String.format(
                    "🎉 *Welcome to your new home!*\n\n" +
                    "• Property: *%s*\n" +
                    "• Monthly Rent: *₹%d*\n" +
                    "• Security Deposit: *₹%d*\n" +
                    "• Lease Period: *%s to %s*\n\n" +
                    "Your digital tenancy contract is now active.",
                    property.getTitle(), monthlyRent, securityDeposit, startDate, endDate
            );
            whatsAppSender.sendTextMessage(cleanPhone, welcomeMsg);
        }

        return "redirect:/tenancy";
    }

    public static String formatWhatsAppNumber(String phone) {
        if (phone == null || phone.isBlank()) return "";
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() == 10) {
            return "91" + digits;
        }
        return digits;
    }

//    @GetMapping("/new")
//    public String showCreateForm(Model model, HttpServletRequest request) {
//        Actor actor = resolveActor(request);
//        if (actor == null) {
//            return "redirect:/web/auth/login";
//        }
//
//        model.addAttribute("property", new PropertyRequestDTO());
//        return "admin/property/form";
//    }

//    @PostMapping("/new")
//    public String createProperty(
//            @ModelAttribute Property property,
//            @RequestParam(value = "totalFloors", defaultValue = "1") int totalFloors,
//            @RequestParam(value = "unitsDistribution", required = false) String unitsDistribution,
//            @RequestParam(value = "floorRent", required = false) Long floorRent,
//            HttpServletRequest request
//    ) {
//        Actor actor = resolveActor(request);
//        if (actor == null) return "redirect:/web/auth/login";
//
//        property.setOwnerAccountId(actor.internalId());
//        Property savedProperty = propertyRepository.save(property);
//
//        if (totalFloors > 0) {
//            long rentToUse = (floorRent != null && floorRent > 0) ? floorRent : savedProperty.getPrice();
//            long depositToUse = savedProperty.getSecurityDeposit() != null ? savedProperty.getSecurityDeposit() : 0L;
//            
//            // Pass the distribution string to the service
//            propertyUnitService.generateCustomMatrix(savedProperty.getId(), totalFloors, unitsDistribution, rentToUse, depositToUse);
//        }
//        return "redirect:/admin/properties";
//    }
    
    
    
    @GetMapping("/new")
    public String showCreateSelection(HttpServletRequest request) {
        Actor actor = resolveActor(request);
        if (actor == null) return "redirect:/web/auth/login";
        return "admin/property/select-type";
    }

    @GetMapping("/new/{type}")
    public String showCreateForm(@PathVariable String type, Model model, HttpServletRequest request) {
        Actor actor = resolveActor(request);
        if (actor == null) return "redirect:/web/auth/login";

        model.addAttribute("property", new PropertyRequestDTO());
        
        if ("matrix".equalsIgnoreCase(type)) {
            return "admin/property/form-matrix";
        }
        return "admin/property/form-standalone";
    }
    
    
    
    
    @PostMapping("/new")
    public String createProperty(
            @ModelAttribute Property property,
            @RequestParam(value = "listingMode", defaultValue = "STANDALONE") String listingMode,
            @RequestParam(value = "totalFloors", defaultValue = "1") int totalFloors,
            @RequestParam(value = "unitsDistribution", required = false) String unitsDistribution,
            @RequestParam(value = "floorRent", required = false) Long floorRent,
            HttpServletRequest request
    ) {
        Actor actor = resolveActor(request);
        if (actor == null) return "redirect:/web/auth/login";

        property.setOwnerAccountId(actor.internalId());

        // For Matrix/Parent Buildings, base rent defines the parent shell value (can be 0 or avg)
        if ("MATRIX".equalsIgnoreCase(listingMode) && property.getPrice() == null) {
            property.setPrice(0L); 
        }

        Property savedProperty = propertyRepository.save(property);

        // Auto-generate child units ONLY if this is a Matrix flow
        if ("MATRIX".equalsIgnoreCase(listingMode) && totalFloors > 0) {
            long rentToUse = (floorRent != null && floorRent > 0) ? floorRent : (property.getPrice() != null ? property.getPrice() : 0L);
            long depositToUse = savedProperty.getSecurityDeposit() != null ? savedProperty.getSecurityDeposit() : 0L;
            
            propertyUnitService.generateCustomMatrix(savedProperty.getId(), totalFloors, unitsDistribution, rentToUse, depositToUse);
        }

        log.info("Broker #{} created property #{} in mode {}", actor.internalId(), savedProperty.getId(), listingMode);
        return "redirect:/admin/properties";
    }
    
    
    
    
    
    @GetMapping("/edit/{id}")
    public String showEditForm(
            @PathVariable Long id,
            Model model,
            HttpServletRequest request
    ) {
        Actor actor = resolveActor(request);
        if (actor == null) {
            return "redirect:/web/auth/login";
        }

        PropertyResponseDTO property = service.get(actor, id);
        model.addAttribute("property", PropertyMapper.toRequestDTO(property));
        model.addAttribute("existingPhotos", property.photos());
        model.addAttribute("propertyId", id);

        return "admin/property/form";
    }

    @PostMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String updateProperty(
            @PathVariable Long id,
            @Validated @ModelAttribute("property") PropertyRequestDTO dto,
            @RequestParam(value = "newPhotos", required = false) List<MultipartFile> newPhotos,
            @RequestParam(value = "removePhotos", required = false) List<String> removePhotos,
            HttpServletRequest request
    ) {
        Actor actor = resolveActor(request);
        if (actor == null) {
            return "redirect:/web/auth/login";
        }

        service.update(actor, id, dto, newPhotos, removePhotos);
        return "redirect:/admin/properties";
    }

    @GetMapping("/delete/{id}")
    public String deleteProperty(@PathVariable Long id, HttpServletRequest request) {
        Actor actor = resolveActor(request);
        if (actor == null) {
            return "redirect:/web/auth/login";
        }

        service.delete(actor, id);
        return "redirect:/admin/properties";
    }

    private Actor resolveActor(HttpServletRequest request) {
        Actor actor = ActorContext.get();
        if (actor != null) {
            return actor;
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            return (Actor) session.getAttribute("ACTOR");
        }
        return null;
    }
}