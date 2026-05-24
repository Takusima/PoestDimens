package com.example.pocketdimensions;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public class ModDimensions {
    public static final String MOD_ID = "poestdimens";


    public static final ResourceKey<Level> POCKET_DIM_KEY = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "pocket")
    );

    public static final ResourceKey<DimensionType> POCKET_DIM_TYPE_KEY = ResourceKey.create(
            Registries.DIMENSION_TYPE,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "pocket_type")
    );
}
