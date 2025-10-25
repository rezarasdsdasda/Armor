package ru.welldev.verify;

import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.scheduler.BukkitRunnable;
import ru.welldev.AntiBots;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@RequiredArgsConstructor
public class VerificationManager {

    private final AntiBots plugin;
    private final Map<UUID, AdvancedVerificationSession> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, Long> verificationStartTimes = new ConcurrentHashMap<>();
    private final Set<UUID> completedVerifications = Collections.synchronizedSet(new HashSet<>());

    private boolean enhancedVerification = false;

    public VerificationManager(AntiBots plugin) {
        this.plugin = plugin;
    }


    public enum VerificationType {
        CAPTCHA, MATH_PROBLEM, ITEM_CLICK, PATTERN_MATCH,
        HUMAN_BEHAVIOR, GEO_VERIFICATION, BIOMETRIC_ANALYSIS
    }

    public void startVerification(Player player) {
        UUID playerId = player.getUniqueId();

        if (enhancedVerification) {
            startEnhancedVerification(player);
        } else {
            startStandardVerification(player);
        }
    }

    private void startStandardVerification(Player player) {
        VerificationType type = getRandomVerificationType();
        AdvancedVerificationSession session = new AdvancedVerificationSession(type);

        sessions.put(player.getUniqueId(), session);
        verificationStartTimes.put(player.getUniqueId(), System.currentTimeMillis());

        switch (type) {
            case CAPTCHA:
                sendCaptchaVerification(player, session);
                break;
            case MATH_PROBLEM:
                sendMathVerification(player, session);
                break;
            case ITEM_CLICK:
                sendItemVerification(player, session);
                break;
            case PATTERN_MATCH:
                sendPatternVerification(player, session);
                break;
            default:
                sendHumanBehaviorTest(player, session);
        }

        startVerificationTimer(player);
    }

    private void sendPatternVerification(Player player, AdvancedVerificationSession session) {

    }

    private void startEnhancedVerification(Player player) {
        // Многоэтапная верификация
        List<VerificationType> types = Arrays.asList(
                VerificationType.CAPTCHA,
                VerificationType.MATH_PROBLEM,
                VerificationType.PATTERN_MATCH
        );

        AdvancedVerificationSession session = new AdvancedVerificationSession(types);
        sessions.put(player.getUniqueId(), session);

        sendMultiStepVerification(player, session);
    }

    private void sendCaptchaVerification(Player player, AdvancedVerificationSession session) {
        String captcha = generateAdvancedCaptcha();
        session.setData("captcha", captcha);

        player.sendMessage(ChatColor.GOLD + "╔══════════════════════════════╗");
        player.sendMessage(ChatColor.GOLD + "║      " + ChatColor.RED + "АНТИ-БОТ ВЕРИФИКАЦИЯ" + ChatColor.GOLD + "     ║");
        player.sendMessage(ChatColor.GOLD + "╠══════════════════════════════╣");
        player.sendMessage(ChatColor.YELLOW + "║ Введите код: " +
                ChatColor.GREEN + ChatColor.BOLD + captcha + ChatColor.YELLOW + "          ║");
        player.sendMessage(ChatColor.GOLD + "╚══════════════════════════════╝");
    }

    private void sendMathVerification(Player player, AdvancedVerificationSession session) {
        int a = ThreadLocalRandom.current().nextInt(1, 20);
        int b = ThreadLocalRandom.current().nextInt(1, 20);
        String operator = ThreadLocalRandom.current().nextBoolean() ? "+" : "-";

        int answer = operator.equals("+") ? a + b : a - b;
        session.setData("answer", String.valueOf(answer));

        player.sendMessage(ChatColor.GOLD + "╔══════════════════════════════╗");
        player.sendMessage(ChatColor.GOLD + "║        " + ChatColor.RED + "МАТЕМАТИЧЕСКАЯ ПРОВЕРКА" + ChatColor.GOLD + "   ║");
        player.sendMessage(ChatColor.GOLD + "╠══════════════════════════════╣");
        player.sendMessage(ChatColor.YELLOW + "║ Решите: " + a + " " + operator + " " + b + " = ?           ║");
        player.sendMessage(ChatColor.GOLD + "╚══════════════════════════════╝");
    }

    private void sendItemVerification(Player player, AdvancedVerificationSession session) {
        Material[] materials = {Material.DIAMOND, Material.GOLD_INGOT, Material.IRON_INGOT, Material.COAL};
        Material target = materials[ThreadLocalRandom.current().nextInt(materials.length)];

        session.setData("item", target.name());

        // Создание книги с инструкциями
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();

        meta.setTitle("Верификация");
        meta.setAuthor("Система защиты");
        meta.setPages(
                "Кликните на предмет: " + target.toString() + "\n\n" +
                        "Это проверка на человеческое поведение."
        );

        book.setItemMeta(meta);
        player.openBook(book);

        player.sendMessage(ChatColor.YELLOW + "Пожалуйста, кликните на предмет: " +
                ChatColor.GREEN + target.toString());
    }

    private String generateAdvancedCaptcha() {
        // Генерация сложной капчи с помехами
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder captcha = new StringBuilder();

        for (int i = 0; i < 8; i++) {
            captcha.append(chars.charAt(ThreadLocalRandom.current().nextInt(chars.length())));
        }

        return captcha.toString();
    }

    private VerificationType getRandomVerificationType() {
        VerificationType[] types = VerificationType.values();
        return types[ThreadLocalRandom.current().nextInt(types.length - 2)]; // Исключаем сложные типы
    }

    private void startVerificationTimer(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                UUID playerId = player.getUniqueId();
                if (sessions.containsKey(playerId) && !completedVerifications.contains(playerId)) {
                    player.kickPlayer(ChatColor.RED + "Время верификации истекло!");
                    sessions.remove(playerId);
                    verificationStartTimes.remove(playerId);
                    plugin.getAnalyticsManager().logVerificationTimeout(player);
                }
            }
        }.runTaskLater(plugin, 6000L); // 5 минут
    }

    public boolean verifyResponse(Player player, String response) {
        UUID playerId = player.getUniqueId();
        AdvancedVerificationSession session = sessions.get(playerId);

        if (session == null) return false;

        boolean verified = session.verify(response);

        if (verified) {
            completeVerification(player);
            return true;
        } else {
            session.incrementAttempts();
            if (session.getAttempts() >= 3) {
                player.kickPlayer(ChatColor.RED + "Слишком много неудачных попыток!");
                sessions.remove(playerId);
                plugin.getAnalyticsManager().logVerificationFailed(player);
            }
            return false;
        }
    }

    private void completeVerification(Player player) {
        UUID playerId = player.getUniqueId();
        sessions.remove(playerId);
        verificationStartTimes.remove(playerId);
        completedVerifications.add(playerId);

        plugin.getProtectionManager().whitelistPlayer(playerId);
        plugin.getAnalyticsManager().logVerificationSuccess(player);

        player.sendMessage(ChatColor.GREEN + "✅ Верификация успешна! Добро пожаловать на сервер!");
    }

    public void setEnhancedVerification(boolean enhanced) {
        this.enhancedVerification = enhanced;
    }
}