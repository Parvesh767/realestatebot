package com.risingbee.realestate.automation.web;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.ActorContext;
import com.risingbee.realestate.automation.domain.Property;
import com.risingbee.realestate.automation.dto.PropertyRequestDTO;
import com.risingbee.realestate.automation.repo.PropertyRepository;
import com.risingbee.realestate.automation.service.PropertyService;
import com.risingbee.realestate.automation.tenancy.domain.TenancyContract;
import com.risingbee.realestate.automation.tenancy.enums.TenancyStatus;
import com.risingbee.realestate.automation.tenancy.repo.TenancyContractRepository;

import lombok.RequiredArgsConstructor;

//Separate UI Controller
@Controller
@RequestMapping("/admin/properties/web")
@RequiredArgsConstructor
public class PropertyWebController {
 
	private final PropertyService service;
	private final PropertyRepository propertyRepository;
	private final TenancyContractRepository contractRepository;
 
 
	@GetMapping
    public String listProperties(Model model) {
        List<Property> properties = propertyRepository.findAll();
        
        // Fetch active/unsettled tenancy contract property IDs
        Set<Long> rentedPropertyIds = contractRepository.findAll().stream()
                .filter(c -> c.getStatus() != TenancyStatus.SETTLED)
                .map(TenancyContract::getPropertyId)
                .collect(Collectors.toSet());

        model.addAttribute("properties", properties);
        model.addAttribute("rentedPropertyIds", rentedPropertyIds);
        model.addAttribute("rentedCount", rentedPropertyIds.size());
        
        return "admin/properties";
    }
 
 
 @PostMapping
 public String createProperty(
         @Validated @ModelAttribute("property") PropertyRequestDTO dto
 ) {
     Actor actor = ActorContext.get();

     service.create(actor, dto, null);

     return "redirect:/admin/properties";
 }
 
 
 @GetMapping("/edit/{id}")
 public String showEditForm(@PathVariable Long id, Model model) {
     Actor actor = ActorContext.get();

     model.addAttribute("property", service.get(actor, id));
     model.addAttribute("propertyId", id);

     return "admin/property/form";
 }
 
// 
// @PostMapping("/{id}")
// public String updateProperty(
//         @PathVariable Long id,
//         @Validated @ModelAttribute("property") PropertyRequestDTO dto
// ) {
//     Actor actor = ActorContext.get();
//
//     service.update(actor, id, dto, null, null);
//
//     return "redirect:/admin/properties";
// }
// 
 
 @GetMapping("/delete/{id}")
 public String deleteProperty(@PathVariable Long id) {
     Actor actor = ActorContext.get();
     service.delete(actor, id);
     return "redirect:/admin/properties";
 }

 
 
}