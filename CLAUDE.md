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

## Code style

- Default to no comments. Only add one when the *why* is non-obvious (a hidden invariant, a save-format constraint, a workaround for a specific Android quirk).
- Don't add backwards-compatibility shims when refactoring code that has no users yet. The save-data rules above are the one exception — those *do* have users.
- Prefer editing existing files over creating new ones.
