package com.example.pocketdimensions;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ModConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue PRUNUS_XP_COST;
    public static final ModConfigSpec.IntValue PRUNUS_HUNGER_COST;
    public static final ModConfigSpec.ConfigValue<String> PARTICLE_COLOR;

    static {
        BUILDER.comment("Настройки карманного измерения").push("general");

        PRUNUS_XP_COST = BUILDER
                .comment("Сколько уровней опыта забирает Prunus (По умолчанию: 2)")
                .defineInRange("prunusXpCost", 2, 0, 100);

        PRUNUS_HUNGER_COST = BUILDER
                .comment("Сколько единиц голода забирает Prunus (По умолчанию: 4)")
                .defineInRange("prunusHungerCost", 4, 0, 20);

        PARTICLE_COLOR = BUILDER
                .comment("Цвет облака частиц при телепортации в формате HEX (По умолчанию: #FF0000 - Красный)")
                .define("particleColor", "#FF0000");

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
