package com.risingbee.realestate.automation.dto;

import java.util.List;

import com.risingbee.realestate.automation.domain.Property;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SearchResultResponse {

	  	public List<Property> properties;
	    public String tag;

//	    public SearchResultResponse(List<Property> properties, String tag) {
//	        this.properties = properties;
//	        this.tag = tag;
//	    }
	
}
