package com.shadaeiou.charmingfarmer.data

data class ReleaseNote(
    val version: String,
    val date: String,
    val bullets: List<String>,
)

val CHANGELOG: List<ReleaseNote> = listOf(
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
