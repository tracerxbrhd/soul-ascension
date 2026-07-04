package dev.uapi.soulascension.condition;

import com.mojang.serialization.MapCodec;
import dev.uapi.soulascension.config.SoulAscensionServerConfig;
import net.neoforged.neoforge.common.conditions.ICondition;

public final class EmblemCraftableCondition implements ICondition {
    public static final EmblemCraftableCondition INSTANCE = new EmblemCraftableCondition();
    public static final MapCodec<EmblemCraftableCondition> CODEC = MapCodec.unit(INSTANCE).stable();
    private EmblemCraftableCondition() {}
    @Override public boolean test(IContext context) {
        return SoulAscensionServerConfig.CONCEALMENT_EMBLEM_ENABLED.get()
            && SoulAscensionServerConfig.EMBLEM_CRAFTABLE.get();
    }
    @Override public MapCodec<? extends ICondition> codec() { return CODEC; }
}
