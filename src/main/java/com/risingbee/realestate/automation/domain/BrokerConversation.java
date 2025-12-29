package com.risingbee.realestate.automation.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.risingbee.realestate.converter.StringListJsonConverter;
import com.risingbee.realestate.enums.AddPropertyStep;
import com.risingbee.realestate.flow.ConversationFlow;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "broker_conversation",
    uniqueConstraints = @UniqueConstraint(columnNames = {"broker_id", "flow"})
)
@Getter @Setter
public class BrokerConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "broker_id", nullable = false)
    private Long brokerId;

    @Enumerated(EnumType.STRING)
    private ConversationFlow flow;

    @Enumerated(EnumType.STRING)
    private AddPropertyStep step;

    private String bhk;
    private String area;
    private Integer price;

    @Column(columnDefinition = "json")
    @Convert(converter = StringListJsonConverter.class)
    private List<String> photos = new ArrayList<>();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    /* helpers */
    public void addPhoto(String url) {
        photos.add(url);
    }
}

