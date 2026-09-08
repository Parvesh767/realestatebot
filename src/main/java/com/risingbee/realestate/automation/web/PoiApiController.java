package com.risingbee.realestate.automation.web;

import com.risingbee.realestate.automation.service.NearbyPlacesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PoiApiController {

    private final NearbyPlacesService nearbyPlacesService;

    @GetMapping("/nearby")
    public ResponseEntity<List<NearbyPlacesService.PlaceSummary>> getNearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "1500") int radius) {
        return ResponseEntity.ok(nearbyPlacesService.findNearbyKeyPoints(lat, lng, radius));
    }
}