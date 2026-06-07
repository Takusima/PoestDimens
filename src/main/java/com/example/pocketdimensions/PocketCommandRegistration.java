package com.example.pocketdimensions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ColorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = "poestdimens")
public class PocketCommandRegistration {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // 1. Команда /prequest <игрок>
        dispatcher.register(Commands.literal("prequest")
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(context, "target");
                            PocketWorldManager manager = PocketWorldManager.get(player.serverLevel());

                            if (player.getUUID().equals(target.getUUID())) {
                                player.sendSystemMessage(Component.literal("§cВы не можете отправить запрос на телепортацию самому себе!"));
                                return 0;
                            }

                            if (manager.hasPocket(target.getUUID())) {
                                PocketCommands.pendingRequests.put(player.getUUID(), target.getUUID());
                                String worldName = manager.getWorldName(target.getUUID());
                                target.sendSystemMessage(Component.literal("§eИгрок " + player.getScoreboardName() + " просит войти в ваш мир [" + worldName + "]."));

                                // Звук колокольчика владельцу мира
                                target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                                        SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1.0F, 1.0F);

                                Component acceptBtn = Component.literal("§a[ПРИНЯТЬ] ")
                                        .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/paccept " + player.getScoreboardName())));

                                Component denyBtn = Component.literal("§c[ОТКЛОНИТЬ]")
                                        .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pdeny " + player.getScoreboardName())));

                                target.sendSystemMessage(Component.empty().append(acceptBtn).append(denyBtn));
                                player.sendSystemMessage(Component.literal("§6Запрос отправлен игроку " + target.getScoreboardName()));
                                return 1;
                            } else {
                                player.sendSystemMessage(Component.literal("§cУ этого игрока ещё нет карманного измерения!"));
                                return 0;
                            }
                        })));

        // 2. Команда /paccept <игрок>
        dispatcher.register(Commands.literal("paccept")
                .then(Commands.argument("guest", EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            ServerPlayer guest = EntityArgument.getPlayer(context, "guest");

                            if (PocketCommands.pendingRequests.get(guest.getUUID()) == player.getUUID()) {

                                if (guest.totalExperience < 2) {
                                    guest.sendSystemMessage(Component.literal("§cНедостаточно опыта для входа в чужой мир! Нужно 2 единицы опыта."));
                                    player.sendSystemMessage(Component.literal("§cТелепортация отменена: у гостя (" + guest.getScoreboardName() + ") не хватает опыта."));
                                    return 0;
                                }

                                PocketCommands.pendingRequests.remove(guest.getUUID());
                                guest.giveExperiencePoints(-2);

                                PocketWorldManager manager = PocketWorldManager.get(player.serverLevel());
                                manager.savePlayerLocation(guest);

                                net.minecraft.core.BlockPos hostPos = manager.getPocketPosition(player.getUUID());
                                guest.teleportTo(player.serverLevel(), hostPos.getX() + 0.5, hostPos.getY() + 1.0, hostPos.getZ() + 0.5, java.util.Set.of(), guest.getYRot(), guest.getXRot());

                                guest.sendSystemMessage(Component.literal("§aВы вошли в мир игрока " + player.getScoreboardName()));
                                player.sendSystemMessage(Component.literal("§6Игрок " + guest.getScoreboardName() + " зашёл в ваш мир."));

                                String worldName = manager.getWorldName(player.getUUID());
                                guest.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
                                guest.connection.send(new ClientboundSetTitleTextPacket(Component.literal(worldName)));
                                guest.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§7Владелец мира: §b" + player.getScoreboardName())));

                                guest.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 1, false, false));
                                guest.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, false, false));
                                return 1;
                            }
                            player.sendSystemMessage(Component.literal("§cНет активного запроса от этого игрока!"));
                            return 0;
                        })));

        // 3. Команда /pdeny <игрок>
        dispatcher.register(Commands.literal("pdeny")
                .then(Commands.argument("guest", EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            ServerPlayer guest = EntityArgument.getPlayer(context, "guest");

                            if (PocketCommands.pendingRequests.get(guest.getUUID()) == player.getUUID()) {
                                PocketCommands.pendingRequests.remove(guest.getUUID());

                                guest.sendSystemMessage(Component.literal("§cИгрок " + player.getScoreboardName() + " отклонил ваш запрос на вход."));
                                player.sendSystemMessage(Component.literal("§eВы отклонили запрос игрока " + guest.getScoreboardName()));
                                return 1;
                            }
                            player.sendSystemMessage(Component.literal("§cНет активного запроса от этого игрока!"));
                            return 0;
                        })));

        // 4. Команда /pkick <игрок>
        dispatcher.register(Commands.literal("pkick")
                .then(Commands.argument("guest", EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer host = context.getSource().getPlayerOrException();
                            ServerPlayer guest = EntityArgument.getPlayer(context, "guest");
                            PocketWorldManager manager = PocketWorldManager.get(host.serverLevel());

                            if (host.getUUID().equals(guest.getUUID())) {
                                host.sendSystemMessage(Component.literal("§cВы не можете выгнать самого себя!"));
                                return 0;
                            }

                            if (manager.isGuestAtPlayerPocket(guest, host.getUUID())) {
                                PocketCommands.kickOrReturnPlayer(guest);
                                guest.sendSystemMessage(Component.literal("§cВладелец измерения выгнал вас!"));
                                host.sendSystemMessage(Component.literal("§6Игрок " + guest.getScoreboardName() + " успешно удалён из вашего мира."));
                                return 1;
                            } else {
                                host.sendSystemMessage(Component.literal("§cЭтот игрок не находится в вашем карманном измерении!"));
                                return 0;
                            }
                        })));

        // 5. Команда /remna <цвет> <название>
        dispatcher.register(Commands.literal("remna")
                .then(Commands.argument("color", ColorArgument.color())
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    ChatFormatting color = ColorArgument.getColor(context, "color");
                                    String name = StringArgumentType.getString(context, "name");

                                    String coloredName = color.toString() + name;

                                    PocketWorldManager.get(player.serverLevel()).setWorldName(player.getUUID(), coloredName);
                                    player.sendSystemMessage(Component.literal("§6Мир успешно переименован в: " + coloredName));
                                    return 1;
                                }))));
    }
}