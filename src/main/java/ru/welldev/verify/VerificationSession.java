package ru.welldev.verify;

import lombok.Data;

@Data
public class VerificationSession {
    private final String captchaText;
    private final long creationTime;
    private int attempts;

    public VerificationSession(String captchaText) {
        this.captchaText = captchaText;
        this.creationTime = System.currentTimeMillis();
        this.attempts = 0;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - creationTime > 300000; // 5 минут
    }

    public void incrementAttempts() {
        attempts++;
    }
}