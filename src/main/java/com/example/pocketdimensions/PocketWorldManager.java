package com.example.pocketdimensions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PocketWorldManager extends SavedData {
    private final Map<UUID, String> worldNames = new HashMap<>();
    private final Map<UUID, BlockPos> worldPositions = new HashMap<>();
    private int lastGridIndex = 0;

    // Хранилище для прошлых локаций игроков до телепортации
    private final Map<UUID, SavedLocation> lastLocations = new HashMap<>();

    public static class SavedLocation {
        public final ResourceKey<Level> dimension;
        public final double x, y, z;
        public final float yaw, pitch;

        public SavedLocation(ResourceKey<Level> dimension, double x, double y, double z, float yaw, float pitch) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    public static PocketWorldManager get(ServerLevel level) {
        SavedData.Factory<PocketWorldManager> factory = new SavedData.Factory<>(
                PocketWorldManager::new,
                PocketWorldManager::load,
                null
        );
        return level.getServer().overworld().getDataStorage().computeIfAbsent(factory, "pocket_worlds");
    }

    public PocketWorldManager() {
    }

    public boolean hasPocket(UUID playerUUID) {
        return worldPositions.containsKey(playerUUID);
    }

    public String getWorldName(UUID playerUUID) {
        return worldNames.getOrDefault(playerUUID, "Новый мир");
    }

    public void setWorldName(UUID playerUUID, String name) {
        worldNames.put(playerUUID, name);
        setDirty();
    }

    public BlockPos getPocketPosition(UUID playerUUID) {
        return worldPositions.get(playerUUID);
    }

    // Сохранить локацию перед входом в карманный мир
    public void savePlayerLocation(ServerPlayer player) {
        // Запоминаем позицию ТОЛЬКО если игрок сейчас НЕ в карманном мире
        if (!player.level().dimension().equals(ModDimensions.POCKET_DIM_KEY)) {
            lastLocations.put(player.getUUID(), new SavedLocation(
                    player.level().dimension(),
                    player.getX(), player.getY(), player.getZ(),
                    player.getYRot(), player.getXRot()
            ));
            setDirty();
        }
    }

    // Получить сохраненную локацию
    public SavedLocation getLastLocation(UUID playerUUID) {
        return lastLocations.get(playerUUID);
    }

    // Быстрая проверка: находится ли гость в зоне 4 чанков (радиус 32 от центра) конкретного хозяина
    public boolean isGuestAtPlayerPocket(ServerPlayer guest, UUID hostUUID) {
        if (!guest.level().dimension().equals(ModDimensions.POCKET_DIM_KEY)) {
            return false;
        }
        BlockPos hostCenter = worldPositions.get(hostUUID);
        if (hostCenter == null) return false;

        double dx = guest.getX() - hostCenter.getX();
        double dz = guest.getZ() - hostCenter.getZ();

        // Если гость внутри квадрата [-32; +31] вокруг центра хозяина
        return dx >= -32 && dx <= 31 && dz >= -32 && dz <= 31;
    }

    public Map<UUID, String> getAllWorldNames() {
        return this.worldNames;
    }

    public int getWorldsCount() {
        return this.worldPositions.size();
    }

    public boolean deletePocket(UUID playerUUID) {
        if (worldPositions.containsKey(playerUUID)) {
            worldPositions.remove(playerUUID);
            worldNames.remove(playerUUID);
            lastLocations.remove(playerUUID);
            setDirty();
            return true;
        }
        return false;
    }

    public BlockPos getOrCreatePocket(UUID playerUUID, ServerLevel pocketLevel) {
        BlockPos pocketSpawn;
        int centerX;
        int centerZ;
        int spawnY = 67;

        if (worldPositions.containsKey(playerUUID)) {
            pocketSpawn = worldPositions.get(playerUUID);
            centerX = pocketSpawn.getX();
            centerZ = pocketSpawn.getZ();
        } else {
            int spacing = 2000;
            centerX = lastGridIndex * spacing;
            centerZ = 0;
            pocketSpawn = new BlockPos(centerX, spawnY, centerZ);

            worldPositions.put(playerUUID, pocketSpawn);
            lastGridIndex++;
            setDirty();
        }

        int minOffset = -32;
        int maxOffset = 31;

        for (int y = 60; y < 250; y++) {
            for (int i = minOffset; i <= maxOffset; i++) {
                pocketLevel.setBlockAndUpdate(new BlockPos(centerX + i, y, centerZ + minOffset), Blocks.BARRIER.defaultBlockState());
                pocketLevel.setBlockAndUpdate(new BlockPos(centerX + i, y, centerZ + maxOffset), Blocks.BARRIER.defaultBlockState());
                pocketLevel.setBlockAndUpdate(new BlockPos(centerX + minOffset, y, centerZ + i), Blocks.BARRIER.defaultBlockState());
                pocketLevel.setBlockAndUpdate(new BlockPos(centerX + maxOffset, y, centerZ + i), Blocks.BARRIER.defaultBlockState());
            }
        }

        for (int xOffset = minOffset; xOffset <= maxOffset; xOffset++) {
            for (int zOffset = minOffset; zOffset <= maxOffset; zOffset++) {
                pocketLevel.setBlockAndUpdate(new BlockPos(centerX + xOffset, 250, centerZ + zOffset), Blocks.BARRIER.defaultBlockState());
            }
        }

        for (int xOffset = minOffset + 1; xOffset < maxOffset; xOffset++) {
            for (int zOffset = minOffset + 1; zOffset < maxOffset; zOffset++) {
                int currentX = centerX + xOffset;
                int currentZ = centerZ + zOffset;

                pocketLevel.setBlockAndUpdate(new BlockPos(currentX, 60, currentZ), Blocks.BEDROCK.defaultBlockState());
                pocketLevel.setBlockAndUpdate(new BlockPos(currentX, 61, currentZ), Blocks.STONE.defaultBlockState());
                pocketLevel.setBlockAndUpdate(new BlockPos(currentX, 62, currentZ), Blocks.STONE.defaultBlockState());
                pocketLevel.setBlockAndUpdate(new BlockPos(currentX, 63, currentZ), Blocks.STONE.defaultBlockState());
                pocketLevel.setBlockAndUpdate(new BlockPos(currentX, 64, currentZ), Blocks.DIRT.defaultBlockState());
                pocketLevel.setBlockAndUpdate(new BlockPos(currentX, 65, currentZ), Blocks.DIRT.defaultBlockState());
                pocketLevel.setBlockAndUpdate(new BlockPos(currentX, 66, currentZ), Blocks.GRASS_BLOCK.defaultBlockState());
            }
        }

        BlockPos centerSurface = new BlockPos(centerX, 66, centerZ);
        pocketLevel.setBlockAndUpdate(centerSurface, Blocks.OBSIDIAN.defaultBlockState());
        pocketLevel.setBlockAndUpdate(centerSurface.north(), Blocks.PODZOL.defaultBlockState());
        pocketLevel.setBlockAndUpdate(centerSurface.south(), Blocks.PODZOL.defaultBlockState());
        pocketLevel.setBlockAndUpdate(centerSurface.east(), Blocks.PODZOL.defaultBlockState());
        pocketLevel.setBlockAndUpdate(centerSurface.west(), Blocks.PODZOL.defaultBlockState());

        return pocketSpawn;
    }

    public static PocketWorldManager load(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        PocketWorldManager manager = new PocketWorldManager();
        CompoundTag namesTag = tag.getCompound("WorldNames");
        for (String key : namesTag.getAllKeys()) {
            manager.worldNames.put(UUID.fromString(key), namesTag.getString(key));
        }
        CompoundTag positionsTag = tag.getCompound("WorldPositions");
        for (String key : positionsTag.getAllKeys()) {
            CompoundTag posTag = positionsTag.getCompound(key);
            BlockPos pos = new BlockPos(posTag.getInt("x"), posTag.getInt("y"), posTag.getInt("z"));
            manager.worldPositions.put(UUID.fromString(key), pos);
        }
        manager.lastGridIndex = tag.getInt("LastGridIndex");

        // Загрузка прошлых локаций
        CompoundTag locsTag = tag.getCompound("LastLocations");
        for (String key : locsTag.getAllKeys()) {
            CompoundTag locTag = locsTag.getCompound(key);
            ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(locTag.getString("dim")));
            manager.lastLocations.put(UUID.fromString(key), new SavedLocation(
                    dimKey,
                    locTag.getDouble("x"), locTag.getDouble("y"), locTag.getDouble("z"),
                    locTag.getFloat("yaw"), locTag.getFloat("pitch")
            ));
        }

        return manager;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        CompoundTag namesTag = new CompoundTag();
        for (Map.Entry<UUID, String> entry : worldNames.entrySet()) {
            namesTag.putString(entry.getKey().toString(), entry.getValue());
        }
        tag.put("WorldNames", namesTag);

        CompoundTag positionsTag = new CompoundTag();
        for (Map.Entry<UUID, BlockPos> entry : worldPositions.entrySet()) {
            CompoundTag posTag = new CompoundTag();
            posTag.putInt("x", entry.getValue().getX());
            posTag.putInt("y", entry.getValue().getY());
            posTag.putInt("z", entry.getValue().getZ());
            positionsTag.put(entry.getKey().toString(), posTag);
        }
        tag.put("WorldPositions", positionsTag);
        tag.putInt("LastGridIndex", lastGridIndex);

        // Сохранение прошлых локаций
        CompoundTag locsTag = new CompoundTag();
        for (Map.Entry<UUID, SavedLocation> entry : lastLocations.entrySet()) {
            CompoundTag locTag = new CompoundTag();
            SavedLocation loc = entry.getValue();
            locTag.putString("dim", loc.dimension.location().toString());
            locTag.putDouble("x", loc.x);
            locTag.putDouble("y", loc.y);
            locTag.putDouble("z", loc.z);
            locTag.putFloat("yaw", loc.yaw);
            locTag.putFloat("pitch", loc.pitch);
            locsTag.put(entry.getKey().toString(), locTag);
        }
        tag.put("LastLocations", locsTag);

        return tag;
    }
}