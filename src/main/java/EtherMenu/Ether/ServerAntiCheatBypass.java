package EtherMenu.Ether;

import EtherMenu.utils.Logger;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Intercepts server-side anti-cheat validation, suspicious activity reports,
 * and kick/ban actions via ASM-injected hooks.
 *
 * The game's anti-cheat classes (AbstractAntiCheat, SuspiciousActivity, GameServer)
 * are patched at install time so their methods call our static hooks first.
 * When the bypass is enabled, hooks return true → original method is skipped.
 */
public class ServerAntiCheatBypass {

    private static volatile ServerAntiCheatBypass instance;

    private final AtomicBoolean globalBypassEnabled = new AtomicBoolean(false);
    private final Map<String, AtomicBoolean> bypassFlags = new ConcurrentHashMap<>();
    private final Set<String> whitelistedPlayers = ConcurrentHashMap.newKeySet();
    private final Map<String, AtomicLong> validationHookCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> reportBlockCounts = new ConcurrentHashMap<>();
    private final AtomicLong kickBlockCount = new AtomicLong(0);

    private static final String[] ANTICHEAT_TYPES = {
            "movement", "xp", "hit", "packet", "permission",
            "fire", "safehouse", "recipe", "player", "checksum",
            "item", "serverCustomization", "safety"
    };

    private ServerAntiCheatBypass() {
        for (String type : ANTICHEAT_TYPES) {
            bypassFlags.put(type, new AtomicBoolean(false));
            validationHookCounts.put(type, new AtomicLong(0));
            reportBlockCounts.put(type, new AtomicLong(0));
        }
    }

    public static ServerAntiCheatBypass getInstance() {
        if (instance == null) {
            synchronized (ServerAntiCheatBypass.class) {
                if (instance == null) {
                    instance = new ServerAntiCheatBypass();
                }
            }
        }
        return instance;
    }

    // ── ASM hook targets ────────────────────────────────────────────────

    /**
     * Called from patched AbstractAntiCheat.validate(UdpConnection, INetworkPacket).
     * @return true to SKIP the original validation (bypass active).
     */
    public static boolean hookValidation(Object antiCheatObj) {
        try {
            ServerAntiCheatBypass self = getInstance();

            // Global bypass
            if (self.globalBypassEnabled.get()) {
                String type = getAntiCheatType(antiCheatObj);
                self.validationHookCounts.getOrDefault(type, new AtomicLong()).incrementAndGet();
                return true;
            }

            // Per-type bypass
            String type = getAntiCheatType(antiCheatObj);
            AtomicBoolean flag = self.bypassFlags.get(type);
            if (flag != null && flag.get()) {
                self.validationHookCounts.getOrDefault(type, new AtomicLong()).incrementAndGet();
                return true;
            }
        } catch (Exception e) {
            Logger.printLog("[AntiCheatBypass] Error in hookValidation: " + e.getMessage());
        }
        return false;
    }

    /**
     * Called from patched SuspiciousActivity.report(AntiCheat).
     * @return true to BLOCK the report.
     */
    public static boolean hookSuspiciousActivity(Object activityObj) {
        try {
            ServerAntiCheatBypass self = getInstance();
            if (self.globalBypassEnabled.get()) {
                self.reportBlockCounts.getOrDefault("unknown", new AtomicLong()).incrementAndGet();
                return true;
            }
        } catch (Exception e) {
            Logger.printLog("[AntiCheatBypass] Error in hookSuspiciousActivity: " + e.getMessage());
        }
        return false;
    }

    /**
     * Called from patched GameServer.kickPlayer(String, String).
     * @return true to BLOCK the kick.
     */
    public static boolean hookKickAction(String playerName, String reason) {
        try {
            ServerAntiCheatBypass self = getInstance();
            if (!self.globalBypassEnabled.get()) return false;

            if (isAntiCheatRelatedKick(reason)) {
                self.kickBlockCount.incrementAndGet();
                Logger.printLog("[AntiCheatBypass] Blocked kick for '" + playerName + "': " + reason);
                return true;
            }
        } catch (Exception e) {
            Logger.printLog("[AntiCheatBypass] Error in hookKickAction: " + e.getMessage());
        }
        return false;
    }

    // ── Public control API ──────────────────────────────────────────────

    public void setGlobalBypass(boolean enabled) {
        globalBypassEnabled.set(enabled);
    }

    public boolean isGlobalBypassEnabled() {
        return globalBypassEnabled.get();
    }

    public void setTypeBypass(String type, boolean enabled) {
        AtomicBoolean flag = bypassFlags.get(type);
        if (flag != null) flag.set(enabled);
    }

    public boolean isTypeBypassEnabled(String type) {
        AtomicBoolean flag = bypassFlags.get(type);
        return flag != null && flag.get();
    }

    public void addWhitelistedPlayer(String username) {
        whitelistedPlayers.add(username);
    }

    public void removeWhitelistedPlayer(String username) {
        whitelistedPlayers.remove(username);
    }

    public long getTotalValidationHooks() {
        return validationHookCounts.values().stream().mapToLong(AtomicLong::get).sum();
    }

    public long getTotalKicksBlocked() {
        return kickBlockCount.get();
    }

    // ── Internals ───────────────────────────────────────────────────────

    private static String getAntiCheatType(Object obj) {
        if (obj == null) return "unknown";
        String simpleName = obj.getClass().getSimpleName();

        if (simpleName.contains("Movement")) return "movement";
        if (simpleName.contains("XP") || simpleName.contains("Experience")) return "xp";
        if (simpleName.contains("Hit") || simpleName.contains("Combat")) return "hit";
        if (simpleName.contains("Packet")) return "packet";
        if (simpleName.contains("Permission")) return "permission";
        if (simpleName.contains("Fire")) return "fire";
        if (simpleName.contains("Safehouse")) return "safehouse";
        if (simpleName.contains("Recipe")) return "recipe";
        if (simpleName.contains("Player")) return "player";
        if (simpleName.contains("Checksum")) return "checksum";
        if (simpleName.contains("Item")) return "item";
        if (simpleName.contains("ServerCustomization")) return "serverCustomization";
        if (simpleName.contains("Safety")) return "safety";
        return "unknown";
    }

    private static boolean isAntiCheatRelatedKick(String reason) {
        if (reason == null) return false;
        String lower = reason.toLowerCase();
        return lower.contains("cheat") || lower.contains("exploit")
                || lower.contains("hack") || lower.contains("suspicious")
                || lower.contains("invalid") || lower.contains("violation")
                || lower.contains("unauthorized") || lower.contains("malformed");
    }
}
