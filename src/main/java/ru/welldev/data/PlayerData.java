package ru.welldev.data;

import lombok.Data;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@Data
public class PlayerData {
    private final Player player;
    private final long joinTime;
    private List<Long> movementTimestamps;
    private List<String> chatMessages;
    private boolean isVerified;
    private double threatLevel;

    public PlayerData(Player player) {
        this.player = player;
        this.joinTime = System.currentTimeMillis();
        this.movementTimestamps = new ArrayList<>();
        this.chatMessages = new ArrayList<>();
        this.isVerified = false;
        this.threatLevel = 0.0;
    }

    public void addMovementTimestamp() {
        movementTimestamps.add(System.currentTimeMillis());
        if (movementTimestamps.size() > 100) {
            movementTimestamps = movementTimestamps.subList(50, 100);
        }
    }

    public void addChatMessage(String message) {
        chatMessages.add(message);
        if (chatMessages.size() > 50) {
            chatMessages = chatMessages.subList(25, 50);
        }
    }
}