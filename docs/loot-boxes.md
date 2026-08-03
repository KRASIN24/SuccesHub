# Loot boxes and the Celestial Cache

SuccessHub loot boxes are earned rewards that remain pending until the user opens them on **The Celestial Cache** (`/loot-boxes`). The backend owns grants, rolls, history, and inventory; the Angular page renders that state and the reveal sequence.

## Lifecycle

```text
task-completion achievement or streak day-close
        |
        v
pending user_loot_box ---> subsequent pending refresh ---> sidebar badge / Cache
        |
        | POST /api/gamification/loot-boxes/{id}/open
        v
3 rolls on a normal first open ---> stacked user_inventory
        |
        v
server-backed Recent History
```

- Task-completion rewards and an explicit close-day response can show an earn toast. The toast does not open a box; it directs the user to the Cache.
- `GET /api/gamification/daily` runs lazy day-close but discards its close result. A streak-milestone box created there may not update the already-loaded badge or show a toast until pending boxes are refreshed.
- A sequential retry after a box reaches `OPENED` returns its stored contents without rerolling or incrementing inventory. Concurrent first-open requests are not serialized; see [Troubleshooting](#duplicate-rewards-after-concurrent-open-requests).
- The Cache loads the box catalog, pending boxes, the last three opened boxes, and the client config together.
- The reveal waits 950 ms, then presents three face-down cards. **Collect** appears after every card is revealed and refreshes pending state and history.

## Active grant sources

| Trigger | Box type | When it runs |
|---------|----------|--------------|
| Achievement unlocked during task completion | `IRON_CHEST` (Treasure Chest) | `GamificationEngineImpl.processTaskCompletion` |
| Streak reaches 7 or 30 | `IRON_CHEST` | Lazy/manual day-close |
| Streak reaches 100 | `SOVEREIGN_VAULT` (Divine Vault) | Lazy/manual day-close |
| Manual dev grant | Caller selects any type | `POST /loot-boxes/grant`, when dev grants are enabled |

Current constraints for boxes granted after the V1 catalog migration:

- `ARCANE_ORB` (Glowing Orb) is available through the manual dev grant only. Its catalog label says “Daily streak qualifier,” but qualifying days do not currently grant it. The migration backfills every pre-existing box as `ARCANE_ORB`, regardless of its original source.
- `WEEKLY_RESET` exists in the backend enum, but no workflow calls it.
- Every achievement granted by task completion uses `IRON_CHEST`; there is no separate rare-achievement mapping.
- Achievements first unlocked during day-close do not currently grant a box. An explicit `POST /close-day` returns them for celebration; lazy close through `GET /daily` discards the close result, so those unlocks are silent.

Treat the box catalog’s `source` strings as display copy, not as proof that a grant workflow is implemented.

## Roll rules and catalog

For a normal, non-concurrent first open, each box makes exactly **three independent rolls**:

1. Select a rarity using the box weights.
2. Select uniformly from rewards of that rarity.
3. Persist the roll and increment the matching inventory stack.

Duplicates are allowed. V1 has no pity timer or guaranteed rarity slot.

| Box type | Common | Rare | Legendary |
|----------|-------:|-----:|----------:|
| `ARCANE_ORB` | 70% | 27% | 3% |
| `IRON_CHEST` | 50% | 44% | 6% |
| `SOVEREIGN_VAULT` | 25% | 60% | 15% |

If the selected rarity has no rewards, the service uses the first non-empty pool in this order: Rare, Common, Legendary. Opening fails with `Reward catalog is empty` only when every pool is empty.

The Liquibase V1 catalog contains:

- one functional `STREAK_SHIELD`, consumed automatically before a missed day resets a streak;
- `DOUBLE_STRIKE` and `SURGE_TOKEN` XP boosts;
- four titles and three avatar frames.

`DOUBLE_STRIKE` and `SURGE_TOKEN` currently have descriptive `effect` text only. The XP engine does not consume or apply them. Titles and frames can be toggled through the inventory API, one equipped item per type, but the current Angular application has no inventory/equip screen and `/profile` is still a placeholder.

## API contract

All endpoints require an authenticated OIDC session and are under `/api/gamification`.

| Method | Path | Behavior and constraints |
|--------|------|--------------------------|
| `GET` | `/config` | Returns `enableLootDevGrants` for the SPA |
| `GET` | `/loot-boxes/types` | Returns the three box types and rarity weights |
| `GET` | `/loot-boxes` | Pending boxes, newest first |
| `GET` | `/loot-boxes/history?limit=3` | Opened boxes with contents, newest first; limit is clamped to 1–10 |
| `POST` | `/loot-boxes/grant` | Body: `{ "boxType": "IRON_CHEST" }`; returns 403 when disabled and 400 for an unknown type |
| `POST` | `/loot-boxes/{id}/open` | Opens an owned box or returns its previously stored contents |
| `GET` | `/inventory` | Returns reward stacks with quantity and equipped state |
| `POST` | `/inventory/{id}/equip` | Toggles titles/frames; functional items are returned unchanged |

Use Swagger UI at http://localhost:8082/swagger-ui/index.html in the `dev` profile for live schemas. The production profile disables Swagger.

## Dev and production behavior

The flag `succeshub.loot.enable-dev-grants` is server-owned and exposed through `GET /config`; there is no Angular build-time environment switch.

| Profile | Flag | Empty Cache CTA |
|---------|------|-----------------|
| `dev` | `true` | Can grant and immediately open the selected box; secondary Summon creates a pending box |
| `prod` | `false` | Disabled until the user has a pending box; manual grant returns 403 |

The property defaults to `false` in `LootProperties`. `application-dev.yaml` explicitly enables it and `application-prod.yaml` explicitly disables it.

## Troubleshooting

### Manual grant returns 403

The backend is running without `succeshub.loot.enable-dev-grants: true`. Start **Spring Boot (SuccesHub)** with the `dev` profile. Do not enable manual grants in production to work around an empty account.

### No Glowing Orb is earned after a qualifying day

This is current behavior. Qualifying days advance the streak; only the 7-, 30-, and 100-day milestones grant boxes. Use a dev grant to test `ARCANE_ORB`.

### An XP boost appears in inventory but has no effect

Only Streak Shield consumption is implemented. `DOUBLE_STRIKE` and `SURGE_TOKEN` are seeded future hooks even though their catalog rows contain effect descriptions.

### Inventory cannot be managed in the UI

The inventory and equip endpoints exist, and the Angular service exposes them, but no current component calls them. The Cache page is limited to opening and history.

### Catalog or column errors after pulling the loot migration

`2026-06-26-01-loot-box-types.xml` adds `box_type` and `effect`, deletes the placeholder reward definitions, and seeds the V1 catalog. The reward foreign keys use `ON DELETE CASCADE`, so this migration also removes inventory stacks and opened-box content that reference the deleted placeholder rows. It also backfills all existing boxes as `ARCANE_ORB`.

Back up a database before applying this migration if its loot history must be preserved. For disposable local data, the safest path is [Reset Dev DB](local-dev.md#clear-the-database-and-re-apply-migrations), then restart Spring Boot so Liquibase builds the catalog from scratch.

### Recent History looks shorter than expected

The Cache requests three opened boxes and flattens at most eight reward entries for display. The API accepts a maximum box limit of 10.

### Duplicate rewards after concurrent open requests

The open service uses an unlocked read-then-write flow. Two simultaneous requests can both observe a `PENDING` box and each persist three rolls before either marks it `OPENED`. The Cache UI serializes its own action, but other clients must not issue concurrent open requests for the same ID. Sequential retries after the first transaction commits are safe.

## Verification

1. Start **Full Stack** with the `dev` profile and sign in as the seeded test user.
2. Open `/loot-boxes`; confirm all three box types and their drop-rate bars load.
3. Select a type and use **Summon a cache (no open)**; confirm the sidebar badge and held count increase.
4. Open the pending cache; reveal all three cards and collect them.
5. Reload the page; confirm the opened rewards remain in Recent History and the box is no longer pending.
6. Start **Spring Boot (SuccesHub) - Prod**; confirm Summon is hidden, an empty Open Cache button is disabled, and a direct grant request returns 403.

Backend unit tests cover the surrounding gamification engine, but CI does not run this workflow against Postgres or Keycloak. See [CI limitations](ci.md#what-ci-does-not-cover-yet).

## Source map

- API: [`GamificationController`](../src/main/java/com/succeshub/appdomain/controller/GamificationController.java)
- Roll, history, and inventory rules: [`LootBoxServiceImpl`](../src/main/java/com/succeshub/appdomain/service/impl/LootBoxServiceImpl.java)
- Box metadata and weights: [`LootBoxType`](../src/main/java/com/succeshub/appdomain/model/LootBoxType.java)
- Grant call sites: [`GamificationEngineImpl`](../src/main/java/com/succeshub/appdomain/service/impl/GamificationEngineImpl.java), [`DailyRitualServiceImpl`](../src/main/java/com/succeshub/appdomain/service/impl/DailyRitualServiceImpl.java)
- Reward catalog migration: [`2026-06-26-01-loot-box-types.xml`](../src/main/resources/db/changelog/2026-06-26-01-loot-box-types.xml)
- Cache UI: [`loot-boxes.component.ts`](../frontend/src/app/features/loot-boxes/loot-boxes.component.ts)
- Shared pending state: [`loot-pending.service.ts`](../frontend/src/app/core/services/loot-pending.service.ts)
