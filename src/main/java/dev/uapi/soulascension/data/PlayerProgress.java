package dev.uapi.soulascension.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record PlayerProgress(int schemaVersion, int level, double damageProgress, double requiredDamage, int unspentPoints,
                             int strength, int endurance, int agility, int intelligence, int perception) {
    public static final int SCHEMA_VERSION = 130;
    public static final Codec<PlayerProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.fieldOf("schema_version").forGetter(PlayerProgress::schemaVersion),
        Codec.INT.optionalFieldOf("level", 0).forGetter(PlayerProgress::level),
        Codec.DOUBLE.optionalFieldOf("damageProgress", 0.0).forGetter(PlayerProgress::damageProgress),
        Codec.DOUBLE.optionalFieldOf("requiredDamage", 0.0).forGetter(PlayerProgress::requiredDamage),
        Codec.INT.optionalFieldOf("unspentPoints", 0).forGetter(PlayerProgress::unspentPoints),
        Codec.INT.optionalFieldOf("strength", 0).forGetter(PlayerProgress::strength),
        Codec.INT.optionalFieldOf("endurance", 0).forGetter(PlayerProgress::endurance),
        Codec.INT.optionalFieldOf("agility", 0).forGetter(PlayerProgress::agility),
        Codec.INT.optionalFieldOf("intelligence", 0).forGetter(PlayerProgress::intelligence),
        Codec.INT.optionalFieldOf("perception", 0).forGetter(PlayerProgress::perception)
    ).apply(instance, PlayerProgress::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerProgress> STREAM_CODEC = new StreamCodec<>() {
        @Override public PlayerProgress decode(RegistryFriendlyByteBuf buffer) {
            return new PlayerProgress(buffer.readVarInt(), buffer.readVarInt(), buffer.readDouble(), buffer.readDouble(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, PlayerProgress value) {
            buffer.writeVarInt(value.schemaVersion()); buffer.writeVarInt(value.level());
            buffer.writeDouble(value.damageProgress()); buffer.writeDouble(value.requiredDamage());
            buffer.writeVarInt(value.unspentPoints()); buffer.writeVarInt(value.strength());
            buffer.writeVarInt(value.endurance()); buffer.writeVarInt(value.agility());
            buffer.writeVarInt(value.intelligence()); buffer.writeVarInt(value.perception());
        }
    };

    public PlayerProgress(int level, double damageProgress, double requiredDamage, int unspentPoints,
                          int strength, int endurance, int agility, int intelligence, int perception) {
        this(SCHEMA_VERSION, level, damageProgress, requiredDamage, unspentPoints,
            strength, endurance, agility, intelligence, perception);
    }

    public static PlayerProgress initial() { return new PlayerProgress(0, 0, 0, 0, 0, 0, 0, 0, 0); }

    public PlayerProgress withRequiredDamage(double value) {
        return new PlayerProgress(level, damageProgress, value, unspentPoints, strength, endurance, agility,
            intelligence, perception);
    }

    public PlayerProgress withAdditionalUnspentPoints(int amount) {
        if (amount <= 0) return this;
        return new PlayerProgress(level, damageProgress, requiredDamage, saturatedAdd(unspentPoints, amount),
            strength, endurance, agility, intelligence, perception);
    }

    public int stat(Stat stat) {
        return switch (stat) {
            case STRENGTH -> strength;
            case ENDURANCE -> endurance;
            case AGILITY -> agility;
            case INTELLIGENCE -> intelligence;
            case PERCEPTION -> perception;
        };
    }

    public PlayerProgress spend(Stat stat) {
        return changeStat(stat, 1, true);
    }

    public PlayerProgress changeStat(Stat stat, int delta, boolean refundDecrease) {
        if (delta != 1 && delta != -1) return this;
        if (delta > 0 && unspentPoints <= 0) return this;
        if (delta < 0 && stat(stat) <= 0) return this;
        int pointDelta = delta > 0 ? -1 : refundDecrease ? 1 : 0;
        return new PlayerProgress(level, damageProgress, requiredDamage, Math.max(0, unspentPoints + pointDelta),
            strength + (stat == Stat.STRENGTH ? delta : 0), endurance + (stat == Stat.ENDURANCE ? delta : 0),
            agility + (stat == Stat.AGILITY ? delta : 0), intelligence + (stat == Stat.INTELLIGENCE ? delta : 0),
            perception + (stat == Stat.PERCEPTION ? delta : 0));
    }

    public PlayerProgress resetStats() {
        return resetStats(0);
    }

    public PlayerProgress resetStats(int pointsLost) {
        int spent = strength + endurance + agility + intelligence + perception;
        int refunded = Math.max(0, spent - Math.max(0, Math.min(spent, pointsLost)));
        return new PlayerProgress(level, damageProgress, requiredDamage, unspentPoints + refunded, 0, 0, 0, 0, 0);
    }

    private static int saturatedAdd(int left, int right) {
        long value = (long) left + right;
        return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }
}
