package dev.uapi.soulascension.client;

import dev.uapi.soulascension.SoulAscensionMod;
import dev.uapi.soulascension.data.SoulAscensionAttachments;
import dev.uapi.soulascension.data.TitleProgress;
import dev.uapi.soulascension.network.ClientTitleCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;

/** Adds the selected title to the vanilla-extracted name tag before it enters the render pipeline. */
@EventBusSubscriber(modid = SoulAscensionMod.MOD_ID, value = Dist.CLIENT)
public final class SoulAscensionNameTagRenderer {
    private SoulAscensionNameTagRenderer() {}

    @SubscribeEvent
    public static void addTitle(RenderNameTagEvent.CanRender event) {
        if (!(event.getEntity() instanceof Player player) || player == Minecraft.getInstance().player) return;
        var titleId = player.getData(SoulAscensionAttachments.ACTIVE_TITLE).titleId();
        if (titleId.equals(TitleProgress.NONE)) return;
        Component title = ClientTitleCatalog.get(titleId)
            .<Component>map(value -> Component.translatable(value.nameKey())).orElse(Component.empty());
        if (title.getString().isEmpty()) return;
        event.setContent(Component.empty().append(event.getContent()).append(" · ").append(title));
    }
}
