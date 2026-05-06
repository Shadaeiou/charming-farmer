package com.shadaeiou.charmingfarmer.data.room

import android.content.Context
import android.util.Log
import org.json.JSONObject

/**
 * One-time migration from the old SharedPreferences blobs to the Room
 * database. Idempotent: if [SystemMetaKeys.MIGRATED_FROM_PREFS] is set,
 * this no-ops.
 *
 * We deliberately leave the old prefs files in place after migrating,
 * so a forced re-migration is possible by clearing the meta marker
 * (debug builds, support escape hatches, etc.).
 */
object LegacyMigrator {

    private const val TAG = "LegacyMigrator"

    fun migrateIfNeeded(context: Context, db: AppDatabase) {
        val meta = db.systemMeta()
        if (meta.get(SystemMetaKeys.MIGRATED_FROM_PREFS) == "true") return

        runCatching {
            migrateFarmState(context, db)
            migrateTransport(context, db)
        }.onFailure {
            Log.w(TAG, "Migration partial/failed: ${it.message}", it)
        }

        meta.put(SystemMetaKeys.MIGRATED_FROM_PREFS, "true")
        Log.i(TAG, "Migration to Room complete")
    }

    private fun migrateFarmState(context: Context, db: AppDatabase) {
        val prefs = context.getSharedPreferences("charming-farmer-v1", Context.MODE_PRIVATE)
        val raw = prefs.getString("state", null) ?: return
        val o = runCatching { JSONObject(raw) }.getOrNull() ?: return

        // game_state singleton
        db.gameState().upsert(GameStateEntity(
            energy = o.optDouble("energy", 50.0).toFloat(),
            maxEnergy = o.optInt("maxEnergy", 50),
            regenMs = o.optLong("regenMs", 3000L),
            lastTickMs = o.optLong("lastTickMs", System.currentTimeMillis()),
            coins = o.optInt("coins", 5),
            harvested = o.optInt("harvested", 0),
            selectedSeed = o.optString("selectedSeed", "CARROT"),
            selectedTree = o.optString("selectedTree").takeIf { it.isNotEmpty() },
        ))

        // plots
        val plotsJson = o.optJSONArray("plots")
        if (plotsJson != null) {
            val plots = mutableListOf<PlotEntity>()
            for (i in 0 until plotsJson.length()) {
                val po = plotsJson.optJSONObject(i) ?: continue
                plots += PlotEntity(
                    position = i,
                    kind = po.optString("kind", "GRASS"),
                    crop = po.optString("crop").takeIf { it.isNotEmpty() },
                    tree = po.optString("tree").takeIf { it.isNotEmpty() },
                    plantedAtMs = po.optLong("plantedAtMs", 0L),
                    watered = po.optBoolean("watered", false),
                    bonusMs = po.optLong("bonusMs", 0L),
                    harvestCount = po.optInt("harvestCount", 0),
                )
            }
            if (plots.isNotEmpty()) db.plots().upsertAll(plots)
        }

        // upgrade_levels
        val upJson = o.optJSONObject("upgrades")
        if (upJson != null) {
            val rows = mutableListOf<UpgradeLevelEntity>()
            val keys = upJson.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                rows += UpgradeLevelEntity(key = k, level = upJson.optInt(k, 0))
            }
            if (rows.isNotEmpty()) db.upgradeLevels().upsertAll(rows)
        }

        // birds_seen
        val birdsJson = o.optJSONObject("birdsSeen")
        if (birdsJson != null) {
            val rows = mutableListOf<BirdSeenEntity>()
            val keys = birdsJson.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val count = birdsJson.optInt(k, 0)
                if (count > 0) rows += BirdSeenEntity(speciesKey = k, count = count)
            }
            if (rows.isNotEmpty()) db.birdsSeen().upsertAll(rows)
        }
    }

    private fun migrateTransport(context: Context, db: AppDatabase) {
        val prefs = context.getSharedPreferences("charming-farmer-transport-v1", Context.MODE_PRIVATE)
        val raw = prefs.getString("state", null) ?: return
        val o = runCatching { JSONObject(raw) }.getOrNull() ?: return

        // inventories — flatten to one row per (location, stack)
        val invs = o.optJSONObject("inventories")
        if (invs != null) {
            val keys = invs.keys()
            val rows = mutableListOf<InventoryStackEntity>()
            while (keys.hasNext()) {
                val location = keys.next()
                val arr = invs.optJSONArray(location) ?: continue
                for (i in 0 until arr.length()) {
                    val s = arr.optJSONObject(i) ?: continue
                    rows += InventoryStackEntity(
                        location = location,
                        type = s.optString("type"),
                        quantity = s.optInt("quantity", 0),
                        score = s.optInt("score", 50),
                        tier = s.optString("tier", "NORMAL"),
                        createdMs = s.optLong("createdMs", 0L),
                    )
                }
            }
            if (rows.isNotEmpty()) db.inventory().insertAll(rows)
        }

        // vehicles
        val vehs = o.optJSONArray("vehicles")
        if (vehs != null) {
            val rows = mutableListOf<VehicleOwnedEntity>()
            for (i in 0 until vehs.length()) {
                val name = vehs.optString(i)
                if (name.isNotBlank()) rows += VehicleOwnedEntity(vehicle = name)
            }
            if (rows.isNotEmpty()) db.vehicles().insertAll(rows)
        }

        // trips
        val trips = o.optJSONArray("trips")
        if (trips != null) {
            for (i in 0 until trips.length()) {
                val t = trips.optJSONObject(i) ?: continue
                db.trips().insert(TransportTripEntity(
                    id = t.optLong("id", 0L),
                    vehicle = t.optString("vehicle"),
                    origin = t.optString("origin"),
                    destination = t.optString("destination"),
                    cargoJson = t.optJSONArray("cargo")?.toString().orEmpty(),
                    startMs = t.optLong("startMs"),
                    durationMs = t.optLong("durationMs"),
                ))
            }
        }

        // counters
        val nextId = o.optLong("nextTripId", 1L)
        db.systemMeta().put(SystemMetaKeys.NEXT_TRIP_ID, nextId.toString())
    }
}

/** Centralized list of meta keys so they don't drift across files. */
object SystemMetaKeys {
    const val MIGRATED_FROM_PREFS = "migrated_from_prefs"
    const val NEXT_TRIP_ID = "next_trip_id"
}
