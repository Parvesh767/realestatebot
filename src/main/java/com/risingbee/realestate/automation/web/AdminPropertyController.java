package com.risingbee.realestate.automation.web;



import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.risingbee.realestate.automation.dto.PropertyRequestDTO;
import com.risingbee.realestate.automation.service.PropertyService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/admin/properties")
@RequiredArgsConstructor
public class AdminPropertyController {

    private final PropertyService service;

    // LIST PAGE
    @GetMapping
    public String listProperties(Model model) {
//        model.addAttribute("properties", service.getAllForCurrentBroker());
        return "admin/property/list";
    }

   
    
    // NEW FORM PAGE
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("property", new PropertyRequestDTO());
        return "admin/property/form";
    }

    // CREATE PROPERTY
    @PostMapping
    public String createProperty(@Validated @ModelAttribute("property") PropertyRequestDTO dto) {
//        service.create(dto);
        return "redirect:/admin/properties";
    }

    // EDIT FORM PAGE
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("property", service.get(id));
        model.addAttribute("propertyId", id);
        return "admin/property/form";
    }

//    // UPDATE PROPERTY
//    @PostMapping("/{id}")
//    public String updateProperty(
//            @PathVariable Long id,
//            @Validated @ModelAttribute("property") PropertyRequestDTO dto
//    ) {
//        service.update(id, dto);
//        return "redirect:/admin/properties";
//    }

    // DELETE (soft)
    @GetMapping("/delete/{id}")
    public String deleteProperty(@PathVariable Long id) {
        service.delete(id);
        return "redirect:/admin/properties";
    }
    
    
    @GetMapping("/test")
    public String test() {
        return "test";
    }
}
