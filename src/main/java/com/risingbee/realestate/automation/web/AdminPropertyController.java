package com.risingbee.realestate.automation.web;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.ActorContext;
import com.risingbee.realestate.automation.dto.PropertyRequestDTO;
import com.risingbee.realestate.automation.dto.PropertyResponseDTO;
import com.risingbee.realestate.automation.service.PropertyService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor

@RequestMapping("/admin/properties")
public class AdminPropertyController {

	private final PropertyService service;

	@GetMapping
	public String listProperties(Model model) {

		Actor actor = ActorContext.get();

		if (actor == null) {
			return "redirect:/auth/login"; // ✅
		}

		model.addAttribute("properties", service.getAllForActor(actor));

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
		Actor actor = ActorContext.get();

		service.create(actor, dto, null);

		return "redirect:/admin/properties";
	}

	// EDIT FORM PAGE
	@GetMapping("/edit/{id}")
	public String showEditForm(
	        @PathVariable Long id, 
	        Model model,
	        HttpSession session
	) {
	    // 1. Retrieve the Actor object from session
	    Actor actor = (Actor) session.getAttribute("ACTOR");
	    if (actor == null) {
	        throw new IllegalStateException("User session expired or unauthorized");
	    }

	    // 2. Fetch the property passing both actor and id
	    PropertyResponseDTO property = service.get(actor, id);

	    // 3. Populate model attributes for Thymeleaf / HTML rendering
	    model.addAttribute("property", property);
	    model.addAttribute("propertyId", id);

	    return "admin/property/form";
	}

//    // UPDATE PROPERTY
	@PostMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public String updateProperty(@PathVariable Long id, @Validated @ModelAttribute("property") PropertyRequestDTO dto,
			@RequestParam(value = "newPhotos", required = false) List<MultipartFile> newPhotos,
			@RequestParam(value = "removePhotos", required = false) List<String> removePhotos, HttpSession session) {
		// 1. Retrieve the Actor object from the session (set during login/verification)
		Actor actor = (Actor) session.getAttribute("ACTOR");
		if (actor == null) {
			throw new IllegalStateException("User session expired or unauthorized");
		}

		// 2. Pass all 6 required parameters to your service update method
		service.update(actor, id, dto, newPhotos, removePhotos);

		return "redirect:/admin/properties";
	}

	// DELETE (soft)
	@GetMapping("/delete/{id}")
	public String deleteProperty(@PathVariable Long id) {
		Actor actor = ActorContext.get();

		service.delete(actor, id);

		return "redirect:/admin/properties";
	}

	@GetMapping("/test")
	public String test() {
		return "test";
	}
}
