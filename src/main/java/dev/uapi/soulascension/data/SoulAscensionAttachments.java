package dev.uapi.soulascension.data;

import dev.uapi.soulascension.SoulAscensionMod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public final class SoulAscensionAttachments {
    private static final DeferredRegister<AttachmentType<?>> TYPES =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, SoulAscensionMod.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerProgress>> PROGRESS = TYPES.register(
        "player_progress", () -> AttachmentType.builder(PlayerProgress::initial).serialize(PlayerProgress.CODEC)
            .copyOnDeath().sync((holder, player) -> holder == player, PlayerProgress.STREAM_CODEC).build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<DamageLedger>> DAMAGE_LEDGER = TYPES.register(
        "damage_ledger", () -> AttachmentType.builder(() -> new DamageLedger()).build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<TitleProgress>> TITLES = TYPES.register(
        "title_progress", () -> AttachmentType.builder(TitleProgress::initial).serialize(TitleProgress.CODEC)
            .copyOnDeath().sync((holder, player) -> holder == player, TitleProgress.STREAM_CODEC).build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<TitleCounters>> TITLE_COUNTERS = TYPES.register(
        "title_counters", () -> AttachmentType.builder(TitleCounters::initial).serialize(TitleCounters.CODEC)
            .copyOnDeath().build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ProfilePrivacyData>> PROFILE_PRIVACY = TYPES.register(
        "profile_privacy", () -> AttachmentType.builder(ProfilePrivacyData::initial).serialize(ProfilePrivacyData.CODEC)
            .copyOnDeath().sync((holder, player) -> holder == player, ProfilePrivacyData.STREAM_CODEC).build());

    private static final StreamCodec<RegistryFriendlyByteBuf, ResourceLocation> TITLE_ID_CODEC = new StreamCodec<>() {
        @Override public ResourceLocation decode(RegistryFriendlyByteBuf buffer) { return buffer.readResourceLocation(); }
        @Override public void encode(RegistryFriendlyByteBuf buffer, ResourceLocation value) { buffer.writeResourceLocation(value); }
    };
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ResourceLocation>> ACTIVE_TITLE = TYPES.register(
        "active_title", () -> AttachmentType.builder(() -> TitleProgress.NONE).serialize(ResourceLocation.CODEC)
            .copyOnDeath().sync((holder, player) -> true, TITLE_ID_CODEC).build());

    private SoulAscensionAttachments() {}
    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }
}
