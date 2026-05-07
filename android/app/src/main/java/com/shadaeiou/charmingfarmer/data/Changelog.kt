package com.shadaeiou.charmingfarmer.data

data class ReleaseNote(
    val version: String,
    val date: String,
    val bullets: List<String>,
)

val CHANGELOG: List<ReleaseNote> = listOf(
    ReleaseNote(
        version = "0.1.47",
        date = "2026-05-06",
        bullets = listOf(
            "🔄 Reset game button is now a true fresh start — wipes the world map, all silos, the malthouse, the brewery cellar, vehicles, bird sightings, upgrades, coins, and energy",
            "After resetting, you land back on a fresh world map with the starter House + Farm + Pond + Bird Hide + Malthouse + Brewery cluster — exactly what a new install sees",
            "Confirmation dialog spells out everything that gets wiped so the button can't be a surprise",
        ),
    ),
    ReleaseNote(
        version = "0.1.44",
        date = "2026-05-06",
        bullets = listOf(
            "🎯 Goals system — complete milestones (coins + harvests) to expand your farm from 4×4 all the way up to 8×8",
            "Four expansion goals: Growing Room (5×5), Real Acreage (6×6), Serious Farm (7×7), Mega Farm (8×8)",
            "Tree plots now show a green-to-yellow harvest-interval bar that fills toward each harvest window and resets to full green after you collect",
            "🦜 Birdwatching speeds up over your session — birds get faster until they plateau at 3× after 8 minutes",
            "Flying litter now drifts through the sky: tap a balloon, paper, leaf, kite, or newspaper by mistake and get a 💥 explosion instead of coins",
        ),
    ),
    ReleaseNote(
        version = "0.1.43",
        date = "2026-05-06",
        bullets = listOf(
            "🗺️ The atlas is gone — replaced by an interactive pixel-style world map showing the actual land you own",
            "Your existing buildings (House, Farm, Pond, Birdwatching, Malthouse, Brewery) appear pre-placed around the origin",
            "Tap a tile with a building to enter it, same as before",
            "Wild land beyond your border shows as grass, water, hills, forest, and dirt — terrain is the same every visit thanks to a coordinate-keyed biome generator",
            "World map is now the default home screen on first launch; existing players resume on whatever they were doing",
        ),
    ),
    ReleaseNote(
        version = "0.1.40",
        date = "2026-05-06",
        bullets = listOf(
            "Season clock on the farm: a 4-quadrant dial (Spring top-right, Summer bottom-right, Fall bottom-left, Winter top-left) with a rotating hand shows exactly where you are in the 2-hour cycle",
            "Seasons now run on a 30-minute game clock instead of the real-world calendar — each full cycle is 2 hours",
            "Farm backgrounds redesigned: dark geometric patterns (triangles for fall, diamonds for winter, hexagons for spring, diagonal stripes for summer) — no emojis, muted dark-mode palette",
        ),
    ),
    ReleaseNote(
        version = "0.1.38",
        date = "2026-05-06",
        bullets = listOf(
            "Upgrade costs now abbreviate to k/m (e.g. 24.5m instead of 24500000)",
            "Seed shelf buttons are more compact — smaller emoji and less padding",
        ),
    ),
    ReleaseNote(
        version = "0.1.37",
        date = "2026-05-06",
        bullets = listOf(
            "🍂 Farm seasons — the background changes to match the real-world season (fall leaves, winter snowfall, spring tulips, summer suns)",
            "Each crop now has planting seasons based on real-world agriculture; out-of-season seeds are dimmed and blocked from planting",
            "Plants survive one season past their last planting season, then wilt (💀) — tap the dead plot to clear it",
            "Trees can only be harvested in their natural season; the harvest window stays open and the fruit becomes collectible once the right season arrives",
            "Season banner shows the current season above the farm",
            "Seed and tree shelf buttons now show their planting / harvest seasons",
        ),
    ),
    ReleaseNote(
        version = "0.1.34",
        date = "2026-05-06",
        bullets = listOf(
            "🍺 Brewery is open! Pick a recipe (Pale Ale, IPA, Dry Stout, Hefeweizen), spend ⚡ + 🪙, and watch a batch run mash → boil → ferment → bottle in real time",
            "Each finished beer gets a BJCP-style score (Aroma 24 / Appearance 6 / Flavor 40 / Mouthfeel 10 / Overall 20) — tap Notes on a bottled batch to see the breakdown",
            "Off-flavor diagnostics on lower scores tell you what went wrong in real-brewer terms (DMS, banana esters, acetaldehyde, etc.)",
            "Sell bottled beer from the brewery cellar — payout scales with quality, so a Grade S stout makes a Grade C IPA look stingy",
            "Transport panel cleanup: each destination is now a full-width labeled button, and shipping rules filter per-item (no more sending hops to the malthouse)",
            "Malthouse icon swapped from 🌾 → 🏭 so it stops looking identical to the farm",
        ),
    ),
    ReleaseNote(
        version = "0.1.29",
        date = "2026-05-06",
        bullets = listOf(
            "New atlas destination: 🌾 Malthouse. Ship grain from the farm and kiln it into pale, Munich, crystal, chocolate, or black malt — each profile takes longer and shifts the score differently",
            "🚚 Transport panel on the farm and malthouse top bars: pick a cargo, pick a destination, watch the wheelbarrow roll. Routes are generic so future locations plug in",
            "Four new grain crops on the farm — Barley, Wheat (grain), Oats, Rye — plus a perennial Hop Bine. They harvest into your silo with a quality score and a tier roll instead of paying coins",
            "Universal quality system: every produced or processed item carries a 0-100 score (Grade S/A/B/C/D/F) and a rare-roll Tier (Mega ✨ / Golden 💛 / Perfect 🏆) that boosts sale prices",
            "Save data moved to a real database (Room) — same farm, same fish, same birds; the schema is now built to grow without breaking older saves",
        ),
    ),
    ReleaseNote(
        version = "0.1.23",
        date = "2026-05-05",
        bullets = listOf(
            "New atlas destination: 🦜 Birdwatching. Tap birds as they fly across the sky (⚡1 each); rarer ones pay way more",
            "Nine birds from sparrow (🪙5) to peacock (🪙2,500); your field-notes count persists across sessions",
            "Atlas overhauled with full descriptions on every tile and now scrolls — fifteen destinations in total",
            "Future plans visible at a glance: Forest, Cellar, Workshop, Greenhouse, Cottage, Shrine, Café joined the roadmap",
        ),
    ),
    ReleaseNote(
        version = "0.1.22",
        date = "2026-05-05",
        bullets = listOf(
            "Two more atlas placeholders: 🐝 Apiary and 🐄 Livestock — coming soon, but visible on the map so you know what's on the roadmap",
        ),
    ),
    ReleaseNote(
        version = "0.1.21",
        date = "2026-05-05",
        bullets = listOf(
            "Growing trees now show the fruit icon in the corner so you always know what's ripening",
        ),
    ),
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
