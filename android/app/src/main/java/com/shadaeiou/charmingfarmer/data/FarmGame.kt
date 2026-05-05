package com.shadaeiou.charmingfarmer.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.pow

enum class CropType(
    val displayName: String,
    val emoji: String,
    val sprout: String,
    val coinCost: Int,
    val growthMs: Long,
    val sellPrice: Int,
    val plantEnergy: Int,
) {
    CARROT("Carrot", "🥕", "🌱", 3, 20_000L, 8, 2),
    POTATO("Potato", "🥔", "🌱", 4, 25_000L, 11, 2),
    LETTUCE("Lettuce", "🥬", "🌱", 5, 30_000L, 13, 2),
    ONION("Onion", "🧅", "🌱", 6, 35_000L, 17, 2),
    WHEAT("Wheat", "🌾", "🌱", 8, 60_000L, 22, 3),
    CORN("Corn", "🌽", "🌱", 9, 65_000L, 25, 3),
    BEAN("Bean", "🫘", "🌱", 10, 70_000L, 28, 3),
    STRAWBERRY("Strawberry", "🍓", "🌱", 11, 80_000L, 32, 3),
    MUSHROOM("Mushroom", "🍄", "🌱", 13, 85_000L, 37, 3),
    APPLE("Apple", "🍎", "🌿", 15, 100_000L, 44, 3),
    BROCCOLI("Broccoli", "🥦", "🌱", 16, 105_000L, 47, 3),
    ZUCCHINI("Zucchini", "🥒", "🌱", 17, 110_000L, 50, 4),
    TOMATO("Tomato", "🍅", "🌿", 18, 120_000L, 55, 4),
    CHERRY("Cherry", "🍒", "🌿", 20, 130_000L, 58, 4),
    BLUEBERRY("Blueberry", "🫐", "🌱", 22, 140_000L, 65, 4),
    PEACH("Peach", "🍑", "🌿", 24, 155_000L, 70, 4),
    BANANA("Banana", "🍌", "🌿", 26, 165_000L, 77, 4),
    ORANGE("Orange", "🍊", "🌿", 28, 175_000L, 83, 4),
    PEAR("Pear", "🍐", "🌿", 32, 200_000L, 95, 5),
    GRAPE("Grape", "🍇", "🌱", 36, 220_000L, 108, 5),
    PEPPER("Pepper", "🫑", "🌿", 38, 235_000L, 114, 5),
    PUMPKIN("Pumpkin", "🎃", "🌿", 40, 300_000L, 140, 6),
    LEMON("Lemon", "🍋", "🌿", 42, 260_000L, 125, 5),
    AVOCADO("Avocado", "🥑", "🌿", 48, 295_000L, 143, 6),
    KIWI("Kiwi", "🥝", "🌱", 55, 330_000L, 164, 6),
    PEANUT("Peanut", "🥜", "🌱", 65, 400_000L, 195, 6),
    GARLIC("Garlic", "🧄", "🌱", 70, 430_000L, 210, 7),
    HOT_PEPPER("Hot Pepper", "🌶️", "🌿", 80, 490_000L, 240, 7),
    CHESTNUT("Chestnut", "🌰", "🌿", 90, 550_000L, 270, 7),
    MANGO("Mango", "🥭", "🌿", 100, 600_000L, 300, 8),
    OLIVE("Olive", "🫒", "🌿", 120, 720_000L, 360, 8),
    PINEAPPLE("Pineapple", "🍍", "🌿", 140, 840_000L, 420, 9),
    WATERMELON("Watermelon", "🍉", "🌿", 160, 960_000L, 480, 9),
    COCONUT("Coconut", "🥥", "🌿", 200, 1_200_000L, 600, 10),
}

enum class PlotKind { GRASS, TILLED, PLANTED }

data class Plot(
    val kind: PlotKind = PlotKind.GRASS,
    val crop: CropType? = null,
    val plantedAtMs: Long = 0L,
    val watered: Boolean = false,
    val bonusMs: Long = 0L,
) {
    fun growthFraction(nowMs: Long): Float {
        if (kind != PlotKind.PLANTED || crop == null) return 0f
        val elapsed = nowMs - plantedAtMs + bonusMs
        return (elapsed.toFloat() / crop.growthMs).coerceIn(0f, 1f)
    }
    fun isReady(nowMs: Long): Boolean = growthFraction(nowMs) >= 1f
}

data class Upgrade(
    val key: String,
    val label: String,
    val desc: String,
    val baseCost: Int,
    val scale: Double,
)

val UPGRADES = listOf(
    Upgrade("maxEnergy", "Bigger Lungs", "+10 max energy", 30, 1.6),
    Upgrade("regen", "Strong Coffee", "−15% regen time", 45, 1.8),
    Upgrade("growthSpeed", "Fertilizer", "+15% grow speed", 60, 1.9),
    Upgrade("sellBonus", "Market Stall", "+2 sell coins", 55, 1.85),
    Upgrade("waterBonus", "Garden Hose", "+5% water bonus", 40, 1.75),
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
    val upgradeLevels: Map<String, Int> = mapOf(
        "maxEnergy" to 0, "regen" to 0,
        "growthSpeed" to 0, "sellBonus" to 0, "waterBonus" to 0,
    ),
    val plots: List<Plot> = List(PLOT_COUNT) { Plot() },
)

class FarmGame(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("charming-farmer-v1", Context.MODE_PRIVATE)

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
            PlotKind.TILLED -> handlePlant(s, idx, now)
            PlotKind.PLANTED -> handlePlanted(s, idx, p, now)
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
        if (state.selectedSeed == crop) return
        state = state.copy(selectedSeed = crop)
        save()
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

    private fun note(msg: String) { feedback = msg; feedbackBad = false }
    private fun fail(msg: String) { feedback = msg; feedbackBad = true }

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
            if (p.crop != null) o.put("crop", p.crop.name)
            plotsJson.put(o)
        }
        val upJson = JSONObject()
        for ((k, v) in s.upgradeLevels) upJson.put(k, v)
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
                plots.add(Plot(
                    kind = PlotKind.valueOf(po.getString("kind")),
                    crop = po.optString("crop").takeIf { it.isNotEmpty() }?.let { CropType.valueOf(it) },
                    plantedAtMs = po.optLong("plantedAtMs"),
                    watered = po.optBoolean("watered"),
                    bonusMs = po.optLong("bonusMs"),
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
            FarmState(
                energy = o.optDouble("energy", STARTING_ENERGY.toDouble()).toFloat(),
                maxEnergy = o.optInt("maxEnergy", STARTING_ENERGY),
                regenMs = o.optLong("regenMs", 3000L),
                lastTickMs = o.optLong("lastTickMs", System.currentTimeMillis()),
                coins = o.optInt("coins", STARTING_COINS),
                harvested = o.optInt("harvested", 0),
                selectedSeed = runCatching { CropType.valueOf(o.optString("selectedSeed", "CARROT")) }
                    .getOrDefault(CropType.CARROT),
                upgradeLevels = upMap,
                plots = if (plots.size == PLOT_COUNT) plots else List(PLOT_COUNT) { Plot() },
            )
        }.getOrElse { FarmState(lastTickMs = System.currentTimeMillis()) }
    }
}
