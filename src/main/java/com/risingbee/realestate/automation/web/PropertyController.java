package com.risingbee.realestate.automation.web;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.ActorContext;
import com.risingbee.realestate.automation.dto.PropertyRequestDTO;
import com.risingbee.realestate.automation.dto.PropertyResponseDTO;
import com.risingbee.realestate.automation.service.PropertyService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService service;

    /* ================= CREATE ================= */

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PropertyResponseDTO> create(
            @RequestPart("data") @Validated PropertyRequestDTO dto,
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos
    ) {
        Actor actor = ActorContext.get();

        return ResponseEntity.ok(
                service.create(actor, dto, photos)
        );
    }

    /* ================= UPDATE ================= */

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PropertyResponseDTO> update(
            @PathVariable Long id,
            @RequestPart("data") @Validated PropertyRequestDTO dto,
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos,
            @RequestPart(value = "removePhotos", required = false) List<String> removePhotos
    ) {
        Actor actor = ActorContext.get();

        return ResponseEntity.ok(
                service.update(actor, id, dto, photos, removePhotos)
        );
    }

    /* ================= GET MY PROPERTIES ================= */

    @GetMapping("/my")
    public ResponseEntity<List<PropertyResponseDTO>> myProperties() {
        Actor actor = ActorContext.get();

        return ResponseEntity.ok(
                service.getAllForActor(actor)
        );
    }
    
    
    

    
    /* ================= GET SINGLE ================= */

    @GetMapping("/{id}")
    public ResponseEntity<PropertyResponseDTO> get(@PathVariable Long id) {
        Actor actor = ActorContext.get();

        return ResponseEntity.ok(
                service.get(actor, id)
        );
    }

    /* ================= DELETE ================= */

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Actor actor = ActorContext.get();

        service.delete(actor, id);
        return ResponseEntity.noContent().build();
    }
    
//    
//    /* ================= UI THMLYF ================= */
//    
//    

    
    
}