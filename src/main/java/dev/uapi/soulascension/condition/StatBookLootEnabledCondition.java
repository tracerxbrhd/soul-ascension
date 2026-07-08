package dev.uapi.soulascension.condition;

import com.mojang.serialization.MapCodec;
import dev.uapi.soulascension.config.SoulAscensionConfigManager;
import net.neoforged.neoforge.common.conditions.ICondition;

/** Data-pack condition used by global loot modifiers for stat Black Books. */
public final class StatBookLootEnabledCondition implements ICondition {
    public static final StatBookLootEnabledCondition INSTANCE = new StatBookLootEnabledCondition();
    public static final MapCodec<StatBookLootEnabledCondition> CODEC = MapCodec.unit(INSTANCE).stable();

    private StatBookLootEnabledCondition() {}

    @Override
    public boolean test(IContext context) {
        return SoulAscensionConfigManager.current().statBookLootEnabled();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
