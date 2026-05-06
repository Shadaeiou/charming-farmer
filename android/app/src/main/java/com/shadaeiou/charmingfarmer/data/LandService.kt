package com.shadaeiou.charmingfarmer.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.shadaeiou.charmingfarmer.data.room.AppDatabase
import com.shadaeiou.charmingfarmer.data.room.LandTileEntity
import com.shadaeiou.charmingfarmer.data.room.LegacyMigrator
import kotlin.math.abs
import kotlin.random.Random

/**
 * Every kind of building or working land that can be placed on the
 * world map. [routeId] is the navigation route the tile opens on tap;
 * [singleton] enforces "only one of these in the world" — fields,
 * tree plots, apiaries, and livestock pens can be placed multiple
 * times, processing buildings cannot.
 *
 * [buildCost] / [buildDurationMs] / [buildEnergy] gate construction
 * and are honored by [DebugSettings.skipTimers] for testing.
 */
enum class StructureType(
    val displayName: String,
    val emoji: String,
    val routeId: String,
    val singleton: Boolean,
    val buildCost: Int,
    val buildDurationMs: Long,
    val buildEnergy: Int,
) {
    HOUSE("House", "🏡", "home", true, 0, 0L, 0),
    FARM_FIELD("Farm Field", "🌾", "home", false, 200, 5 * 60_000L, 4),
    TREE_PLOT("Tree Plot", "🌳", "home", false, 400, 10 * 60_000L, 5),
    APIARY("Apiary", "🐝", "apiary", false, 700, 20 * 60_000L, 6),
    LIVESTOCK("Livestock", "🐄", "livestock", false, 800, 20 * 60_000L, 6),
    POND("Pond", "🎣", "pond", true, 600, 15 * 60_000L, 5),
    BIRDWATCHING("Bird Hide", "🦜", "birds", true, 400, 10 * 60_000L, 4),
    MALTHOUSE("Malthouse", "🏭", "malthouse", true, 800, 20 * 60_000L, 6),
    BREWERY("Brewery", "🍺", "brewery", true, 1500, 45 * 60_000L, 8),
    KITCHEN("Kitchen", "🍳", "kitchen", true, 1200, 30 * 60_000L, 7),
    CELLAR("Cellar", "🍷", "cellar", true, 2000, 60 * 60_000L, 9),
    MARKET("Market", "🏪", "market", true, 1800, 45 * 60_000L, 8),
    MINE("Mine", "⛏️", "mine", true, 2500, 90 * 60_000L, 10),
    WORKSHOP("Workshop", "⚒️", "workshop", true, 1800, 45 * 60_000L, 8),
    GREENHOUSE("Greenhouse", "🌷", "greenhouse", true, 2000, 60 * 60_000L, 9),
    COTTAGE("Cottage", "🛖", "cottage", true, 1500, 45 * 60_000L, 7),
    SHRINE("Shrine", "⛪", "shrine", true, 3000, 120 * 60_000L, 10),
    CAFE("Café", "☕", "cafe", true, 1200, 30 * 60_000L, 7),
    FOREST("Forest", "🌲", "forest", true, 1500, 45 * 60_000L, 8),
    ;

    companion object {
        fun valueOfOrNull(name: String): StructureType? =
            runCatching { valueOf(name) }.getOrNull()
    }
}

/**
 * Terrain type for an unowned tile. Drives both the price modifier
 * (water/hills cost more) and the rendered tile texture. Generated
 * deterministically from (x, y) so the same coordinate always looks
 * the same — no DB rows for unowned land.
 */
enum class Biome(val displayName: String, val priceMultiplier: Double) {
    GRASS("Grass", 1.0),
    DIRT("Dirt", 0.9),
    WATER("Water", 1.6),
    HILL("Hill", 1.4),
    FOREST("Forest", 1.2),
}

/**
 * Hash (x, y) into a Biome. Fixed seed -> deterministic terrain,
 * same coordinate always returns the same biome across launches.
 *
 * Distribution: ~5% water, ~10% hill, ~8% forest, ~5% dirt patch,
 * rest grass. Tiles are independent — clustered terrain comes later
 * via Perlin noise if we want it.
 */
fun biomeAt(x: Int, y: Int): Biome {
    val seed = (x * 73856093L) xor (y * 19349663L) xor 0x5deeceL
    val rng = Random(seed)
    val r = rng.nextDouble()
    return when {
        r < 0.05 -> Biome.WATER
        r < 0.15 -> Biome.HILL
        r < 0.23 -> Biome.FOREST
        r < 0.28 -> Biome.DIRT
        else -> Biome.GRASS
    }
}

data class LandTile(
    val x: Int,
    val y: Int,
    val ownedAtMs: Long? = null,
    val structure: StructureType? = null,
    val buildStartedMs: Long? = null,
    val buildDurationMs: Long? = null,
) {
    val owned: Boolean get() = ownedAtMs != null

    val isBuilding: Boolean
        get() = buildStartedMs != null && buildDurationMs != null

    fun buildProgress(nowMs: Long): Float {
        if (DebugSettings.skipTimers) return 1f
        val started = buildStartedMs ?: return 0f
        val duration = buildDurationMs ?: return 0f
        return ((nowMs - started).toFloat() / duration).coerceIn(0f, 1f)
    }

    fun buildComplete(nowMs: Long): Boolean {
        if (DebugSettings.skipTimers) return true
        val started = buildStartedMs ?: return false
        val duration = buildDurationMs ?: return false
        return nowMs - started >= duration
    }

    fun buildRemainingMs(nowMs: Long): Long {
        if (DebugSettings.skipTimers) return 0L
        val started = buildStartedMs ?: return 0L
        val duration = buildDurationMs ?: return 0L
        return (started + duration - nowMs).coerceAtLeast(0L)
    }
}

class LandService private constructor(appContext: Context) {

    private val db = AppDatabase.get(appContext).also {
        LegacyMigrator.migrateIfNeeded(appContext, it)
    }

    /**
     * Owned tiles only. Unowned tiles are computed on demand from
     * [biomeAt] + [purchasePrice] so the world is effectively infinite
     * with zero storage until purchase.
     */
    private val _ownedTiles = mutableStateMapOf<Pair<Int, Int>, LandTile>()
    var revisionTick: Int by mutableStateOf(0)
        private set

    init { load() }

    fun ownedTiles(): Collection<LandTile> = _ownedTiles.values

    fun tileAt(x: Int, y: Int): LandTile? = _ownedTiles[x to y]

    fun structureCount(type: StructureType): Int =
        _ownedTiles.values.count { it.structure == type && !it.isBuilding }

    fun hasStructure(type: StructureType): Boolean =
        _ownedTiles.values.any { it.structure == type && !it.isBuilding }

    /**
     * Cost to purchase the unowned tile at [x],[y]. Chebyshev distance
     * from the House at (0,0) plus the biome multiplier. Quadratic in
     * distance so the world feels gated as you expand outward.
     */
    fun purchasePrice(x: Int, y: Int): Int {
        if (tileAt(x, y) != null) return 0
        val distance = maxOf(abs(x), abs(y))
        val basePrice = 50 + distance * distance * 25
        val biomeMod = biomeAt(x, y).priceMultiplier
        return (basePrice * biomeMod).toInt().coerceAtLeast(1)
    }

    /**
     * Mark a tile as owned. Returns the new tile, or null if the tile
     * was already owned or the player couldn't afford the price.
     */
    fun buyLand(
        x: Int,
        y: Int,
        nowMs: Long,
        spendCoins: (Int) -> Boolean,
    ): LandTile? {
        if (tileAt(x, y) != null) return null
        val price = purchasePrice(x, y)
        if (!spendCoins(price)) return null
        val tile = LandTile(x = x, y = y, ownedAtMs = nowMs)
        _ownedTiles[x to y] = tile
        db.landTiles().upsert(tile.toEntity())
        bump()
        return tile
    }

    /**
     * Begin a construction project on an owned, empty tile. Returns
     * null if the tile isn't owned, already has a structure or
     * in-progress build, or if the chosen structure is singleton and
     * one already exists in the world.
     */
    fun startBuild(
        x: Int,
        y: Int,
        type: StructureType,
        nowMs: Long,
        spendCoins: (Int) -> Boolean,
        spendEnergy: (Int) -> Boolean,
    ): LandTile? {
        val existing = tileAt(x, y) ?: return null
        if (!existing.owned) return null
        if (existing.structure != null || existing.isBuilding) return null
        if (type.singleton && hasStructure(type)) return null
        if (!spendCoins(type.buildCost)) return null
        if (!spendEnergy(type.buildEnergy)) return null
        val updated = existing.copy(
            structure = type,
            buildStartedMs = nowMs,
            buildDurationMs = type.buildDurationMs,
        )
        _ownedTiles[x to y] = updated
        db.landTiles().upsert(updated.toEntity())
        bump()
        return updated
    }

    /** Drain finished builds — clears the buildStartedMs/duration fields. */
    fun tick(nowMs: Long) {
        val toComplete = _ownedTiles.values.filter { it.isBuilding && it.buildComplete(nowMs) }
        if (toComplete.isEmpty()) return
        db.runInTransaction {
            for (tile in toComplete) {
                val finished = tile.copy(buildStartedMs = null, buildDurationMs = null)
                _ownedTiles[tile.x to tile.y] = finished
                db.landTiles().upsert(finished.toEntity())
            }
        }
        bump()
    }

    private fun bump() { revisionTick = revisionTick + 1 }

    private fun load() {
        db.landTiles().getAll().forEach { row ->
            row.toLandTileOrNull()?.let { _ownedTiles[it.x to it.y] = it }
        }
    }

    private fun LandTile.toEntity(): LandTileEntity = LandTileEntity(
        x = x, y = y,
        ownedAtMs = ownedAtMs,
        structure = structure?.name,
        buildStartedMs = buildStartedMs,
        buildDurationMs = buildDurationMs,
    )

    private fun LandTileEntity.toLandTileOrNull(): LandTile? {
        // Defensive: a tile without an owned-at timestamp shouldn't be
        // in the table at all, but skip rather than crash.
        if (ownedAtMs == null) return null
        return LandTile(
            x = x,
            y = y,
            ownedAtMs = ownedAtMs,
            structure = structure?.let { StructureType.valueOfOrNull(it) },
            buildStartedMs = buildStartedMs,
            buildDurationMs = buildDurationMs,
        )
    }

    companion object {
        @Volatile private var instance: LandService? = null

        fun get(context: Context): LandService {
            val existing = instance
            if (existing != null) return existing
            return synchronized(this) {
                instance ?: LandService(context.applicationContext).also { instance = it }
            }
        }
    }
}
