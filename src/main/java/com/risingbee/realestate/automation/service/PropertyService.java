package com.risingbee.realestate.automation.service;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.risingbee.realestate.auth.AuthorizationService;
import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.enums.Capability;
import com.risingbee.realestate.automation.domain.BrokerConversation;
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

    /* =========================
       CREATE (REST / API)
       ========================= */

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

        Property property = PropertyMapper.toEntity(dto, ownerAccountId);

        if (rl != null) {
            property.updateLocation(rl.city(), rl.locality());
        }

        if (photos != null && !photos.isEmpty()) {
            property.addPhotos(photoStorageService.saveAll(photos));
        }

        repository.save(property);
        return PropertyMapper.toDTO(property);
    }

    /* =========================
       UPDATE
       ========================= */

    public PropertyResponseDTO update(
            Actor actor,
            Long id,
            PropertyRequestDTO dto,
            List<MultipartFile> newPhotos,
            List<String> removePhotos
    ) {
        authz.require(actor, Capability.ADD_PROPERTY);
        
        Long ownerAccountId = actor.internalId();
        if (ownerAccountId == null) {
            throw new IllegalStateException("Actor has no account identity");
        }
        
        Property property = loadOwnedProperty(actor, id);

        property.updateDetails(
                dto.getTitle(),
                dto.getPrice() != null ? dto.getPrice().longValue() : null,
                dto.getDescription(),
                dto.getMapLink()
        );

        if (dto.getArea() != null) {
            ResolvedLocation rl = locationResolver
                    .resolveText(tokenizeArea(dto.getArea()))
                    .orElse(null);

            if (rl != null) {
                property.updateLocation(rl.city(), rl.locality());
            }
        }

        if (removePhotos != null && !removePhotos.isEmpty()) {
            List<String> normalizedPaths = removePhotos.stream().map(this::normalizePath).toList();
            normalizedPaths.forEach(photoStorageService::delete);
            property.removePhotos(normalizedPaths);
        }

        if (newPhotos != null && !newPhotos.isEmpty()) {
            property.addPhotos(photoStorageService.saveAll(newPhotos));
        }

        repository.save(property);
        return PropertyMapper.toDTO(property);
    }

    /* =========================
       CREATE (WhatsApp flow)
       ========================= */

    @SuppressWarnings("unchecked")
    public Property createFromConversation(
            Actor actor,
            BrokerConversation conv
    ) {
        authz.require(actor, Capability.ADD_PROPERTY);

        Long ownerAccountId = actor.internalId();
        if (ownerAccountId == null) {
            throw new IllegalStateException("Actor has no account identity");
        }

        ResolvedLocation rl = locationResolver
            .resolveText(tokenizeArea(conv.get("area", String.class)))
            .orElse(null);

        Integer priceVal = conv.get("price", Integer.class);

        Property property = new Property(
            ownerAccountId,
            buildTitle(conv, rl),
            conv.get("bhk", String.class),
            priceVal != null ? priceVal.longValue() : null,
            rl != null ? rl.city() : null,
            rl != null ? rl.locality() : null
        );

        List<String> photos = conv.get("photos", List.class);
        if (photos != null) {
            property.addPhotos(photos);
        }

        repository.save(property);
        return property;
    }

    /* =========================
       READ
       ========================= */

    public PropertyResponseDTO get(Actor actor, Long id) {
        return PropertyMapper.toDTO(loadOwnedProperty(actor, id));
    }

    public List<PropertyResponseDTO> getAllForActor(Actor actor) {
        authz.require(actor, Capability.ADD_PROPERTY);

        Long accountId = actor.internalId();
        if (accountId == null) {
            throw new IllegalStateException("Actor has no account identity");
        }

        return repository
            .findByOwnerAccountId(accountId)
            .stream()
            .map(PropertyMapper::toDTO)
            .toList();
    }

    public List<Property> findMatchesForSearchers(
            String bhk,
            String cityCode,
            String localityCode,
            Long min,
            Long max
    ) {
        return repository.searchPublic(bhk, cityCode, localityCode, min, max);
    }

    /* =========================
       DELETE (soft)
       ========================= */

    public void delete(Actor actor, Long id) {
        Property property = loadOwnedProperty(actor, id);
        property.deactivate();
        repository.save(property);
    }

    /* =========================
       INTERNAL HELPERS
       ========================= */

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
        if (urlOrPath.startsWith("http")) {
            return URI.create(urlOrPath).getPath();
        }
        return urlOrPath;
    }

    private String buildTitle(BrokerConversation conv, ResolvedLocation rl) {
        String bhk = conv.get("bhk", String.class);
        if (rl != null && rl.locality() != null) {
            return bhk + " in " + rl.locality();
        }
        return bhk + " Property";
    }
}