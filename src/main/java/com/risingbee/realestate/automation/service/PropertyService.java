package com.risingbee.realestate.automation.service;

import com.risingbee.realestate.automation.domain.Property;
import com.risingbee.realestate.automation.domain.Broker;
import com.risingbee.realestate.automation.dto.PropertyRequestDTO;
import com.risingbee.realestate.automation.dto.PropertyResponseDTO;
import com.risingbee.realestate.automation.mapper.PropertyMapper;
import com.risingbee.realestate.automation.repo.PropertyRepository;
import com.risingbee.realestate.automation.repo.BrokerRepository;
import com.risingbee.realestate.automation.tenant.BrokerContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PropertyService {

    private final PropertyRepository repository;
    private final BrokerRepository brokerRepository;

    public PropertyResponseDTO create(PropertyRequestDTO dto) {
        Broker broker = BrokerContext.get();
        if (broker == null) throw new IllegalStateException("No broker in context");

        Property property = PropertyMapper.toEntity(dto);
        property.setBroker(broker);
        repository.save(property);
        return PropertyMapper.toDTO(property);
    }

    public PropertyResponseDTO update(Long id, PropertyRequestDTO dto) {
        Property p = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found"));

        // ensure the property belongs to the current broker
        Long currentBrokerId = BrokerContext.id();
        if (!p.getBroker().getId().equals(currentBrokerId)) {
            throw new SecurityException("Property does not belong to current broker");
        }

        p.setTitle(dto.getTitle());
        p.setArea(dto.getArea());
        p.setPrice(dto.getPrice());
        p.setBhk(dto.getBhk());
        p.setDescription(dto.getDescription());
        p.setMapLink(dto.getMapLink());
        p.setPhotosJson(dto.getPhotosCsv());
        repository.save(p);
        return PropertyMapper.toDTO(p);
    }

    public PropertyResponseDTO get(Long id) {
        Property p = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found"));

        Long currentBrokerId = BrokerContext.id();
        if (!p.getBroker().getId().equals(currentBrokerId)) {
            throw new SecurityException("Property does not belong to current broker");
        }

        return PropertyMapper.toDTO(p);
    }

    public List<PropertyResponseDTO> getAllForCurrentBroker() {
        Long brokerId = BrokerContext.id();
        if (brokerId == null) throw new IllegalStateException("No broker in context");
        return repository.findByBrokerIdAndActiveTrue(brokerId).stream()
                .map(PropertyMapper::toDTO)
                .toList();
    }

    public void delete(Long id) {
        Property p = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found"));
        Long brokerId = BrokerContext.id();
        if (!p.getBroker().getId().equals(brokerId)) {
            throw new SecurityException("Property does not belong to current broker");
        }
        p.setActive(false);
        repository.save(p);
    }

    /**
     * Find matches for a parsed user requirement. Accepts nulls gracefully.
     */
    public List<Property> findMatches(String bhk, String location, Integer minBudget, Integer maxBudget, Long optionalBrokerId) {
        Long brokerId = (optionalBrokerId != null) ? optionalBrokerId : BrokerContext.id();
        if (brokerId == null) throw new IllegalStateException("No broker in context");
        return repository.findMatches(brokerId, bhk, location, minBudget, maxBudget);
    }
}
