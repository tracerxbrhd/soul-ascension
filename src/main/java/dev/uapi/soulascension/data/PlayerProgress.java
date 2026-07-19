package dev.uapi.soulascension.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record PlayerProgress(int dataVersion, int level, double damageProgress, double requiredDamage, int unspentPoints,
                             int strength, int endurance, int agility, int intelligence, int perception) {
    /** Independent persistent-data version; it intentionally does not mirror the mod SemVer. */
    public static final int DATA_VERSION = 2;

    public static final Codec<PlayerProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        PersistedDataVersion.codec(DATA_VERSION, "Soul character progress").fieldOf("dataVersion")
            .forGetter(PlayerProgress::dataVersion),
        Codec.INT.fieldOf("level").forGetter(PlayerProgress::level),
        Codec.DOUBLE.fieldOf("damageProgress").forGetter(PlayerProgress::damageProgress),
        Codec.DOUBLE.fieldOf("requiredDamage").forGetter(PlayerProgress::requiredDamage),
        Codec.INT.fieldOf("unspentPoints").forGetter(PlayerProgress::unspentPoints),
        Codec.INT.fieldOf("strength").forGetter(PlayerProgress::strength),
        Codec.INT.fieldOf("endurance").forGetter(PlayerProgress::endurance),
        Codec.INT.fieldOf("agility").forGetter(PlayerProgress::agility),
        Codec.INT.fieldOf("intelligence").forGetter(PlayerProgress::intelligence),
        Codec.INT.fieldOf("perception").forGetter(PlayerProgress::perception)
    ).apply(instance, PlayerProgress::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerProgress> STREAM_CODEC = new StreamCodec<>() {
        @Override public PlayerProgress decode(RegistryFriendlyByteBuf buffer) {
            int version = buffer.readVarInt();
            PersistedDataVersion.require(version, DATA_VERSION, "Soul character progress");
            return new PlayerProgress(version, buffer.readVarInt(), buffer.readDouble(), buffer.readDouble(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, PlayerProgress value) {
            buffer.writeVarInt(value.dataVersion()); buffer.writeVarInt(value.level());
            buffer.writeDouble(value.damageProgress()); buffer.writeDouble(value.requiredDamage());
            buffer.writeVarInt(value.unspentPoints()); buffer.writeVarInt(value.strength());
            buffer.writeVarInt(value.endurance()); buffer.writeVarInt(value.agility());
            buffer.writeVarInt(value.intelligence()); buffer.writeVarInt(value.perception());
        }
    };

    public PlayerProgress {
        PersistedDataVersion.require(dataVersion, DATA_VERSION, "Soul character progress");
    }

    public PlayerProgress(int level, double damageProgress, double requiredDamage, int unspentPoints,
                          int strength, int endurance, int agility, int intelligence, int perception) {
        this(DATA_VERSION, level, damageProgress, requiredDamage, unspentPoints,
            strength, endurance, agility, intelligence, perception);
    }

    public static PlayerProgress initial() { return new PlayerProgress(0, 0, 0, 0, 0, 0, 0, 0, 0); }

    /** Strict current-format validation. It never upgrades or repairs persisted values. */
    public boolean isUsable() {
        return level >= 0
            && finiteNonNegative(damageProgress)
            && finiteNonNegative(requiredDamage)
            && unspentPoints >= 0
            && strength >= 0
            && endurance >= 0
            && agility >= 0
            && intelligence >= 0
            && perception >= 0;
    }

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
        int current = stat(stat);
        if (delta > 0 && current == Integer.MAX_VALUE) return this;
        if (delta < 0 && current <= 0) return this;
        int pointDelta = delta > 0 ? -1 : refundDecrease ? 1 : 0;
        int remaining = pointDelta > 0 ? saturatedAdd(unspentPoints, pointDelta)
            : Math.max(0, unspentPoints + pointDelta);
        return new PlayerProgress(level, damageProgress, requiredDamage, remaining,
            strength + (stat == Stat.STRENGTH ? delta : 0), endurance + (stat == Stat.ENDURANCE ? delta : 0),
            agility + (stat == Stat.AGILITY ? delta : 0), intelligence + (stat == Stat.INTELLIGENCE ? delta : 0),
            perception + (stat == Stat.PERCEPTION ? delta : 0));
    }

    public PlayerProgress resetStats() {
        return resetStats(0);
    }

    public PlayerProgress resetStats(int pointsLost) {
        long exactSpent = (long) strength + endurance + agility + intelligence + perception;
        int spent = (int) Math.min(Integer.MAX_VALUE, Math.max(0L, exactSpent));
        int refunded = Math.max(0, spent - Math.max(0, Math.min(spent, pointsLost)));
        return new PlayerProgress(level, damageProgress, requiredDamage,
            saturatedAdd(unspentPoints, refunded), 0, 0, 0, 0, 0);
    }

    private static int saturatedAdd(int left, int right) {
        long value = (long) left + right;
        return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private static boolean finiteNonNegative(double value) {
        return Double.isFinite(value) && value >= 0.0;
    }
}
