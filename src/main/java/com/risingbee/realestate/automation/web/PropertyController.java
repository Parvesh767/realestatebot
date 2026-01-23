package com.risingbee.realestate.automation.web;


import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PropertyResponseDTO> create(
            @RequestPart("data") @Validated PropertyRequestDTO dto,
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos,
            Authentication auth
    ) {
        return ResponseEntity.ok(service.create(dto, photos));
    }
    
    @PutMapping(
    	    value = "/{id}",
    	    consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    	)
    	public ResponseEntity<PropertyResponseDTO> update(
    	        @PathVariable Long id,
    	        @RequestPart("data") @Validated PropertyRequestDTO dto,
    	        @RequestPart(value = "photos", required = false) List<MultipartFile> photos,
    	        @RequestPart(value = "removePhotos", required = false) List<String> removePhotos,
    	        Authentication auth
    	) {
    	
    	log.info("removePhotosJson = {}", removePhotos);

    	    return ResponseEntity.ok(
    	        service.update(id, dto, photos, removePhotos)
    	    );
    	}
    
    
//    @GetMapping("/{id}")
//    public ResponseEntity<PropertyResponseDTO> getById(@PathVariable Long id) {
//        return ResponseEntity.ok(service.getById(id));
//    }



    
//    @PostMapping
//    public ResponseEntity<PropertyResponseDTO> create(
//            @Validated @RequestBody PropertyRequestDTO dto,
//            Authentication auth
//    ) {
////        Long brokerId = Long.valueOf(auth.getName());
//        return ResponseEntity.ok(service.create(dto));
//    }

//    @PutMapping("/{id}")
//    public ResponseEntity<PropertyResponseDTO> update(
//            @PathVariable Long id,
//            @Validated @RequestBody PropertyRequestDTO dto) {
//        return ResponseEntity.ok(service.update(id, dto));
//    }

//    @GetMapping("/{id}")
//    public ResponseEntity<PropertyResponseDTO> get(@PathVariable Long id) {
//        return ResponseEntity.ok(service.get(id));
//    }

//    @GetMapping
//    public ResponseEntity<List<PropertyResponseDTO>> getAll(Authentication auth) {
//        
//    	Long brokerId = Long.valueOf(auth.getName());
//    	
//    	return ResponseEntity.ok(service.getAllForCurrentBroker());
//    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
