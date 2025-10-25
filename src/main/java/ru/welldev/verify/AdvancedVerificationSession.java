package ru.welldev.verify;

import lombok.Data;
import java.util.*;

@Data
public class AdvancedVerificationSession {
    private final List<VerificationManager.VerificationType> types;
    private final Map<String, Object> data = new HashMap<>();
    private final long startTime;

    private int currentStep = 0;
    private int attempts = 0;
    private boolean completed = false;

    public AdvancedVerificationSession(VerificationManager.VerificationType type) {
        this.types = Collections.singletonList(type);
        this.startTime = System.currentTimeMillis();
    }

    public AdvancedVerificationSession(List<VerificationManager.VerificationType> types) {
        this.types = types;
        this.startTime = System.currentTimeMillis();
    }

    public boolean verify(String response) {
        attempts++;

        VerificationManager.VerificationType currentType = types.get(currentStep);
        String expected = (String) data.get("answer");

        if (expected != null && expected.equalsIgnoreCase(response)) {
            currentStep++;
            if (currentStep >= types.size()) {
                completed = true;
                return true;
            }
        }

        return completed;
    }

    public void setData(String key, Object value) {
        data.put(key, value);
    }
}