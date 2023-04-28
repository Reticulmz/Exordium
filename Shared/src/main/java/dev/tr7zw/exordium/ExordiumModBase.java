package dev.tr7zw.exordium;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.tr7zw.config.CustomConfigScreen;
import dev.tr7zw.exordium.Config.ComponentSettings;
import dev.tr7zw.exordium.util.DelayedRenderCallManager;
import dev.tr7zw.exordium.util.NametagScreenBuffer;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.Screen;

public abstract class ExordiumModBase {

    public static ExordiumModBase instance;
    private static boolean forceBlend;

    public Config config;
    private final File settingsFile = new File("config", "exordium.json");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private NametagScreenBuffer nametagScreenBuffer;
    private RenderTarget temporaryScreenOverwrite = null;
    public static SignSettings signSettings = new SignSettings();
    public static NametagSettings nametagSettings = new NametagSettings();
    private final DelayedRenderCallManager delayedRenderCallManager = new DelayedRenderCallManager();

    public void onInitialize() {
		instance = this;
        if (settingsFile.exists()) {
            try {
                config = gson.fromJson(new String(Files.readAllBytes(settingsFile.toPath()), StandardCharsets.UTF_8),
                        Config.class);
            } catch (Exception ex) {
                System.out.println("Error while loading config! Creating a new one!");
                ex.printStackTrace();
            }
        }
        if (config == null) {
            config = new Config();
            writeConfig();
        } else {
            if(ConfigUpgrader.upgradeConfig(config)) {
                writeConfig(); // Config got modified
            }
        }
		initModloader();
	}
	
    public void writeConfig() {
        if (settingsFile.exists())
            settingsFile.delete();
        try {
            Files.write(settingsFile.toPath(), gson.toJson(config).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
    
    public NametagScreenBuffer getNameTagScreenBuffer() {
        if(nametagScreenBuffer == null) {
            nametagScreenBuffer = new NametagScreenBuffer(1000/config.targetFPSNameTags);
        }
        return nametagScreenBuffer;
    }
    
    public DelayedRenderCallManager getDelayedRenderCallManager() {
        return delayedRenderCallManager;
    }
    
    public abstract void initModloader();

    public Screen createConfigScreen(Screen parent) {
        CustomConfigScreen screen = new CustomConfigScreen(parent, "text.exordium.title") {

            @Override
            public void initialize() {
                List<OptionInstance<?>> options = new ArrayList<>();
                
                options.add(getOnOffOption("text.exordium.enableSignBuffering", () -> config.enableSignBuffering,
                        (b) -> config.enableSignBuffering = b));

                options.add(getOnOffOption("text.exordium.enableNametagScreenBuffering", () -> config.enableNametagScreenBuffering,
                        (b) -> config.enableNametagScreenBuffering = b));
                options.add(getIntOption("text.exordium.targetFPSNameTags", 30, 80, () -> config.targetFPSNameTags, (v) -> config.targetFPSNameTags = v));

                addSettings(options, config.chatSettings, "chat");
                addSettings(options, config.debugScreenSettings, "debug");
                addSettings(options, config.healthSettings, "health");
                addSettings(options, config.hotbarSettings, "hotbar");
                addSettings(options, config.experienceSettings, "experience");
                addSettings(options, config.scoreboardSettings, "scoreboard");
              
                getOptions().addSmall(options.toArray(new OptionInstance[0]));
                
            }

            private void addSettings(List<OptionInstance<?>> options, ComponentSettings settings, String name) {
                options.add(getOnOffOption("text.exordium.setting." + name + ".enabled", () -> settings.enabled,
                        (b) -> settings.enabled = b));
                options.add(getIntOption("text.exordium.setting." + name + ".fps", 5, 60, () -> settings.maxFps, (v) -> settings.maxFps = v));
                options.add(getOnOffOption("text.exordium.setting." + name + ".forceblend", () -> settings.forceBlend,
                        (b) -> settings.forceBlend = b));
            }
            
            @Override
            public void save() {
                writeConfig();
            }

            @Override
            public void reset() {
                config = new Config();
                writeConfig();
            }

        };

        return screen;
    }

    public static boolean isForceBlend() {
        return forceBlend;
    }

    public static void setForceBlend(boolean forceBlend) {
        ExordiumModBase.forceBlend = forceBlend;
    }

    public static void correctBlendMode() {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }

    public RenderTarget getTemporaryScreenOverwrite() {
        return temporaryScreenOverwrite;
    }

    public void setTemporaryScreenOverwrite(RenderTarget temporaryScreenOverwrite) {
        this.temporaryScreenOverwrite = temporaryScreenOverwrite;
    }
    
}
