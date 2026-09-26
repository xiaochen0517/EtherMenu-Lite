package EtherMenu.Ether;

import EtherMenu.utils.Logger;
import zombie.characters.CharacterStat;
import zombie.characters.IsoPlayer;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prevents the server from overwriting locally-modified player values.
 *
 * When protection is active for a category (stats/skills/traits), this class
 * stores the "desired" values and re-applies them every tick, effectively
 * making the local player immune to server-authoritative sync for those fields.
 *
 * Works alongside the existing Stats.set() interceptor in EtherAPI — this class
 * adds an extra layer by re-applying values that the server might reset through
 * other code paths (e.g. full player sync packets, trait resets, etc.).
 */
public class ServerSyncBlocker {

    private static volatile ServerSyncBlocker instance;

    private volatile boolean blockStatsSync;
    private volatile boolean blockSkillsSync;
    private volatile boolean blockTraitsSync;

    private final Map<CharacterStat, Float> protectedStats = new ConcurrentHashMap<>();
    private final Map<String, Integer> protectedSkillLevels = new ConcurrentHashMap<>();
    private final Map<String, Float> protectedSkillXP = new ConcurrentHashMap<>();
    private final Set<String> protectedTraits = ConcurrentHashMap.newKeySet();

    private ServerSyncBlocker() {
    }

    public static ServerSyncBlocker getInstance() {
        if (instance == null) {
            synchronized (ServerSyncBlocker.class) {
                if (instance == null) {
                    instance = new ServerSyncBlocker();
                }
            }
        }
        return instance;
    }

    // ── Stats protection ────────────────────────────────────────────────

    public void enableStatsProtection() {
        blockStatsSync = true;
        Logger.printLog("[SyncBlocker] Stats protection enabled");
    }

    public void disableStatsProtection() {
        blockStatsSync = false;
        protectedStats.clear();
        Logger.printLog("[SyncBlocker] Stats protection disabled");
    }

    public boolean isStatsProtectionEnabled() {
        return blockStatsSync;
    }

    public void protectStat(String statName, float value) {
        CharacterStat stat = CharacterStat.REGISTRY.get(statName.toUpperCase());
        if (stat != null) {
            protectedStats.put(stat, value);
        } else {
            Logger.printLog("[SyncBlocker] Unknown stat: " + statName);
        }
    }

    public void unprotectStat(String statName) {
        CharacterStat stat = CharacterStat.REGISTRY.get(statName.toUpperCase());
        if (stat != null) {
            protectedStats.remove(stat);
        }
    }

    // ── Skills protection ───────────────────────────────────────────────

    public void enableSkillsProtection() {
        blockSkillsSync = true;
        Logger.printLog("[SyncBlocker] Skills protection enabled");
    }

    public void disableSkillsProtection() {
        blockSkillsSync = false;
        protectedSkillLevels.clear();
        protectedSkillXP.clear();
        Logger.printLog("[SyncBlocker] Skills protection disabled");
    }

    public boolean isSkillsProtectionEnabled() {
        return blockSkillsSync;
    }

    public void protectSkill(String perkName, int level, float xp) {
        protectedSkillLevels.put(perkName, level);
        protectedSkillXP.put(perkName, xp);
        Logger.printLog("[SyncBlocker] Protected skill: " + perkName + " level=" + level);
    }

    // ── Traits protection ───────────────────────────────────────────────

    public void enableTraitsProtection() {
        blockTraitsSync = true;
        Logger.printLog("[SyncBlocker] Traits protection enabled");
    }

    public void disableTraitsProtection() {
        blockTraitsSync = false;
        protectedTraits.clear();
        Logger.printLog("[SyncBlocker] Traits protection disabled");
    }

    public boolean isTraitsProtectionEnabled() {
        return blockTraitsSync;
    }

    // ── Full protection toggle ──────────────────────────────────────────

    public void enableFullProtection() {
        blockStatsSync = true;
        blockSkillsSync = true;
        blockTraitsSync = true;
        Logger.printLog("[SyncBlocker] Full sync protection enabled");
    }

    public void disableFullProtection() {
        blockStatsSync = false;
        blockSkillsSync = false;
        blockTraitsSync = false;
        protectedStats.clear();
        protectedSkillLevels.clear();
        protectedSkillXP.clear();
        protectedTraits.clear();
        Logger.printLog("[SyncBlocker] Full sync protection disabled");
    }

    public boolean isAnyProtectionActive() {
        return blockStatsSync || blockSkillsSync || blockTraitsSync;
    }

    // ── Reapply protected values (called each tick) ─────────────────────

    public void reapplyProtectedValues() {
        IsoPlayer player = IsoPlayer.getInstance();
        if (player == null) return;

        try {
            // Re-enforce stats
            if (blockStatsSync && !protectedStats.isEmpty()) {
                for (Map.Entry<CharacterStat, Float> entry : protectedStats.entrySet()) {
                    player.getStats().set(entry.getKey(), entry.getValue());
                }
            }

            // Re-enforce skill levels
            if (blockSkillsSync && !protectedSkillLevels.isEmpty()) {
                for (Map.Entry<String, Integer> entry : protectedSkillLevels.entrySet()) {
                    setSkillLevelDirect(player, entry.getKey(), entry.getValue());

                    Float xp = protectedSkillXP.get(entry.getKey());
                    if (xp != null) {
                        setSkillXPDirect(player, entry.getKey(), xp);
                    }
                }
            }
        } catch (Exception e) {
            Logger.printLog("[SyncBlocker] Error reapplying values: " + e.getMessage());
        }
    }

    // ── Auto-capture: snapshot current values as protected ──────────────

    /**
     * Captures the player's current stats as protected values.
     * Useful to "lock in" god-mode stats.
     */
    public void captureCurrentStats() {
        IsoPlayer player = IsoPlayer.getInstance();
        if (player == null) return;

        for (CharacterStat stat : CharacterStat.ORDERED_STATS) {
            try {
                float val = player.getStats().get(stat);
                protectedStats.put(stat, val);
            } catch (Exception ignored) {
            }
        }
        Logger.printLog("[SyncBlocker] Captured " + protectedStats.size() + " stat values");
    }

    // ── Internal helpers ────────────────────────────────────────────────

    private void setSkillLevelDirect(IsoPlayer player, String perkName, int level) {
        try {
            zombie.characters.skills.PerkFactory.Perk perk =
                    zombie.characters.skills.PerkFactory.getPerkFromName(perkName);
            if (perk != null) {
                player.setPerkLevelDebug(perk, level);
            }
        } catch (Exception e) {
            Logger.printLog("[SyncBlocker] Error setting skill level: " + e.getMessage());
        }
    }

    private void setSkillXPDirect(IsoPlayer player, String perkName, float xp) {
        try {
            zombie.characters.skills.PerkFactory.Perk perk =
                    zombie.characters.skills.PerkFactory.getPerkFromName(perkName);
            if (perk != null) {
                player.getXp().setXPToLevel(perk, (int) xp);
            }
        } catch (Exception e) {
            Logger.printLog("[SyncBlocker] Error setting skill XP: " + e.getMessage());
        }
    }
}
