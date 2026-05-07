package com.shadaeiou.charmingfarmer.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.shadaeiou.charmingfarmer.data.room.AppDatabase
import com.shadaeiou.charmingfarmer.data.room.BirdSeenEntity
import com.shadaeiou.charmingfarmer.data.room.GameStateEntity
import com.shadaeiou.charmingfarmer.data.room.LegacyMigrator
import com.shadaeiou.charmingfarmer.data.room.PlotEntity
import com.shadaeiou.charmingfarmer.data.room.UpgradeLevelEntity
import kotlin.math.pow
import kotlin.random.Random

enum class Season(val displayName: String, val emoji: String) {
    SPRING("Spring", "🌷"),
    SUMMER("Summer", "☀️"),
    FALL("Fall", "🍁"),
    WINTER("Winter", "❄️");

    companion object {
        private val ORDER = listOf(SPRING, SUMMER, FALL, WINTER)

        /** 30 real minutes per season; 30 s when skipTimers is on for rapid testing. */
        val SEASON_MS: Long get() = if (DebugSettings.skipTimers) 30_000L else 30L * 60_000L
        val CYCLE_MS: Long get() = SEASON_MS * 4

        fun fromEpoch(epochMs: Long, nowMs: Long): Season {
            val elapsed = (nowMs - epochMs).coerceAtLeast(0L)
            return ORDER[((elapsed / SEASON_MS) % 4).toInt()]
        }

        /** 0.0 = start of Spring, 0.25 = start of Summer, 0.5 = Fall, 0.75 = Winter, 1.0 = Spring again. */
        fun cycleProgress(epochMs: Long, nowMs: Long): Float {
            val elapsed = (nowMs - epochMs).coerceAtLeast(0L)
            return ((elapsed % CYCLE_MS).toFloat() / CYCLE_MS).coerceIn(0f, 1f)
        }

        /** How many seasons back from [current] until we hit a season in [plantSeasons]. */
        fun seasonsSinceLastPlantable(current: Season, plantSeasons: Set<Season>): Int {
            if (current in plantSeasons) return 0
            for (steps in 1..3) {
                val idx = (ORDER.indexOf(current) - steps + 4) % 4
                if (ORDER[idx] in plantSeasons) return steps
            }
            return 4
        }
    }
}

enum class CropType(
    val displayName: String,
    val emoji: String,
    val sprout: String,
    val coinCost: Int,
    val growthMs: Long,
    val sellPrice: Int,
    val plantEnergy: Int,
    /** If non-null, harvesting this crop deposits the item into the FARM
     * silo with a rolled quality instead of paying coins. */
    val inventoryItem: ItemType? = null,
    /** Seasons in which this crop can be planted. */
    val plantSeasons: Set<Season> = Season.entries.toSet(),
) {
    CARROT("Carrot", "🥕", "🌱", 3, 20_000L, 8, 2, plantSeasons = setOf(Season.SPRING, Season.FALL)),
    POTATO("Potato", "🥔", "🌱", 5, 25_000L, 13, 2, plantSeasons = setOf(Season.SPRING, Season.FALL)),
    LETTUCE("Lettuce", "🥬", "🌱", 8, 35_000L, 21, 2, plantSeasons = setOf(Season.SPRING, Season.FALL)),
    ONION("Onion", "🧅", "🌱", 12, 50_000L, 32, 2, plantSeasons = setOf(Season.SPRING, Season.FALL)),
    BARLEY("Barley", "🌾", "🌱", 6, 30_000L, 0, 2, ItemType.BARLEY, setOf(Season.SPRING, Season.FALL)),
    WHEATGRAIN("Wheat (grain)", "🌾", "🌱", 10, 45_000L, 0, 2, ItemType.WHEAT_GRAIN, setOf(Season.SPRING, Season.FALL)),
    OATS_CROP("Oats", "🌾", "🌱", 14, 60_000L, 0, 2, ItemType.OATS, setOf(Season.SPRING, Season.FALL)),
    RYE_CROP("Rye", "🌾", "🌱", 18, 75_000L, 0, 2, ItemType.RYE, setOf(Season.FALL, Season.WINTER)),
    WHEAT("Wheat", "🌾", "🌱", 17, 65_000L, 45, 3, plantSeasons = setOf(Season.SPRING, Season.FALL)),
    CORN("Corn", "🌽", "🌱", 25, 90_000L, 67, 3, plantSeasons = setOf(Season.SUMMER)),
    BEAN("Bean", "🫘", "🌱", 36, 120_000L, 96, 3, plantSeasons = setOf(Season.SPRING, Season.SUMMER)),
    STRAWBERRY("Strawberry", "🍓", "🌱", 52, 160_000L, 139, 3, plantSeasons = setOf(Season.SPRING, Season.SUMMER)),
    MUSHROOM("Mushroom", "🍄", "🌱", 75, 210_000L, 200, 4, plantSeasons = setOf(Season.FALL, Season.WINTER, Season.SPRING)),
    BROCCOLI("Broccoli", "🥦", "🌱", 108, 300_000L, 288, 4, plantSeasons = setOf(Season.SPRING, Season.FALL)),
    ZUCCHINI("Zucchini", "🥒", "🌱", 155, 420_000L, 413, 4, plantSeasons = setOf(Season.SUMMER)),
    TOMATO("Tomato", "🍅", "🌿", 225, 540_000L, 600, 4, plantSeasons = setOf(Season.SUMMER)),
    BLUEBERRY("Blueberry", "🫐", "🌱", 325, 720_000L, 867, 5, plantSeasons = setOf(Season.SPRING, Season.SUMMER)),
    GRAPE("Grape", "🍇", "🌱", 468, 960_000L, 1_248, 5, plantSeasons = setOf(Season.SUMMER, Season.FALL)),
    PUMPKIN("Pumpkin", "🎃", "🌿", 675, 1_320_000L, 1_800, 5, plantSeasons = setOf(Season.SUMMER, Season.FALL)),
    PEPPER("Pepper", "🫑", "🌿", 972, 1_800_000L, 2_592, 6, plantSeasons = setOf(Season.SUMMER)),
    PINEAPPLE("Pineapple", "🍍", "🌿", 1_400, 2_400_000L, 3_733, 6, plantSeasons = setOf(Season.SUMMER, Season.FALL)),
    WATERMELON("Watermelon", "🍉", "🌿", 2_000, 3_240_000L, 5_333, 7, plantSeasons = setOf(Season.SUMMER)),
    GARLIC("Garlic", "🧄", "🌱", 2_900, 4_500_000L, 7_733, 7, plantSeasons = setOf(Season.FALL, Season.WINTER, Season.SPRING)),
    KIWI("Kiwi", "🥝", "🌱", 4_200, 6_000_000L, 11_200, 7, plantSeasons = setOf(Season.FALL)),
    PEANUT("Peanut", "🥜", "🌱", 6_000, 8_100_000L, 16_000, 8, plantSeasons = setOf(Season.SUMMER, Season.FALL)),
    HOT_PEPPER("Hot Pepper", "🌶️", "🌿", 8_650, 10_800_000L, 23_067, 8, plantSeasons = setOf(Season.SUMMER)),
    SNAP_PEA("Snap Pea", "🫛", "🌱", 12_500, 14_400_000L, 33_333, 9, plantSeasons = setOf(Season.SPRING, Season.FALL)),
    SAFFRON("Saffron", "🌸", "🌱", 18_000, 18_000_000L, 48_000, 9, plantSeasons = setOf(Season.FALL)),
    VANILLA("Vanilla", "🌺", "🌿", 26_000, 21_600_000L, 69_333, 10, plantSeasons = setOf(Season.SUMMER, Season.FALL)),
    PURPLE_YAM("Purple Yam", "🍠", "🌱", 37_500, 25_200_000L, 100_000, 10, plantSeasons = setOf(Season.SUMMER, Season.FALL)),
    TRUFFLE("Truffle", "🌰", "🌱", 54_000, 28_800_000L, 144_000, 10, plantSeasons = setOf(Season.FALL, Season.WINTER)),
}

enum class TreeType(
    val displayName: String,
    val treeEmoji: String,
    val fruitEmoji: String,
    val coinCost: Int,
    val lifeMs: Long,
    val harvestIntervalMs: Long,
    val maxHarvests: Int,
    val sellPrice: Int,
    val plantEnergy: Int,
    /** If non-null, harvesting deposits the item into the FARM silo
     * (with quality) instead of paying coins. */
    val inventoryItem: ItemType? = null,
    /** Seasons in which ripe fruit can be collected. */
    val harvestSeasons: Set<Season> = Season.entries.toSet(),
) {
    APPLE_TREE("Apple Tree", "🌳", "🍎", 500, 3_600_000L, 1_200_000L, 3, 400, 4, harvestSeasons = setOf(Season.FALL)),
    PEACH_TREE("Peach Tree", "🌳", "🍑", 1_500, 7_200_000L, 1_800_000L, 4, 1_000, 5, harvestSeasons = setOf(Season.SUMMER)),
    LEMON_TREE("Lemon Tree", "🌲", "🍋", 4_000, 10_800_000L, 2_700_000L, 4, 2_500, 6, harvestSeasons = setOf(Season.WINTER, Season.SPRING)),
    MANGO_TREE("Mango Tree", "🌴", "🥭", 12_000, 18_000_000L, 3_600_000L, 5, 6_000, 7, harvestSeasons = setOf(Season.SUMMER)),
    COCONUT_PALM("Coconut Palm", "🌴", "🥥", 35_000, 28_800_000L, 5_760_000L, 5, 15_000, 9, harvestSeasons = setOf(Season.SUMMER, Season.FALL)),
    HOP_BINE("Hop Bine", "🌿", "🌿", 250, 7_200_000L, 1_800_000L, 4, 0, 3, ItemType.HOPS_CASCADE, setOf(Season.SUMMER, Season.FALL)),
}

/** Roll a quality score for a freshly harvested crop or fruit. */
fun harvestScore(watered: Boolean): Int {
    val baseRoll = Random.nextInt(40, 71) // 40-70 inclusive of low, exclusive of high
    val waterBonus = if (watered) 15 else 0
    return (baseRoll + waterBonus).coerceIn(0, 100)
}

enum class PlotKind { GRASS, TILLED, PLANTED, TREE }

data class Plot(
    val kind: PlotKind = PlotKind.GRASS,
    val crop: CropType? = null,
    val tree: TreeType? = null,
    val plantedAtMs: Long = 0L,
    val watered: Boolean = false,
    val bonusMs: Long = 0L,
    val harvestCount: Int = 0,
) {
    fun growthFraction(nowMs: Long): Float {
        if (kind != PlotKind.PLANTED || crop == null) return 0f
        if (DebugSettings.skipTimers) return 1f
        val elapsed = nowMs - plantedAtMs + bonusMs
        return (elapsed.toFloat() / crop.growthMs).coerceIn(0f, 1f)
    }
    fun isReady(nowMs: Long): Boolean = growthFraction(nowMs) >= 1f

    fun treeIsDead(nowMs: Long): Boolean =
        tree != null && nowMs >= plantedAtMs + tree.lifeMs

    fun treeWindowsDue(nowMs: Long): Int {
        val t = tree ?: return 0
        if (DebugSettings.skipTimers) return t.maxHarvests
        return ((nowMs - plantedAtMs) / t.harvestIntervalMs).toInt().coerceAtMost(t.maxHarvests)
    }

    fun treeHarvestReady(nowMs: Long): Boolean =
        tree != null && !treeIsDead(nowMs) && treeWindowsDue(nowMs) > harvestCount

    fun treeNextHarvestMs(): Long {
        val t = tree ?: return 0L
        return plantedAtMs + (harvestCount + 1) * t.harvestIntervalMs
    }

    fun treeLifeFraction(nowMs: Long): Float {
        val t = tree ?: return 0f
        return ((nowMs - plantedAtMs).toFloat() / t.lifeMs).coerceIn(0f, 1f)
    }

    /** True if this planted crop has survived two seasons past its last planting season. */
    fun isCropDead(currentSeason: Season): Boolean {
        val c = crop ?: return false
        if (kind != PlotKind.PLANTED) return false
        return Season.seasonsSinceLastPlantable(currentSeason, c.plantSeasons) >= 2
    }

    /** True when a harvest window is open but the current season isn't a harvest season. */
    fun treeReadyWrongSeason(nowMs: Long, currentSeason: Season): Boolean {
        val t = tree ?: return false
        return !treeIsDead(nowMs) && treeWindowsDue(nowMs) > harvestCount &&
            currentSeason !in t.harvestSeasons
    }

    /** Fraction (0–1) of the current harvest interval elapsed since the last harvest.
     *  Returns 0 right after a harvest (bar resets to green), 1 when the next harvest is ready. */
    fun treeHarvestIntervalFraction(nowMs: Long): Float {
        val t = tree ?: return 0f
        if (DebugSettings.skipTimers) return 1f
        val intervalStart = plantedAtMs + harvestCount.toLong() * t.harvestIntervalMs
        return ((nowMs - intervalStart).toFloat() / t.harvestIntervalMs).coerceIn(0f, 1f)
    }
}

data class Upgrade(
    val key: String,
    val label: String,
    val desc: String,
    val baseCost: Int,
    val scale: Double,
)

val UPGRADES = listOf(
    Upgrade("maxEnergy", "Bigger Lungs", "+10 max energy", 500, 3.0),
    Upgrade("regen", "Strong Coffee", "−15% regen time", 800, 3.2),
    Upgrade("growthSpeed", "Fertilizer", "+15% grow speed", 1_000, 3.5),
    Upgrade("sellBonus", "Market Stall", "+2 sell coins", 600, 3.0),
    Upgrade("waterBonus", "Garden Hose", "+5% water bonus", 450, 2.9),
)

data class Goal(
    val id: String,
    val title: String,
    val description: String,
    val coinsRequired: Int,
    val harvestsRequired: Int,
    val newPlotCount: Int,
)

val FARM_GOALS = listOf(
    Goal("expand_5x5", "Growing Room", "Expand to a 5×5 farm", 500, 25, 25),
    Goal("expand_6x6", "Real Acreage", "Expand to a 6×6 farm", 2_000, 75, 36),
    Goal("expand_7x7", "Serious Farm", "Expand to a 7×7 farm", 8_000, 150, 49),
    Goal("expand_8x8", "Mega Farm", "Expand to an 8×8 farm", 30_000, 300, 64),
)

const val ENERGY_TILL = 3
const val ENERGY_WATER = 1
const val ENERGY_HARVEST = 1
const val STARTING_PLOT_COUNT = 16
const val STARTING_COINS = 5
const val STARTING_ENERGY = 50
const val MIN_REGEN_MS = 800L

data class FarmState(
    val energy: Float = STARTING_ENERGY.toFloat(),
    val maxEnergy: Int = STARTING_ENERGY,
    val regenMs: Long = 3000L,
    val lastTickMs: Long = 0L,
    val coins: Int = STARTING_COINS,
    val harvested: Int = 0,
    val selectedSeed: CropType = CropType.CARROT,
    val selectedTree: TreeType? = null,
    val upgradeLevels: Map<String, Int> = mapOf(
        "maxEnergy" to 0, "regen" to 0,
        "growthSpeed" to 0, "sellBonus" to 0, "waterBonus" to 0,
    ),
    val plotCount: Int = STARTING_PLOT_COUNT,
    val plots: List<Plot> = List(STARTING_PLOT_COUNT) { Plot() },
    val birdsSeen: Map<String, Int> = emptyMap(),
    val completedGoals: Set<String> = emptySet(),
)

class FarmGame(context: Context) {
    private val appContext = context.applicationContext
    private val db = AppDatabase.get(appContext).also {
        LegacyMigrator.migrateIfNeeded(appContext, it)
    }
    private val transport = TransportService.get(appContext)

    var state: FarmState by mutableStateOf(load())
        private set
    var feedback: String? by mutableStateOf(null)
        private set
    var feedbackBad: Boolean by mutableStateOf(false)
        private set

    var seasonEpochMs: Long = 0L
        private set

    init {
        val stored = db.systemMeta().get("season_epoch")?.toLongOrNull()
        seasonEpochMs = stored ?: System.currentTimeMillis().also { epoch ->
            db.systemMeta().put("season_epoch", epoch.toString())
        }
        if (state.lastTickMs == 0L) {
            state = state.copy(lastTickMs = System.currentTimeMillis())
        }
    }

    fun currentSeason(nowMs: Long = System.currentTimeMillis()): Season =
        Season.fromEpoch(seasonEpochMs, nowMs)

    fun seasonCycleProgress(nowMs: Long = System.currentTimeMillis()): Float =
        Season.cycleProgress(seasonEpochMs, nowMs)

    fun tick(nowMs: Long = System.currentTimeMillis()) {
        val s = state
        if (s.energy >= s.maxEnergy) {
            if (s.lastTickMs != nowMs) state = s.copy(lastTickMs = nowMs)
            return
        }
        val elapsed = nowMs - s.lastTickMs
        if (elapsed < s.regenMs) return
        val gained = (elapsed / s.regenMs).toInt()
        val newEnergy = (s.energy + gained).coerceAtMost(s.maxEnergy.toFloat())
        state = s.copy(energy = newEnergy, lastTickMs = nowMs - elapsed % s.regenMs)
    }

    fun clickPlot(idx: Int) {
        val now = System.currentTimeMillis()
        tick(now)
        val s = state
        if (idx !in s.plots.indices) return
        val p = s.plots[idx]
        when (p.kind) {
            PlotKind.GRASS -> handleTill(s, idx)
            PlotKind.TILLED -> if (s.selectedTree != null) handlePlantTree(s, idx, now)
                               else handlePlant(s, idx, now)
            PlotKind.PLANTED -> handlePlanted(s, idx, p, now)
            PlotKind.TREE -> handleTreeTap(s, idx, p, now)
        }
    }

    private fun handleTill(s: FarmState, idx: Int) {
        if (s.energy < ENERGY_TILL) { fail("Need ⚡$ENERGY_TILL"); return }
        state = s.copy(
            energy = s.energy - ENERGY_TILL,
            plots = s.plots.replaceAt(idx, Plot(kind = PlotKind.TILLED)),
        )
        note("Tilled the soil.")
        save()
    }

    private fun handlePlant(s: FarmState, idx: Int, now: Long) {
        val crop = s.selectedSeed
        val currentSeason = currentSeason(now)
        if (currentSeason !in crop.plantSeasons) {
            val seasonOrder = listOf(Season.SPRING, Season.SUMMER, Season.FALL, Season.WINTER)
            val seasons = crop.plantSeasons
                .sortedBy { seasonOrder.indexOf(it) }
                .joinToString(", ") { "${it.emoji} ${it.displayName}" }
            fail("${crop.displayName} only grows in $seasons.")
            return
        }
        if (s.coins < crop.coinCost) { fail("Need 🪙${crop.coinCost}"); return }
        if (s.energy < crop.plantEnergy) { fail("Need ⚡${crop.plantEnergy}"); return }
        val growthSpeedLvl = s.upgradeLevels["growthSpeed"] ?: 0
        val speedBonus = (crop.growthMs * 0.15 * growthSpeedLvl).toLong()
        state = s.copy(
            energy = s.energy - crop.plantEnergy,
            coins = s.coins - crop.coinCost,
            plots = s.plots.replaceAt(idx, Plot(
                kind = PlotKind.PLANTED,
                crop = crop,
                plantedAtMs = now,
                bonusMs = speedBonus,
            )),
        )
        note("Planted ${crop.displayName.lowercase()}!")
        save()
    }

    private fun handlePlanted(s: FarmState, idx: Int, p: Plot, now: Long) {
        val crop = p.crop ?: return
        val currentSeason = currentSeason(now)
        if (p.isCropDead(currentSeason)) {
            state = s.copy(plots = s.plots.replaceAt(idx, Plot(kind = PlotKind.TILLED)))
            note("Cleared the dead ${crop.displayName.lowercase()} 💀")
            save()
            return
        }
        when {
            p.isReady(now) -> {
                if (s.energy < ENERGY_HARVEST) { fail("Need ⚡$ENERGY_HARVEST"); return }
                val item = crop.inventoryItem
                if (item != null) {
                    // Grain / hops / other raw ingredients route to the silo
                    // with a quality roll instead of selling for coins.
                    val score = harvestScore(p.watered)
                    val tier = ItemTier.roll()
                    transport.addToInventory(Location.FARM, ItemStack(
                        type = item,
                        quantity = 1,
                        score = score,
                        tier = tier,
                        createdMs = now,
                    ))
                    state = s.copy(
                        energy = s.energy - ENERGY_HARVEST,
                        harvested = s.harvested + 1,
                        plots = s.plots.replaceAt(idx, Plot()),
                    )
                    val grade = ItemGrade.fromScore(score).display
                    val tierTag = if (tier != ItemTier.NORMAL) " ${tier.emojiSuffix}" else ""
                    note("Harvested ${crop.displayName.lowercase()} → silo (Grade $grade$tierTag, $score)")
                    save()
                    return
                }
                val sellBonusLvl = s.upgradeLevels["sellBonus"] ?: 0
                val earned = crop.sellPrice + sellBonusLvl * 2
                state = s.copy(
                    energy = s.energy - ENERGY_HARVEST,
                    coins = s.coins + earned,
                    harvested = s.harvested + 1,
                    plots = s.plots.replaceAt(idx, Plot()),
                )
                note("Harvested ${crop.displayName.lowercase()}! +🪙$earned")
                save()
            }
            !p.watered -> {
                if (s.energy < ENERGY_WATER) { fail("Need ⚡$ENERGY_WATER"); return }
                val elapsed = now - p.plantedAtMs + p.bonusMs
                val remaining = (crop.growthMs - elapsed).coerceAtLeast(0)
                val waterBonusLvl = s.upgradeLevels["waterBonus"] ?: 0
                val bonus = (remaining * (0.30 + waterBonusLvl * 0.05)).toLong()
                state = s.copy(
                    energy = s.energy - ENERGY_WATER,
                    plots = s.plots.replaceAt(idx, p.copy(watered = true, bonusMs = p.bonusMs + bonus)),
                )
                note("Watered the ${crop.displayName.lowercase()}.")
                save()
            }
            else -> fail("Already watered. Be patient!")
        }
    }

    fun selectSeed(crop: CropType) {
        if (state.selectedSeed == crop && state.selectedTree == null) return
        state = state.copy(selectedSeed = crop, selectedTree = null)
        save()
    }

    fun selectTree(tree: TreeType) {
        if (state.selectedTree == tree) return
        state = state.copy(selectedTree = tree)
        save()
    }

    private fun handlePlantTree(s: FarmState, idx: Int, now: Long) {
        val tree = s.selectedTree ?: return
        if (s.coins < tree.coinCost) { fail("Need 🪙${tree.coinCost}"); return }
        if (s.energy < tree.plantEnergy) { fail("Need ⚡${tree.plantEnergy}"); return }
        state = s.copy(
            energy = s.energy - tree.plantEnergy,
            coins = s.coins - tree.coinCost,
            plots = s.plots.replaceAt(idx, Plot(
                kind = PlotKind.TREE,
                tree = tree,
                plantedAtMs = now,
            )),
        )
        note("Planted ${tree.displayName.lowercase()}!")
        save()
    }

    private fun handleTreeTap(s: FarmState, idx: Int, p: Plot, now: Long) {
        val tree = p.tree ?: return
        val currentSeason = currentSeason(now)
        val seasonOrder = listOf(Season.SPRING, Season.SUMMER, Season.FALL, Season.WINTER)
        when {
            p.treeIsDead(now) -> {
                state = s.copy(plots = s.plots.replaceAt(idx, Plot()))
                note("Removed the dead ${tree.displayName.lowercase()}.")
                save()
            }
            p.treeReadyWrongSeason(now, currentSeason) -> {
                val seasons = tree.harvestSeasons
                    .sortedBy { seasonOrder.indexOf(it) }
                    .joinToString(", ") { "${it.emoji} ${it.displayName}" }
                note("${tree.displayName} ripens in $seasons. Waiting for the right season.")
            }
            p.treeHarvestReady(now) -> {
                if (s.energy < ENERGY_HARVEST) { fail("Need ⚡$ENERGY_HARVEST"); return }
                val windowsDue = p.treeWindowsDue(now)
                val item = tree.inventoryItem
                if (item != null) {
                    val score = harvestScore(false)
                    val tier = ItemTier.roll()
                    transport.addToInventory(Location.FARM, ItemStack(
                        type = item,
                        quantity = 1,
                        score = score,
                        tier = tier,
                        createdMs = now,
                    ))
                    state = s.copy(
                        energy = s.energy - ENERGY_HARVEST,
                        harvested = s.harvested + 1,
                        plots = s.plots.replaceAt(idx, p.copy(harvestCount = windowsDue)),
                    )
                    val grade = ItemGrade.fromScore(score).display
                    val tierTag = if (tier != ItemTier.NORMAL) " ${tier.emojiSuffix}" else ""
                    note("Harvested ${tree.displayName.lowercase()} → silo (Grade $grade$tierTag, $score)")
                    save()
                    return
                }
                val sellBonusLvl = s.upgradeLevels["sellBonus"] ?: 0
                val earned = tree.sellPrice + sellBonusLvl * 2
                state = s.copy(
                    energy = s.energy - ENERGY_HARVEST,
                    coins = s.coins + earned,
                    harvested = s.harvested + 1,
                    plots = s.plots.replaceAt(idx, p.copy(harvestCount = windowsDue)),
                )
                note("Harvested ${tree.displayName.lowercase()}! +🪙$earned")
                save()
            }
            else -> {
                val allDone = p.treeWindowsDue(now) >= tree.maxHarvests
                val msg = if (allDone) {
                    "All harvests done. Dies in ${prettyMs(p.plantedAtMs + tree.lifeMs - now)}."
                } else {
                    "Next harvest in ${prettyMs(p.treeNextHarvestMs() - now)}."
                }
                note(msg)
            }
        }
    }

    fun upgradeCost(up: Upgrade): Int {
        val lvl = state.upgradeLevels[up.key] ?: 0
        return (up.baseCost * up.scale.pow(lvl.toDouble())).toInt()
    }

    fun buyUpgrade(up: Upgrade) {
        val s = state
        val cost = upgradeCost(up)
        if (s.coins < cost) { fail("Need 🪙$cost"); return }
        val lvl = (s.upgradeLevels[up.key] ?: 0) + 1
        var next = s.copy(
            coins = s.coins - cost,
            upgradeLevels = s.upgradeLevels.toMutableMap().apply { put(up.key, lvl) },
        )
        next = when (up.key) {
            "maxEnergy" -> next.copy(
                maxEnergy = next.maxEnergy + 10,
                energy = next.energy + 10f,
            )
            "regen" -> next.copy(
                regenMs = (next.regenMs * 0.85).toLong().coerceAtLeast(MIN_REGEN_MS),
            )
            else -> next
        }
        state = next
        note("✨ ${up.label}!")
        save()
    }

    fun reset() {
        // Full-game reset: wipes every gameplay table (farm, silos,
        // brewery, malthouse, world map, vehicles, birds), reseeds the
        // starter layout, and reloads every singleton service. The
        // legacy-migration marker in system_meta is preserved so
        // LegacyMigrator doesn't re-import old SharedPreferences blobs.
        GameReset.resetEverything(appContext)
        state = load()
        note("Fresh start!")
    }

    // Lets other screens (e.g. fishing) consume from the shared energy pool
    // without exposing the private state setter. Returns false if there
    // wasn't enough energy; the caller chooses how to surface that.
    fun spendEnergy(amount: Int): Boolean {
        tick()
        val s = state
        if (DebugSettings.infiniteEnergy) {
            // Don't actually subtract — also keep the bar pinned to max
            // so the player sees "infinite" rather than a slow drain back.
            if (s.energy < s.maxEnergy) {
                state = s.copy(energy = s.maxEnergy.toFloat())
                save()
            }
            return true
        }
        if (s.energy < amount) return false
        state = s.copy(energy = s.energy - amount)
        save()
        return true
    }

    fun addCoins(amount: Int) {
        if (amount == 0) return
        state = state.copy(coins = state.coins + amount)
        save()
    }

    fun completeGoal(goalId: String) {
        val goal = FARM_GOALS.find { it.id == goalId } ?: return
        val s = state
        if (goalId in s.completedGoals) return
        if (s.coins < goal.coinsRequired || s.harvested < goal.harvestsRequired) {
            fail("Need 🪙${goal.coinsRequired} coins and ${goal.harvestsRequired} harvests")
            return
        }
        val newPlots = if (goal.newPlotCount > s.plots.size) {
            s.plots + List(goal.newPlotCount - s.plots.size) { Plot() }
        } else s.plots
        state = s.copy(
            plotCount = goal.newPlotCount,
            completedGoals = s.completedGoals + goalId,
            plots = newPlots,
        )
        note("🎉 ${goal.title}! Farm expanded!")
        save()
    }

    fun recordBird(name: String) {
        val current = state.birdsSeen[name] ?: 0
        state = state.copy(birdsSeen = state.birdsSeen + (name to current + 1))
        save()
    }

    private fun note(msg: String) { feedback = msg; feedbackBad = false }
    private fun fail(msg: String) { feedback = msg; feedbackBad = true }

    private fun prettyMs(ms: Long): String {
        val s = ms / 1000
        return when {
            s <= 0 -> "now"
            s < 60 -> "${s}s"
            s < 3600 -> "${s / 60}m"
            else -> "${s / 3600}h ${(s % 3600) / 60}m"
        }
    }

    private fun List<Plot>.replaceAt(idx: Int, value: Plot): List<Plot> =
        toMutableList().also { it[idx] = value }

    fun save() {
        val s = state
        db.runInTransaction {
            db.gameState().upsert(GameStateEntity(
                energy = s.energy,
                maxEnergy = s.maxEnergy,
                regenMs = s.regenMs,
                lastTickMs = s.lastTickMs,
                coins = s.coins,
                harvested = s.harvested,
                selectedSeed = s.selectedSeed.name,
                selectedTree = s.selectedTree?.name,
            ))
            db.plots().upsertAll(s.plots.mapIndexed { i, p ->
                PlotEntity(
                    position = i,
                    kind = p.kind.name,
                    crop = p.crop?.name,
                    tree = p.tree?.name,
                    plantedAtMs = p.plantedAtMs,
                    watered = p.watered,
                    bonusMs = p.bonusMs,
                    harvestCount = p.harvestCount,
                )
            })
            db.upgradeLevels().upsertAll(s.upgradeLevels.map {
                UpgradeLevelEntity(it.key, it.value)
            })
            // Birds are append-only per species; we upsert each with
            // its current count. Pruning species the player has zero
            // of is unnecessary — the table is small.
            db.birdsSeen().upsertAll(s.birdsSeen
                .filter { it.value > 0 }
                .map { BirdSeenEntity(it.key, it.value) })
            db.systemMeta().put("plot_count", s.plotCount.toString())
            db.systemMeta().put("completed_goals", s.completedGoals.joinToString(","))
        }
    }

    private fun load(): FarmState {
        val gs = db.gameState().getOrNull() ?: return seedNewGameState()
        val plotCount = db.systemMeta().get("plot_count")?.toIntOrNull() ?: STARTING_PLOT_COUNT
        val completedGoals = db.systemMeta().get("completed_goals")
            ?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
        val plotRows = db.plots().getAll().sortedBy { it.position }
        val plotMap = plotRows.associateBy { it.position }
        val plots = List(plotCount) { idx -> plotMap[idx]?.let { rowToPlot(it) } ?: Plot() }
        val upMap: MutableMap<String, Int> = db.upgradeLevels().getAll()
            .associate { it.key to it.level }
            .toMutableMap()
        if (!upMap.containsKey("maxEnergy")) upMap["maxEnergy"] = 0
        if (!upMap.containsKey("regen")) upMap["regen"] = 0
        if (!upMap.containsKey("growthSpeed")) upMap["growthSpeed"] = 0
        if (!upMap.containsKey("sellBonus")) upMap["sellBonus"] = 0
        if (!upMap.containsKey("waterBonus")) upMap["waterBonus"] = 0
        val birdsMap = db.birdsSeen().getAll().associate { it.speciesKey to it.count }
        return FarmState(
            energy = gs.energy,
            maxEnergy = gs.maxEnergy,
            regenMs = gs.regenMs,
            lastTickMs = gs.lastTickMs.takeIf { it != 0L } ?: System.currentTimeMillis(),
            coins = gs.coins,
            harvested = gs.harvested,
            selectedSeed = runCatching { CropType.valueOf(gs.selectedSeed) }
                .getOrDefault(CropType.CARROT),
            selectedTree = gs.selectedTree?.let {
                runCatching { TreeType.valueOf(it) }.getOrNull()
            },
            upgradeLevels = upMap,
            plotCount = plotCount,
            plots = plots,
            birdsSeen = birdsMap,
            completedGoals = completedGoals,
        )
    }

    private fun seedNewGameState(): FarmState = FarmState(
        lastTickMs = System.currentTimeMillis(),
    )

    private fun rowToPlot(row: PlotEntity): Plot {
        val kind = runCatching { PlotKind.valueOf(row.kind) }.getOrDefault(PlotKind.GRASS)
        val crop = row.crop?.let { runCatching { CropType.valueOf(it) }.getOrNull() }
        val tree = row.tree?.let { runCatching { TreeType.valueOf(it) }.getOrNull() }
        // Defensive: if a save references a CropType / TreeType we've
        // since removed, the plot reverts to grass instead of crashing.
        val resolvedKind = when {
            kind == PlotKind.PLANTED && crop == null -> PlotKind.GRASS
            kind == PlotKind.TREE && tree == null -> PlotKind.GRASS
            else -> kind
        }
        return Plot(
            kind = resolvedKind,
            crop = if (resolvedKind == PlotKind.PLANTED) crop else null,
            tree = if (resolvedKind == PlotKind.TREE) tree else null,
            plantedAtMs = row.plantedAtMs,
            watered = row.watered,
            bonusMs = row.bonusMs,
            harvestCount = row.harvestCount,
        )
    }
}
