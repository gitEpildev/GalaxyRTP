# GalaxyRealmsAPI Plugin

REST API plugin for Galaxy Realms Minecraft server. Handles rank delivery from the web store.

## API Endpoints

All endpoints require `?key=YOUR_SECRET_KEY` or `Authorization: Bearer YOUR_SECRET_KEY`

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/status` | GET | Server status and player count |
| `/api/check-player` | GET | Check if player is online |
| `/api/get-rank` | GET | Get player's current rank |
| `/api/grant-rank` | POST | Grant rank to player |
| `/api/grant-vault` | POST | Grant AxVaults vault access (axvaults.vault.5–25) |
| `/api/grant-crate-key` | POST | Grant PhoenixCrates crate keys (Op, Galaxy, Cosmic, etc.) |
| `/api/crate-keys` | GET | List purchasable crate key identifiers (all 6 types) |
| `/api/vault-range` | GET | Vault min/max (e.g. 5–25) for store offerings |

## Configuration

Edit `config.yml`:

```yaml
api:
  enabled: true
  port: 8080
  host: "0.0.0.0"
  secret-key: "your-secret-key-here"
  rate-limit:
    enabled: true
    requests-per-minute: 60
```

**Store items (vaults & crate keys):**

```yaml
vaults:
  min: 5
  max: 25

crates:
  keys:
    - Op
    - Cosmic
    - Galaxy
    - Nebula
    - Supernove
    - Vote_Crate
  require-online: true
  command: phoenixcrates
```

See [STORE_ITEMS.md](STORE_ITEMS.md) for full config and API details.

## Building

```bash
cd src
mvn clean package
# JAR output: target/GalaxyRealmsAPI-1.0.jar
```

## Current Setup

- **Internal Port:** 8080 (inside container)
- **Host Port:** 8083 (mapped via Docker)
- **Public URL:** https://api.galaxyrealms.online/api/v2
- **Routing:** Cloudflare Tunnel → localhost:8083

## Credentials

See `API_CREDENTIALS.txt` for the current secret key.

---

## Purchasable store items – full documentation

The API supports two types of purchasable items for your website store:

| Type | What players get | Config section | List endpoint | Grant endpoint |
|------|------------------|----------------|---------------|----------------|
| **Vaults** | Extra AxVaults storage (vault #5–#25) | `vaults` | `GET /api/vault-range` | `POST /api/grant-vault` |
| **Crate keys** | PhoenixCrates keys (6 types) | `crates` | `GET /api/crate-keys` | `POST /api/grant-crate-key` |

Use the **list** endpoints to build your store UI (what you can sell). Use the **grant** endpoints when a player completes a purchase.

**Full documentation:** See **[STORE_ITEMS.md](STORE_ITEMS.md)** for complete request/response specs, config, all 6 crate key types, error codes, and cURL examples.

---

### 1. Vault storage (AxVaults)

For a website page where players buy vault storage (e.g. vaults 5–25):

- **`GET /api/vault-range`** — returns `{ "success": true, "min": 5, "max": 25 }`. Use this to build vault products (e.g. vault 5 … vault 25) without hardcoding.

1. **AxVaults** uses permissions `axvaults.vault.<number>`. Each number = one vault. **Config:** `vaults.min` / `vaults.max` in `config.yml` (default 5–25).

2. **`POST /api/grant-vault`** — grant a vault to a player.

   **Body (JSON):**
   ```json
   { "username": "PlayerName", "vault": 7 }
   ```
   or `{ "uuid": "player-uuid", "vault": 7 }`. Use `vault_number` instead of `vault` if you prefer.

   **Response:**
   ```json
   { "success": true, "message": "Vault 7 access granted", "player": "PlayerName", "uuid": "...", "vault": 7, "permission": "axvaults.vault.7" }
   ```

3. **Website flow:** On purchase, your backend calls `grant-vault` with the player’s username or UUID and the vault number (5–25). The plugin adds the permission via LuckPerms; no server restart needed.

4. **AxVaults `permission-mode`** (in AxVaults `config.yml`):
   - `0`: Each permission = one vault (e.g. `axvaults.vault.7` = vault #7 only). Use for selling individual vaults 5–25.
   - `1`: Highest gives all previous (e.g. `axvaults.vault.10` = vaults 1–10). Use for tiers like “5 vaults”, “10 vaults”, etc.

## Crate store (PhoenixCrates)

For a website page where players buy crate keys (all 6 key types):

**All 6 purchasable crate keys:**

| Key (use in API) | Display name | Crate |
|------------------|--------------|-------|
| `Op` | Op Crate Key | Op |
| `Cosmic` | Cosmic Crate Key | Cosmic |
| `Galaxy` | Galaxy Crate Key | Galaxy |
| `Nebula` | Nebula Crate Key | Nebula |
| `Supernove` | Supernova Crate Key | Supernova |
| `Vote_Crate` | Vote Crate Key | Asteroid / Vote |

- **`GET /api/crate-keys`** — returns `{ "success": true, "keys": ["Op", "Cosmic", "Galaxy", "Nebula", "Supernove", "Vote_Crate"], "count": 6 }`. Use this to build one product per key type without hardcoding.

1. **PhoenixCrates** keys are identified by **key identifier** (e.g. `Galaxy`, `Op`). Your `config.yml` has `crates.keys` listing allowed identifiers.

2. **`POST /api/grant-crate-key`** — grant crate key(s) to a player.

   **Body (JSON):**
   ```json
   { "username": "PlayerName", "key": "Galaxy", "amount": 1 }
   ```
   or `{ "uuid": "player-uuid", "key": "Galaxy", "amount": 2 }`. Use `key_id` instead of `key` if you prefer. `amount` is optional (default 1).

   **Response:**
   ```json
   { "success": true, "message": "Crate key(s) granted", "player": "PlayerName", "uuid": "...", "key": "Galaxy", "amount": 1 }
   ```

3. **Website flow:** On purchase, your backend calls `grant-crate-key` with the player’s username or UUID, the key identifier (from `crates.keys`), and optional amount. The plugin runs `phoenixcrates giveKey <key> <player> <amount>`; keys are given in‑game.

4. **Config:** `crates.require-online: true` (default) means the player must be online to receive keys, since PhoenixCrates gives physical key items to their inventory. Set to `false` only if your setup supports offline key delivery. `crates.command` is the PhoenixCrates command (default `phoenixcrates`; use `pc` if that’s your alias).
