package ru.welldev.engine;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import ru.welldev.AntiBots;
import ru.welldev.engine.profile.BehaviorProfile;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class BehavioralAnalysisEngine implements Listener {

    private final AntiBots plugin;
    private final Map<UUID, BehaviorProfile> behaviorProfiles = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastMovements = new ConcurrentHashMap<>();

    public BehavioralAnalysisEngine(AntiBots plugin) {
        this.plugin = plugin;
    }


    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        BehaviorProfile profile = behaviorProfiles.computeIfAbsent(playerId,
                k -> new BehaviorProfile(player));

        profile.recordMovement(event);
        analyzeMovementPatterns(player, profile);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        BehaviorProfile profile = behaviorProfiles.get(player.getUniqueId());

        if (profile != null) {
            profile.recordChat(event.getMessage());
            analyzeChatPatterns(player, profile);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        BehaviorProfile profile = behaviorProfiles.get(player.getUniqueId());

        if (profile != null) {
            profile.recordInteraction(event);
            analyzeInteractionPatterns(player, profile);
        }
    }

    private void analyzeMovementPatterns(Player player, BehaviorProfile profile) {
        // Анализ паттернов движения на основе ML
        double regularityScore = calculateMovementRegularity(profile);
        double humanlikenessScore = calculateHumanlikeness(profile);

        if (regularityScore > 0.8 || humanlikenessScore < 0.3) {
            plugin.getThreatDetection().flagSuspiciousBehavior(player,
                    "Bot-like movement patterns");
        }
    }

    private void analyzeChatPatterns(Player player, BehaviorProfile profile) {
        // Анализ паттернов чата
        if (profile.getChatMessages().size() > 10) {
            double repetitionScore = calculateChatRepetition(profile);
            if (repetitionScore > 0.7) {
                plugin.getThreatDetection().flagSuspiciousBehavior(player,
                        "Repetitive chat patterns");
            }
        }
    }

    private void analyzeInteractionPatterns(Player player, BehaviorProfile profile) {
        // Анализ взаимодействий с миром
        double clickRegularity = calculateClickRegularity(profile);
        if (clickRegularity > 0.9) {
            plugin.getThreatDetection().flagSuspiciousBehavior(player,
                    "Robotic interaction patterns");
        }
    }

    private double calculateMovementRegularity(BehaviorProfile profile) {
        // Расчет регулярности движений (боты часто имеют идеально регулярные движения)
        List<Long> timestamps = profile.getMovementTimestamps();
        if (timestamps.size() < 10) return 0.0;

        // Анализ стандартного отклонения
        return 0.0; // Упрощенная реализация
    }

    private double calculateHumanlikeness(BehaviorProfile profile) {
        // Расчет "человекоподобия" поведения
        return 0.8; // Упрощенная реализация
    }

    private double calculateChatRepetition(BehaviorProfile profile) {
        // Расчет повторяемости сообщений
        List<String> messages = profile.getChatMessages();
        Set<String> unique = new HashSet<>(messages);

        return 1.0 - ((double) unique.size() / messages.size());
    }

    private double calculateClickRegularity(BehaviorProfile profile) {
        // Расчет регулярности кликов
        return 0.0; // Упрощенная реализация
    }
}