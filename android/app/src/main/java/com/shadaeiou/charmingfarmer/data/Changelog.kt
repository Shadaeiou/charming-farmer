package com.shadaeiou.charmingfarmer.data

data class ReleaseNote(
    val version: String,
    val date: String,
    val bullets: List<String>,
)

val CHANGELOG: List<ReleaseNote> = listOf(
    ReleaseNote(
        version = "0.3.0",
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
