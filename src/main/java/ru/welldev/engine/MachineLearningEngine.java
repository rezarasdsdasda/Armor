package ru.welldev.engine;

import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.welldev.AntiBots;
import ru.welldev.data.IPData;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class MachineLearningEngine {

    private final AntiBots plugin;
    private final Map<String, Double> ipThreatScores = new ConcurrentHashMap<>();
    private final Map<String, Double> behaviorPatterns = new ConcurrentHashMap<>();
    private final Map<String, Integer> connectionPatterns = new ConcurrentHashMap<>();

    private File modelFile;
    private FileConfiguration modelConfig;

    public MachineLearningEngine(AntiBots antiBots) {

        this.plugin = plugin;
    }

    public void loadModels() {
        modelFile = new File(plugin.getDataFolder(), "ml_models.yml");
        if (!modelFile.exists()) {
            plugin.saveResource("ml_models.yml", false);
        }
        modelConfig = YamlConfiguration.loadConfiguration(modelFile);

        loadTrainedModels();
    }

    public double analyzeConnection(String ip, String username, long timestamp) {
        double threatScore = 0.0;

        threatScore += analyzeIPHistory(ip) * 0.3;

        threatScore += analyzeUsernamePattern(username) * 0.2;

        threatScore += analyzeTimingPattern(timestamp) * 0.2;

        threatScore += analyzeBehavioralPattern(ip) * 0.3;

        return Math.min(threatScore, 1.0);
    }

    private double analyzeIPHistory(String ip) {
        Double cachedScore = ipThreatScores.get(ip);
        if (cachedScore != null) return cachedScore;

        double score = 0.0;

        if (checkBlacklists(ip)) score += 0.6;

        if (isSuspiciousLocation(ip)) score += 0.3;

        score += calculateIPReputation(ip);

        ipThreatScores.put(ip, score);
        return score;
    }

    private double analyzeUsernamePattern(String username) {
        String lower = username.toLowerCase();

        if (username.matches(".*[a-z]{6}.*") && username.matches(".*[0-9]{4}.*")) {
            return 0.8;
        }

        if (hasRepeatingChars(username, 4)) return 0.6;

        String[] botPatterns = {"bot", "mine", "auto", "farm", "hack"};
        for (String pattern : botPatterns) {
            if (lower.contains(pattern)) return 0.7;
        }

        return 0.1;
    }

    private double analyzeTimingPattern(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);

        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int second = cal.get(Calendar.SECOND);

        if (minute == 0 && second < 10) return 0.4;
        if (minute == 30 && second < 10) return 0.4;

        return 0.1;
    }

    private double analyzeBehavioralPattern(String ip) {
        Integer connections = connectionPatterns.get(ip);
        if (connections == null) return 0.1;

        if (connections > 50) return 0.8;
        if (connections > 20) return 0.5;
        if (connections > 10) return 0.3;

        return 0.1;
    }

    private boolean checkBlacklists(String ip) {
        List<String> blacklists = Arrays.asList(
                "192.168.1.0/24",
                "10.0.0.0/8"
        );

        return blacklists.stream().anyMatch(range -> isIPInRange(ip, range));
    }

    private boolean isSuspiciousLocation(String ip) {
        List<String> highRiskCountries = Arrays.asList("CN", "RU", "BR", "IN");
        String country = plugin.getNetworkAnalysis().getCountryCode(ip);

        return highRiskCountries.contains(country);
    }

    private double calculateIPReputation(String ip) {
        IPData data = plugin.getProtectionManager().getIPData(ip);
        if (data == null) return 0.1;

        double reputation = 0.0;
        reputation += data.getFailedLogins() * 0.1;
        reputation += data.getSuccessfulVerifications() * -0.05;
        reputation += data.getThreatScore() * 0.01;

        return Math.min(reputation, 0.5);
    }

    private boolean hasRepeatingChars(String str, int maxRepeat) {
        char[] chars = str.toCharArray();
        int count = 1;

        for (int i = 1; i < chars.length; i++) {
            if (chars[i] == chars[i-1]) {
                count++;
                if (count >= maxRepeat) return true;
            } else {
                count = 1;
            }
        }

        return false;
    }

    private boolean isIPInRange(String ip, String range) {
        return ip.startsWith(range.split("/")[0]);
    }

    public void saveModels() {
        try {
            modelConfig.save(modelFile);
        } catch (IOException e) {
        }
    }

    private void loadTrainedModels() {
    }
}