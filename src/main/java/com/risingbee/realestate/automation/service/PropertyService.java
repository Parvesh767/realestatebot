package com.risingbee.realestate.automation.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.risingbee.realestate.automation.domain.Broker;
import com.risingbee.realestate.automation.domain.BrokerConversation;
import com.risingbee.realestate.automation.domain.Property;
import com.risingbee.realestate.automation.dto.PropertyRequestDTO;
import com.risingbee.realestate.automation.dto.PropertyResponseDTO;
import com.risingbee.realestate.automation.exception.AccessDeniedException;
import com.risingbee.realestate.automation.exception.ResourceNotFoundException;
import com.risingbee.realestate.automation.mapper.PropertyMapper;
import com.risingbee.realestate.automation.repo.PropertyRepository;
import com.risingbee.realestate.automation.tenant.BrokerContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PropertyService {

    private final PropertyRepository repository;

    /* =========================
       CREATE (REST / API)
       ========================= */
    public PropertyResponseDTO create(PropertyRequestDTO dto) {

        Broker broker = requireBroker();

        Property property = PropertyMapper.toEntity(dto, broker);

        repository.save(property);
        return PropertyMapper.toDTO(property);
    }

    /* =========================
       CREATE (WhatsApp flow)
       ========================= */
    public Property createFromConversation(BrokerConversation conv) {

        Broker broker = requireBroker();

        Property p = Property.builder()
                .broker(broker)
                .bhk(conv.getBhk())
                .area(conv.getArea())
                .price(conv.getPrice())
                .title(conv.getBhk() + " in " + conv.getArea())
                .active(true)
                .build();

        p.setPhotos(conv.getPhotos());

        repository.save(p);
        return p;
    }


    /* =========================
       UPDATE
       ========================= */
    public PropertyResponseDTO update(Long id, PropertyRequestDTO dto) {

        Property p = loadOwnedProperty(id);

        if (dto.getTitle() != null)       p.setTitle(dto.getTitle());
        if (dto.getArea() != null)        p.setArea(dto.getArea());
        if (dto.getCity() != null)        p.setCity(dto.getCity());
        if (dto.getPrice() != null)       p.setPrice(dto.getPrice());
        if (dto.getBhk() != null)         p.setBhk(dto.getBhk());
        if (dto.getDescription() != null) p.setDescription(dto.getDescription());
        if (dto.getMapLink() != null)     p.setMapLink(dto.getMapLink());
        if (dto.getPhotos() != null)      p.setPhotos(dto.getPhotos());

        repository.save(p);
        return PropertyMapper.toDTO(p);
    }

    /* =========================
       READ
       ========================= */
    public PropertyResponseDTO get(Long id) {
        return PropertyMapper.toDTO(loadOwnedProperty(id));
    }

    public List<PropertyResponseDTO> getAllForCurrentBroker() {

        Long brokerId = BrokerContext.id();
        if (brokerId == null) throw new IllegalStateException("No broker in context");

        return repository.findByBrokerIdAndActiveTrue(brokerId)
                .stream()
                .map(PropertyMapper::toDTO)
                .toList();
    }

    /* =========================
       DELETE (soft)
       ========================= */
    public void delete(Long id) {

        Property p = loadOwnedProperty(id);
        p.setActive(false);
        repository.save(p);
    }

    /* =========================
       MATCHING
       ========================= */
    public List<Property> findMatches(
            String bhk,
            String area,
            Integer minBudget,
            Integer maxBudget,
            Long optionalBrokerId
    ) {

        Long brokerId = optionalBrokerId != null
                ? optionalBrokerId
                : BrokerContext.id();

        if (brokerId == null) {
            throw new IllegalStateException("No broker in context");
        }

        return repository.findMatches(
                brokerId,
                blankToNull(bhk),
                blankToNull(area),
                minBudget,
                maxBudget
        );
    }

    /* =========================
       INTERNAL HELPERS
       ========================= */
    public Broker requireBroker() {
        Broker broker = BrokerContext.get();
        if (broker == null) {
            throw new IllegalStateException("No broker in context");
        }
        return broker;
    }

    private Property loadOwnedProperty(Long id) {

        Property p = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found: " + id));

        Long brokerId = BrokerContext.id();
        if (brokerId == null) {
            throw new IllegalStateException("No broker in context");
        }

        if (!p.getBroker().getId().equals(brokerId)) {
            throw new AccessDeniedException("Property does not belong to current broker");
        }

        return p;
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}

