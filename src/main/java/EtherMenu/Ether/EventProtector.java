package EtherMenu.Ether;

import EtherMenu.GameClientWrapper;
import EtherMenu.utils.Logger;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.network.GameClient;
import zombie.network.packets.character.PlayerPacket;
import zombie.network.PacketTypes;
import zombie.network.ZomboidNetData;

import java.util.*;

/**
 * EventProtector - Prevents anti-cheat from monitoring login/logout events
 *
 * PZ Build 42.16 compatibility notes:
 *  - PacketTypes.PacketType.Validate no longer exists. The old single-packet
 *    validation was replaced by the layered pipeline:
 *    PacketAuthorization (capability check) -> INetworkPacket.isConsistent
 *    (structure check) -> per-packet AntiCheat[] (behaviour checks), plus the
 *    Checksum packet (Lua/script/anim integrity) and per-connection
 *    PacketValidator counters.
 *  - IsoPlayer no longer has a "connected" field and UdpConnection no longer
 *    has a "validated" field. Connection state is GameClient.connection != null;
 *    checksum state lives in UdpConnection.checksumState.
 */
public class EventProtector {
    private static EventProtector instance;
    private final GameClientWrapper wrapper;
    private final Map<String, Long> lastChecks = new HashMap<>();
    private final Set<String> protectedEvents = new HashSet<>();
    private static final long CHECK_COOLDOWN = 2000; // 2 seconds cooldown between checks
    private final SafeAPI safeAPI;

    private EventProtector() {
        this.wrapper = GameClientWrapper.get();
        this.safeAPI = SafeAPI.getInstance();
    }

    public static EventProtector getInstance() {
        if (instance == null) {
            instance = new EventProtector();
        }
        return instance;
    }

    public void handlePacket(String command, Map<String, Object> data) {
        try {
            String username = IsoPlayer.getInstance().getUsername();
            if (!shouldProcessPacket(username)) {
                return;
            }

            switch (command) {
                case "join_request":
                    handleJoinRequest(data);
                    break;
                case "heartbeat_request":
                    handleHeartbeatRequest(data);
                    break;
            }
        } catch (Exception e) {
            Logger.printLog("Error handling packet: " + e.getMessage());
        }
    }

    private boolean shouldProcessPacket(String username) {
        long now = System.currentTimeMillis();
        Long lastCheck = lastChecks.get(username);
        if (lastCheck == null || (now - lastCheck) > CHECK_COOLDOWN) {
            lastChecks.put(username, now);
            return true;
        }
        return false;
    }

    private void handleJoinRequest(Map<String, Object> data) {
        String serverFragment = (String)data.get("message");
        String responseFragment = safeAPI.generateResponseKey(serverFragment);
        // Send response through GameClient
    }

    private void handleHeartbeatRequest(Map<String, Object> data) {
        String currentKey = (String)data.get("message");
        if (!safeAPI.verifyHeartbeat(currentKey)) {
            Logger.printLog("Invalid heartbeat key detected");
        }
    }

    /**
     * Protects against event detection and handles packet interception

    public boolean handlePacket(ByteBuffer buffer, ZomboidNetData data) {
        try {
            // Intercept and potentially modify packet data
            preprocessPacket(buffer, data);

            // Use wrapper to safely call GameClient methods
            if (!wrapper.gameLoadingDealWithNetData(data)) {
                wrapper.mainLoopDealWithNetData(data);
            }

            return true;
        } catch (Exception e) {
            Logger.printLog("Error handling packet: " + e.getMessage());
            return false;
        }
    }

    private void preprocessPacket(ByteBuffer buffer, ZomboidNetData data) {
        // Add any packet preprocessing/cleaning here
        if (buffer != null && data != null) {
            // Example: Clean certain packet types
            switch(data.type) {
                case Validate:
                    cleanValidatePacket(buffer);
                    break;
                case PlayerConnect:
                    cleanPlayerConnectPacket(buffer);
                    break;
                // Add other cases as needed
            }
        }
    }

    private void cleanValidatePacket(ByteBuffer buffer) {
        // Clean validation packets
        try {
            // Add validation packet cleaning logic
        } catch (Exception e) {
            Logger.printLog("Error cleaning validate packet: " + e.getMessage());
        }
    }

    private void cleanPlayerConnectPacket(ByteBuffer buffer) {
        // Clean player connect packets
        try {
            // Add player connect packet cleaning logic
        } catch (Exception e) {
            Logger.printLog("Error cleaning player connect packet: " + e.getMessage());
        }
    }*/

    private void initializeProtectedEvents() {
        protectedEvents.add("OnPlayerConnect");
        protectedEvents.add("OnPlayerDisconnect");
        protectedEvents.add("OnCreatePlayer");
        protectedEvents.add("OnLogin");
        protectedEvents.add("OnLoginState");
        protectedEvents.add("OnLoginStateSuccess");
        protectedEvents.add("OnCharacterConnect");
        protectedEvents.add("OnCoopClientConnect");
        protectedEvents.add("OnGameStart");
        protectedEvents.add("OnCreateLivingCharacter");
        protectedEvents.add("OnClientCommand");
        protectedEvents.add("OnServerCommand");
    }

    public void installProtection() {
        try {
            // Clear any pending network data
            wrapper.clearIncomingNetData();

            // NOTE (B42.16): onlineId is assigned by the server during the
            // CreatePlayer/ConnectedPacket handshake. Overwriting it locally with a
            // random value desynchronizes the player<->connection mapping, so it
            // must not be touched here. The IsoPlayer "connected" field no longer
            // exists either.

            // Initialize protected state
            initializeProtectedState();
        } catch (Exception e) {
            Logger.printLog("Failed to install protection: " + e.getMessage());
        }
    }

    private void initializeProtectedState() {
        try {
            if (GameClient.connection != null) {
                // NOTE (B42.16): UdpConnection no longer has a "validated" field.
                // Checksum state is tracked in UdpConnection.checksumState and is
                // driven by the Checksum packet handshake, not by a local flag.
                wrapper.clearIncomingNetData();
            }
        } catch (Exception e) {
            Logger.printLog("Error initializing protected state: " + e.getMessage());
        }
    }

    private void sendFakePlayerUpdate(IsoPlayer player) {
        try {
            if (GameClient.connection != null) {
                PlayerPacket packet = new PlayerPacket();
                if (packet.set(player) != null) {
                    ByteBufferWriter writer = GameClient.connection.startPacket();
                    PacketTypes.PacketType.PlayerUpdateReliable.doPacket(writer);
                    packet.write(writer);
                    PacketTypes.PacketType.PlayerUpdateReliable.send(GameClient.connection);
                }
            }
        } catch (Exception e) {
            Logger.printLog("Error sending player update: " + e.getMessage());
        }
    }

    private short generateSafeID() {
        return (short) (Math.abs(new Random().nextInt()) % 10000 + 1);
    }

    public boolean shouldBlockEvent(String eventName) {
        if (!protectedEvents.contains(eventName)) {
            return false;
        }

        long now = System.currentTimeMillis();
        Long lastCheck = lastChecks.get(eventName);
        if (lastCheck == null || (now - lastCheck) > CHECK_COOLDOWN) {
            lastChecks.put(eventName, now);
            return true;
        }

        return false;
    }

    // Modified method to hook into network packets
    /**
     * Observe incoming packets without dropping any of them.
     *
     * B42.16: this used to drop Login / PlayerConnect / PlayerUpdateReliable (and
     * the since-removed Validate) packets. Those packets are REQUIRED on the
     * client to complete the login handshake, the Checksum exchange and the 30s
     * player-update keepalive (PacketValidator.playerUpdateTimeout). Dropping
     * them caused immediate desync/disconnect, so this is observe-only now.
     */
    public static void filterIncomingPackets() {
        try {
            GameClientWrapper wrapper = GameClientWrapper.get();
            ArrayList<ZomboidNetData> netData = wrapper.getIncomingNetData();

            if (netData == null) return;

            int login = 0, playerConnect = 0, playerUpdate = 0;
            for (ZomboidNetData packet : netData) {
                if (packet == null || packet.type == null) continue;

                short packetId = packet.type.getId();
                if (packetId == PacketTypes.PacketType.Login.getId()) {
                    login++;
                } else if (packetId == PacketTypes.PacketType.PlayerConnect.getId()) {
                    playerConnect++;
                } else if (packetId == PacketTypes.PacketType.PlayerUpdateReliable.getId()) {
                    playerUpdate++;
                }
            }

            if (login + playerConnect + playerUpdate > 0) {
                Logger.printLog("Observed incoming packets: Login=" + login
                        + " PlayerConnect=" + playerConnect
                        + " PlayerUpdateReliable=" + playerUpdate);
            }
        } catch (Exception e) {
            Logger.printLog("Error observing packets: " + e.getMessage());
        }
    }

    // Call this method periodically to maintain protection
    public void maintain() {
        filterIncomingPackets();

        // Use wrapper to handle player updates
        IsoPlayer player = IsoPlayer.getInstance();
        if (player != null) {
            try {
                // B42.16: IsoPlayer no longer has a "connected" field.
                // Connection state is derived from GameClient.connection.
                if (GameClient.connection != null) {
                    sendFakePlayerUpdate(player);
                }
            } catch (Exception e) {
                Logger.printLog("Error maintaining connection: " + e.getMessage());
            }
        }
    }
}