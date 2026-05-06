package com.shadaeiou.charmingfarmer.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.shadaeiou.charmingfarmer.data.room.AppDatabase
import com.shadaeiou.charmingfarmer.data.room.LegacyMigrator

/**
 * App-wide debug toggles for play-testing and demos. State is
 * persisted in `system_meta` so flags survive across launches —
 * intentional: a debug session that disappears on restart is
 * useless for testing offline progression.
 *
 * Each toggle is a separate switch so testers can isolate one
 * mechanic at a time. The flags are read at the time check
 * sites:
 *   - skipTimers        Plot, KilnRun, Trip, fishing bite,
 *                       bird spawn intervals
 *   - infiniteEnergy    FarmGame.spendEnergy
 *   - infiniteCoins     handled by an explicit "Add 10k coins"
 *                       button rather than skipping cost
 *                       checks (lets you still feel costs)
 */
object DebugSettings {

    private const val KEY_SKIP_TIMERS = "debug_skip_timers"
    private const val KEY_INFINITE_ENERGY = "debug_infinite_energy"

    var skipTimers: Boolean by mutableStateOf(false)
        private set
    var infiniteEnergy: Boolean by mutableStateOf(false)
        private set

    @Volatile private var initialized = false
    private var db: AppDatabase? = null

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val database = AppDatabase.get(context.applicationContext).also {
                LegacyMigrator.migrateIfNeeded(context.applicationContext, it)
            }
            db = database
            skipTimers = database.systemMeta().get(KEY_SKIP_TIMERS) == "true"
            infiniteEnergy = database.systemMeta().get(KEY_INFINITE_ENERGY) == "true"
            initialized = true
        }
    }

    fun setSkipTimers(value: Boolean) {
        skipTimers = value
        db?.systemMeta()?.put(KEY_SKIP_TIMERS, value.toString())
    }

    fun setInfiniteEnergy(value: Boolean) {
        infiniteEnergy = value
        db?.systemMeta()?.put(KEY_INFINITE_ENERGY, value.toString())
    }
}
