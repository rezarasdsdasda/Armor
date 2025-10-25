package ru.welldev.protect;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.scheduler.BukkitRunnable;
import ru.welldev.AntiBots;
import ru.welldev.data.IPData;
import ru.welldev.data.PlayerSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@RequiredArgsConstructor
public class ProtectionManager implements Listener {

    private final AntiBots plugin;
    private final Map<UUID, PlayerSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, IPData> ipDatabase = new ConcurrentHashMap<>();
    private final Set<UUID> whitelisted = Collections.synchronizedSet(new HashSet<>());
    private final Set<String> blockedIPs = Collections.synchronizedSet(new HashSet<>());
    private final Map<UUID, Long> connectionTimes = new ConcurrentHashMap<>();

    // Advanced protection
    private boolean underAttack = false;
    private long lastAttackTime = 0;
    private int connectionRate = 0;

    public ProtectionManager(AntiBots plugin) {
        this.plugin = plugin;
    }


    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        String ip = event.getAddress().getHostAddress();
        String name = event.getName();

        // 1. Проверка IP репутации
        if (checkIPReputation(ip)) {
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_BANNED);
            event.setKickMessage("Ваш IP адрес заблокирован системой защиты");
            return;
        }

        // 2. Геолокация и прокси проверка
        if (detectVPNProxy(ip)) {
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_BANNED);
            event.setKickMessage("Обнаружено использование VPN/Прокси");
            return;
        }

        // 3. Проверка скорости подключений
        if (checkConnectionRate(ip)) {
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_BANNED);
            event.setKickMessage("Слишком частые подключения");
            return;
        }

        // 4. Анализ имени игрока
        if (analyzeUsername(name)) {
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_BANNED);
            event.setKickMessage("Подозрительное имя игрока");
            return;
        }

        // 5. ML анализ подключения
        double threatLevel = plugin.getMlEngine().analyzeConnection(ip, name, System.currentTimeMillis());
        if (threatLevel > 0.8) {
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_BANNED);
            event.setKickMessage("Обнаружена подозрительная активность");
            return;
        }
    }

    @EventHandler
    public void onServerPing(ServerListPingEvent event) {
        // Защита от пинг-флуда
        String address = event.getAddress().getHostAddress();
        IPData ipData = ipDatabase.computeIfAbsent(address, k -> new IPData());

        ipData.incrementPings();
        if (ipData.getPingCount() > 100) { // Лимит пингов
            event.setMotd("Server under protection");
            event.setMaxPlayers(0);
        }
    }

    private boolean checkIPReputation(String ip) {
        IPData data = ipDatabase.get(ip);
        if (data == null) return false;

        return data.getThreatScore() > 80 ||
                data.getBlockedConnections() > 10 ||
                blockedIPs.contains(ip);
    }

    private boolean detectVPNProxy(String ip) {
        // Проверка по известным базам VPN
        if (plugin.getNetworkAnalysis().isVPNIP(ip)) return true;

        // Проверка хоста
        String host = plugin.getNetworkAnalysis().reverseDNSLookup(ip);
        if (host != null) {
            return host.contains("vpn") ||
                    host.contains("proxy") ||
                    host.contains("hosting");
        }

        return false;
    }

    private boolean checkConnectionRate(String ip) {
        IPData data = ipDatabase.computeIfAbsent(ip, k -> new IPData());
        long currentTime = System.currentTimeMillis();

        // Окно 10 секунд
        data.getConnectionTimes().removeIf(time -> currentTime - time > 10000);
        data.getConnectionTimes().add(currentTime);

        return data.getConnectionTimes().size() > 5; // Максимум 5 подключений за 10 секунд
    }

    private boolean analyzeUsername(String username) {
        // Проверка паттернов ботов
        String lower = username.toLowerCase();

        // Слишком длинное/короткое имя
        if (username.length() < 2 || username.length() > 16) return true;

        // Случайные символы
        if (username.matches(".*[a-z]{4}.*") && username.matches(".*[0-9]{4}.*")) return true;

        // Запрещенные слова
        String[] bannedPatterns = {"bot", "hack", "cheat", "minebot", "null"};
        for (String pattern : bannedPatterns) {
            if (lower.contains(pattern)) return true;
        }

        return false;
    }

    public void startMonitoring() {
        new BukkitRunnable() {
            @Override
            public void run() {
                monitorConnectionPatterns();
                cleanupOldData();
                updateAttackStatus();
            }
        }.runTaskTimer(plugin, 100L, 100L); // Каждые 5 секунд
    }

    private void monitorConnectionPatterns() {
        long currentTime = System.currentTimeMillis();
        int connectionsLastMinute = 0;

        for (IPData data : ipDatabase.values()) {
            data.getConnectionTimes().removeIf(time -> currentTime - time > 60000);
            connectionsLastMinute += data.getConnectionTimes().size();
        }

        connectionRate = connectionsLastMinute;

        // Обнаружение DDoS/ботнета
        if (connectionsLastMinute > 100) {
            underAttack = true;
            lastAttackTime = currentTime;
            activateEmergencyMode();
        }
    }

    private void activateEmergencyMode() {
        // Активация режима защиты от DDoS
        plugin.getLogger().warning("DDoS attack detected! Activating emergency protection...");

        // Временное ограничение подключений
        plugin.getConfigManager().setMaxConnectionsPerIP(1);

        // Дополнительные проверки
        plugin.getVerificationManager().setEnhancedVerification(true);
    }

    private void cleanupOldData() {
        long cutoffTime = System.currentTimeMillis() - 3600000; // 1 час

        ipDatabase.entrySet().removeIf(entry ->
                entry.getValue().getLastSeen() < cutoffTime &&
                        entry.getValue().getThreatScore() < 10
        );
    }

    private void updateAttackStatus() {
        if (underAttack && System.currentTimeMillis() - lastAttackTime > 300000) { // 5 минут
            underAttack = false;
            plugin.getConfigManager().setMaxConnectionsPerIP(3);
            plugin.getVerificationManager().setEnhancedVerification(false);
        }
    }

    public void shutdown() {
        sessions.clear();
        ipDatabase.clear();
        whitelisted.clear();
        blockedIPs.clear();
        connectionTimes.clear();
    }
}