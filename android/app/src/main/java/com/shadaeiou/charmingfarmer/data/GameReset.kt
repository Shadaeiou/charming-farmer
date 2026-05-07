package com.shadaeiou.charmingfarmer.data

import android.content.Context
import com.shadaeiou.charmingfarmer.data.room.AppDatabase
import com.shadaeiou.charmingfarmer.data.room.GameStateEntity
import com.shadaeiou.charmingfarmer.data.room.LandTileEntity
import com.shadaeiou.charmingfarmer.data.room.LegacyMigrator
import com.shadaeiou.charmingfarmer.data.room.PlotEntity
import com.shadaeiou.charmingfarmer.data.room.SystemMetaKeys
import com.shadaeiou.charmingfarmer.data.room.VehicleOwnedEntity

/**
 * Wipes every gameplay table back to a fresh-install state and
 * re-seeds the starter layout (House + adjacent buildings + 16 grass
 * plots + Wheelbarrow). Migration markers in `system_meta` are
 * preserved so [LegacyMigrator] doesn't re-import old SharedPreferences
 * blobs on next launch.
 *
 * After the DB is wiped, every singleton service is told to reload
 * from disk so the in-memory state matches. Callers should also pop
 * the navigation stack so any per-screen [FarmGame] instances get
 * recreated against the fresh DB instead of redisplaying stale
 * Compose state.
 */
object GameReset {

    fun resetEverything(context: Context) {
        val ctx = context.applicationContext
        val db = AppDatabase.get(ctx).also {
            // Belt-and-braces: a reset shouldn't be the moment we discover
            // the legacy migration hasn't run.
            LegacyMigrator.migrateIfNeeded(ctx, it)
        }
        val now = System.currentTimeMillis()

        db.runInTransaction {
            // Wipe every gameplay table.
            db.plots().deleteAll()
            db.upgradeLevels().deleteAll()
            db.birdsSeen().deleteAll()
            db.inventory().deleteAll()
            db.vehicles().deleteAll()
            db.trips().deleteAll()
            db.kilnRuns().deleteAll()
            db.brewBatches().deleteAll()
            db.landTiles().deleteAll()

            // Reset the singleton game_state row to defaults.
            db.gameState().upsert(
                GameStateEntity(
                    energy = STARTING_ENERGY.toFloat(),
                    maxEnergy = STARTING_ENERGY,
                    regenMs = 3000L,
                    lastTickMs = now,
                    coins = STARTING_COINS,
                    harvested = 0,
                    selectedSeed = CropType.CARROT.name,
                    selectedTree = null,
                ),
            )

            // Re-seed the starter plot grid.
            db.plots().upsertAll(List(STARTING_PLOT_COUNT) {
                PlotEntity(
                    position = it,
                    kind = PlotKind.GRASS.name,
                    crop = null,
                    tree = null,
                    plantedAtMs = 0L,
                    watered = false,
                    bonusMs = 0L,
                    harvestCount = 0,
                )
            })

            // Re-seed the starter buildings on the world map. Mirrors
            // MIGRATION_3_4 so a wiped player matches a fresh install.
            db.landTiles().upsertAll(starterLandTiles(now))

            // Re-seed the free starter vehicle.
            db.vehicles().insert(VehicleOwnedEntity(VehicleType.WHEELBARROW.name))

            // Clear every system_meta key EXCEPT the legacy-migration
            // marker. Letting load() defaults handle the rest is simpler
            // than restoring every key by hand and dodges drift if a new
            // service adds its own meta keys.
            db.systemMeta().deleteAllExcept(listOf(SystemMetaKeys.MIGRATED_FROM_PREFS))
        }

        // Reload every singleton from the freshly-wiped DB so the UI
        // shows the new state immediately.
        TransportService.get(ctx).reload()
        Malthouse.get(ctx).reload()
        Brewery.get(ctx).reload()
        LandService.get(ctx).reload()
        DebugSettings.reload(ctx)
    }

    private fun starterLandTiles(nowMs: Long): List<LandTileEntity> = listOf(
        LandTileEntity(0, 0, nowMs, "HOUSE", null, null),
        LandTileEntity(1, 0, nowMs, "FARM_FIELD", null, null),
        LandTileEntity(-1, 0, nowMs, "POND", null, null),
        LandTileEntity(0, 1, nowMs, "BIRDWATCHING", null, null),
        LandTileEntity(2, 0, nowMs, "MALTHOUSE", null, null),
        LandTileEntity(3, 0, nowMs, "BREWERY", null, null),
    )
}
