# GalaxyRTP

Cross-platform RTP (Random Teleport) and spawn plugin for Velocity + Paper (hub / eu / usa).

- **Paper plugin**: Runs on backend servers (USA, EU). Handles `/rtp`, `/forcertp`, `/rtpconnect`, `/spawn`.
- **Velocity plugin**: Runs on proxy. Forwards plugin messages so players connect to the right server and get RTP'd on arrival.

## How it sends players to /rtp

1. **On hub, EU, or USA**: `/rtp` opens the region menu (`rtp_region_menu` via DeluxeMenus). No hub-only restriction.
2. **Region choice**: Player clicks USA or EU → DeluxeMenus runs `[connect] usa` or `[connect] eu` (BungeeCord/Velocity connect).
3. **Cross-server RTP**: When connecting from hub to a backend, DeluxeMenus uses `/rtpconnect <player> <server> <world_key>`. That command:
   - Sends plugin message `galaxyrtp:main` with `(serverName, worldName)` via `MessageProtocol.encodeConnectRequest()`.
   - Sends BungeeCord `Connect` to move the player to the target server.
4. **Velocity**: Receives the `galaxyrtp:main` message, decodes `(serverName, worldName)`, stores as pending.
5. **Backend (USA/EU)**: On player join, `PendingRtpListener` runs; if there’s a pending RTP for that player, it calls `SafeRtpService.runRtp()` to teleport them into the world.
6. **Same-server RTP**: On USA or EU, the menu uses `/forcertp <world_name>` (e.g. `forcertp eu_main_world`) to RTP the player in the current server’s world.

See [docs/RTP_FLOW.md](docs/RTP_FLOW.md) for the full flow and integration with DeluxeMenus.

## Commands

| Command      | Description                                                 | Permission   |
|-------------|-------------------------------------------------------------|--------------|
| `/rtp`      | Open RTP region menu (all servers)                          | `rtp.use`    |
| `/rtpmenu`  | Open RTP region menu                                        | `rtp.use`    |
| `/rtpconnect <player> <server> <world_key>` | Connect player to server + RTP in world | `rtp.use`    |
| `/forcertp [player] <world_name>` | RTP player into a world on this server      | `rtp.use`    |
| `/spawn`    | Teleport to hub spawn or connect to hub                     | `spawn.use`  |

## Config

Edit `config.yml` for server names, worlds, spawn location, radius overrides, and cooldowns.

## Building

Requires Java 21, Paper API, Velocity API. Build both the Paper and Velocity modules and copy the JARs to your servers.
