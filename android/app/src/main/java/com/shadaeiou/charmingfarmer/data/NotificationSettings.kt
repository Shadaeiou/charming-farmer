package com.shadaeiou.charmingfarmer.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.shadaeiou.charmingfarmer.data.room.AppDatabase
import com.shadaeiou.charmingfarmer.data.room.LegacyMigrator

/**
 * Per-channel push-notification preferences. Persisted in
 * `system_meta` like every other small toggle. Channels:
 *   - cropsReady   — a planted crop is ready to harvest
 *   - craftDone    — a kiln / brewery / kitchen run completes
 *   - tripDone     — a transport trip arrives
 *
 * The two top-level switches let players quickly silence everything
 * (`pushEnabled = false`) or block delivery while the app is in the
 * foreground (`silentInForeground = true`). Channel toggles are only
 * meaningful when [pushEnabled] is true.
 */
object NotificationSettings {

    private const val KEY_PUSH_ENABLED = "notif_push_enabled"
    private const val KEY_SILENT_FG = "notif_silent_in_foreground"
    private const val KEY_CROPS_READY = "notif_crops_ready"
    private const val KEY_CRAFT_DONE = "notif_craft_done"
    private const val KEY_TRIP_DONE = "notif_trip_done"

    var pushEnabled: Boolean by mutableStateOf(true)
        private set
    var silentInForeground: Boolean by mutableStateOf(true)
        private set
    var cropsReady: Boolean by mutableStateOf(true)
        private set
    var craftDone: Boolean by mutableStateOf(true)
        private set
    var tripDone: Boolean by mutableStateOf(true)
        private set

    /**
     * True if the app currently has a live foreground Compose host;
     * push handlers consult this so a silent-in-foreground player
     * doesn't get a notification for an event they can already see.
     */
    @Volatile var appInForeground: Boolean = false

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
            pushEnabled = readBool(database, KEY_PUSH_ENABLED, true)
            silentInForeground = readBool(database, KEY_SILENT_FG, true)
            cropsReady = readBool(database, KEY_CROPS_READY, true)
            craftDone = readBool(database, KEY_CRAFT_DONE, true)
            tripDone = readBool(database, KEY_TRIP_DONE, true)
            initialized = true
        }
    }

    /** True if a notification for [channel] should be delivered now. */
    fun shouldNotify(channel: Channel): Boolean {
        if (!pushEnabled) return false
        if (silentInForeground && appInForeground) return false
        return when (channel) {
            Channel.CROPS_READY -> cropsReady
            Channel.CRAFT_DONE -> craftDone
            Channel.TRIP_DONE -> tripDone
        }
    }

    fun updatePushEnabled(value: Boolean) { pushEnabled = value; persist(KEY_PUSH_ENABLED, value) }
    fun updateSilentInForeground(value: Boolean) { silentInForeground = value; persist(KEY_SILENT_FG, value) }
    fun updateCropsReady(value: Boolean) { cropsReady = value; persist(KEY_CROPS_READY, value) }
    fun updateCraftDone(value: Boolean) { craftDone = value; persist(KEY_CRAFT_DONE, value) }
    fun updateTripDone(value: Boolean) { tripDone = value; persist(KEY_TRIP_DONE, value) }

    private fun persist(key: String, value: Boolean) {
        db?.systemMeta()?.put(key, value.toString())
    }

    private fun readBool(database: AppDatabase, key: String, default: Boolean): Boolean {
        return database.systemMeta().get(key)?.let { it == "true" } ?: default
    }

    enum class Channel { CROPS_READY, CRAFT_DONE, TRIP_DONE }
}
