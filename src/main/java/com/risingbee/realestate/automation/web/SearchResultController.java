package com.risingbee.realestate.automation.web;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.risingbee.realestate.automation.domain.Property;
import com.risingbee.realestate.automation.parser.LocationResolver;
import com.risingbee.realestate.automation.parser.ParsedRequest;
import com.risingbee.realestate.automation.parser.ResolvedLocation;
import com.risingbee.realestate.automation.parser.SimpleParser;
import com.risingbee.realestate.automation.service.PropertyService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class SearchResultController {

    private final PropertyService propertyService;
    private final LocationResolver locationResolver;

    @PostMapping("/search/results")
    public String searchResults(
            @RequestParam String query,
            Model model
    ) {
        ParsedRequest parsed = SimpleParser.parse(query);

        ResolvedLocation rl = locationResolver.resolve(parsed).orElse(null);

        if (rl == null) {
            model.addAttribute("error", "Invalid location");
            return "search/index";
        }

        List<Property> results =
                propertyService.findMatchesForSearchers(
                        parsed.bhk(),
                        rl.city(),
                        rl.locality(),
                        parsed.minBudget(),
                        parsed.maxBudget()
                );

        model.addAttribute("results", results);

        return "search/results";
    }
}
