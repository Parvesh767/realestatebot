package com.risingbee.realestate.automation.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.risingbee.realestate.automation.domain.Broker;
import com.risingbee.realestate.automation.domain.Lead;
import com.risingbee.realestate.automation.domain.Property;
import com.risingbee.realestate.automation.parser.ParsedRequest;
import com.risingbee.realestate.automation.parser.SimpleParser;
import com.risingbee.realestate.automation.parser.WhatsAppPayloadExtractor;
import com.risingbee.realestate.automation.repo.BrokerRepository;
import com.risingbee.realestate.automation.repo.LeadRepository;
import com.risingbee.realestate.automation.tenant.BrokerContext;
import com.risingbee.realestate.enums.BrokerStatus;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

@Slf4j

@Service
@RequiredArgsConstructor
public class WhatsAppService {

	private final LeadService leadService;
	private final PropertyService propertyService;
	private final WhatsAppSender whatsAppSender;	
	private final BrokerOnboardingServiceImpl brokerOnboardingService;	
	private final WhatsAppPayloadExtractor extractor;	
	private final BrokerRepository brokerRepository;

	
	private static final int FREE_LEAD_LIMIT = 10;

	public void handleIncoming(Map<String, Object> payload) {

	    log.info("Handling incoming WhatsApp message...");

	    Optional<String> phoneOpt = extractor.extractPhone(payload);
	    Optional<String> textOpt  = extractor.extractText(payload);

	    
	    if (phoneOpt.isEmpty() || textOpt.isEmpty()) {
	        log.warn("Required fields missing from webhook payload");
	        return;
	    }

	    String from = phoneOpt.get();
	    String text = textOpt.get().trim().toLowerCase();

	    Broker broker = BrokerContext.get();
	    if (broker == null) {
	        log.warn("No broker in context");
	        return;
	    }

	    if (broker.getStatus() == BrokerStatus.ONBOARDING) {
	    	brokerOnboardingService.handle(broker, text);
	        return;
	    }
	  
	    if (broker.getStatus() != BrokerStatus.ACTIVE) {
	        return; // suspended or invalid
	    }

	    // ✅ YES FLOW (NO parsing, NO lead creation)
	    if (text.equals("yes")) {
	        handleYesConfirmation(payload);
	        return;
	    }

	    // 🔍 SEARCH FLOW
	    ParsedRequest parsed = SimpleParser.parse(text);
	    
	    
	    boolean hasSearchIntent =
	            parsed.bhk() != null &&
	            (parsed.minBudget() != null || parsed.maxBudget() != null) &&
	            (parsed.city() != null || parsed.location() != null);

	    if (!hasSearchIntent) {
	        log.info("No search intent detected. Skipping lead creation.");
	        return;
	    }
	    
//	    if (!parsed.hasSearchIntent()) {
//	        log.info("No search intent detected. Skipping lead creation.");
//	        whatsAppSender.sendTextMessage(
//	            from,
//	            "Please share your requirement, e.g. 2 BHK rent in Gurgaon 30k"
//	        );
//	        return;
//	    }
	    

	    if (!parsed.valid()) {
	        handleInvalidRequest(parsed, from);
	        return;
	    }
	    
	    
	    Long brokerId = broker.getId();

	    log.info("Context brokerId = {}", brokerId);
	    log.info("DB existsById = {}", brokerRepository.existsById(brokerId));

	    // ✅ create lead only for real searches
	    leadService.createFromParsed(
	            from,
	            text,
	            parsed.bhk(),
	            parsed.minBudget(),
	            parsed.maxBudget(),
	            parsed.location(),
	            parsed.city()
	    );

	    List<Property> matches = propertyService.findMatches(
	    		parsed.title(),
	    		parsed.bhk(),
	            parsed.city(),
	            parsed.minBudget(),
	            parsed.maxBudget(),
//	            parsed.city(),
	            broker.getId()
	            
	    );
	    
	    
	    log.warn(
	    	    "INTENT CHECK  → title={} bhk={}, min={}, max={}, city={}, location={}",
	    	    
	    	    parsed.title(),    
	    	    parsed.bhk(),
	    	    parsed.minBudget(),
	    	    parsed.maxBudget(),
	    	    parsed.city(),
	    	    parsed.location()
	    	);

	    if (matches.isEmpty()) {
	        whatsAppSender.sendTextMessage(from,
	                "I couldn't find matching properties. Try changing budget or location.");
	        return;
	    }

	    
	    sendPropertyList(from, matches);
	}

	
	
	/* ---------------- Defensive JSON extractors ---------------- */

	private void handleYesConfirmation(Map<String, Object> payload) {	
		

		Optional<String> phoneOpt = extractor.extractPhone(payload);
		if (phoneOpt.isEmpty()) {
			return;
		}

		String phone = phoneOpt.get();

		

	    Long brokerId = BrokerContext.id();

	    Instant startOfMonth = LocalDate.now()
	            .withDayOfMonth(1)
	            .atStartOfDay(ZoneId.systemDefault())
	            .toInstant();

	    long usedLeads = LeadRepository.countMonthlyLeads(
	            brokerId,
	            startOfMonth
	    );

	    if (usedLeads >= FREE_LEAD_LIMIT) {
	        whatsAppSender.sendTextMessage(
	                phone,
	                """
	                🚫 Free lead limit reached.

	                Please contact the broker directly
	                or upgrade your plan to receive more enquiries.
	                """
	        );
	        return;
	    }
		
		
		
		// 1. Find latest lead for this phone
		Optional<Lead> latestLeadOpt = leadService.findLatestByPhone(phone);

		if (latestLeadOpt.isEmpty()) {
			whatsAppSender.sendTextMessage(phone, "I couldn’t find a recent enquiry. Please search again.");
			return;
		}

		Lead lead = latestLeadOpt.get();
		Broker broker = BrokerContext.get();

		// 2. Notify broker
		String brokerMessage = """
				📢 New Interested Lead

				📞 Phone: %s
				🏠 Requirement: %s
				📍 Location: %s
				💰 Budget: %s
				""".formatted(phone, lead.getBhk(), lead.getLocation(), formatBudget(lead));

		whatsAppSender.sendTextMessage(broker.getPhone(), brokerMessage);

		// 3. Confirm to user
		whatsAppSender.sendTextMessage(phone, "✅ Thanks! The broker has been notified and will contact you shortly.");
		
	}

	private String formatAmount(Integer amount) {
		return String.format("%,d", amount);
	}

	private String formatBudget(Lead lead) {

		Integer min = lead.getMinBudget();
		Integer max = lead.getMaxBudget();

		if (min == null && max == null) {
			return "Not specified";
		}

		if (min != null && max != null && min.equals(max)) {
			return "₹" + formatAmount(min);
		}

		if (min != null && max != null) {
			return "₹" + formatAmount(min) + " – ₹" + formatAmount(max);
		}

		if (min != null) {
			return "From ₹" + formatAmount(min);
		}

		return "Up to ₹" + formatAmount(max);
	}
	
	private void sendPropertyList(String phone, List<Property> matches) {

	    StringBuilder reply = new StringBuilder("Here are some matching properties:\n\n");

	    matches.stream()
	            .limit(5)
	            .forEach(p -> reply.append("🏠 ").append(p.getBhk()).append("\n")
	            	    .append((p.getTitle() == null || p.getTitle().isBlank()) ? p.getBhk() : p.getTitle())
	            	    .append('\n')
	                    .append("📍 ").append(p.getArea()).append('\n')
	                    .append("💰 ₹").append(p.getPrice()).append(" / month\n")
	                    .append("---------------------\n"));

	    reply.append("\nReply YES to connect with the broker.");

	    whatsAppSender.sendTextMessage(phone, reply.toString());
	}

	
	private void handleInvalidRequest(ParsedRequest parsed, String phone) {

	    if ("PURCHASE_BUDGET_NOT_SUPPORTED".equals(parsed.invalidReason())) {
	        whatsAppSender.sendTextMessage(phone,
	                """
	                I currently help with rental properties only.
	                Please share your monthly rent budget (e.g. 25k, 30k).
	                """
	        );
	    } else {
	        whatsAppSender.sendTextMessage(phone,
	                "Sorry, I couldn't understand your request. Please try again.");
	    }
	}

}
