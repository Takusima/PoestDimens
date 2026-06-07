package com.example.pocketdimensions;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = "poestdimens")
public class CommandRegistry {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("Remna")
                .then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(context -> {
                            String name = StringArgumentType.getString(context, "name");
                            PocketWorldManager.get(context.getSource().getLevel())
                                    .setWorldName(context.getSource().getPlayerOrException().getUUID(), name);
                            context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§6Мир переименован!"), false);
                            return 1;
                        })));

        event.getDispatcher().register(Commands.literal("Prequest")
                .then(Commands.argument("target", StringArgumentType.word())
                        .executes(context -> {
                            // Логику запроса перенесем сюда из PocketCommands
                            return 1; // Здесь будет вызов метода из PocketCommands
                        })));

        // Аналогично для Paccept...
    }
}