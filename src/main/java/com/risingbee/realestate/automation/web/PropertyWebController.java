package com.risingbee.realestate.automation.web;

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
import com.risingbee.realestate.automation.dto.PropertyRequestDTO;
import com.risingbee.realestate.automation.service.PropertyService;

import lombok.RequiredArgsConstructor;

//Separate UI Controller
@Controller
@RequestMapping("/admin/properties/web")
@RequiredArgsConstructor
public class PropertyWebController {
 private final PropertyService service;
 
 
 @GetMapping
 public String listProperties(Model model) {
     Actor actor = ActorContext.get();

     model.addAttribute("properties",
             service.getAllForActor(actor));

     return "admin/property/list";
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