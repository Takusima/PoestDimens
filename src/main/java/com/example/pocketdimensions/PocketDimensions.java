package com.example.pocketdimensions;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(PocketDimensions.MODID)
public class PocketDimensions {
    public static final String MODID = "poestdimens";

    public PocketDimensions(IEventBus modEventBus, ModContainer modContainer) {
        // Современная регистрация конфигурации в NeoForge 1.21.1 через ModContainer
        modContainer.registerConfig(ModConfig.Type.COMMON, com.example.pocketdimensions.ModConfig.SPEC);
    }
}