package com.shadaeiou.charmingfarmer.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.shadaeiou.charmingfarmer.data.room.AppDatabase
import com.shadaeiou.charmingfarmer.data.room.KilnRunEntity
import com.shadaeiou.charmingfarmer.data.room.LegacyMigrator
import kotlin.random.Random

/**
 * A kilning profile is the temperature/time recipe applied to germinated
 * grain to produce a specific malt color/flavor profile. Real-world:
 *  - Pale: ~80°C, fast — light, high-enzyme base malt
 *  - Munich: ~110°C, slightly toasty
 *  - Crystal: stewed first to convert starches, then kilned — caramel
 *  - Chocolate: high heat, long — deep coffee-cocoa notes, no roast bite
 *  - Black: roasted to char — espresso, dry bitter
 */
enum class KilnProfile(
    val displayName: String,
    val emoji: String,
    val outputType: ItemType,
    val durationMs: Long,
    val energyCost: Int,
    /** Score adjustment relative to input score. Crystal/chocolate are
     * trickier so they shave a few points; pale is forgiving. */
    val scoreOffset: Int,
) {
    PALE("Pale", "🟡", ItemType.MALT_PALE, 5 * 60_000L, 3, +2),
    MUNICH("Munich", "🟠", ItemType.MALT_MUNICH, 8 * 60_000L, 4, 0),
    CRYSTAL("Crystal", "🟤", ItemType.MALT_CRYSTAL, 10 * 60_000L, 5, -3),
    CHOCOLATE("Chocolate", "🟫", ItemType.MALT_CHOCOLATE, 12 * 60_000L, 6, -5),
    BLACK("Black", "⚫", ItemType.MALT_BLACK, 15 * 60_000L, 7, -6),
}

data class KilnRun(
    val id: Long,
    val inputType: ItemType,
    val inputScore: Int,
    val inputTier: ItemTier,
    val profile: KilnProfile,
    val startMs: Long,
) {
    fun progress(nowMs: Long): Float {
        if (DebugSettings.skipTimers) return 1f
        return ((nowMs - startMs).toFloat() / profile.durationMs).coerceIn(0f, 1f)
    }

    fun isComplete(nowMs: Long): Boolean =
        DebugSettings.skipTimers || nowMs - startMs >= profile.durationMs

    fun remainingMs(nowMs: Long): Long {
        if (DebugSettings.skipTimers) return 0L
        return (startMs + profile.durationMs - nowMs).coerceAtLeast(0)
    }
}

/**
 * Malthouse equipment tier. Caps the output score and determines how many
 * concurrent kiln slots the player can run.
 */
enum class MalthouseTier(
    val displayName: String,
    val qualityCap: Int,
    val kilnSlots: Int,
    val unlockCost: Int,
) {
    BASIC("Stovetop kiln", 70, 1, 0),
    DRUM("Drum kiln", 82, 2, 800),
    PRO("Pro malting floor", 92, 3, 8_000),
}

private const val META_TIER = "malthouse_tier"
private const val META_NEXT_RUN_ID = "malthouse_next_run_id"

class Malthouse private constructor(appContext: Context) {

    private val db = AppDatabase.get(appContext).also {
        LegacyMigrator.migrateIfNeeded(appContext, it)
    }

    val activeRuns = mutableStateListOf<KilnRun>()
    var tier: MalthouseTier by mutableStateOf(MalthouseTier.BASIC)
        private set
    private var nextRunId: Long = 1L
    var revisionTick: Int by mutableStateOf(0)
        private set

    init {
        load()
    }

    fun availableSlots(): Int = (tier.kilnSlots - activeRuns.size).coerceAtLeast(0)

    /**
     * Kick off a kilning run. Returns null if no slot is free, the
     * input doesn't exist in the malthouse inventory, or the player
     * couldn't afford the energy cost.
     */
    fun startKiln(
        transport: TransportService,
        inputType: ItemType,
        profile: KilnProfile,
        nowMs: Long,
        spendEnergy: (Int) -> Boolean,
    ): KilnRun? {
        if (availableSlots() == 0) return null
        val (afterRemove, pulled) = transport.inventoryAt(Location.MALTHOUSE)
            .remove(inputType, 1) ?: return null
        if (!spendEnergy(profile.energyCost)) return null
        transport.setInventory(Location.MALTHOUSE, afterRemove)
        val pulledStack = pulled.firstOrNull() ?: return null
        val run = KilnRun(
            id = nextRunId++,
            inputType = inputType,
            inputScore = pulledStack.score,
            inputTier = pulledStack.tier,
            profile = profile,
            startMs = nowMs,
        )
        db.runInTransaction {
            db.kilnRuns().insert(run.toEntity())
            db.systemMeta().put(META_NEXT_RUN_ID, nextRunId.toString())
        }
        activeRuns += run
        bump()
        return run
    }

    /** Drain finished kilns into the malthouse inventory. */
    fun tick(transport: TransportService, nowMs: Long) {
        val done = activeRuns.filter { it.isComplete(nowMs) }
        if (done.isEmpty()) return
        for (run in done) {
            val skillBonus = Random.nextInt(0, 16)
            val producedScore = computeOutputScore(
                inputAverage = run.inputScore + run.profile.scoreOffset,
                skillBonus = skillBonus,
                equipmentCap = tier.qualityCap,
            )
            transport.addToInventory(
                Location.MALTHOUSE,
                ItemStack(
                    type = run.profile.outputType,
                    quantity = 1,
                    score = producedScore,
                    tier = run.inputTier,
                    createdMs = nowMs,
                ),
            )
        }
        db.runInTransaction {
            done.forEach { db.kilnRuns().deleteById(it.id) }
        }
        activeRuns.removeAll(done)
        bump()
    }

    /** Drop in-memory state and re-read from DB. Used by full-game reset. */
    fun reload() {
        activeRuns.clear()
        tier = MalthouseTier.BASIC
        nextRunId = 1L
        load()
        bump()
    }

    fun upgrade(to: MalthouseTier): Boolean {
        if (to.ordinal <= tier.ordinal) return false
        tier = to
        db.systemMeta().put(META_TIER, to.name)
        bump()
        return true
    }

    private fun bump() { revisionTick = revisionTick + 1 }

    private fun load() {
        tier = db.systemMeta().get(META_TIER)
            ?.let { runCatching { MalthouseTier.valueOf(it) }.getOrNull() }
            ?: MalthouseTier.BASIC
        nextRunId = db.systemMeta().get(META_NEXT_RUN_ID)?.toLongOrNull()?.coerceAtLeast(1L) ?: 1L
        db.kilnRuns().getAll().forEach { row ->
            row.toKilnRunOrNull()?.let { activeRuns += it }
        }
    }

    private fun KilnRun.toEntity(): KilnRunEntity = KilnRunEntity(
        id = id,
        inputType = inputType.name,
        inputScore = inputScore,
        inputTier = inputTier.name,
        profile = profile.name,
        startMs = startMs,
    )

    private fun KilnRunEntity.toKilnRunOrNull(): KilnRun? {
        val type = ItemType.valueOfOrNull(inputType) ?: return null
        val profileEnum = runCatching { KilnProfile.valueOf(profile) }.getOrNull() ?: return null
        return KilnRun(
            id = id,
            inputType = type,
            inputScore = inputScore.coerceIn(0, 100),
            inputTier = runCatching { ItemTier.valueOf(inputTier) }.getOrDefault(ItemTier.NORMAL),
            profile = profileEnum,
            startMs = startMs,
        )
    }

    companion object {
        @Volatile private var instance: Malthouse? = null

        fun get(context: Context): Malthouse {
            val existing = instance
            if (existing != null) return existing
            return synchronized(this) {
                instance ?: Malthouse(context.applicationContext).also { instance = it }
            }
        }
    }
}
