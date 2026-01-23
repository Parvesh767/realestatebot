package com.risingbee.realestate.automation.service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.risingbee.realestate.automation.actor.ActorContext;
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

    /* =========================
       CREATE (REST / API)
       ========================= */

    public PropertyResponseDTO create(
            PropertyRequestDTO dto,
            List<MultipartFile> photos
    ) {
        ActorContext.requireCapability(Capability.ADD_PROPERTY);

        Long ownerAccountId = ActorContext.get().internalId();

        ResolvedLocation rl =
                locationResolver
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
            Long id,
            PropertyRequestDTO dto,
            List<MultipartFile> newPhotos,
            List<String> removePhotos
    ) {
        Property property = loadOwnedProperty(id);

        property.updateDetails(
                dto.getTitle(),
                dto.getPrice(),
                dto.getDescription(),
                dto.getMapLink()
        );

        if (dto.getArea() != null) {
            ResolvedLocation rl =
                    locationResolver
                            .resolveText(tokenizeArea(dto.getArea()))
                            .orElse(null);

            if (rl != null) {
                property.updateLocation(rl.city(), rl.locality());
            }
        }

        if (removePhotos != null && !removePhotos.isEmpty()) {
            List<String> normalizedPaths =
                    removePhotos.stream().map(this::normalizePath).toList();

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

    public Property createFromConversation(BrokerConversation conv) {
        ActorContext.requireCapability(Capability.ADD_PROPERTY);

        Long ownerAccountId = ActorContext.get().internalId();

        ResolvedLocation rl =
                locationResolver
                        .resolveText(tokenizeArea(conv.getArea()))
                        .orElse(null);

        Property property = new Property(
                ownerAccountId,
                buildTitle(conv, rl),
                conv.getBhk(),
                conv.getPrice(),
                rl != null ? rl.city() : null,
                rl != null ? rl.locality() : null
        );

        property.addPhotos(conv.getPhotos());
        repository.save(property);
        return property;
    }

    /* =========================
       READ
       ========================= */

    public PropertyResponseDTO get(Long id) {
        return PropertyMapper.toDTO(loadOwnedProperty(id));
    }

    public List<PropertyResponseDTO> getAllForCurrentActor() {
        ActorContext.requireCapability(Capability.ADD_PROPERTY);

        Long accountId = ActorContext.get().internalId();

        return repository
                .findByOwnerAccountIdAndActiveTrue(accountId)
                .stream()
                .map(PropertyMapper::toDTO)
                .toList();
    }

    public List<Property> findMatchesForSearchers(
            String bhk,
            String cityCode,
            String localityCode,
            Integer min,
            Integer max
    ) {
        return repository.searchPublic(bhk, cityCode, localityCode, min, max);
    }

    /* =========================
       DELETE (soft)
       ========================= */

    public void delete(Long id) {
        Property property = loadOwnedProperty(id);
        property.deactivate();
        repository.save(property);
    }

    /* =========================
       INTERNAL HELPERS
       ========================= */

    private Property loadOwnedProperty(Long id) {

        Property property =
                repository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Property not found: " + id));

        Long accountId = ActorContext.get().internalId();
        if (!property.getOwnerAccountId().equals(accountId)) {
            throw new AccessDeniedException("Property does not belong to current actor");
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
        if (rl != null && rl.locality() != null) {
            return conv.getBhk() + " in " + rl.locality();
        }
        return conv.getBhk() + " Property";
    }
}
