package dev.uapi.soulascension.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/** Exact-version validation shared by Soul Ascension's persisted attachment codecs. */
final class PersistedDataVersion {
    private PersistedDataVersion() {
    }

    static Codec<Integer> codec(int expected, String structureName) {
        return Codec.INT.validate(actual -> actual == expected
            ? DataResult.success(actual)
            : DataResult.error(() -> structureName + " requires dataVersion=" + expected
                + " but found " + actual + "; cross-version loading is not supported"));
    }

    static int require(int actual, int expected, String structureName) {
        if (actual != expected) {
            throw new IllegalArgumentException(structureName + " requires dataVersion=" + expected
                + " but found " + actual + "; cross-version loading is not supported");
        }
        return actual;
    }
}
