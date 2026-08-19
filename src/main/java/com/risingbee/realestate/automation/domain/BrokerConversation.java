package com.risingbee.realestate.automation.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.risingbee.realestate.enums.AddPropertyStep;
import com.risingbee.realestate.enums.BrokerOnboardingStep;
import com.risingbee.realestate.flow.ConversationFlow;
import com.risingbee.realestate.flow.ConversationPhase;
import com.risingbee.realestate.flow.FlowType;
import com.risingbee.realestate.flow.state.AddPropertyState;
import com.risingbee.realestate.flow.state.OnboardingState;
import com.risingbee.realestate.flow.state.SearchState;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

@Entity
@Table(
    name = "conversation",
    uniqueConstraints = @UniqueConstraint(columnNames = { "owner_account_id", "flow" })
)
@Getter
public class BrokerConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_account_id", nullable = false)
    private Long ownerAccountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationFlow flow;

    /* ---------- FLOW-SPECIFIC STEPS ---------- */

    @Enumerated(EnumType.STRING)
    private BrokerOnboardingStep onboardingStep;

    @Enumerated(EnumType.STRING)
    private AddPropertyStep addPropertyStep;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationPhase phase;

    /* ---------- FLOW DATA ---------- */

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> state = new HashMap<>();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "processed_message_ids_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> processedMessageIds = new ArrayList<>();
    
    
    

    @Enumerated(EnumType.STRING)
    private FlowType FlowType;
    
    @Enumerated(EnumType.STRING)
    private OnboardingState onboardingState;

    @Enumerated(EnumType.STRING)
    private AddPropertyState addPropertyState;

    @Enumerated(EnumType.STRING)
    private SearchState searchState;

    private String contextJson; // flexible storage

    

    protected BrokerConversation() {}

    /* ---------- state helpers ---------- */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {

        Object value = state.get(key);

        if (value == null)
            return null;

        if (type.isInstance(value))
            return type.cast(value);

        if (type == Set.class && value instanceof List<?> list)
            return (T) new HashSet<>(list);

        if (type == List.class && value instanceof Set<?> set)
            return (T) new ArrayList<>(set);

        throw new IllegalStateException(
            "Cannot convert state[" + key + "] from "
            + value.getClass() + " to " + type
        );
    }
    
    
    public Set<String> getStringSet(String key) {

        Object raw = state.get(key);

        if (raw == null)
            return new HashSet<>();

        if (raw instanceof Set<?> set)
            return (Set<String>) set;

        if (raw instanceof List<?> list)
            return new HashSet<>(list.stream()
                    .map(Object::toString)
                    .toList());

        throw new IllegalStateException("Invalid type for " + key);
    }
    
    
    

//    public void put(String key, Object value) {
//        state.put(key, value);
//        touch();
//    }
//    
    
    public void put(String key, Object value) {
        if (key == null)
            throw new IllegalArgumentException("State key cannot be null");

        state.put(key, value);
        touch();
    }
    
    

    /* ---------- step setters ---------- */

    public void setOnboardingStep(BrokerOnboardingStep step) {
        this.onboardingStep = step;
        touch();
    }

    public void setAddPropertyStep(AddPropertyStep step) {
        this.addPropertyStep = step;
        touch();
    }

    public BrokerOnboardingStep getOnboardingStep() {
        return onboardingStep;
    }

    public AddPropertyStep getAddPropertyStep() {
        return addPropertyStep;
    }

    /* ---------- lifecycle ---------- */

    public boolean markMessageProcessed(String messageId) {
        if (processedMessageIds.contains(messageId)) return false;
        processedMessageIds.add(messageId);
        if (processedMessageIds.size() > 50) processedMessageIds.remove(0);
        return true;
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
    
    
    public void startFlow(
            ConversationFlow flow,
            ConversationPhase phase
    ) {
        this.flow = flow;
        this.phase = phase;
        touch();
    }
    
    
    public static BrokerConversation createForAccount(Long accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException(
                "Conversation requires a non-null accountId"
            );
        }

        BrokerConversation c = new BrokerConversation();
        c.ownerAccountId = accountId;
        c.phase = ConversationPhase.CHOOSING_FLOW;
        c.flow = ConversationFlow.SEARCH;
        c.updatedAt = Instant.now();
        return c;
    }
	
	
	public void clearState() {
	    state.clear();
	    onboardingStep = null;
	    addPropertyStep = null;
	    flow = ConversationFlow.SEARCH;
	    phase = ConversationPhase.CHOOSING_FLOW;
	    touch();
	}

}
