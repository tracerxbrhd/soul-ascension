package dev.uapi.soulascension.client;

/** Shared Soul Ascension palette and alpha helpers for resource-pack-backed UI. */
public final class SoulUiTheme {
    public static final int BACKGROUND_OVERLAY = 0xA8080612;
    public static final int TEXT = 0xFFF1E9FF;
    public static final int MUTED = 0xFF9B91AA;
    public static final int VALUE = 0xFFD79BFF;
    public static final int POSITIVE = 0xFFB9FFDB;
    public static final int ACCENT = 0xFFD66BFF;
    public static final int DIVIDER = 0xFF8E4BC4;

    private SoulUiTheme() {}

    public static int withOpacity(int argb, double opacity) {
        double safe = Double.isFinite(opacity) ? Math.max(0.0, Math.min(1.0, opacity)) : 1.0;
        int sourceAlpha = argb >>> 24;
        int alpha = (int) Math.round(sourceAlpha * safe);
        return (argb & 0x00FFFFFF) | alpha << 24;
    }
}
