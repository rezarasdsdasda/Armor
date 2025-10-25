package ru.welldev.engine;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import ru.welldev.AntiBots;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class ThreatDetectionEngine {

    private final AntiBots plugin;
    private final Map<UUID, List<Long>> playerMovements = new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> playerMessages = new ConcurrentHashMap<>();
    private final Set<UUID> suspiciousPlayers = Collections.synchronizedSet(new HashSet<>());

    public ThreatDetectionEngine(AntiBots plugin) {
        this.plugin = plugin;
    }


    public void analyzeMovement(Player player, PlayerMoveEvent event) {
        if (!plugin.getConfigManager().isEnableBehaviorAnalysis()) return;

        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        playerMovements.computeIfAbsent(playerId, k -> new ArrayList<>())
                .add(currentTime);

        // Очищаем старые данные
        cleanupOldData(playerId, currentTime);

        // Анализ паттернов движения
        if (detectBotMovementPattern(playerId)) {
            markAsSuspicious(player, "Подозрительные паттерны движения");
        }
    }

    public void analyzeChat(Player player, String message) {
        UUID playerId = player.getUniqueId();

        // Сохраняем сообщения для анализа
        playerMessages.computeIfAbsent(playerId, k -> new ArrayList<>())
                .add(message.toLowerCase());

        // Анализ паттернов чата
        if (detectBotChatPattern(playerId)) {
            markAsSuspicious(player, "Подозрительные паттерны чата");
        }
    }

    private boolean detectBotMovementPattern(UUID playerId) {
        List<Long> movements = playerMovements.get(playerId);
        if (movements == null || movements.size() < 10) return false;

        // Анализ частоты движений
        long timeSpan = movements.get(movements.size() - 1) - movements.get(0);
        double movementsPerSecond = (double) movements.size() / (timeSpan / 1000.0);

        // Боты часто имеют слишком равномерные или быстрые движения
        return movementsPerSecond > 20.0 || isTooUniform(movements);
    }

    private boolean detectBotChatPattern(UUID playerId) {
        List<String> messages = playerMessages.get(playerId);
        if (messages == null || messages.size() < 3) return false;

        // Проверка на повторяющиеся сообщения
        Set<String> uniqueMessages = new HashSet<>(messages);
        double uniquenessRatio = (double) uniqueMessages.size() / messages.size();

        return uniquenessRatio < 0.3; // Слишком много повторений
    }

    private boolean isTooUniform(List<Long> timestamps) {
        if (timestamps.size() < 3) return false;

        // Проверка на равномерность интервалов
        double variance = calculateVariance(timestamps);
        return variance < 10.0; // Слишком равномерно
    }

    private double calculateVariance(List<Long> timestamps) {
        double mean = 0;
        for (int i = 1; i < timestamps.size(); i++) {
            mean += timestamps.get(i) - timestamps.get(i - 1);
        }
        mean /= (timestamps.size() - 1);

        double variance = 0;
        for (int i = 1; i < timestamps.size(); i++) {
            double diff = (timestamps.get(i) - timestamps.get(i - 1)) - mean;
            variance += diff * diff;
        }

        return variance / (timestamps.size() - 1);
    }

    private void cleanupOldData(UUID playerId, long currentTime) {
        long cutoffTime = currentTime - 30000; // 30 секунд

        // Очистка данных движений
        List<Long> movements = playerMovements.get(playerId);
        if (movements != null) {
            movements.removeIf(time -> time < cutoffTime);
            if (movements.isEmpty()) {
                playerMovements.remove(playerId);
            }
        }

        // Очистка данных чата
        List<String> messages = playerMessages.get(playerId);
        if (messages != null && messages.size() > 50) {
            // Оставляем только последние 50 сообщений
            messages = messages.subList(Math.max(0, messages.size() - 50), messages.size());
            playerMessages.put(playerId, messages);
        }
    }

    private void markAsSuspicious(Player player, String reason) {
        suspiciousPlayers.add(player.getUniqueId());
        plugin.getAnalyticsManager().logSuspiciousActivity(player, reason);

        if (plugin.getProtectionManager().isPlayerVerified(player)) {
            // Для уже верифицированных игроков - дополнительная проверка
            player.sendMessage("Обнаружена подозрительная активность. Требуется повторная проверка.");
            plugin.getVerificationManager().startVerification(player);
        }
    }

    public double calculateThreatLevel(UUID playerId) {
        // Расчет уровня угрозы от 0.0 до 1.0
        double threatLevel = 0.0;

        if (suspiciousPlayers.contains(playerId)) {
            threatLevel += 0.5;
        }

        // Дополнительные факторы угрозы
        List<Long> movements = playerMovements.get(playerId);
        if (movements != null && movements.size() > 50) {
            threatLevel += 0.3;
        }

        return Math.min(threatLevel, 1.0);
    }
}