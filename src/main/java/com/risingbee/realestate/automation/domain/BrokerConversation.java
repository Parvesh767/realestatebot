package com.risingbee.realestate.automation.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.risingbee.realestate.enums.AddPropertyStep;
import com.risingbee.realestate.flow.ConversationFlow;
import com.risingbee.realestate.flow.ConversationPhase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;@Entity
@Table(
	    name = "conversation",
	    uniqueConstraints = @UniqueConstraint(
	        columnNames = { "owner_account_id", "flow" }
	    )
	)
	@Getter
	public class BrokerConversation {

	    private static final int MAX_PROCESSED_IDS = 50;

	    @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

	    @Column(name = "owner_account_id", nullable = false)
	    private Long ownerAccountId;

	    @Enumerated(EnumType.STRING)
	    private ConversationFlow flow;

	    @Enumerated(EnumType.STRING)
	    private AddPropertyStep step;

	    @Enumerated(EnumType.STRING)
	    @Column(nullable = false)
	    private ConversationPhase phase;

	    private String bhk;
	    private String area;
	    private Integer price;

	    @Column(columnDefinition = "jsonb")
	    @JdbcTypeCode(SqlTypes.JSON)
	    private List<String> photos = new ArrayList<>();

	    @Column(nullable = false)
	    private Instant updatedAt = Instant.now();

	    @Column(name = "processed_message_ids_json", columnDefinition = "jsonb")
	    @JdbcTypeCode(SqlTypes.JSON)
	    private List<String> processedMessageIds = new ArrayList<>();

	    protected BrokerConversation() {}

	    /* ---------- domain mutations ---------- */

	    public void setStep(AddPropertyStep step) {
	        this.step = step;
	        touch();
	    }

	    public void setBhk(String bhk) {
	        this.bhk = bhk;
	        touch();
	    }

	    public void setArea(String area) {
	        this.area = area;
	        touch();
	    }

	    public void setPrice(Integer price) {
	        this.price = price;
	        touch();
	    }

	    public void addPhoto(String url) {
	        photos.add(url);
	        touch();
	    }

	    public void setFlow(ConversationFlow flow) {
	        this.flow = flow;
	        touch();
	    }

	    public void setPhase(ConversationPhase phase) {
	        this.phase = phase;
	        touch();
	    }

	    /* ---------- idempotency ---------- */

	    public boolean markMessageProcessed(String messageId) {
	        if (processedMessageIds.contains(messageId)) {
	            return false;
	        }
	        processedMessageIds.add(messageId);
	        if (processedMessageIds.size() > MAX_PROCESSED_IDS) {
	            processedMessageIds.remove(0);
	        }
	        return true;
	    }

	    private void touch() {
	        this.updatedAt = Instant.now();
	    }

	    /* ---------- factories ---------- */

	    public static BrokerConversation start(
	        Long ownerAccountId,
	        ConversationFlow flow
	    ) {
	        BrokerConversation c = new BrokerConversation();
	        c.ownerAccountId = ownerAccountId;
	        c.flow = flow;
	        c.phase = ConversationPhase.IN_FLOW;
	        c.step = AddPropertyStep.ASK_BHK;
	        c.updatedAt = Instant.now();
	        return c;
	    }

	    public static BrokerConversation startChoosingFlow(
	        Long ownerAccountId
	    ) {
	        BrokerConversation c = new BrokerConversation();
	        c.ownerAccountId = ownerAccountId;
	        c.phase = ConversationPhase.CHOOSING_FLOW;
	        c.updatedAt = Instant.now();
	        return c;
	    }
	    
	    public void clearProcessedMessages() {
	        processedMessageIds.clear();
	        touch();
	    }
	    
	    
	    public static BrokerConversation createForAccount(Long accountId) {
	        BrokerConversation c = new BrokerConversation();
	        c.ownerAccountId = accountId; // TEMP: column rename later
	        c.phase = ConversationPhase.CHOOSING_FLOW;
	        c.updatedAt = Instant.now();
	        return c;
	    }

	}
