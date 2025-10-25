package ru.welldev.engine.profile;

import lombok.Data;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
public class BehaviorProfile {
    private final Player player;
    private final List<Long> movementTimestamps = new CopyOnWriteArrayList<>();
    private final List<String> chatMessages = new CopyOnWriteArrayList<>();
    private final List<Long> interactionTimestamps = new CopyOnWriteArrayList<>();
    private final Map<String, Object> behaviorMetrics = new ConcurrentHashMap<>();

    private long profileStartTime;
    private double threatScore;

    public BehaviorProfile(Player player) {
        this.player = player;
        this.profileStartTime = System.currentTimeMillis();
        this.threatScore = 0.0;
    }

    public void recordMovement(PlayerMoveEvent event) {
        movementTimestamps.add(System.currentTimeMillis());
        // Ограничение размера
        if (movementTimestamps.size() > 1000) {
            movementTimestamps.subList(0, 200).clear();
        }
    }

    public void recordChat(String message) {
        chatMessages.add(message);
        if (chatMessages.size() > 100) {
            chatMessages.subList(0, 20).clear();
        }
    }

    public void recordInteraction(PlayerInteractEvent event) {
        interactionTimestamps.add(System.currentTimeMillis());
        if (interactionTimestamps.size() > 500) {
            interactionTimestamps.subList(0, 100).clear();
        }
    }
}