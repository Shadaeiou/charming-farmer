# CLAUDE.md

Project-specific rules for Claude Code working on this repo. Read this before making any changes.

## Branching

- **Always commit and push directly to `main`.** Never create feature branches; the user can't trigger CI from anything else.
- The `Build Android` workflow only fires on `push` to `main` (or `v*` tags). A push to any other branch produces no APK.
- Don't open pull requests — just push.

## Save data must survive every update

Persistence lives in **Room** ([`data/room/AppDatabase.kt`](android/app/src/main/java/com/shadaeiou/charmingfarmer/data/room/AppDatabase.kt), database file `charming-farmer.db`). The legacy SharedPreferences blobs (`charming-farmer-v1`, `charming-farmer-transport-v1`) are still on disk for one release as a rollback safety net, but the source of truth is now the Room schema. `FarmGame` and `TransportService` both read/write via DAOs.

Players have farms with hours of real progress sitting in these tables. Treat the schema like a public API.

**Hard rules:**

- **Never rename or drop a column without a Room migration.** Bump `AppDatabase.version` and supply a `Migration` that ALTER TABLEs the existing data into the new shape. Never just edit the entity in place — that crashes on every existing install.
- **Adding a column is OK** if you provide a `defaultValue` in the `@ColumnInfo` annotation OR ship the migration that ALTERs it in. Don't rely on Kotlin defaults; SQLite needs the value at the column level.
- **Adding a table is OK** with a `CREATE TABLE` migration. Forgetting the migration crashes the next launch.
- **Renaming an enum value** (e.g. `CropType.CARROT` → `CropType.ROOT_VEGGIE`) silently breaks every plot that contains the old name. If it has to happen, write a Room migration that runs `UPDATE plots SET crop = 'ROOT_VEGGIE' WHERE crop = 'CARROT'`. Don't just rely on `valueOf()` failing into a default.
- **Removing a `CropType` / `TreeType` / `ItemType` entirely** orphans every row that references it. Decoders already revert orphaned plots/stacks defensively, but a migration that scrubs the dead rows is the clean version.
- **Never wipe user data without a clear "fresh start" gesture.** `FarmGame.reset()` is the only legit erase path, and even it leaves `inventory_stacks` alone because silos belong to the location, not the farm save.

When in doubt: load, don't crash, never wipe.

### Adding a Room migration (cookbook)

The schema is currently at **v3** with `MIGRATION_1_2` (added `kiln_runs`) and `MIGRATION_2_3` (added `brew_batches`). To go to v4:

```kotlin
// 1. Bump version + add the new entity to the @Database annotation
@Database(entities = [..., NewEntity::class], version = 4, exportSchema = false)

// 2. Define the migration
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS new_table (...)")
        // or ALTER TABLE existing_table ADD COLUMN new_col ...
    }
}

// 3. Add it to the builder chain
Room.databaseBuilder(...)
    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
    .build()
```

**Always keep all prior migrations in `addMigrations()`.** Players can be on any old version; Room walks the chain from where they are.

If you ever truly cannot migrate (catastrophic schema change), gate it behind the `system_meta` table and write a Kotlin-level migration that reads old rows and writes new ones before dropping the old table. Never call `fallbackToDestructiveMigration()` — that drops player data.

### Persistence is 100% Room

Game data, UI state (last-visited screen), debug toggles — everything lives in Room. **Don't introduce new `SharedPreferences` files.** The only remaining `SharedPreferences` reads in the codebase are inside [`LegacyMigrator`](android/app/src/main/java/com/shadaeiou/charmingfarmer/data/room/LegacyMigrator.kt), which copies the original pre-Room blobs over once.

If you need a one-key-value pair, add it to `system_meta` with a typed key constant in the file that owns the data.

### Legacy SharedPreferences

- [`LegacyMigrator`](android/app/src/main/java/com/shadaeiou/charmingfarmer/data/room/LegacyMigrator.kt) runs once on first Room load and copies the old prefs blobs into Room. It's idempotent (gated by `system_meta.migrated_from_prefs`) so leaving it in place forever is fine.
- The original prefs files are not deleted. After we ship a release or two with Room stable on real devices, delete the prefs files in a future commit.
- **Do not write to those prefs keys anymore.** Anything new lives in Room.

## Changelog

Every player-visible change adds a `ReleaseNote(...)` to the **top** of the `CHANGELOG` list in [`data/Changelog.kt`](android/app/src/main/java/com/shadaeiou/charmingfarmer/data/Changelog.kt). The Settings screen renders the first entry expanded; older entries hide behind "Show all updates".

### Version label

The label **must** be `"0.1.<N>"` where `<N>` is the `versionCode` the build will produce *for the commit you're about to make*. That number is `git rev-list --count HEAD` **+ 1** at the moment you commit.

```
# Run this *before* committing — the result is the version label to use
echo "0.1.$(($(git rev-list --count HEAD) + 1))"
```

Why this matters:
- [`build.gradle.kts`](android/app/build.gradle.kts) hard-codes `versionName = "0.1.$gitCommits"`. The Settings screen shows that exact string. If your changelog says `0.2.0` but the build ships `0.1.6`, players see two different version numbers in the same app — confusing and wrong.
- **Never use a different minor (`0.2.x`, `1.0.0`, etc.) without first changing the `versionName` template** in `build.gradle.kts` to match. They are coupled.
- A commit that fails CI (lint error, signing failure, etc.) still increments the local count but doesn't ship a release. That means a version number can be "skipped" — e.g. `0.1.8` failed lint, `0.1.9` is the actual fix. That's fine: the changelog should label the *shipping* version, not the failed attempt.

### Other fields

- `date` is `YYYY-MM-DD` of the commit day.
- `bullets` are short, player-facing, present-tense. ✓ "Adds tomato seeds" — ✗ "Refactor CropType enum to include tomato".
- Group related bullets in one entry. Don't split one feature across multiple `ReleaseNote`s.

### When to add an entry

| Change type | Add entry? |
|---|---|
| New crop, upgrade, or game mechanic | Yes |
| Balance tweak (cost/grow time/sell price changes) | Yes |
| Bug fix the player would notice | Yes |
| UI/layout fix the player would notice | Yes |
| Pure refactor, dependency bump, comment change | No |
| Build/CI fix that doesn't change app behavior | No |
| Editing this CLAUDE.md or README.md | No |
| Fixing a typo in a previous changelog entry | No (just edit it in place) |

### Never

- **Never delete or reorder past entries.** They're a permanent record of what shipped. If a version label was wrong, edit it in place — but the existence and ordering of entries is sacrosanct, since players see them.
- **Never use a fabricated version number** (e.g. picking `0.2.0` to mean "big release"). The number is mechanical: it's the git commit count.

## Versioning

`versionCode` and `versionName` are computed from `git rev-list --count HEAD` in [`build.gradle.kts`](android/app/build.gradle.kts). **Never set them by hand.** Every commit bumps the version automatically; that's how the in-app updater knows there's something newer.

## Don't change

- **`applicationId`** (`com.shadaeiou.charmingfarmer`) — Android treats a different applicationId as a different app, so changing it makes the updater silently install alongside the old version instead of upgrading it.
- **The release keystore.** Signed by a different key = Android refuses to install the update over the existing app. There is no recovery path.

## Secrets

- `release.jks` — never commit (already in `.gitignore`).
- `FCM_SERVICE_ACCOUNT_JSON` — never paste anywhere except the GitHub Actions secret. This one *can* send pushes as the project.
- `google-services.json` is **safe to commit** — it's a public Firebase identifier, not an authentication credential.

## Build verification

Local sandbox can't install the Android SDK (no internet). Don't try `./gradlew assembleDebug` — it'll fail. Trust the code, push to `main`, watch CI in the GitHub Actions tab.

## Architecture patterns

These have settled across the codebase. Match them when adding new features.

### Service singleton

Every app-wide game service uses this shape:

```kotlin
class Brewery private constructor(appContext: Context) {
    private val db = AppDatabase.get(appContext).also {
        LegacyMigrator.migrateIfNeeded(appContext, it)
    }
    // ... mutable state, public API ...

    companion object {
        @Volatile private var instance: Brewery? = null
        fun get(context: Context): Brewery {
            val existing = instance
            if (existing != null) return existing
            return synchronized(this) {
                instance ?: Brewery(context.applicationContext).also { instance = it }
            }
        }
    }
}
```

Examples already in the tree: `FarmGame`, `TransportService`, `Malthouse`, `Brewery`, `DebugSettings`. Any new game subsystem (Apiary, Kitchen, Cellar, ...) should follow this.

The `LegacyMigrator.migrateIfNeeded()` call in `init` is **mandatory** — it's the safety net that ensures the migration runs no matter which screen the player lands on first.

### Composables get state via `remember { Service.get(ctx) }`

Each screen (e.g. `BreweryScreen`, `MalthouseScreen`) does:

```kotlin
val game = remember { FarmGame(ctx.applicationContext) }
val transport = remember { TransportService.get(ctx.applicationContext) }
val brewery = remember { Brewery.get(ctx.applicationContext) }
```

`FarmGame` is a `class` (not singleton); it's safe to instantiate per-screen because it only owns `mutableStateOf` of `FarmState`, which all reads from Room. Other services are singletons because they own collections that need to stay coherent across screens (in-flight trips, brew batches, etc.).

For collection state held outside Compose (e.g. `mutableStateListOf<Trip>` inside `TransportService`), expose a `var revisionTick: Int by mutableStateOf(0)` that you `bump()` after every mutation. Composables read `revisionTick` at the top of the function (or annotate with `@Suppress("UNUSED_EXPRESSION") service.revisionTick` so it's tracked but the value is ignored). That's the recomposition trigger.

### Universal quality model

Every produced or processed item — crops in the silo, malt at the malthouse, bottled beer at the brewery, eventually cooked dishes and honey jars — is an `ItemStack(type, quantity, score, tier, createdMs)`. Don't invent a new "thing" data class for a new feature; use ItemStack and add a new entry to `ItemType`.

Quality math:
- **Score (0-100)** is the fine-grained quality. Bucketed by `ItemGrade` (S/A/B/C/D/F) for display.
- **Tier (NORMAL/MEGA/GOLDEN/PERFECT)** is the rare-roll modifier. Roll on harvest via `ItemTier.roll()`.
- Recipes that consume multiple ItemStacks should compute their output via `computeOutputScore(inputAverage, skillBonus, equipmentCap)` — the standard formula already used by `Malthouse` and `Brewery`.
- Sale price scales as `basePrice × (score/100)² × tier.priceMultiplier` via `ItemStack.unitSellPrice()`.

If a feature needs to track ingredient tier through processing (a "championship" pumpkin pie), pass the LOWEST tier of any input through to the output. Garbage in, garbage out — even for tier rolls.

### Inventories live in `TransportService`

`TransportService` owns a `Map<Location, Inventory>`. Adding to or removing from an inventory is `transport.addToInventory(loc, stack)` or `transport.setInventory(loc, newInv)`. **Don't keep parallel inventory state in your own service** — that's how shipping arrives at a destination that already has stale data.

When pulling from an inventory in a recipe, use `inventory.removeBest(type, qty)` — that takes top-quality stacks first, which matches "the player puts their best stuff into the brew."

### Routing rules

`Location.accepts(item: ItemType): Boolean` is the single answer for "what can ship where". When you add a new `ItemType` or `Location`, edit this method to declare what makes sense:

```kotlin
fun Location.accepts(item: ItemType): Boolean = when (this) {
    FARM -> true                                        // catch-all return
    MALTHOUSE -> item in MALTING_GRAINS                // raw grains only
    BREWERY -> item.name.startsWith("MALT_") || ...    // brewing inputs
    KITCHEN -> false                                    // not built yet
    MARKET -> true                                      // sells anything
    CELLAR -> item.name.startsWith("BEER_")            // beer ages here
}
```

The transport panel uses this to filter destination buttons per item, so the player never sees a "send hops to malthouse" option.

### Honor `DebugSettings.skipTimers` everywhere

Any new mechanic with a real-time clock — bird spawn intervals, fishing bites, brew stages, transport trips, kiln runs, plot growth — must short-circuit when `DebugSettings.skipTimers == true`. Pattern:

```kotlin
fun isComplete(nowMs: Long): Boolean =
    DebugSettings.skipTimers || nowMs - startMs >= durationMs
```

It's a static singleton; no plumbing needed. The Settings screen toggle drives play-testing.

## Adding a new atlas destination

When you add a new playable location, touch all of these in one commit:

1. `Location` enum — new entry with display name + emoji
2. `Location.accepts()` — declare what cargo can come in
3. New `ServiceName.kt` data class following the singleton pattern above
4. New Room entity + DAO + migration if it needs to persist anything beyond `system_meta` keys
5. New `*Screen.kt` Composable
6. `MapScreen` — add a `Destination(...)` entry with `unlocked = true`
7. `MapScreen` — add `onGoToX: () -> Unit` parameter and threaded callback
8. `MainActivity` — add `composable("x") { ... }` route + `onGoToX` callback that calls `saveLastDest("x")`
9. Changelog entry — yes, this is player-visible

Forgetting #2 makes the transport panel show no destinations from this location. Forgetting #8 means tapping the tile crashes navigation.

## Compose pitfalls that have bitten me

I can't run a local build, so these cost a CI cycle each. Watch for them:

- **Missing imports for inline-qualified Compose APIs.** Writing `androidx.compose.material.icons.Icons.Filled.LocalShipping` doesn't auto-resolve — `LocalShipping` is an extension property on `Icons.Filled` from the `material-icons-extended` artifact, and it requires `import androidx.compose.material.icons.filled.LocalShipping` to be visible. Same shape for any `Icons.Filled.*` you haven't used in this file before. Always add the explicit import.
- **`var foo by mutableStateOf(...)` + `fun setFoo(...)` collide on JVM.** The property auto-generates a `setFoo(Z)V` setter; a hand-written function with the same name produces "Platform declaration clash" at compile time. Fix: name the function `updateFoo`, `applyFoo`, or anything that isn't `set<Property>`.
- **Adding a new Compose API call to a file means checking imports.** The `remember`, `mutableStateOf`, `LaunchedEffect`, `DisposableEffect`, `mutableLongStateOf`, `mutableStateListOf` set drifts per file. When I add the first call of one of those to a screen, I need to verify it's imported.

## Code style

- Default to no comments. Only add one when the *why* is non-obvious (a hidden invariant, a save-format constraint, a workaround for a specific Android quirk).
- Don't add backwards-compatibility shims when refactoring code that has no users yet. The save-data rules above are the one exception — those *do* have users.
- Prefer editing existing files over creating new ones.
