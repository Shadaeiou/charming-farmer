package com.shadaeiou.charmingfarmer.data

data class ReleaseNote(
    val version: String,
    val date: String,
    val bullets: List<String>,
)

val CHANGELOG: List<ReleaseNote> = listOf(
    ReleaseNote(
        version = "0.2.0",
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
        version = "0.1.0",
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
