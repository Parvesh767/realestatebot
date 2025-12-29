package com.risingbee.realestate.handler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.risingbee.realestate.automation.domain.BrokerConversation;
import com.risingbee.realestate.automation.domain.Property;
import com.risingbee.realestate.automation.dto.MediaInput;
import com.risingbee.realestate.automation.repo.BrokerConversationRepository;
import com.risingbee.realestate.automation.repo.PropertyRepository;
import com.risingbee.realestate.automation.service.PropertyService;
import com.risingbee.realestate.automation.service.WhatsAppMediaService;
import com.risingbee.realestate.automation.service.WhatsAppSender;
import com.risingbee.realestate.automation.tenant.BrokerContext;
import com.risingbee.realestate.enums.AddPropertyStep;
import com.risingbee.realestate.flow.ConversationFlow;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddPropertyHandler {

	private final BrokerConversationRepository conversationRepo;
	private final WhatsAppSender whatsAppSender;
	private final PropertyRepository propertyRepository;
	private final WhatsAppMediaService mediaService;
	private final PropertyService propertyService;

	public void handle(String from, String message, Optional<MediaInput> mediaOpt) {

		Long brokerId = BrokerContext.id();
		if (brokerId == null)
			return;

		BrokerConversation conv = conversationRepo.findByBrokerIdAndFlow(brokerId, ConversationFlow.ADD_PROPERTY)
				.orElseGet(() -> startNewConversation(brokerId));

		switch (conv.getStep()) {
		case ASK_BHK -> handleBhk(conv, from, message);
		case ASK_LOCATION -> handleLocation(conv, from, message);
		case ASK_PRICE -> handlePrice(conv, from, message);
		case ASK_PHOTOS -> handlePhotos(conv, from, message, mediaOpt);
		case PREVIEW -> handlePreview(conv, from, message);
		}
	}

	@Transactional
	private void createProperty(BrokerConversation conv) {

	    Property p = new Property();
	    p.setBroker(BrokerContext.get());
	    p.setBhk(conv.getBhk());
	    p.setArea(conv.getArea());
	    p.setPrice(conv.getPrice());

	    // ✅ defensive copy (important)
	    p.setPhotos(new ArrayList<>(conv.getPhotos()));

	    p.setTitle(conv.getBhk() + " in " + conv.getArea());
	    p.setActive(true);

	    propertyRepository.save(p);
	}

	private BrokerConversation startNewConversation(Long brokerId) {
		BrokerConversation c = new BrokerConversation();
		c.setBrokerId(brokerId);
		c.setFlow(ConversationFlow.ADD_PROPERTY);
		c.setStep(AddPropertyStep.ASK_BHK);
		conversationRepo.save(c);
		return c;
	}

	private void handleBhk(BrokerConversation conv, String phone, String message) {
		String bhk = message.toUpperCase().replaceAll("\\s+", "");

		if (!bhk.matches("\\d+BHK")) {
			whatsAppSender.sendTextMessage(phone, "Please enter valid BHK (e.g. 1BHK, 2BHK)");
			return;
		}

		conv.setBhk(bhk);
		conv.setStep(AddPropertyStep.ASK_LOCATION);
		conv.setUpdatedAt(Instant.now());
		conversationRepo.save(conv);

		whatsAppSender.sendTextMessage(phone, "📍 What is the location? (e.g. Golf Course Road)");
	}

	private void handleLocation(BrokerConversation conv, String phone, String message) {
		String location = message.trim();

		if (location.length() < 3) {
			whatsAppSender.sendTextMessage(phone, "Please enter a valid location (e.g. Golf Course Road)");
			return;
		}

		conv.setArea(location);
		conv.setStep(AddPropertyStep.ASK_PRICE);
		conv.setUpdatedAt(Instant.now());
		conversationRepo.save(conv);

		whatsAppSender.sendTextMessage(phone, "💰 What is the monthly rent? (e.g. 24000)");
	}

	private void handlePrice(BrokerConversation conv, String phone, String message) {
		String raw = message.toLowerCase().replaceAll("[^0-9k]", "");

		if (raw.isBlank()) {
			whatsAppSender.sendTextMessage(phone, "Please enter a valid rent amount (e.g. 24000 or 25k)");
			return;
		}

		Integer price;
		try {
			if (raw.endsWith("k")) {
				price = Integer.parseInt(raw.replace("k", "")) * 1000;
			} else {
				price = Integer.parseInt(raw);
			}
		} catch (NumberFormatException e) {
			whatsAppSender.sendTextMessage(phone, "Invalid amount. Please enter numbers only (e.g. 24000)");
			return;
		}

		if (price < 1000) {
			whatsAppSender.sendTextMessage(phone, "Rent seems too low. Please confirm the monthly rent.");
			return;
		}

		conv.setPrice(price);
		conv.setStep(AddPropertyStep.ASK_PHOTOS);
		conv.setUpdatedAt(Instant.now());
		conversationRepo.save(conv);

		whatsAppSender.sendTextMessage(phone, """
				📸 Please send property photos now.

				You can send multiple photos.
				Type DONE when finished.
				""");
	}


	private void handlePhotos(
	        BrokerConversation conv,
	        String from,
	        String message,
	        Optional<MediaInput> mediaOpt
	) {

	    // 1️⃣ Image upload (highest priority)
	    if (mediaOpt.isPresent()) {

	        String storedPath = mediaService.downloadAndStore(mediaOpt.get());
	        conv.addPhoto(storedPath);
	        conv.setUpdatedAt(Instant.now());
	        conversationRepo.save(conv);

	        whatsAppSender.sendTextMessage(
	            from,
	            "📷 Photo added. Send more photos or type *DONE*"
	        );
	        return;
	    }

	    // 2️⃣ DONE command → move to PREVIEW
	    if (message != null && message.equalsIgnoreCase("DONE")) {

	        if (conv.getPhotos().isEmpty()) {
	            whatsAppSender.sendTextMessage(
	                from,
	                "❌ Please upload at least one photo before continuing."
	            );
	            return;
	        }

	        conv.setStep(AddPropertyStep.PREVIEW);
	        conv.setUpdatedAt(Instant.now());
	        conversationRepo.save(conv);

	        whatsAppSender.sendTextMessage(from, buildPreview(conv));
	        whatsAppSender.sendTextMessage(
	            from,
	            "✅ Type *CONFIRM* to publish\n❌ Type *CANCEL* to discard"
	        );
	        return;
	    }

	    // 3️⃣ Invalid input
	    whatsAppSender.sendTextMessage(
	        from,
	        "📸 Please send property photos or type *DONE* when finished."
	    );
	}

	
	private String buildPreview(BrokerConversation conv) {

	    StringBuilder sb = new StringBuilder();

	    sb.append("🏠 *Property Preview*\n\n");
	    sb.append("• BHK: ").append(conv.getBhk()).append("\n");
	    sb.append("• Area: ").append(conv.getArea()).append("\n");
	    sb.append("• Price: ₹").append(conv.getPrice()).append("\n");
	    sb.append("• Photos: ").append(conv.getPhotos().size()).append("\n\n");

	    sb.append("Is everything correct?");

	    return sb.toString();
	}

	
	private void handlePreview(BrokerConversation conv, String from, String message) {

		if (message == null) {
			sendPreviewHint(from);
			return;
		}

		switch (message.trim().toUpperCase()) {

		case "CONFIRM" -> {
			createProperty(conv);
			conversationRepo.delete(conv);

			whatsAppSender.sendTextMessage(from, "🎉 *Property published successfully!*");
		}

		case "CANCEL" -> {
			conversationRepo.delete(conv);

			whatsAppSender.sendTextMessage(from, "❌ Property creation cancelled.");
		}

		default -> sendPreviewHint(from);
		}
	}

	private void sendPreviewHint(String phone) {
		whatsAppSender.sendTextMessage(phone, "⚠️ Please type *CONFIRM* to publish or *CANCEL* to discard.");
	}

	@Transactional
	public void handleConfirm(BrokerConversation conv) {

		if (conv.getStep() != AddPropertyStep.PREVIEW) {
			return; // idempotent safety
		}

		Property property = Property.builder().broker(propertyService.requireBroker())
				.title(conv.getBhk() + " in " + conv.getArea()).bhk(conv.getBhk()).area(conv.getArea())
				.price(conv.getPrice()).photos(new ArrayList<>(conv.getPhotos())).active(true).build();

		propertyRepository.save(property);

		// cleanup
		conversationRepo.delete(conv);

		whatsAppSender.sendTextMessage(propertyService.requireBroker().getPhone(),
				"🎉 *Property Published Successfully!*");
	}

	@Transactional
	public void handleCancel(BrokerConversation conv) {

		conversationRepo.delete(conv);

		whatsAppSender.sendTextMessage(propertyService.requireBroker().getPhone(), "❌ Property creation cancelled.");
	}

}
