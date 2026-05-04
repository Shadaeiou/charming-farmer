# Charming Farmer

A tiny Android farming sim built around one simple idea: **energy is the only currency that matters.** Energy refills slowly while you wait. You spend it on the farm. Coins are just a way to remember that you spent your energy well.

## How it plays

You get a 4×4 farm. Each plot starts as grass. Tap to interact:

| Plot is... | Tap does | Costs |
|---|---|---|
| Grass | Tills the soil | ⚡3 |
| Tilled | Plants the selected seed | 🪙 + ⚡ for that crop |
| Growing | Waters it (one-time, shaves 30% off remaining time) | ⚡1 |
| Ready | Harvests the crop into coins | ⚡1 |

Energy regenerates over real time, even when the app is closed — bring back a full battery after a coffee break.

### Crops

| Crop | Seed cost | Plant ⚡ | Grow time | Sells for |
|---|---|---|---|---|
| 🥕 Carrot | 3 | 2 | 20s | 8 |
| 🌾 Wheat | 8 | 3 | 1m | 22 |
| 🍅 Tomato | 18 | 4 | 2m | 55 |
| 🎃 Pumpkin | 40 | 6 | 5m | 140 |

### Upgrades

Sink coins into your farmer:
- **Bigger Lungs** — +10 max energy per level
- **Strong Coffee** — −15% regen time per level (floor 0.8s/⚡)

Both scale up in price, so the more you buy, the more carrots you'd better grow.

## Stack

Built on the [Shadaeiou/android-template](https://github.com/Shadaeiou/android-template) scaffold:

- Kotlin 2.0.21, Jetpack Compose (BOM 2024.10.01), Material 3
- compileSdk 35, minSdk 26, targetSdk 35
- Auto-versioning from git commit count, signed release APKs via GitHub Actions
- In-app updater that pulls the latest signed APK from this repo's GitHub Releases
- Optional FCM push that nudges installed clients when a new release ships
- Game state persisted to `SharedPreferences` — your farm survives reboots

## Build

```bash
cd android
./gradlew :app:assembleDebug      # debug build
./gradlew :app:installDebug       # install on connected device
```

Releases publish via the `Build Android` workflow on every push to `main`. See the original [android-template README](https://github.com/Shadaeiou/android-template) for keystore + Firebase setup.

## Layout

| Where | What |
|---|---|
| [`data/FarmGame.kt`](android/app/src/main/java/com/shadaeiou/charmingfarmer/data/FarmGame.kt) | All game state, actions, persistence |
| [`ui/HomeScreen.kt`](android/app/src/main/java/com/shadaeiou/charmingfarmer/ui/HomeScreen.kt) | The farm itself — grid, seed shelf, upgrades |
| [`ui/Theme.kt`](android/app/src/main/java/com/shadaeiou/charmingfarmer/ui/Theme.kt) | Warm farm palette (light & dark) |
| [`ui/SettingsScreen.kt`](android/app/src/main/java/com/shadaeiou/charmingfarmer/ui/SettingsScreen.kt) | Updater, version, changelog |
