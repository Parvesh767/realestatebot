package com.risingbee.realestate.add;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.risingbee.realestate.enums.AddPropertyStep;

@Component
public class AddPropertyState {

    private final Map<String, AddPropertyStep> stepByPhone = new ConcurrentHashMap<>();
    private final Map<String, PropertyDraft> draftByPhone = new ConcurrentHashMap<>();

    public void start(String phone) {
        stepByPhone.put(phone, AddPropertyStep.ASK_BHK);
        draftByPhone.put(phone, new PropertyDraft());
    }

    public AddPropertyStep getStep(String phone) {
        return stepByPhone.get(phone);
    }

    public PropertyDraft getDraft(String phone) {
        return draftByPhone.get(phone);
    }

    public void next(String phone, AddPropertyStep next) {
        stepByPhone.put(phone, next);
    }

    public void clear(String phone) {
        stepByPhone.remove(phone);
        draftByPhone.remove(phone);
    }
}

