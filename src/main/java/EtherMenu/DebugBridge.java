package EtherMenu;

/**
 * Bridge class for stealth debug mode bypass.
 * Instead of setting Core.debug = true (which leaks into network packets),
 * we set this flag and patch Core.getDebug()/isInDebug()/isDebugEnabled()
 * to read from here. The actual Core.debug field stays false.
 */
public class DebugBridge {
    public static volatile boolean debugOverride = false;
}
