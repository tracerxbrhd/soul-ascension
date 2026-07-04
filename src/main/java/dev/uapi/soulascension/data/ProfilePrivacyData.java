package dev.uapi.soulascension.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Persistent, owner-synchronized profile privacy settings. */
public record ProfilePrivacyData(boolean profileHidden, boolean concealmentUnlocked,
                                 long lastVisibilityChangeTime, boolean initialized) {
    public static final Codec<ProfilePrivacyData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.optionalFieldOf("profileHidden", false).forGetter(ProfilePrivacyData::profileHidden),
        Codec.BOOL.optionalFieldOf("concealmentUnlocked", false).forGetter(ProfilePrivacyData::concealmentUnlocked),
        Codec.LONG.optionalFieldOf("lastVisibilityChangeTime", 0L).forGetter(ProfilePrivacyData::lastVisibilityChangeTime),
        Codec.BOOL.optionalFieldOf("initialized", false).forGetter(ProfilePrivacyData::initialized)
    ).apply(instance, ProfilePrivacyData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProfilePrivacyData> STREAM_CODEC = new StreamCodec<>() {
        @Override public ProfilePrivacyData decode(RegistryFriendlyByteBuf buffer) {
            return new ProfilePrivacyData(buffer.readBoolean(), buffer.readBoolean(), buffer.readVarLong(), buffer.readBoolean());
        }

        @Override public void encode(RegistryFriendlyByteBuf buffer, ProfilePrivacyData value) {
            buffer.writeBoolean(value.profileHidden());
            buffer.writeBoolean(value.concealmentUnlocked());
            buffer.writeVarLong(value.lastVisibilityChangeTime());
            buffer.writeBoolean(value.initialized());
        }
    };

    public static ProfilePrivacyData initial() {
        return new ProfilePrivacyData(false, false, 0L, false);
    }

    public ProfilePrivacyData initialized(boolean hidden) {
        return initialized ? this : new ProfilePrivacyData(hidden, concealmentUnlocked, 0L, true);
    }

    public ProfilePrivacyData withHidden(boolean hidden) {
        return new ProfilePrivacyData(hidden, concealmentUnlocked, System.currentTimeMillis(), true);
    }

    public ProfilePrivacyData activateEmblem(boolean unlockToggle, boolean setHidden) {
        return new ProfilePrivacyData(setHidden || profileHidden, concealmentUnlocked || unlockToggle,
            System.currentTimeMillis(), true);
    }
}
