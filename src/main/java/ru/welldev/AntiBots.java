package ru.welldev;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.welldev.analyticks.AnalyticsManager;
import ru.welldev.engine.BehavioralAnalysisEngine;
import ru.welldev.engine.MachineLearningEngine;
import ru.welldev.engine.NetworkAnalysisEngine;
import ru.welldev.engine.ThreatDetectionEngine;
import ru.welldev.protect.ProtectionManager;
import ru.welldev.storage.ConfigManager;
import ru.welldev.verify.VerificationManager;

@Getter
public class AntiBots extends JavaPlugin implements Listener {

    private ProtectionManager protectionManager;
    private VerificationManager verificationManager;
    private AnalyticsManager analyticsManager;
    private ConfigManager configManager;
    private ThreatDetectionEngine threatDetection;
    private NetworkAnalysisEngine networkAnalysis;
    private BehavioralAnalysisEngine behavioralAnalysis;
    private MachineLearningEngine mlEngine;

    @Override
    public void onEnable() {
        // Инициализация всех компонентов
        this.configManager = new ConfigManager(this);
        this.analyticsManager = new AnalyticsManager(this);
        this.mlEngine = new MachineLearningEngine(this);
        this.networkAnalysis = new NetworkAnalysisEngine(this);
        this.behavioralAnalysis = new BehavioralAnalysisEngine(this);
        this.threatDetection = new ThreatDetectionEngine(this);
        this.verificationManager = new VerificationManager(this);
        this.protectionManager = new ProtectionManager(this);

        // Загрузка ML моделей
        mlEngine.loadModels();

        // Регистрация событий
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(protectionManager, this);
        getServer().getPluginManager().registerEvents((Listener) verificationManager, this);
        getServer().getPluginManager().registerEvents(behavioralAnalysis, this);

        // Запуск мониторинга
        protectionManager.startMonitoring();
        networkAnalysis.startMonitoring();
    }

    @Override
    public void onDisable() {
        if (protectionManager != null) {
            protectionManager.shutdown();
        }
        if (mlEngine != null) {
            mlEngine.saveModels();
        }
    }

    public Object getConfigManager() {
        return null;
    }
}