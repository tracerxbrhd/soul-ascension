package dev.uapi.soulascension.progression;

import com.google.gson.JsonObject;
import dev.uapi.reward.RewardContext;
import dev.uapi.soulascension.config.SoulAscensionServerConfig;
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
        int safeLevel = Math.max(1, level);
        double levelOffset = safeLevel - 1.0;
        double linear = SoulAscensionServerConfig.BASE_REQUIRED_DAMAGE.get()
            + levelOffset * SoulAscensionServerConfig.LINEAR_PER_LEVEL.get();
        double result = linear
            * Math.pow(safeLevel, SoulAscensionServerConfig.POWER_SCALING.get())
            * Math.pow(SoulAscensionServerConfig.EXPONENTIAL_MULTIPLIER.get(), levelOffset);
        if (!Double.isFinite(result)) result = Double.MAX_VALUE;
        result = Math.max(SoulAscensionServerConfig.MIN_REQUIRED_DAMAGE.get(), result);
        double maximum = SoulAscensionServerConfig.MAX_REQUIRED_DAMAGE.get();
        return maximum > 0 ? Math.min(maximum, result) : result;
    }

    public static void refreshRules(ServerPlayer player) {
        PlayerProgress old = get(player);
        int maximum = SoulAscensionServerConfig.MAX_LEVEL.get();
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
        int maxLevel = SoulAscensionServerConfig.MAX_LEVEL.get();
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

    public static int pointsAwardedForLevel(int reachedLevel) {
        return reachedLevel > 0 ? SoulAscensionServerConfig.POINTS_PER_LEVEL.get() : 0;
    }

    public static boolean changeStat(ServerPlayer player, Stat stat, int delta) {
        if (delta != 1 && delta != -1) return false;
        if (delta < 0 && !SoulAscensionServerConfig.ALLOW_STAT_DECREASE.get()) return false;
        PlayerProgress old = get(player);
        int maxPerStat = SoulAscensionServerConfig.MAX_POINTS_PER_STAT.get();
        if (delta > 0 && SoulAscensionServerConfig.LIMIT_STAT_POINTS.get()
            && maxPerStat > 0 && old.stat(stat) >= maxPerStat) return false;
        PlayerProgress updated = old.changeStat(stat, delta, SoulAscensionServerConfig.REFUND_DECREASED_POINTS.get());
        if (updated == old) return false;
        player.setData(SoulAscensionAttachments.PROGRESS, updated);
        AttributeService.apply(player, updated);
        TitleService.evaluate(player);
        return true;
    }

    public static boolean reset(ServerPlayer player) {
        if (!SoulAscensionServerConfig.ALLOW_RESET.get()) return false;
        PlayerProgress updated = get(player).resetStats();
        player.setData(SoulAscensionAttachments.PROGRESS, updated);
        AttributeService.apply(player, updated);
        TitleService.evaluate(player);
        return true;
    }

    public static ResetResult resetWithAmnesia(ServerPlayer player) {
        PlayerProgress old = get(player);
        int allocated = old.strength() + old.endurance() + old.agility() + old.intelligence() + old.perception();
        if (!SoulAscensionServerConfig.ALLOW_RESET.get())
            return new ResetResult(false, allocated, 0, 0);
        int lost = SoulAscensionServerConfig.AMNESIA_POINT_LOSS_ENABLED.get()
            ? (int) Math.floor(allocated * SoulAscensionServerConfig.AMNESIA_POINT_LOSS_PERCENT.get() / 100.0)
            : 0;
        lost = Math.max(0, Math.min(allocated, lost));
        PlayerProgress updated = old.resetStats(lost);
        player.setData(SoulAscensionAttachments.PROGRESS, updated);
        AttributeService.apply(player, updated);
        TitleService.evaluate(player);
        return new ResetResult(true, allocated, allocated - lost, lost);
    }

    /** Altar/admin respec: preserves level and accumulated damage progress and refunds every allocated point. */
    public static ResetResult respecWithoutLoss(ServerPlayer player) {
        PlayerProgress old = get(player);
        int allocated = old.strength() + old.endurance() + old.agility() + old.intelligence() + old.perception();
        PlayerProgress updated = old.resetStats();
        player.setData(SoulAscensionAttachments.PROGRESS, updated);
        AttributeService.apply(player, updated);
        TitleService.evaluate(player);
        return new ResetResult(true, allocated, allocated, 0);
    }

    /** Compatibility alias for integrations compiled against 1.0.x. */
    @Deprecated(forRemoval = false)
    public static ResetResult resetWithAmnesiaScroll(ServerPlayer player) {
        return resetWithAmnesia(player);
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
        double intelligenceBonus = 1.0 + progress.intelligence()
            * SoulAscensionServerConfig.INTELLIGENCE_REWARD_XP_PERCENT.get() / 100.0;
        addExperience(context.player(), amount * context.instance().difficulty().rewardMultiplier() * intelligenceBonus);
        return amount > 0;
    }
}
