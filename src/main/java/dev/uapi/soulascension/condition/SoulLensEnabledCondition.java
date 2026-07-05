package dev.uapi.soulascension.condition;

import com.mojang.serialization.MapCodec;
import dev.uapi.soulascension.config.SoulAscensionConfigManager;
import net.neoforged.neoforge.common.conditions.ICondition;

public final class SoulLensEnabledCondition implements ICondition {
    public static final SoulLensEnabledCondition INSTANCE = new SoulLensEnabledCondition();
    public static final MapCodec<SoulLensEnabledCondition> CODEC = MapCodec.unit(INSTANCE).stable();
    private SoulLensEnabledCondition() {}
    @Override public boolean test(IContext context) { return SoulAscensionConfigManager.current().soulLensEnabled(); }
    @Override public MapCodec<? extends ICondition> codec() { return CODEC; }
}
