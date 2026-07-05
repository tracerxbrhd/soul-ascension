package dev.uapi.soulascension.network;

import dev.uapi.soulascension.SoulAscensionMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ApplyStatAllocationPayload(int strength, int endurance, int agility, int intelligence, int perception)
        implements CustomPacketPayload {
    public static final Type<ApplyStatAllocationPayload> TYPE = new Type<>(SoulAscensionMod.id("apply_stat_allocation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ApplyStatAllocationPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public ApplyStatAllocationPayload decode(RegistryFriendlyByteBuf buffer) {
            return new ApplyStatAllocationPayload(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt());
        }

        @Override public void encode(RegistryFriendlyByteBuf buffer, ApplyStatAllocationPayload value) {
            buffer.writeVarInt(value.strength());
            buffer.writeVarInt(value.endurance());
            buffer.writeVarInt(value.agility());
            buffer.writeVarInt(value.intelligence());
            buffer.writeVarInt(value.perception());
        }
    };

    public int[] increments() {
        return new int[] {strength, endurance, agility, intelligence, perception};
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
