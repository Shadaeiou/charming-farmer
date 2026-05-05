package com.shadaeiou.charmingfarmer.data

data class ReleaseNote(
    val version: String,
    val date: String,
    val bullets: List<String>,
)

val CHANGELOG: List<ReleaseNote> = listOf(
    ReleaseNote(
        version = "0.1.20",
        date = "2026-05-05",
        bullets = listOf(
            "New atlas destination: 🎣 Pond. Tap the water to cast a line (⚡5), wait for the bite, then tap during the window to reel it in",
            "Ten fish from minnow (🪙10) to whale (🪙8000); rarer fish are wildly more lucrative",
            "Energy and coins are shared with the farm — fishing is just another way to spend the same energy pool",
            "Atlas now has three rows: Farm + Pond, Mine + Kitchen, Market — Mine is back in its old spot",
        ),
    ),
    ReleaseNote(
        version = "0.1.18",
        date = "2026-05-05",
        bullets = listOf(
            "Hold any seed or tree button to see a coins-per-minute tooltip above it; release to dismiss",
        ),
    ),
    ReleaseNote(
        version = "0.1.17",
        date = "2026-05-05",
        bullets = listOf(
            "Long-press any seed or tree button to see its coins-per-minute breakdown",
        ),
    ),
    ReleaseNote(
        version = "0.1.16",
        date = "2026-05-05",
        bullets = listOf(
            "Upgrades rebalanced — early levels still affordable, but each level costs 3–3.5× the last, making mid and late levels a real goal to save toward",
        ),
    ),
    ReleaseNote(
        version = "0.1.14",
        date = "2026-05-05",
        bullets = listOf(
            "Small crop icon now sits in the top-left of every planted plot, so you can tell carrots from corn while they're still seedlings",
        ),
    ),
    ReleaseNote(
        version = "0.1.13",
        date = "2026-05-05",
        bullets = listOf(
            "Atlas map with Farm, Mine, Kitchen, and Market — Mine, Kitchen, and Market coming soon",
            "Map icon in top bar opens the atlas; X button closes it back to your last screen",
            "App remembers which screen you were on and reopens it next launch",
            "Unaffordable seeds and trees are now greyed out in place (order unchanged)",
            "Seed and tree shelf buttons are more compact — more items visible at a glance",
            "Upgrades now cost significantly more at higher levels",
        ),
    ),
    ReleaseNote(
        version = "0.1.11",
        date = "2026-05-05",
        bullets = listOf(
            "Crops now exponentially expensive — Truffle tops at 54,000 coins",
            "Removed tree-grown crops (apples, peaches, mangoes…) — plant real trees instead",
            "Five plantable trees: Apple, Peach, Lemon, Mango, Coconut Palm",
            "Trees live 1–8 hours and yield multiple harvests; miss a window, lose that harvest",
            "Dead trees must be tapped to clear before tilling again",
            "Tree nursery scrolls alongside the seed shelf",
        ),
    ),
    ReleaseNote(
        version = "0.1.9",
        date = "2026-05-05",
        bullets = listOf(
            "Asks for notification permission on first launch — without it, Android 13+ silently dropped every update prompt",
        ),
    ),
    ReleaseNote(
        version = "0.1.6",
        date = "2026-05-05",
        bullets = listOf(
            "30 new crops — fruits, veggies, and nuts from carrot to coconut",
            "Rarer crops cost more and take longer; coconut tops at 20 min",
            "Three new upgrades: Fertilizer, Market Stall, Garden Hose",
            "Seed shelf now scrolls horizontally to browse all 34 crops",
            "Upgrade bar scrolls sideways; affordable upgrades shown first",
            "Farm grid now fills the full screen on all phone sizes",
        ),
    ),
    ReleaseNote(
        version = "0.1.2",
        date = "2026-05-04",
        bullets = listOf(
            "Initial harvest: 4x4 farm with energy-driven actions",
            "Four crops: carrots, wheat, tomatoes, pumpkins",
            "Watering shaves off remaining grow time",
            "Upgrades: max energy and faster regen",
            "Saves locally so your farm survives reboots",
        ),
    ),
)
