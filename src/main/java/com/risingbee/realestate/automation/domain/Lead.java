package com.risingbee.realestate.automation.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.risingbee.realestate.converter.StringListJsonConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "leads")
@Getter
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String phoneNumber;
    private String name;

    private Integer minBudget;
    private Integer maxBudget;
    private String bhk;

    private String cityCode;
    private String localityCode;

    private String rawMessage;

    private Instant createdAt;

    @Column(name = "owner_account_id", nullable = false)
    private Long ownerAccountId;

    @Column(name = "confirmed_message_ids_json", columnDefinition = "json")
    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = StringListJsonConverter.class)
    private List<String> confirmedMessageIds = new ArrayList<>();

    protected Lead() {}

    public Lead(
        String phoneNumber,
        Long ownerAccountId,
        String bhk,
        Integer minBudget,
        Integer maxBudget,
        String cityCode,
        String localityCode,
        String rawMessage
    ) {
        this.phoneNumber = phoneNumber;
        this.ownerAccountId = ownerAccountId;
        this.bhk = bhk;
        this.minBudget = minBudget;
        this.maxBudget = maxBudget;
        this.cityCode = cityCode;
        this.localityCode = localityCode;
        this.rawMessage = rawMessage;
        this.createdAt = Instant.now();
    }

    public boolean markYesProcessed(String messageId) {
        if (confirmedMessageIds.contains(messageId)) {
            return false;
        }
        confirmedMessageIds.add(messageId);
        return true;
    }
}
