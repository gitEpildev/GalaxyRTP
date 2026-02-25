# Purchasable store items – full documentation

The GalaxyRealmsAPI supports two types of purchasable items for your website store: **vault storage** (AxVaults) and **crate keys** (PhoenixCrates). Use the **list** endpoints to build your store UI; use the **grant** endpoints when a player completes a purchase.

| Type | What players get | Config | List endpoint | Grant endpoint |
|------|------------------|--------|---------------|----------------|
| **Vaults** | Extra AxVaults storage (vault #5–#25) | `vaults` | `GET /api/vault-range` | `POST /api/grant-vault` |
| **Crate keys** | PhoenixCrates keys (6 types) | `crates` | `GET /api/crate-keys` | `POST /api/grant-crate-key` |

All endpoints require `Authorization: Bearer YOUR_SECRET_KEY` or `?key=YOUR_SECRET_KEY`.

---

## 1. Vault storage (AxVaults)

**What it is:** Extra private vaults for the AxVaults plugin. Players open them with `/axvault` or `/vault`. Each vault number is a separate storage (e.g. vault #7 = 54 slots). You sell vaults **#5 through #25** (configurable).

### Config (`config.yml` → `vaults`)

```yaml
vaults:
  min: 5          # Smallest vault number you can sell (inclusive)
  max: 25         # Largest vault number you can sell (inclusive)
```

- Only vault numbers between `min` and `max` can be granted via the API.
- AxVaults uses permissions `axvaults.vault.<number>`. Granting vault 7 adds `axvaults.vault.7`.

### `GET /api/vault-range`

Returns the allowed vault range so your website can build vault products without hardcoding.

**Request:** `GET /api/vault-range`

**Success response (200):**
```json
{
  "success": true,
  "min": 5,
  "max": 25
}
```

**Usage:** Fetch on store load. Create one product per vault number from `min` to `max` (e.g. "Vault 5", "Vault 6", … "Vault 25"), or sell tiers (e.g. "5 vaults" = vault 5, "10 vaults" = vault 10) if using AxVaults `permission-mode: 1`.

### `POST /api/grant-vault`

Grants a specific vault to a player (adds the corresponding LuckPerms permission).

**Request:** `POST /api/grant-vault`  
**Headers:** `Content-Type: application/json`

**Body (JSON):**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `username` | string | One of `username` or `uuid` | Minecraft username |
| `uuid` | string | One of `username` or `uuid` | Player UUID |
| `vault` | number | Yes | Vault number (must be between `vaults.min` and `vaults.max`) |
| `vault_number` | number | Alt. to `vault` | Same as `vault` |

**Examples:**
```json
{ "username": "Steve", "vault": 7 }
```
```json
{ "uuid": "069a79f444e94726a5befca90e38aaf5", "vault": 10 }
```

**Success response (200):**
```json
{
  "success": true,
  "message": "Vault 7 access granted",
  "player": "Steve",
  "uuid": "069a79f444e94726a5befca90e38aaf5",
  "vault": 7,
  "permission": "axvaults.vault.7"
}
```

**Error responses:**

| Status | Cause |
|--------|--------|
| 400 | Missing `username`/`uuid`; missing or invalid `vault`; vault outside range; could not resolve player UUID |
| 401 | Invalid authentication |
| 500 | LuckPerms not available |

**Website flow:**  
1. `GET /api/vault-range` → build vault products (e.g. 5–25).  
2. On purchase → `POST /api/grant-vault` with `username` or `uuid` and `vault`.  
3. Permission is applied immediately; no restart. Player uses `/axvault` in-game.

**AxVaults `permission-mode`** (in AxVaults `config.yml`):
- **`0`:** Each permission = one vault. `axvaults.vault.7` = only vault #7. Use for selling individual vaults (e.g. 5–25).
- **`1`:** Highest includes all lower. `axvaults.vault.10` = vaults #1–#10. Use for tiers like "5 vaults", "10 vaults", etc.

---

## 2. Crate keys (PhoenixCrates)

**What it is:** Keys that open PhoenixCrates crates. Players use keys at crate blocks in-game. You sell **6 key types**, each linked to a crate.

### Purchasable key types (all 6)

| Key identifier | Display name (example) | Crate |
|----------------|------------------------|-------|
| `Op` | Op Crate Key | Op |
| `Cosmic` | Cosmic Crate Key | Cosmic |
| `Galaxy` | Galaxy Crate Key | Galaxy |
| `Nebula` | Nebula Crate Key | Nebula |
| `Supernove` | Supernova Crate Key | Supernova |
| `Vote_Crate` | Vote Crate Key | Asteroid / Vote |

Use the **exact** `key` values in API requests. Fetch the list at runtime via `GET /api/crate-keys`.

### Config (`config.yml` → `crates`)

```yaml
crates:
  keys:
    - Op
    - Cosmic
    - Galaxy
    - Nebula
    - Supernove
    - Vote_Crate
  require-online: true    # If true, player must be online to receive keys (default)
  command: phoenixcrates  # PhoenixCrates command (use "pc" if that's your alias)
```

- Only keys listed under `crates.keys` can be granted.  
- With `require-online: true`, the player must be online to receive keys (physical items).

### `GET /api/crate-keys`

Returns all purchasable crate key identifiers (the 6 types above).

**Request:** `GET /api/crate-keys`

**Success response (200):**
```json
{
  "success": true,
  "keys": ["Op", "Cosmic", "Galaxy", "Nebula", "Supernove", "Vote_Crate"],
  "count": 6
}
```

**Usage:** Fetch on store load. Build one product per `keys` entry (e.g. "Galaxy Crate Key", "Op Crate Key"), then use that same `key` in `POST /api/grant-crate-key` on purchase.

### `POST /api/grant-crate-key`

Grants crate key(s) to a player. Runs `phoenixcrates giveKey <key> <player> <amount>` (or your configured `crates.command`).

**Request:** `POST /api/grant-crate-key`  
**Headers:** `Content-Type: application/json`

**Body (JSON):**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `username` | string | One of `username` or `uuid` | Minecraft username |
| `uuid` | string | One of `username` or `uuid` | Player UUID |
| `key` | string | Yes | Key identifier (must be in `crates.keys`), e.g. `Galaxy`, `Op` |
| `key_id` | string | Alt. to `key` | Same as `key` |
| `amount` | number | No | Number of keys to give (default `1`, min `1`) |

**Examples:**
```json
{ "username": "Steve", "key": "Galaxy", "amount": 1 }
```
```json
{ "uuid": "069a79f444e94726a5befca90e38aaf5", "key": "Op", "amount": 3 }
```

**Success response (200):**
```json
{
  "success": true,
  "message": "Crate key(s) granted",
  "player": "Steve",
  "uuid": "069a79f444e94726a5befca90e38aaf5",
  "key": "Galaxy",
  "amount": 1
}
```

**Error responses:**

| Status | Cause |
|--------|--------|
| 400 | Missing `key`/`key_id`; invalid key (not in `crates.keys`); missing `username`/`uuid`; could not resolve player; player offline when `require-online: true` |
| 401 | Invalid authentication |

**Website flow:**  
1. `GET /api/crate-keys` → use `keys` to build your 6 crate-key products.  
2. On purchase → `POST /api/grant-crate-key` with `username` or `uuid`, `key` (one of the 6), and optional `amount`.  
3. Keys are given in-game shortly after; player uses them at crate blocks.

---

## cURL examples

**List purchasable crate keys:**
```bash
curl -s -H "Authorization: Bearer YOUR_SECRET_KEY" \
  "https://api.galaxyrealms.online/api/crate-keys"
```

**Grant 2 Galaxy keys to player "Steve":**
```bash
curl -s -X POST -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_SECRET_KEY" \
  -d '{"username":"Steve","key":"Galaxy","amount":2}' \
  "https://api.galaxyrealms.online/api/grant-crate-key"
```

**Get vault range:**
```bash
curl -s -H "Authorization: Bearer YOUR_SECRET_KEY" \
  "https://api.galaxyrealms.online/api/vault-range"
```

**Grant vault 12 to player "Steve":**
```bash
curl -s -X POST -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_SECRET_KEY" \
  -d '{"username":"Steve","vault":12}' \
  "https://api.galaxyrealms.online/api/grant-vault"
```

---

## Summary

- **Vaults:** `GET /api/vault-range` → build products; `POST /api/grant-vault` with `vault` (5–25) on purchase.  
- **Crate keys:** `GET /api/crate-keys` → build 6 products; `POST /api/grant-crate-key` with `key` (Op, Cosmic, Galaxy, Nebula, Supernove, Vote_Crate) and optional `amount` on purchase.
