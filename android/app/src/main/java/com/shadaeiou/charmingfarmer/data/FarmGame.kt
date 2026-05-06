package com.shadaeiou.charmingfarmer.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.pow
import kotlin.random.Random

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
) {
    CARROT("Carrot", "🥕", "🌱", 3, 20_000L, 8, 2),
    POTATO("Potato", "🥔", "🌱", 5, 25_000L, 13, 2),
    LETTUCE("Lettuce", "🥬", "🌱", 8, 35_000L, 21, 2),
    ONION("Onion", "🧅", "🌱", 12, 50_000L, 32, 2),
    BARLEY("Barley", "🌾", "🌱", 6, 30_000L, 0, 2, ItemType.BARLEY),
    WHEATGRAIN("Wheat (grain)", "🌾", "🌱", 10, 45_000L, 0, 2, ItemType.WHEAT_GRAIN),
    OATS_CROP("Oats", "🌾", "🌱", 14, 60_000L, 0, 2, ItemType.OATS),
    RYE_CROP("Rye", "🌾", "🌱", 18, 75_000L, 0, 2, ItemType.RYE),
    WHEAT("Wheat", "🌾", "🌱", 17, 65_000L, 45, 3),
    CORN("Corn", "🌽", "🌱", 25, 90_000L, 67, 3),
    BEAN("Bean", "🫘", "🌱", 36, 120_000L, 96, 3),
    STRAWBERRY("Strawberry", "🍓", "🌱", 52, 160_000L, 139, 3),
    MUSHROOM("Mushroom", "🍄", "🌱", 75, 210_000L, 200, 4),
    BROCCOLI("Broccoli", "🥦", "🌱", 108, 300_000L, 288, 4),
    ZUCCHINI("Zucchini", "🥒", "🌱", 155, 420_000L, 413, 4),
    TOMATO("Tomato", "🍅", "🌿", 225, 540_000L, 600, 4),
    BLUEBERRY("Blueberry", "🫐", "🌱", 325, 720_000L, 867, 5),
    GRAPE("Grape", "🍇", "🌱", 468, 960_000L, 1_248, 5),
    PUMPKIN("Pumpkin", "🎃", "🌿", 675, 1_320_000L, 1_800, 5),
    PEPPER("Pepper", "🫑", "🌿", 972, 1_800_000L, 2_592, 6),
    PINEAPPLE("Pineapple", "🍍", "🌿", 1_400, 2_400_000L, 3_733, 6),
    WATERMELON("Watermelon", "🍉", "🌿", 2_000, 3_240_000L, 5_333, 7),
    GARLIC("Garlic", "🧄", "🌱", 2_900, 4_500_000L, 7_733, 7),
    KIWI("Kiwi", "🥝", "🌱", 4_200, 6_000_000L, 11_200, 7),
    PEANUT("Peanut", "🥜", "🌱", 6_000, 8_100_000L, 16_000, 8),
    HOT_PEPPER("Hot Pepper", "🌶️", "🌿", 8_650, 10_800_000L, 23_067, 8),
    SNAP_PEA("Snap Pea", "🫛", "🌱", 12_500, 14_400_000L, 33_333, 9),
    SAFFRON("Saffron", "🌸", "🌱", 18_000, 18_000_000L, 48_000, 9),
    VANILLA("Vanilla", "🌺", "🌿", 26_000, 21_600_000L, 69_333, 10),
    PURPLE_YAM("Purple Yam", "🍠", "🌱", 37_500, 25_200_000L, 100_000, 10),
    TRUFFLE("Truffle", "🌰", "🌱", 54_000, 28_800_000L, 144_000, 10),
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
) {
    APPLE_TREE("Apple Tree", "🌳", "🍎", 500, 3_600_000L, 1_200_000L, 3, 400, 4),
    PEACH_TREE("Peach Tree", "🌳", "🍑", 1_500, 7_200_000L, 1_800_000L, 4, 1_000, 5),
    LEMON_TREE("Lemon Tree", "🌲", "🍋", 4_000, 10_800_000L, 2_700_000L, 4, 2_500, 6),
    MANGO_TREE("Mango Tree", "🌴", "🥭", 12_000, 18_000_000L, 3_600_000L, 5, 6_000, 7),
    COCONUT_PALM("Coconut Palm", "🌴", "🥥", 35_000, 28_800_000L, 5_760_000L, 5, 15_000, 9),
    HOP_BINE("Hop Bine", "🌿", "🌿", 250, 7_200_000L, 1_800_000L, 4, 0, 3, ItemType.HOPS_CASCADE),
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
        val elapsed = nowMs - plantedAtMs + bonusMs
        return (elapsed.toFloat() / crop.growthMs).coerceIn(0f, 1f)
    }
    fun isReady(nowMs: Long): Boolean = growthFraction(nowMs) >= 1f

    fun treeIsDead(nowMs: Long): Boolean =
        tree != null && nowMs >= plantedAtMs + tree.lifeMs

    fun treeWindowsDue(nowMs: Long): Int {
        val t = tree ?: return 0
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

const val ENERGY_TILL = 3
const val ENERGY_WATER = 1
const val ENERGY_HARVEST = 1
const val PLOT_COUNT = 16
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
    val plots: List<Plot> = List(PLOT_COUNT) { Plot() },
    val birdsSeen: Map<String, Int> = emptyMap(),
)

class FarmGame(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("charming-farmer-v1", Context.MODE_PRIVATE)
    private val transport = TransportService.get(context)

    var state: FarmState by mutableStateOf(load())
        private set
    var feedback: String? by mutableStateOf(null)
        private set
    var feedbackBad: Boolean by mutableStateOf(false)
        private set

    init {
        if (state.lastTickMs == 0L) {
            state = state.copy(lastTickMs = System.currentTimeMillis())
        }
    }

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
        when {
            p.treeIsDead(now) -> {
                state = s.copy(plots = s.plots.replaceAt(idx, Plot()))
                note("Removed the dead ${tree.displayName.lowercase()}.")
                save()
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
        prefs.edit().remove("state").apply()
        state = FarmState(lastTickMs = System.currentTimeMillis())
        note("Fresh start!")
        save()
    }

    // Lets other screens (e.g. fishing) consume from the shared energy pool
    // without exposing the private state setter. Returns false if there
    // wasn't enough energy; the caller chooses how to surface that.
    fun spendEnergy(amount: Int): Boolean {
        tick()
        val s = state
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
        val plotsJson = JSONArray()
        for (p in s.plots) {
            val o = JSONObject()
                .put("kind", p.kind.name)
                .put("plantedAtMs", p.plantedAtMs)
                .put("watered", p.watered)
                .put("bonusMs", p.bonusMs)
                .put("harvestCount", p.harvestCount)
            if (p.crop != null) o.put("crop", p.crop.name)
            if (p.tree != null) o.put("tree", p.tree.name)
            plotsJson.put(o)
        }
        val upJson = JSONObject()
        for ((k, v) in s.upgradeLevels) upJson.put(k, v)
        val birdsJson = JSONObject()
        for ((k, v) in s.birdsSeen) birdsJson.put(k, v)
        val json = JSONObject()
            .put("energy", s.energy.toDouble())
            .put("maxEnergy", s.maxEnergy)
            .put("regenMs", s.regenMs)
            .put("lastTickMs", s.lastTickMs)
            .put("coins", s.coins)
            .put("harvested", s.harvested)
            .put("selectedSeed", s.selectedSeed.name)
            .put("upgrades", upJson)
            .put("plots", plotsJson)
            .put("birdsSeen", birdsJson)
        if (s.selectedTree != null) json.put("selectedTree", s.selectedTree.name)
        prefs.edit().putString("state", json.toString()).apply()
    }

    private fun load(): FarmState {
        val raw = prefs.getString("state", null) ?: return FarmState()
        return runCatching {
            val o = JSONObject(raw)
            val plotsJson = o.getJSONArray("plots")
            val plots = ArrayList<Plot>(plotsJson.length())
            for (i in 0 until plotsJson.length()) {
                val po = plotsJson.getJSONObject(i)
                val kind = runCatching { PlotKind.valueOf(po.getString("kind")) }.getOrDefault(PlotKind.GRASS)
                val crop = po.optString("crop").takeIf { it.isNotEmpty() }
                    ?.let { runCatching { CropType.valueOf(it) }.getOrNull() }
                val tree = po.optString("tree").takeIf { it.isNotEmpty() }
                    ?.let { runCatching { TreeType.valueOf(it) }.getOrNull() }
                val resolvedKind = when {
                    kind == PlotKind.PLANTED && crop == null -> PlotKind.GRASS
                    kind == PlotKind.TREE && tree == null -> PlotKind.GRASS
                    else -> kind
                }
                plots.add(Plot(
                    kind = resolvedKind,
                    crop = if (resolvedKind == PlotKind.PLANTED) crop else null,
                    tree = if (resolvedKind == PlotKind.TREE) tree else null,
                    plantedAtMs = po.optLong("plantedAtMs"),
                    watered = po.optBoolean("watered"),
                    bonusMs = po.optLong("bonusMs"),
                    harvestCount = po.optInt("harvestCount"),
                ))
            }
            val upMap = mutableMapOf<String, Int>()
            o.optJSONObject("upgrades")?.let { upObj ->
                val keys = upObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    upMap[k] = upObj.getInt(k)
                }
            }
            if (!upMap.containsKey("maxEnergy")) upMap["maxEnergy"] = 0
            if (!upMap.containsKey("regen")) upMap["regen"] = 0
            if (!upMap.containsKey("growthSpeed")) upMap["growthSpeed"] = 0
            if (!upMap.containsKey("sellBonus")) upMap["sellBonus"] = 0
            if (!upMap.containsKey("waterBonus")) upMap["waterBonus"] = 0
            val birdsMap = mutableMapOf<String, Int>()
            o.optJSONObject("birdsSeen")?.let { obj ->
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    birdsMap[k] = obj.optInt(k, 0)
                }
            }
            FarmState(
                energy = o.optDouble("energy", STARTING_ENERGY.toDouble()).toFloat(),
                maxEnergy = o.optInt("maxEnergy", STARTING_ENERGY),
                regenMs = o.optLong("regenMs", 3000L),
                lastTickMs = o.optLong("lastTickMs", System.currentTimeMillis()),
                coins = o.optInt("coins", STARTING_COINS),
                harvested = o.optInt("harvested", 0),
                selectedSeed = runCatching { CropType.valueOf(o.optString("selectedSeed", "CARROT")) }
                    .getOrDefault(CropType.CARROT),
                selectedTree = o.optString("selectedTree").takeIf { it.isNotEmpty() }
                    ?.let { runCatching { TreeType.valueOf(it) }.getOrNull() },
                upgradeLevels = upMap,
                plots = if (plots.size == PLOT_COUNT) plots else List(PLOT_COUNT) { Plot() },
                birdsSeen = birdsMap,
            )
        }.getOrElse { FarmState(lastTickMs = System.currentTimeMillis()) }
    }
}
