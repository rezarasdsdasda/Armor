package ru.welldev.data;

import lombok.Data;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
public class IPData {
    private final List<Long> connectionTimes = new CopyOnWriteArrayList<>();
    private final List<Long> pingTimes = new CopyOnWriteArrayList<>();

    private int failedLogins = 0;
    private int successfulVerifications = 0;
    private int blockedConnections = 0;
    private int pingCount = 0;
    private double threatScore = 0.0;
    private long firstSeen;
    private long lastSeen;

    public IPData() {
        this.firstSeen = System.currentTimeMillis();
        this.lastSeen = System.currentTimeMillis();
    }

    public void incrementPings() {
        pingCount++;
        lastSeen = System.currentTimeMillis();
    }

    public void addConnectionTime(long time) {
        connectionTimes.add(time);
        lastSeen = System.currentTimeMillis();
    }
}