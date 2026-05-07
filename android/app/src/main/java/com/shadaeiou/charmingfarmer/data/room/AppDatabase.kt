package com.shadaeiou.charmingfarmer.data.room

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema version 1 — mirrors the data we currently persist across the
 * three SharedPreferences blobs. Every entity uses TEXT for enum names
 * so adding/removing/renaming an enum value never crashes the load:
 * the DAOs return raw strings, the in-memory model decides whether to
 * accept them.
 *
 * Why entities instead of a single big JSON blob:
 *   - Inventories grow without bound; row-per-stack lets us query with
 *     SQL ("biggest stack of barley", "all bottles aged > 7 days")
 *     instead of deserialising everything every action.
 *   - Atomic multi-table updates via @Transaction: shipping cargo
 *     subtracts from origin AND adds to destination AND inserts the
 *     trip in one commit, so we can never half-fail.
 *   - Schema migrations are versioned by Room; the CLAUDE.md
 *     "never break saves" rule becomes structurally enforced when we
 *     bump VERSION = 2.
 */

// -- Entities ---------------------------------------------------------

/** Singleton row holding all the FarmState scalars. id is always 1. */
@Entity(tableName = "game_state")
data class GameStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val energy: Float,
    @ColumnInfo(name = "max_energy") val maxEnergy: Int,
    @ColumnInfo(name = "regen_ms") val regenMs: Long,
    @ColumnInfo(name = "last_tick_ms") val lastTickMs: Long,
    val coins: Int,
    val harvested: Int,
    @ColumnInfo(name = "selected_seed") val selectedSeed: String,
    @ColumnInfo(name = "selected_tree") val selectedTree: String?,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

/** One row per plot position (0..plotCount - 1). plotCount expands as
 *  the player completes farm-expansion goals (16 → 25 → 36 → 49 → 64). */
@Entity(tableName = "plots")
data class PlotEntity(
    @PrimaryKey val position: Int,
    val kind: String,
    val crop: String?,
    val tree: String?,
    @ColumnInfo(name = "planted_at_ms") val plantedAtMs: Long,
    val watered: Boolean,
    @ColumnInfo(name = "bonus_ms") val bonusMs: Long,
    @ColumnInfo(name = "harvest_count") val harvestCount: Int,
)

@Entity(tableName = "upgrade_levels")
data class UpgradeLevelEntity(
    @PrimaryKey val key: String,
    val level: Int,
)

@Entity(tableName = "birds_seen")
data class BirdSeenEntity(
    @PrimaryKey @ColumnInfo(name = "species_key") val speciesKey: String,
    val count: Int,
)

@Entity(tableName = "inventory_stacks")
data class InventoryStackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val location: String,
    val type: String,
    val quantity: Int,
    val score: Int,
    val tier: String,
    @ColumnInfo(name = "created_ms") val createdMs: Long,
)

@Entity(tableName = "vehicles_owned")
data class VehicleOwnedEntity(
    @PrimaryKey val vehicle: String,
)

@Entity(tableName = "transport_trips")
data class TransportTripEntity(
    @PrimaryKey val id: Long,
    val vehicle: String,
    val origin: String,
    val destination: String,
    @ColumnInfo(name = "cargo_json") val cargoJson: String,
    @ColumnInfo(name = "start_ms") val startMs: Long,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
)

/** Generic key/value table for migration markers, counters, etc. */
@Entity(tableName = "system_meta")
data class SystemMetaEntity(
    @PrimaryKey val key: String,
    val value: String,
)

/**
 * One row per kilning run currently underway in the malthouse. Schema
 * added in v2; v1 installs migrate via [MIGRATION_1_2].
 */
@Entity(tableName = "kiln_runs")
data class KilnRunEntity(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "input_type") val inputType: String,
    @ColumnInfo(name = "input_score") val inputScore: Int,
    @ColumnInfo(name = "input_tier") val inputTier: String,
    val profile: String,
    @ColumnInfo(name = "start_ms") val startMs: Long,
)

/**
 * One row per brewing batch in flight. Recipe references [BeerRecipe]
 * by name; ingredient_scores_json carries the input quality data
 * captured at brew start so the final BJCP score can be computed
 * deterministically when the batch finishes. Stage transitions live
 * entirely in the engine — only stage_started_ms is persisted, so
 * resuming after a kill resumes from the right point in the right
 * stage. Schema added in v3.
 */
@Entity(tableName = "brew_batches")
data class BrewBatchEntity(
    @PrimaryKey val id: Long,
    val recipe: String,
    val stage: String,
    @ColumnInfo(name = "stage_started_ms") val stageStartedMs: Long,
    @ColumnInfo(name = "ingredient_scores_json") val ingredientScoresJson: String,
    @ColumnInfo(name = "ingredient_tier") val ingredientTier: String,
)

/**
 * One row per OWNED tile in the world map. Unowned tiles are virtual
 * — they're computed on demand from a deterministic biome function
 * keyed on (x, y), so an infinite map costs zero storage until the
 * player buys land. Schema added in v4.
 *
 * Composite primary key (x, y) — Room supports this via the
 * primaryKeys array on @Entity.
 */
@Entity(tableName = "land_tiles", primaryKeys = ["x", "y"])
data class LandTileEntity(
    val x: Int,
    val y: Int,
    @ColumnInfo(name = "owned_at_ms") val ownedAtMs: Long?,
    val structure: String?,
    @ColumnInfo(name = "build_started_ms") val buildStartedMs: Long?,
    @ColumnInfo(name = "build_duration_ms") val buildDurationMs: Long?,
)

// -- DAOs -------------------------------------------------------------

@Dao
interface GameStateDao {
    @Query("SELECT * FROM game_state WHERE id = :id LIMIT 1")
    fun getOrNull(id: Int = GameStateEntity.SINGLETON_ID): GameStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(state: GameStateEntity)
}

@Dao
interface PlotDao {
    @Query("SELECT * FROM plots ORDER BY position")
    fun getAll(): List<PlotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(plot: PlotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAll(plots: List<PlotEntity>)

    @Query("DELETE FROM plots")
    fun deleteAll()
}

@Dao
interface UpgradeLevelDao {
    @Query("SELECT * FROM upgrade_levels")
    fun getAll(): List<UpgradeLevelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: UpgradeLevelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAll(entities: List<UpgradeLevelEntity>)

    @Query("DELETE FROM upgrade_levels")
    fun deleteAll()
}

@Dao
interface BirdSeenDao {
    @Query("SELECT * FROM birds_seen")
    fun getAll(): List<BirdSeenEntity>

    @Query("SELECT count FROM birds_seen WHERE species_key = :key LIMIT 1")
    fun getCount(key: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: BirdSeenEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAll(entities: List<BirdSeenEntity>)

    @Query("DELETE FROM birds_seen")
    fun deleteAll()
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_stacks WHERE location = :location")
    fun getForLocation(location: String): List<InventoryStackEntity>

    @Query("SELECT * FROM inventory_stacks")
    fun getAll(): List<InventoryStackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: InventoryStackEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(entities: List<InventoryStackEntity>)

    @Query("DELETE FROM inventory_stacks WHERE location = :location")
    fun deleteForLocation(location: String)

    @Query("DELETE FROM inventory_stacks")
    fun deleteAll()

    /**
     * Atomic replace of the entire inventory at a single location. Used
     * when the in-memory Inventory has been recomputed (e.g. after a
     * remove() that pulled from multiple stacks).
     */
    @Transaction
    fun replaceForLocation(location: String, entities: List<InventoryStackEntity>) {
        deleteForLocation(location)
        if (entities.isNotEmpty()) insertAll(entities)
    }
}

@Dao
interface VehicleDao {
    @Query("SELECT vehicle FROM vehicles_owned")
    fun getAll(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: VehicleOwnedEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(entities: List<VehicleOwnedEntity>)

    @Query("DELETE FROM vehicles_owned")
    fun deleteAll()
}

@Dao
interface TripDao {
    @Query("SELECT * FROM transport_trips")
    fun getAll(): List<TransportTripEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: TransportTripEntity)

    @Query("DELETE FROM transport_trips WHERE id = :id")
    fun deleteById(id: Long)

    @Query("DELETE FROM transport_trips")
    fun deleteAll()
}

@Dao
interface SystemMetaDao {
    @Query("SELECT value FROM system_meta WHERE key = :key LIMIT 1")
    fun get(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun put(entity: SystemMetaEntity)

    fun put(key: String, value: String) = put(SystemMetaEntity(key, value))

    /**
     * Wipe every meta key except [keepKeys]. Used by full-game reset to
     * preserve the legacy-migration marker without restoring every other
     * service's defaults by hand.
     */
    @Query("DELETE FROM system_meta WHERE key NOT IN (:keepKeys)")
    fun deleteAllExcept(keepKeys: List<String>)
}

@Dao
interface KilnRunDao {
    @Query("SELECT * FROM kiln_runs")
    fun getAll(): List<KilnRunEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(run: KilnRunEntity)

    @Query("DELETE FROM kiln_runs WHERE id = :id")
    fun deleteById(id: Long)

    @Query("DELETE FROM kiln_runs")
    fun deleteAll()
}

@Dao
interface BrewBatchDao {
    @Query("SELECT * FROM brew_batches")
    fun getAll(): List<BrewBatchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(batch: BrewBatchEntity)

    @Query("DELETE FROM brew_batches WHERE id = :id")
    fun deleteById(id: Long)

    @Query("DELETE FROM brew_batches")
    fun deleteAll()
}

@Dao
interface LandTileDao {
    @Query("SELECT * FROM land_tiles")
    fun getAll(): List<LandTileEntity>

    @Query("SELECT * FROM land_tiles WHERE x = :x AND y = :y LIMIT 1")
    fun get(x: Int, y: Int): LandTileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(tile: LandTileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAll(tiles: List<LandTileEntity>)

    @Query("DELETE FROM land_tiles WHERE x = :x AND y = :y")
    fun deleteAt(x: Int, y: Int)

    @Query("DELETE FROM land_tiles")
    fun deleteAll()
}

// -- Migrations -------------------------------------------------------

val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS kiln_runs (
                id INTEGER NOT NULL PRIMARY KEY,
                input_type TEXT NOT NULL,
                input_score INTEGER NOT NULL,
                input_tier TEXT NOT NULL,
                profile TEXT NOT NULL,
                start_ms INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS brew_batches (
                id INTEGER NOT NULL PRIMARY KEY,
                recipe TEXT NOT NULL,
                stage TEXT NOT NULL,
                stage_started_ms INTEGER NOT NULL,
                ingredient_scores_json TEXT NOT NULL,
                ingredient_tier TEXT NOT NULL
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS land_tiles (
                x INTEGER NOT NULL,
                y INTEGER NOT NULL,
                owned_at_ms INTEGER,
                structure TEXT,
                build_started_ms INTEGER,
                build_duration_ms INTEGER,
                PRIMARY KEY(x, y)
            )
            """.trimIndent()
        )
        // Seed the initial layout. Existing players see their stuff
        // already placed so the world-map transition isn't disorienting;
        // brand-new players see the same starter cluster. Coordinates
        // are signed, House at origin, neighbours laid out along the
        // four cardinal directions.
        val now = System.currentTimeMillis()
        val seedTiles = listOf(
            Triple(0, 0, "HOUSE"),
            Triple(1, 0, "FARM_FIELD"),
            Triple(-1, 0, "POND"),
            Triple(0, 1, "BIRDWATCHING"),
            Triple(2, 0, "MALTHOUSE"),
            Triple(3, 0, "BREWERY"),
        )
        for ((x, y, structure) in seedTiles) {
            db.execSQL(
                "INSERT OR IGNORE INTO land_tiles (x, y, owned_at_ms, structure) VALUES (?, ?, ?, ?)",
                arrayOf<Any>(x, y, now, structure),
            )
        }
    }
}

// -- Database ---------------------------------------------------------

@Database(
    entities = [
        GameStateEntity::class,
        PlotEntity::class,
        UpgradeLevelEntity::class,
        BirdSeenEntity::class,
        InventoryStackEntity::class,
        VehicleOwnedEntity::class,
        TransportTripEntity::class,
        SystemMetaEntity::class,
        KilnRunEntity::class,
        BrewBatchEntity::class,
        LandTileEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameState(): GameStateDao
    abstract fun plots(): PlotDao
    abstract fun upgradeLevels(): UpgradeLevelDao
    abstract fun birdsSeen(): BirdSeenDao
    abstract fun inventory(): InventoryDao
    abstract fun vehicles(): VehicleDao
    abstract fun trips(): TripDao
    abstract fun systemMeta(): SystemMetaDao
    abstract fun kilnRuns(): KilnRunDao
    abstract fun brewBatches(): BrewBatchDao
    abstract fun landTiles(): LandTileDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            val existing = instance
            if (existing != null) return existing
            return synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }
        }

        private fun build(appContext: Context): AppDatabase =
            // allowMainThreadQueries is intentional — FarmGame loads its
            // state synchronously in init for Compose, and individual
            // saves are short single-row writes. If save latency ever
            // becomes noticeable we'll move writes to Dispatchers.IO via
            // a coroutine scope held on the FarmGame, but at our scale
            // this stays well under a frame.
            Room.databaseBuilder(appContext, AppDatabase::class.java, "charming-farmer.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .allowMainThreadQueries()
                .build()
    }
}
