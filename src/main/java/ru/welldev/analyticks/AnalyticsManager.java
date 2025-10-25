package ru.welldev.analyticks;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import ru.welldev.AntiBot;
import ru.welldev.AntiBots;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class AnalyticsManager {

    private final AntiBot plugin;
    private final AtomicInteger totalVerifications = new AtomicInteger(0);
    private final AtomicInteger successfulVerifications = new AtomicInteger(0);
    private final AtomicInteger failedVerifications = new AtomicInteger(0);
    private final AtomicInteger blockedBots = new AtomicInteger(0);

    public AnalyticsManager(AntiBots antiBots) {
    }

    public void logVerificationSuccess(Player player) {
        successfulVerifications.incrementAndGet();
        totalVerifications.incrementAndGet();

        logToFile("SUCCESS", player.getName() + " successfully verified");
    }

    public void logVerificationFailed(Player player) {
        failedVerifications.incrementAndGet();
        totalVerifications.incrementAndGet();

        logToFile("FAILED", player.getName() + " failed verification");
    }

    public void logSuspiciousActivity(Player player, String reason) {
        logToFile("SUSPICIOUS", player.getName() + " - " + reason);
    }

    public void logBlockedBot(Player player, String reason) {
        blockedBots.incrementAndGet();
        logToFile("BLOCKED", player.getName() + " blocked - " + reason);
    }

    private void logToFile(String type, String message) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String logEntry = String.format("[%s] [%s] %s", timestamp, type, message);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(
                plugin.getDataFolder() + "/antibot.log", true))) {
            writer.write(logEntry);
            writer.newLine();
        } catch (IOException e) {
            plugin.getLogger().warning("Could not write to log file: " + e.getMessage());
        }
    }

    public void printStatistics() {
        plugin.getLogger().info("=== AntiBot Statistics ===");
        plugin.getLogger().info("Total verifications: " + totalVerifications.get());
        plugin.getLogger().info("Successful: " + successfulVerifications.get());
        plugin.getLogger().info("Failed: " + failedVerifications.get());
        plugin.getLogger().info("Blocked bots: " + blockedBots.get());

        if (totalVerifications.get() > 0) {
            double successRate = (double) successfulVerifications.get() / totalVerifications.get() * 100;
            plugin.getLogger().info("Success rate: " + String.format("%.2f", successRate) + "%");
        }
    }
}