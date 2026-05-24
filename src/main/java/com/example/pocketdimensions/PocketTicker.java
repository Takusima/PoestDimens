package com.example.pocketdimensions;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = "poestdimens")
public class PocketTicker {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.level().isClientSide()) {

            if (player.level().dimension().equals(ModDimensions.POCKET_DIM_KEY)) {
                double x = player.getX();
                double z = player.getZ();

                // Находим центр текущей платформы на сетке (шаг 2000 блоков по оси X)
                long currentWorldId = Math.round(x / 2000.0);
                double relativeX = x - (currentWorldId * 2000);

                // Корректируем границы:
                // Радиус платформы в менеджере равен 32 блокам (стены стоят на координатах centerX +- 32 и centerZ +- 32).
                // Ставим проверку на > 34, чтобы триггер смерти срабатывал только за пределами невидимых стен-барьеров.
                if (Math.abs(relativeX) > 34 || Math.abs(z) > 34 || player.getY() < -64) {
                    player.kill();
                }
            }
        }
    }
}