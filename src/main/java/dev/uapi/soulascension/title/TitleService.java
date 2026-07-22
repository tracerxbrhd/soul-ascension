package dev.uapi.soulascension.title;

import dev.uapi.soulascension.data.SoulAscensionAttachments;
import dev.uapi.soulascension.data.ActiveTitleData;
import dev.uapi.soulascension.data.PlayerProgress;
import dev.uapi.soulascension.data.TitleProgress;
import dev.uapi.soulascension.data.TitleCounters;
import dev.uapi.soulascension.progression.SoulAscensionService;
import dev.uapi.soulascension.network.ClientTitleDefinition;
import dev.uapi.soulascension.network.TitleDefinitionsPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class TitleService {
    private TitleService() {}

    public static TitleProgress get(ServerPlayer player) { return player.getData(SoulAscensionAttachments.TITLES); }

    public static int evaluate(ServerPlayer player) {
        PlayerProgress progress = SoulAscensionService.get(player);
        if (!progress.isUsable()) return 0;
        TitleProgress original = get(player);
        TitleCounters counters = player.getData(SoulAscensionAttachments.TITLE_COUNTERS);
        TitleProgress updated = original.retain(TitleRegistry.ids());
        List<TitleDefinition> unlockedNow = new ArrayList<>();
        boolean changed;
        do {
            changed = false;
            for (TitleDefinition definition : TitleRegistry.all()) {
                if (updated.unlocked().contains(definition.id()) || !definition.matches(player, progress, updated, counters)) continue;
                updated = updated.unlock(definition.id()); unlockedNow.add(definition); changed = true;
            }
        } while (changed);

        if (!updated.activeTitle().equals(TitleProgress.NONE)) {
            TitleDefinition active = TitleRegistry.get(updated.activeTitle()).orElse(null);
            if (active == null || !active.available() || !updated.unlocked().contains(active.id()))
                updated = updated.activeTitle(TitleProgress.NONE);
        }
        if (!updated.equals(original)) player.setData(SoulAscensionAttachments.TITLES, updated);
        Identifier displayed = dev.uapi.soulascension.config.SoulAscensionConfigManager.current().showTitlesInNameplate()
            ? updated.activeTitle() : TitleProgress.NONE;
        if (!player.getData(SoulAscensionAttachments.ACTIVE_TITLE).titleId().equals(displayed))
            player.setData(SoulAscensionAttachments.ACTIVE_TITLE, ActiveTitleData.of(displayed));
        if (!original.activeTitle().equals(updated.activeTitle())) player.refreshTabListName();
        unlockedNow.forEach(definition -> player.sendSystemMessage(Component.translatable(
            "message.soul_ascension.title_unlocked", Component.translatable(definition.nameKey()))));
        return unlockedNow.size();
    }

    public static boolean select(ServerPlayer player, Identifier id) {
        TitleProgress old = get(player);
        if (!id.equals(TitleProgress.NONE)) {
            TitleDefinition definition = TitleRegistry.get(id).orElse(null);
            if (definition == null || !definition.available() || !old.unlocked().contains(id)) return false;
        }
        if (old.activeTitle().equals(id)) return true;
        player.setData(SoulAscensionAttachments.TITLES, old.activeTitle(id));
        player.setData(SoulAscensionAttachments.ACTIVE_TITLE, ActiveTitleData.of(
            dev.uapi.soulascension.config.SoulAscensionConfigManager.current().showTitlesInNameplate()
                ? id : TitleProgress.NONE));
        player.refreshTabListName();
        return true;
    }

    public static boolean forceUnlock(ServerPlayer player, Identifier id) {
        TitleDefinition definition = TitleRegistry.get(id).orElse(null);
        if (definition == null || !definition.available()) return false;
        TitleProgress old = get(player);
        if (!old.unlocked().contains(id)) {
            player.setData(SoulAscensionAttachments.TITLES, old.unlock(id));
            player.sendSystemMessage(Component.translatable("message.soul_ascension.title_unlocked",
                Component.translatable(definition.nameKey())));
        }
        return true;
    }

    public static void resetTitles(ServerPlayer player) {
        player.setData(SoulAscensionAttachments.TITLES, TitleProgress.initial());
        player.setData(SoulAscensionAttachments.TITLE_COUNTERS, TitleCounters.initial());
        player.setData(SoulAscensionAttachments.ACTIVE_TITLE, ActiveTitleData.none());
        player.refreshTabListName();
        evaluate(player);
    }

    public static void addEntityKill(ServerPlayer player, Identifier id) {
        if (!TitleRegistry.tracksEntity(id)) return;
        TitleCounters value = player.getData(SoulAscensionAttachments.TITLE_COUNTERS).addEntityKill(id);
        player.setData(SoulAscensionAttachments.TITLE_COUNTERS, value); evaluate(player);
    }

    public static void addCollectedItem(ServerPlayer player, Identifier id, long amount) {
        if (amount <= 0 || !TitleRegistry.tracksItem(id)) return;
        TitleCounters value = player.getData(SoulAscensionAttachments.TITLE_COUNTERS).addCollectedItem(id, amount);
        player.setData(SoulAscensionAttachments.TITLE_COUNTERS, value); evaluate(player);
    }

    public static void addMinedBlock(ServerPlayer player, Identifier id) {
        if (!TitleRegistry.tracksBlock(id)) return;
        TitleCounters value = player.getData(SoulAscensionAttachments.TITLE_COUNTERS).addMinedBlock(id);
        player.setData(SoulAscensionAttachments.TITLE_COUNTERS, value); evaluate(player);
    }

    public static void syncDefinitions(ServerPlayer player) {
        List<ClientTitleDefinition> values = TitleRegistry.all().stream().filter(TitleDefinition::available)
            .map(value -> new ClientTitleDefinition(value.id(), value.nameKey(), value.descriptionKey(),
                value.icon(), value.order(), value.hidden())).toList();
        PacketDistributor.sendToPlayer(player, new TitleDefinitionsPayload(values));
    }

    public static Component activeTitleName(ServerPlayer player) {
        Identifier id = get(player).activeTitle();
        return TitleRegistry.get(id).filter(TitleDefinition::available)
            .<Component>map(value -> Component.translatable(value.nameKey())).orElse(Component.empty());
    }
}
