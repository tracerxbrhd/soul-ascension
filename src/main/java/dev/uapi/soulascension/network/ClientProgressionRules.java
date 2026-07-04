package dev.uapi.soulascension.network;

/** Client cache populated exclusively by the authoritative server. */
public final class ClientProgressionRules {
    private static boolean amnesiaPointLossEnabled;
    private static double amnesiaPointLossPercent;
    private static boolean soulLensEnabled = true;
    private static double soulLensRange = 64.0;
    private static int soulLensUpdateInterval = 10;
    private static boolean soulLensBlockHotbarScroll = true;
    private static double soulLensIdleOverlayOpacity = 0.25;
    private static double soulLensActiveOverlayOpacity = 0.85;
    private static double soulLensHiddenOverlayOpacity = 0.75;
    private static boolean soulLensShowIdleHint = true;

    private ClientProgressionRules() {}

    public static void replace(ProgressionRulesPayload payload) {
        amnesiaPointLossEnabled = payload.amnesiaPointLossEnabled();
        amnesiaPointLossPercent = Math.max(0.0, Math.min(100.0, payload.amnesiaPointLossPercent()));
        soulLensEnabled = payload.soulLensEnabled();
        soulLensRange = Math.max(1.0, Math.min(256.0, payload.soulLensRange()));
        soulLensUpdateInterval = Math.max(1, Math.min(200, payload.soulLensUpdateInterval()));
        soulLensBlockHotbarScroll = payload.soulLensBlockHotbarScroll();
        soulLensIdleOverlayOpacity = clampOpacity(payload.soulLensIdleOverlayOpacity());
        soulLensActiveOverlayOpacity = clampOpacity(payload.soulLensActiveOverlayOpacity());
        soulLensHiddenOverlayOpacity = clampOpacity(payload.soulLensHiddenOverlayOpacity());
        soulLensShowIdleHint = payload.soulLensShowIdleHint();
    }

    public static boolean amnesiaPointLossEnabled() { return amnesiaPointLossEnabled; }
    public static double amnesiaPointLossPercent() { return amnesiaPointLossPercent; }
    public static boolean soulLensEnabled() { return soulLensEnabled; }
    public static double soulLensRange() { return soulLensRange; }
    public static int soulLensUpdateInterval() { return soulLensUpdateInterval; }
    public static boolean soulLensBlockHotbarScroll() { return soulLensBlockHotbarScroll; }
    public static double soulLensIdleOverlayOpacity() { return soulLensIdleOverlayOpacity; }
    public static double soulLensActiveOverlayOpacity() { return soulLensActiveOverlayOpacity; }
    public static double soulLensHiddenOverlayOpacity() { return soulLensHiddenOverlayOpacity; }
    public static boolean soulLensShowIdleHint() { return soulLensShowIdleHint; }

    private static double clampOpacity(double value) {
        return Double.isFinite(value) ? Math.max(0.0, Math.min(1.0, value)) : 1.0;
    }
}
