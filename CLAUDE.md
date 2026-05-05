# CLAUDE.md

Project-specific rules for Claude Code working on this repo. Read this before making any changes.

## Branching

- **Always commit and push directly to `main`.** Never create feature branches; the user can't trigger CI from anything else.
- The `Build Android` workflow only fires on `push` to `main` (or `v*` tags). A push to any other branch produces no APK.
- Don't open pull requests — just push.

## Save data must survive every update

The game persists state to `SharedPreferences` under key `charming-farmer-v1` in [`FarmGame.kt`](android/app/src/main/java/com/shadaeiou/charmingfarmer/data/FarmGame.kt). Players have farms with hours of progress in there. Treat the save format like a public API.

**Hard rules:**

- **Never rename the prefs key** (`charming-farmer-v1`). A rename = every user starts from scratch.
- **Never delete or rename existing JSON fields** when persisting (`energy`, `coins`, `plots[].kind`, etc.). `FarmGame.save()` and `FarmGame.load()` must stay round-trip compatible.
- **Adding new fields is fine** — but `load()` must default them sensibly via `optInt` / `optLong` / `optString(...).takeIf { ... }` so old saves without that field still load.
- **Renaming an enum value** (e.g. `CropType.CARROT` → `CropType.ROOT_VEGGIE`) silently breaks every save that contains the old name. If it has to happen, write a migration: read the old name in `load()`, map it to the new one. Don't rely on `valueOf()` alone.
- **Removing a `CropType` entirely** orphans every plot planted with it. Migrate those plots to `Plot()` (empty grass) inside `load()`, don't crash.
- **If you ever bump the prefs key intentionally** (rare, only for a deliberately destructive schema change), add migration code that reads the old key and writes the new one before clearing — never just drop user data.

When in doubt: load, don't crash, never wipe.

## Changelog

Every user-visible change updates [`data/Changelog.kt`](android/app/src/main/java/com/shadaeiou/charmingfarmer/data/Changelog.kt). Add a new `ReleaseNote(...)` entry to the **top** of the `CHANGELOG` list — the Settings screen renders the first entry expanded.

- `version` should be `"0.1.<N>"` where `<N>` matches the upcoming git commit count (i.e. `git rev-list --count HEAD` *after* your commit). The build's `versionName` derives from the same number, so they line up on installed devices.
- `date` is `YYYY-MM-DD`.
- `bullets` are short, player-facing, present-tense. "Adds tomato seeds" not "Refactor CropType enum to include tomato".
- Bug fixes, balance tweaks, and new mechanics all go in. Internal refactors that change nothing visible to the player don't need an entry.

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

## Code style

- Default to no comments. Only add one when the *why* is non-obvious (a hidden invariant, a save-format constraint, a workaround for a specific Android quirk).
- Don't add backwards-compatibility shims when refactoring code that has no users yet. The save-data rules above are the one exception — those *do* have users.
- Prefer editing existing files over creating new ones.
