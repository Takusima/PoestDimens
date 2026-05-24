package com.example.pocketdimensions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.*;

@EventBusSubscriber(modid = "poestdimens")
public class PocketCommands {
    public static final Map<UUID, UUID> pendingRequests = new HashMap<>();
    private static final Map<UUID, Integer> teleportTimers = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        String msg = event.getRawText().trim();

        if (msg.equalsIgnoreCase("Prunus")) {
            PocketWorldManager manager = PocketWorldManager.get(player.serverLevel());
            boolean hasPocket = manager.hasPocket(player.getUUID());

            if (!hasPocket) {
                int pearls = countItem(player, Items.ENDER_PEARL);
                int emeralds = countItem(player, Items.EMERALD);
                int levers = countItem(player, Items.LEVER);

                if (pearls < 1 || emeralds < 4 || levers < 1) {
                    player.sendSystemMessage(Component.literal("§cНедостаточно ресурсов! Нужно: 1 Эндер-жемчуг, 4 Изумруда, 1 Рычаг."));
                    player.sendSystemMessage(Component.literal("§7У вас: Жемчуг: " + pearls + "/1, Изумруды: " + emeralds + "/4, Рычаги: " + levers + "/1"));
                    event.setCanceled(true);
                    return;
                }

                removeItem(player, Items.ENDER_PEARL, 1);
                removeItem(player, Items.EMERALD, 4);
                removeItem(player, Items.LEVER, 1);
            } else {
                boolean missingExp = player.totalExperience < 2;
                boolean missingFood = player.getFoodData().getFoodLevel() < 4;

                if (missingExp && missingFood) {
                    player.sendSystemMessage(Component.literal("§cНедостаточно сил! Требуется 2 ед. опыта и уровень сытости от 4."));
                    event.setCanceled(true);
                    return;
                } else if (missingExp) {
                    player.sendSystemMessage(Component.literal("§cНедостаточно опыта! Требуется 2 единицы опыта."));
                    event.setCanceled(true);
                    return;
                } else if (missingFood) {
                    player.sendSystemMessage(Component.literal("§cВы слишком голодны для совершения телепортации!"));
                    event.setCanceled(true);
                    return;
                }

                player.giveExperiencePoints(-2);
                player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 4);
            }

            teleportTimers.put(player.getUUID(), 60);
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 1, false, false));
            player.sendSystemMessage(Component.literal("§aРитуал запущен. Телепортация..."));
            event.setCanceled(true);
            return;
        }

        if (msg.equalsIgnoreCase("Malus")) {
            if (player.serverLevel().dimension() == ModDimensions.POCKET_DIM_KEY) {
                kickOrReturnPlayer(player);
                player.sendSystemMessage(Component.literal("§eВы вернулись обратно."));
            } else {
                player.sendSystemMessage(Component.literal("§cВы не находитесь в карманном измерении!"));
            }
            event.setCanceled(true);
            return;
        }
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && teleportTimers.containsKey(p.getUUID())) {
            int time = teleportTimers.get(p.getUUID());
            p.serverLevel().sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY()+1, p.getZ(), 10, 0.5, 0.5, 0.5, 0.05);
            if (time <= 0) {
                teleportTimers.remove(p.getUUID());
                executeTeleport(p);
            } else teleportTimers.put(p.getUUID(), time - 1);
        }
    }

    public static void executeTeleport(ServerPlayer p) {
        ServerLevel pocket = p.getServer().getLevel(ModDimensions.POCKET_DIM_KEY);
        PocketWorldManager manager = PocketWorldManager.get(pocket);

        manager.savePlayerLocation(p);
        BlockPos pos = manager.getOrCreatePocket(p.getUUID(), pocket);

        p.teleportTo(pocket, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, Set.of(), p.getYRot(), p.getXRot());

        String worldName = manager.getWorldName(p.getUUID());
        String ownerName = p.getScoreboardName();

        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(worldName)));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§7Владелец мира: §b" + ownerName)));

        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 1, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, false, false));
    }

    public static int countItem(ServerPlayer player, net.minecraft.world.item.Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    public static void removeItem(ServerPlayer player, net.minecraft.world.item.Item item, int amount) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                if (stack.getCount() >= amount) {
                    stack.shrink(amount);
                    return;
                } else {
                    amount -= stack.getCount();
                    stack.setCount(0);
                }
            }
        }
    }

    public static void kickOrReturnPlayer(ServerPlayer player) {
        ServerLevel overworld = player.getServer().overworld();
        PocketWorldManager manager = PocketWorldManager.get(overworld);
        PocketWorldManager.SavedLocation loc = manager.getLastLocation(player.getUUID());

        if (loc != null) {
            ServerLevel targetDim = player.getServer().getLevel(loc.dimension);
            if (targetDim != null) {
                player.teleportTo(targetDim, loc.x, loc.y, loc.z, Set.of(), loc.yaw, loc.pitch);
                return;
            }
        }
        BlockPos spawnPos = overworld.getSharedSpawnPos();
        player.teleportTo(overworld, spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, Set.of(), 0, 0);
    }
}