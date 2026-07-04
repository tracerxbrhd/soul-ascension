package dev.uapi.soulascension.network;

/** Client cache populated exclusively by the authoritative server. */
public final class ClientProgressionRules {
    private static boolean amnesiaPointLossEnabled;
    private static double amnesiaPointLossPercent;
    private static boolean soulLensEnabled = true;
    private static double soulLensRange = 64.0;
    private static int soulLensUpdateInterval = 10;
    private static boolean soulLensBlockHotbarScroll = true;

    private ClientProgressionRules() {}

    public static void replace(ProgressionRulesPayload payload) {
        amnesiaPointLossEnabled = payload.amnesiaPointLossEnabled();
        amnesiaPointLossPercent = Math.max(0.0, Math.min(100.0, payload.amnesiaPointLossPercent()));
        soulLensEnabled = payload.soulLensEnabled();
        soulLensRange = Math.max(1.0, Math.min(256.0, payload.soulLensRange()));
        soulLensUpdateInterval = Math.max(1, Math.min(200, payload.soulLensUpdateInterval()));
        soulLensBlockHotbarScroll = payload.soulLensBlockHotbarScroll();
    }

    public static boolean amnesiaPointLossEnabled() { return amnesiaPointLossEnabled; }
    public static double amnesiaPointLossPercent() { return amnesiaPointLossPercent; }
    public static boolean soulLensEnabled() { return soulLensEnabled; }
    public static double soulLensRange() { return soulLensRange; }
    public static int soulLensUpdateInterval() { return soulLensUpdateInterval; }
    public static boolean soulLensBlockHotbarScroll() { return soulLensBlockHotbarScroll; }
}
