package dev.uapi.soulascension.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record DamageLedger(Map<UUID, Double> credited) {
    public DamageLedger() { this(Map.of()); }

    public double credited(UUID player) { return credited.getOrDefault(player, 0.0); }

    public DamageLedger withCredit(UUID player, double amount) {
        Map<UUID, Double> copy = new HashMap<>(credited);
        copy.put(player, amount);
        return new DamageLedger(Map.copyOf(copy));
    }
}
