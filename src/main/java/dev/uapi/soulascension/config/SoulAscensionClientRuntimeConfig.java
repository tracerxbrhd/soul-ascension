package dev.uapi.soulascension.config;

public record SoulAscensionClientRuntimeConfig(boolean showAttributeNamespaces, String hiddenAttributes,
                                                String visibleAttributes, String attributeCategories,
                                                boolean showPlayerPreview) {
    public static SoulAscensionClientRuntimeConfig defaults() {
        return new SoulAscensionClientRuntimeConfig(false, SoulAscensionClientConfig.DEFAULT_HIDDEN_ATTRIBUTES,
            "", "", false);
    }
}
