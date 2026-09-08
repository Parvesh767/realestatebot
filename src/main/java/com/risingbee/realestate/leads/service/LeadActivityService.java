package com.risingbee.realestate.leads.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.risingbee.realestate.leads.domain.LeadActivity;
import com.risingbee.realestate.leads.enums.ActivityType;
import com.risingbee.realestate.leads.repo.LeadActivityRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class LeadActivityService {

    private final LeadActivityRepository activityRepository;

    public LeadActivity log(Long leadId, Long accountId, ActivityType type, String description) {
        LeadActivity activity = new LeadActivity(leadId, accountId, type, description);
        LeadActivity saved = activityRepository.save(activity);
        log.info("Lead activity recorded: type={}, leadId={}, ownerAccountId={}", type, leadId, accountId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<LeadActivity> getTimelineForLead(Long leadId) {
        return activityRepository.findByLeadIdOrderByCreatedAtDesc(leadId);
    }

    @Transactional(readOnly = true)
    public List<LeadActivity> getRecentActivitiesForOwner(Long ownerAccountId) {
        return activityRepository.findByAccountIdOrderByCreatedAtDesc(ownerAccountId);
    }
}