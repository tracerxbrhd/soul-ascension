package dev.uapi.soulascension.progression;

import com.google.gson.JsonObject;
import dev.uapi.reward.RewardContext;
import dev.uapi.soulascension.config.SoulAscensionConfigManager;
import dev.uapi.soulascension.config.SoulAscensionRuntimeConfig;
import dev.uapi.soulascension.config.AttributeRewardsConfig;
import dev.uapi.soulascension.data.SoulAscensionAttachments;
import dev.uapi.soulascension.data.PlayerProgress;
import dev.uapi.soulascension.data.Stat;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import dev.uapi.soulascension.title.TitleService;

public final class SoulAscensionService {
    public record ResetResult(boolean changed, int allocated, int refunded, int lost) {}

    private SoulAscensionService() {}

    public static PlayerProgress get(ServerPlayer player) { return player.getData(SoulAscensionAttachments.PROGRESS); }
    public static double requiredDamage(int level) {
        SoulAscensionRuntimeConfig config = SoulAscensionConfigManager.current();
        int safeLevel = Math.max(1, level);
        double levelOffset = safeLevel - 1.0;
        double linear = config.baseRequiredDamage() + levelOffset * config.linearPerLevel();
        double result = linear
            * Math.pow(safeLevel, config.powerScaling())
            * Math.pow(config.exponentialMultiplier(), levelOffset);
        if (!Double.isFinite(result)) result = Double.MAX_VALUE;
        result = Math.max(config.minRequiredDamage(), result);
        double maximum = config.maxRequiredDamage();
        return maximum > 0 ? Math.min(maximum, result) : result;
    }

    public static void refreshRules(ServerPlayer player) {
        PlayerProgress old = get(player);
        int maximum = SoulAscensionConfigManager.current().maxLevel();
        int level = maximum > 0 ? Math.min(old.level(), maximum) : old.level();
        double progress = maximum > 0 && level >= maximum ? 0 : old.damageProgress();
        double required = requiredDamage(level);
        if (level != old.level() || Double.compare(progress, old.damageProgress()) != 0
            || Double.compare(old.requiredDamage(), required) != 0) {
            player.setData(SoulAscensionAttachments.PROGRESS, new PlayerProgress(level, progress, required,
                old.unspentPoints(), old.strength(), old.endurance(), old.agility(), old.intelligence(), old.perception()));
        }
    }

    public static int addExperience(ServerPlayer player, double amount) {
        if (amount <= 0) return 0;
        PlayerProgress old = get(player);
        int maxLevel = SoulAscensionConfigManager.current().maxLevel();
        if (maxLevel > 0 && old.level() >= maxLevel) return 0;
        int level = old.level();
        int points = old.unspentPoints();
        double progress = old.damageProgress() + amount;
        int gained = 0;
        while (progress >= requiredDamage(level) && (maxLevel == 0 || level < maxLevel)) {
            progress -= requiredDamage(level);
            level++;
            points += pointsAwardedForLevel(level);
            gained++;
        }
        if (maxLevel > 0 && level >= maxLevel) progress = 0;
        PlayerProgress updated = new PlayerProgress(level, progress, requiredDamage(level), points, old.strength(), old.endurance(),
            old.agility(), old.intelligence(), old.perception());
        player.setData(SoulAscensionAttachments.PROGRESS, updated);
        if (gained > 0) player.sendSystemMessage(Component.translatable("message.soul_ascension.level_up", level));
        if (gained > 0) TitleService.evaluate(player);
        return gained;
    }

    public static boolean spendPoint(ServerPlayer player, Stat stat) {
        return changeStat(player, stat, 1);
    }

    /** Atomically validates and applies client-previewed positive stat increments. */
    public static boolean applyAllocation(ServerPlayer player, int[] increments) {
        if (increments == null || increments.length != Stat.values().length) return false;
        PlayerProgress old = get(player);
        long requested = 0;
        for (int index = 0; index < increments.length; index++) {
            int increment = increments[index];
            if (increment < 0) return false;
            requested += increment;
            if (requested > old.unspentPoints()) return false;
            SoulAscensionRuntimeConfig config = SoulAscensionConfigManager.current();
            int maximum = config.maxPointsPerStat();
            if (config.limitStatPoints() && maximum > 0
                && old.stat(Stat.values()[index]) + increment > maximum) return false;
        }
        if (requested <= 0) return false;
        PlayerProgress updated = new PlayerProgress(old.level(), old.damageProgress(), old.requiredDamage(),
            old.unspentPoints() - (int) requested,
            old.strength() + increments[Stat.STRENGTH.ordinal()],
            old.endurance() + increments[Stat.ENDURANCE.ordinal()],
            old.agility() + increments[Stat.AGILITY.ordinal()],
            old.intelligence() + increments[Stat.INTELLIGENCE.ordinal()],
            old.perception() + increments[Stat.PERCEPTION.ordinal()]);
        player.setData(SoulAscensionAttachments.PROGRESS, updated);
        AttributeService.apply(player, updated);
        TitleService.evaluate(player);
        return true;
    }

    public static int pointsAwardedForLevel(int reachedLevel) {
        return reachedLevel > 0 ? SoulAscensionConfigManager.current().pointsPerLevel() : 0;
    }

    public static boolean changeStat(ServerPlayer player, Stat stat, int delta) {
        if (delta != 1 && delta != -1) return false;
        SoulAscensionRuntimeConfig config = SoulAscensionConfigManager.current();
        if (delta < 0 && !config.allowStatDecrease()) return false;
        PlayerProgress old = get(player);
        int maxPerStat = config.maxPointsPerStat();
        if (delta > 0 && config.limitStatPoints()
            && maxPerStat > 0 && old.stat(stat) >= maxPerStat) return false;
        PlayerProgress updated = old.changeStat(stat, delta, config.refundDecreasedPoints());
        if (updated == old) return false;
        player.setData(SoulAscensionAttachments.PROGRESS, updated);
        AttributeService.apply(player, updated);
        TitleService.evaluate(player);
        return true;
    }

    public static boolean reset(ServerPlayer player) {
        if (!SoulAscensionConfigManager.current().allowReset()) return false;
        PlayerProgress updated = get(player).resetStats();
        player.setData(SoulAscensionAttachments.PROGRESS, updated);
        AttributeService.apply(player, updated);
        TitleService.evaluate(player);
        return true;
    }

    public static ResetResult resetWithAmnesia(ServerPlayer player) {
        PlayerProgress old = get(player);
        int allocated = old.strength() + old.endurance() + old.agility() + old.intelligence() + old.perception();
        SoulAscensionRuntimeConfig config = SoulAscensionConfigManager.current();
        if (!config.allowReset())
            return new ResetResult(false, allocated, 0, 0);
        int lost = config.amnesiaPointLossEnabled()
            ? (int) Math.floor(allocated * config.amnesiaPointLossPercent() / 100.0)
            : 0;
        lost = Math.max(0, Math.min(allocated, lost));
        PlayerProgress updated = old.resetStats(lost);
        player.setData(SoulAscensionAttachments.PROGRESS, updated);
        AttributeService.apply(player, updated);
        TitleService.evaluate(player);
        return new ResetResult(true, allocated, allocated - lost, lost);
    }

    public static void addPoints(ServerPlayer player, int amount) {
        PlayerProgress old = get(player);
        player.setData(SoulAscensionAttachments.PROGRESS, new PlayerProgress(old.level(), old.damageProgress(), old.requiredDamage(),
            Math.max(0, old.unspentPoints() + amount), old.strength(), old.endurance(), old.agility(),
            old.intelligence(), old.perception()));
    }

    public static void resetAll(ServerPlayer player) {
        PlayerProgress initial = PlayerProgress.initial().withRequiredDamage(requiredDamage(0));
        player.setData(SoulAscensionAttachments.PROGRESS, initial);
        AttributeService.apply(player, initial);
        TitleService.evaluate(player);
    }

    public static boolean grantRewardExperience(RewardContext context, JsonObject data, RandomSource random) {
        int min = Math.max(0, GsonHelper.getAsInt(data, "min", 1));
        int max = Math.max(min, GsonHelper.getAsInt(data, "max", min));
        int amount = min + random.nextInt(max - min + 1);
        PlayerProgress progress = get(context.player());
        double intelligenceBonus = AttributeRewardsConfig.affectsSoulProgression()
            ? AttributeRewardsConfig.experienceMultiplier(progress.intelligence()) : 1.0;
        addExperience(context.player(), amount * context.instance().difficulty().rewardMultiplier() * intelligenceBonus);
        return amount > 0;
    }
}
