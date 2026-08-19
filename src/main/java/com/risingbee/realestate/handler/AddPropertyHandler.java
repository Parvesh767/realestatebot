package com.risingbee.realestate.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.domain.BrokerConversation;
import com.risingbee.realestate.automation.dto.MediaInput;
import com.risingbee.realestate.automation.repo.BrokerConversationRepository;
import com.risingbee.realestate.automation.service.PropertyService;
import com.risingbee.realestate.automation.service.WhatsAppMediaService;
import com.risingbee.realestate.automation.service.WhatsAppSender;
import com.risingbee.realestate.enums.AddPropertyStep;
import com.risingbee.realestate.flow.ConversationFlow;
import com.risingbee.realestate.lifecycle.ConversationLifecycleManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddPropertyHandler {

    private final BrokerConversationRepository conversationRepo;
    private final WhatsAppSender whatsAppSender;
    private final WhatsAppMediaService mediaService;
    private final PropertyService propertyService;
    private final ConversationLifecycleManager lifecycleManager;

 
    public void handle(
            Actor actor,
            String message,
            Optional<MediaInput> mediaOpt,
            Optional<String> messageIdOpt
    ) {
        Optional<BrokerConversation> convOpt =
            lifecycleManager.acquire(
                actor.internalId(),
                ConversationFlow.ADD_PROPERTY,
                messageIdOpt,
                actor.externalId()
            );

        if (convOpt.isEmpty()) return;

        BrokerConversation conv = convOpt.get();

        AddPropertyStep step = conv.getAddPropertyStep();

        if (step == null) {
            step = AddPropertyStep.ASK_BHK;
            conv.setAddPropertyStep(step);
            conversationRepo.save(conv);
        }

        switch (step) {
            case ASK_BHK      -> handleBhk(conv, actor.externalId(), message);
            case ASK_LOCATION -> handleLocation(conv, actor.externalId(), message);
            case ASK_PRICE    -> handlePrice(conv, actor.externalId(), message);
            case ASK_PHOTOS   -> handlePhotos(conv, actor.externalId(), message, mediaOpt);
            case PREVIEW      -> handlePreview(conv, actor, message);
        }
    }



    
   
    
    /* ---------------- STEP HANDLERS ---------------- */
    
    
    

    private void handleBhk(
    	    BrokerConversation conv,
    	    String phone,
    	    String message
    	) {
    	    String bhk = message.toUpperCase().replaceAll("\\s+", "");

    	    if (!bhk.matches("\\d+BHK")) {
    	        send(phone, "Please enter valid BHK (e.g. 1BHK, 2BHK)");
    	        return;
    	    }

    	    conv.put("bhk", bhk);
    	    conv.setAddPropertyStep(AddPropertyStep.ASK_LOCATION);
    	    conversationRepo.save(conv);

    	    send(phone, "📍 What is the location? (e.g. Golf Course Road)");
    	}


    private void handleLocation(
    	    BrokerConversation conv,
    	    String phone,
    	    String message
    	) {
    	    if (message == null || message.trim().length() < 3) {
    	        send(phone, "Please enter a valid location (e.g. Golf Course Road)");
    	        return;
    	    }

    	    conv.put("area", message.trim());
    	    conv.setAddPropertyStep(AddPropertyStep.ASK_PRICE);
    	    conversationRepo.save(conv);

    	    send(phone, "💰 What is the monthly rent? (e.g. 24000)");
    	}


    private void handlePrice(
    	    BrokerConversation conv,
    	    String phone,
    	    String message
    	) {
    	    Integer price;

    	    try {
    	        String raw = message.toLowerCase().replaceAll("[^0-9k]", "");
    	        price = raw.endsWith("k")
    	            ? Integer.parseInt(raw.replace("k", "")) * 1000
    	            : Integer.parseInt(raw);
    	    } catch (Exception e) {
    	        send(phone, "Invalid amount. Please enter numbers only (e.g. 24000)");
    	        return;
    	    }

    	    if (price < 1000) {
    	        send(phone, "Rent seems too low. Please confirm the monthly rent.");
    	        return;
    	    }

    	    conv.put("price", price);
    	    conv.setAddPropertyStep(AddPropertyStep.ASK_PHOTOS);
    	    conversationRepo.save(conv);

    	    send(phone, """
    	        📸 Please send property photos now.
    	        You can send multiple photos.
    	        Type DONE when finished.
    	        """);
    	}

    @SuppressWarnings("unchecked")
    private void handlePhotos(
        BrokerConversation conv,
        String phone,
        String message,
        Optional<MediaInput> mediaOpt
    ) {
        List<String> photos =
            Optional.ofNullable(conv.get("photos", List.class))
                    .orElseGet(ArrayList::new);

        if (mediaOpt.isPresent()) {
            String path = mediaService.downloadAndStore(mediaOpt.get());
            photos.add(path);
            conv.put("photos", photos);
            conversationRepo.save(conv);

            send(phone, "📷 Photo added. Send more photos or type *DONE*");
            return;
        }

        if ("DONE".equalsIgnoreCase(message)) {
            if (photos.isEmpty()) {
                send(phone, "❌ Please upload at least one photo.");
                return;
            }

            conv.setAddPropertyStep(AddPropertyStep.PREVIEW);
            conversationRepo.save(conv);

            send(phone, buildPreview(conv));
            send(phone, "✅ Type *CONFIRM* to publish\n❌ Type *CANCEL* to discard");
            return;
        }

        send(phone, "📸 Please send property photos or type *DONE*.");
    }


    private void handlePreview(
        BrokerConversation conv,
      Actor actor,
        String message
    ) {
        if (message == null) {
            sendPreviewHint(actor.externalId());
            return;
        }

        switch (message.trim().toUpperCase()) {

            case "CONFIRM" -> {
                propertyService.createFromConversation(actor,conv);
                conversationRepo.delete(conv);
                send(actor.externalId(), "🎉 *Property published successfully!*");
            }

            case "CANCEL" -> {
                conversationRepo.delete(conv);
                send(actor.externalId(), "❌ Property creation cancelled.");
            }

            default -> sendPreviewHint(actor.externalId());
        }
    }

    /* ---------------- HELPERS ---------------- */

    private String buildPreview(BrokerConversation conv) {
        return """
            🏠 *Property Preview*

            • BHK: %s
            • Area: %s
            • Price: ₹%d
            • Photos: %d

            Is everything correct?
            """.formatted(
                conv.get("bhk",String.class),
                conv.get("area",String.class),
                conv.get("price",Integer.class),
                Optional.ofNullable(conv.get("photos", List.class)).map(List::size).orElse(0)
                );
              
      
    }

    private void sendPreviewHint(String phone) {
        send(phone, "⚠️ Type *CONFIRM* to publish or *CANCEL* to discard.");
    }

    private void sendError(String phone) {
        send(phone, "Something went wrong. Please try again.");
    }

    private void send(String phone, String message) {
        whatsAppSender.sendTextMessage(phone, message);
    }
}
