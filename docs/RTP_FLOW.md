# GalaxyRTP – How players get sent to /rtp

## Overview

GalaxyRTP works with **Velocity** (hub) and **Paper** (USA, EU backends). Players on hub use `/rtp` to choose a region (USA or EU), get connected to the backend, and are RTP’d into the chosen world (Overworld, Nether, End).

---

## Flow 1: /rtp on Hub (region choice)

1. Player runs `/rtp` on hub.
2. `RtpCommand` checks:
   - Sender is a player.
   - Has `rtp.use`.
   - Current server is hub (`PaperConfig.isHub()`).
3. If hub: `Bukkit.dispatchCommand(console, "dm open rtp_region_menu " + player.getName())` – opens DeluxeMenus region menu.
4. Region menu shows USA and EU. Player clicks one.
5. DeluxeMenus runs:
   - `[connect] usa` or `[connect] eu` – connects player to USA/EU backend via BungeeCord/Velocity.

---

## Flow 2: Cross-server RTP (hub → backend)

When a player on hub chooses USA or EU, the menu can use `/rtpconnect` instead of `[connect]` to both connect them **and** schedule an RTP on arrival.

1. DeluxeMenus runs: `/rtpconnect <player> <server> <world_key>`  
   Example: `/rtpconnect PlayerName eu overworld`
2. `RtpConnectCommand`:
   - Validates `server` (usa/eu) and `world_key` (overworld/nether/end).
   - Resolves `world_name` from config (e.g. `overworld` → `eu_main_world`).
   - Checks target online, cooldown.
3. Sends two plugin messages:
   - **`galaxyrtp:main`**: `MessageProtocol.encodeConnectRequest(serverName, worldName)` → bytes `(server, world)`.
   - **`BungeeCord`**: `Connect` + `serverName` – moves player to that server.
4. **Velocity** `RtpVelocityPlugin.onPluginMessageFromBackend`:
   - Receives `galaxyrtp:main`.
   - Decodes `(serverName, worldName)`.
   - Forwards the message to the **target server** when the player connects there (via Velocity forwarding).
5. **Paper backend** `RtpMessageListener`:
   - Receives the forwarded message.
   - Calls `PendingRtpListener.addPending(plugin, playerUuid, worldName)`.
6. **Paper backend** `PendingRtpListener.onJoin`:
   - On `PlayerJoinEvent`, after 20 ticks, checks for pending RTP for that player.
   - If found: `SafeRtpService.runRtp(plugin, player, worldName)` – teleports player into the world.

---

## Flow 3: Same-server RTP (already on USA or EU)

When the player is already on USA or EU, the menu uses `/forcertp` to RTP them in the current server.

1. DeluxeMenus runs: `[player] forcertp eu_main_world` (or `Spawn-Lobby`, `eu_main_world_nether`, etc.).
2. `ForceRtpCommand`:
   - If 1 arg and sender is player: `target = sender`, `worldName = args[0]`.
   - If 2 args: `target = Bukkit.getPlayerExact(args[0])`, `worldName = args[1]`.
3. Resolves `World` from `worldName` (fallback to `world` or first world).
4. `SafeRtpService.runRtp(plugin, target, world.getName())` – finds a safe spot and teleports.

---

## Plugin message protocol

Channel: `galaxyrtp:main`

**Connect request** (hub → Velocity → backend):

- `MessageProtocol.encodeConnectRequest(serverName, worldName)`:
  - `DataOutputStream`: `writeUTF(serverName)`, `writeUTF(worldName)`.

**RTP pending** (backend → Velocity → backend):

- `MessageProtocol.encodeRtpPending(playerUuid, worldName)`:
  - `DataOutputStream`: `writeUTF(uuid.toString())`, `writeUTF(worldName)`.

---

## DeluxeMenus integration

- **`rtp_region_menu.yml`**: Opens on `/rtp`, `/rtpmenu`, `/rtppickregion`. USA/EU buttons run `[connect] usa` or `[connect] eu`.
- **`rtp_world_menu.yml`**: Opens on `/rtp_world`. Overworld/Nether/End buttons run `[player] forcertp <world_name>` (e.g. `forcertp eu_main_world`).
- Region menus can use `/rtpconnect` when sending from hub to backend so the player is RTP’d on arrival.

---

## Config mapping

`config.yml`:

- `server-names`: hub, eu, usa.
- `worlds`: `key` (overworld/nether/end) → `world-name` per server (e.g. `eu_main_world`).
- `servers`: per-server `default-radius`, `group-radius-overrides` (e.g. vip: 5000).
- `spawn`: hub spawn location.
- `cooldown`: default and fast cooldown seconds.
