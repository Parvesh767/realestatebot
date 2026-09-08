package com.risingbee.realestate.automation.service;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.risingbee.realestate.auth.AuthorizationService;
import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.enums.Capability;
import com.risingbee.realestate.automation.domain.Property;
import com.risingbee.realestate.automation.dto.PropertyRequestDTO;
import com.risingbee.realestate.automation.dto.PropertyResponseDTO;
import com.risingbee.realestate.automation.exception.AccessDeniedException;
import com.risingbee.realestate.automation.exception.ResourceNotFoundException;
import com.risingbee.realestate.automation.mapper.PropertyMapper;
import com.risingbee.realestate.automation.parser.LocationResolver;
import com.risingbee.realestate.automation.parser.ResolvedLocation;
import com.risingbee.realestate.automation.repo.PropertyRepository;
import com.risingbee.realestate.automation.service.storage_service.PhotoStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PropertyService {

    private final PropertyRepository repository;
    private final PhotoStorageService photoStorageService;
    private final LocationResolver locationResolver;
    private final AuthorizationService authz;

    /* =========================        LOOKUP        ========================= */

    @Transactional(readOnly = true)
    public Optional<Property> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public PropertyResponseDTO get(Actor actor, Long id) {
        return PropertyMapper.toDTO(loadOwnedProperty(actor, id));
    }

    @Transactional(readOnly = true)
    public List<PropertyResponseDTO> getAllForActor(Actor actor) {
        authz.require(actor, Capability.ADD_PROPERTY);
        Long accountId = actor.internalId();
        if (accountId == null) {
            throw new IllegalStateException("Actor has no account identity");
        }
        return repository.findByOwnerAccountIdOrderByCreatedAtDesc(accountId)
                .stream()
                .map(PropertyMapper::toDTO)
                .toList();
    }

    /**
     * Used by SearcherSearchHandler for WhatsApp matching
     */
    @Transactional(readOnly = true)
    public List<Property> findMatchesForSearchers(
            String bhk,
            String cityCode,
            String localityCode,
            Long min,
            Long max
    ) {
        return repository.searchPublic(bhk, cityCode, localityCode, min, max);
    }

    /* =========================        CREATE        ========================= */

    public PropertyResponseDTO create(
            Actor actor,
            PropertyRequestDTO dto,
            List<MultipartFile> photos
    ) {
        authz.require(actor, Capability.ADD_PROPERTY);
        Long ownerAccountId = actor.internalId();
        if (ownerAccountId == null) {
            throw new IllegalStateException("Actor has no account identity");
        }

        ResolvedLocation rl = locationResolver
                .resolveText(tokenizeArea(dto.getArea()))
                .orElse(null);

        // Populate location codes if resolved
        if (rl != null) {
            dto.setCityCode(rl.city());
            dto.setLocalityCode(rl.locality());
        }

        Property property = PropertyMapper.toEntity(dto, ownerAccountId);

        // Upload and bind multipart photos if provided
        if (photos != null && !photos.isEmpty()) {
            property.addPhotos(photoStorageService.saveAll(photos));
        }

        repository.save(property);
        log.info("Created property #{} for ownerAccountId={}", property.getId(), ownerAccountId);
        return PropertyMapper.toDTO(property);
    }

    /* =========================        UPDATE        ========================= */

    public PropertyResponseDTO update(
            Actor actor,
            Long id,
            PropertyRequestDTO dto,
            List<MultipartFile> newPhotos,
            List<String> removePhotos
    ) {
        authz.require(actor, Capability.ADD_PROPERTY);
        Property property = loadOwnedProperty(actor, id);

        // Re-resolve location if area text was updated
        if (dto.getArea() != null && !dto.getArea().isBlank()) {
            ResolvedLocation rl = locationResolver.resolveText(tokenizeArea(dto.getArea())).orElse(null);
            if (rl != null) {
                dto.setCityCode(rl.city());
                dto.setLocalityCode(rl.locality());
            }
        }

        PropertyMapper.updateEntity(property, dto);

        // Process photo removals
        if (removePhotos != null && !removePhotos.isEmpty()) {
            List<String> normalizedPaths = removePhotos.stream()
                    .map(this::normalizePath)
                    .toList();
            normalizedPaths.forEach(photoStorageService::delete);
            property.removePhotos(normalizedPaths);
        }

        // Process new photo uploads
        if (newPhotos != null && !newPhotos.isEmpty()) {
            property.addPhotos(photoStorageService.saveAll(newPhotos));
        }

        repository.save(property);
        log.info("Updated property #{} for ownerAccountId={}", property.getId(), actor.internalId());
        return PropertyMapper.toDTO(property);
    }

    /* =========================        DELETE        ========================= */

    public void delete(Actor actor, Long id) {
        Property property = loadOwnedProperty(actor, id);
        property.deactivate();
        repository.save(property);
        log.info("Deactivated property #{} for ownerAccountId={}", id, actor.internalId());
    }

    /* =========================        HELPERS        ========================= */

    private Property loadOwnedProperty(Actor actor, Long id) {
        authz.require(actor, Capability.ADD_PROPERTY);
        Long accountId = actor.internalId();
        if (accountId == null) {
            throw new IllegalStateException("Actor has no account identity");
        }

        Property property = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found: " + id));

        if (!property.getOwnerAccountId().equals(accountId)) {
            throw new AccessDeniedException("Property does not belong to actor");
        }
        return property;
    }

    private List<String> tokenizeArea(String area) {
        if (area == null || area.isBlank()) {
            return List.of();
        }
        return Arrays.stream(area.split("\\s+"))
                .map(String::toLowerCase)
                .toList();
    }

    private String normalizePath(String urlOrPath) {
        if (urlOrPath != null && urlOrPath.startsWith("http")) {
            return URI.create(urlOrPath).getPath();
        }
        return urlOrPath;
    }
}