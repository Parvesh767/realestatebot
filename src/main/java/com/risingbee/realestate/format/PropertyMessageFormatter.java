package com.risingbee.realestate.format;

import java.util.List;

import org.springframework.stereotype.Component;

import com.risingbee.realestate.automation.domain.Property;

@Component
public class PropertyMessageFormatter {

    public String format(List<Property> properties) {
        StringBuilder sb = new StringBuilder();
        sb.append("Here are some matching properties:\n\n");

        properties.stream()
            .limit(3)
            .forEach(p -> sb.append(
                "🏠 ").append(p.getTitle()).append("\n")
//                .append("📍 ").append(p.getArea()).append("\n")
                .append("💰 ₹").append(p.getPrice()).append("/month\n\n")
            );

        sb.append("Reply *YES* to connect with the broker.");
        return sb.toString();
    }
    
    
    


	public String formatAmount(Integer min) {
		 return String.format("%,d", min);
	}
}


