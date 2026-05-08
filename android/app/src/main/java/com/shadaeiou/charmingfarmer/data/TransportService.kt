package com.shadaeiou.charmingfarmer.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.shadaeiou.charmingfarmer.data.room.AppDatabase
import com.shadaeiou.charmingfarmer.data.room.InventoryStackEntity
import com.shadaeiou.charmingfarmer.data.room.LegacyMigrator
import com.shadaeiou.charmingfarmer.data.room.SystemMetaKeys
import com.shadaeiou.charmingfarmer.data.room.TransportTripEntity
import com.shadaeiou.charmingfarmer.data.room.VehicleOwnedEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * Every location that can ship or receive cargo declares itself here.
 * Adding a new location is a one-line enum addition; routes between any
 * pair are then automatically supported by the transport panel.
 */
enum class Location(val displayName: String, val emoji: String) {
    FARM("Farm", "🌾"),
    MALTHOUSE("Malthouse", "🏭"),
    BREWERY("Brewery", "🍺"),
    KITCHEN("Kitchen", "🍳"),
    MARKET("Market", "🏪"),
    CELLAR("Cellar", "🍷"),
    ;

    /**
     * What this location is willing to receive. FARM / MARKET are the
     * universal sinks; specialised buildings only accept their inputs
     * (or, in MARKET's case, anything because everything sells).
     */
    fun accepts(item: ItemType): Boolean = when (this) {
        FARM -> true
        MALTHOUSE -> item in MALTING_GRAINS
        BREWERY -> item in BREWERY_INPUTS || item.name.startsWith("HOPS")
            || item.name.startsWith("YEAST_") || item.name.startsWith("MALT_")
        // Kitchen accepts raw farm crops as forward-looking ingredient
        // inventory. Recipes are still coin-priced today.
        KITCHEN -> item.name.startsWith("CROP_")
        MARKET -> true
        CELLAR -> item.name.startsWith("BEER_")
    }

    companion object {
        private val MALTING_GRAINS = setOf(
            ItemType.BARLEY, ItemType.WHEAT_GRAIN, ItemType.OATS, ItemType.RYE,
        )
        private val BREWERY_INPUTS = setOf(ItemType.HOPS)
    }
}

enum class VehicleType(
    val displayName: String,
    val emoji: String,
    val capacityLbs: Int,
    val tripDurationMs: Long,
    val energyCost: Int,
    val coinCost: Int,
    val unlockCost: Int,
) {
    WHEELBARROW("Wheelbarrow", "🛒", 5, 5 * 60_000L, 2, 0, 0),
    HANDCART("Handcart", "🛍️", 12, 6 * 60_000L, 2, 0, 200),
    HORSE_CART("Horse Cart", "🐎", 25, 8 * 60_000L, 3, 0, 600),
    CARGO_BIKE("Cargo Bike", "🚲", 45, 5 * 60_000L, 3, 0, 2_500),
    TRUCK("Tractor", "🚜", 100, 4 * 60_000L, 5, 5, 6_000),
    PICKUP_TRUCK("Pickup Truck", "🛻", 200, 4 * 60_000L, 5, 8, 35_000),
    DELIVERY_VAN("Delivery Van", "🚐", 400, 3 * 60_000L, 6, 12, 100_000),
    TRAIN("Train", "🚂", 500, 2 * 60_000L, 8, 25, 80_000),
    BOX_TRUCK("Box Truck", "🚚", 800, 3 * 60_000L, 7, 20, 500_000),
}

/**
 * One actual vehicle owned by the player. Multiple instances of the
 * same [type] can be owned (Box Truck 1, Box Truck 2, …). Identified
 * uniquely by [id]; trips reserve a specific instance, not just a type.
 */
data class OwnedVehicle(
    val id: Long,
    val type: VehicleType,
    val customName: String,
    val colorArgb: Int,
) {
    /** The user-facing name. Empty customName falls back to "Type N"
     *  but that fallback is computed by the service when a vehicle is
     *  first inserted, so this is a simple non-blank getter. */
    val displayName: String
        get() = customName.ifBlank { type.displayName }
}

data class Trip(
    val id: Long,
    val vehicleId: Long,
    val vehicleType: VehicleType,
    val origin: Location,
    val destination: Location,
    val cargo: List<ItemStack>,
    val startMs: Long,
    val durationMs: Long,
) {
    fun progress(nowMs: Long): Float {
        if (DebugSettings.skipTimers) return 1f
        return ((nowMs - startMs).toFloat() / durationMs).coerceIn(0f, 1f)
    }

    fun isComplete(nowMs: Long): Boolean =
        DebugSettings.skipTimers || nowMs - startMs >= durationMs

    fun remainingMs(nowMs: Long): Long {
        if (DebugSettings.skipTimers) return 0L
        return (startMs + durationMs - nowMs).coerceAtLeast(0)
    }
}

/**
 * App-wide singleton, backed by Room. State this service owns:
 *   - Per-location inventories (one row per ItemStack in inventory_stacks)
 *   - Owned vehicle instances (vehicles_owned, multi-instance per type)
 *   - In-flight trips (transport_trips, vehicleId reserves a specific
 *     vehicle until the trip arrives)
 *   - nextTripId counter (system_meta)
 */
class TransportService private constructor(appContext: Context) {

    private val db = AppDatabase.get(appContext).also {
        LegacyMigrator.migrateIfNeeded(appContext, it)
    }

    private val _inventories: MutableMap<Location, Inventory> = mutableMapOf()
    private val _ownedVehicles: MutableMap<Long, OwnedVehicle> = mutableMapOf()
    val activeTrips = mutableStateListOf<Trip>()
    private var nextTripId: Long = 1L

    var revisionTick: Int by mutableStateOf(0)
        private set

    init {
        load()
        ensureStarterWheelbarrow()
        // Migrated rows from MIGRATION_8_9 land with custom_name="" —
        // backfill a sensible default so the garage doesn't show empty
        // labels.
        backfillCustomNames()
    }

    fun inventoryAt(location: Location): Inventory =
        _inventories[location] ?: Inventory()

    fun vehiclesOwned(): List<OwnedVehicle> =
        _ownedVehicles.values.sortedWith(
            compareBy({ it.type.ordinal }, { it.id })
        )

    fun vehicleById(id: Long): OwnedVehicle? = _ownedVehicles[id]

    fun vehicleAvailable(id: Long, nowMs: Long): Boolean {
        if (id !in _ownedVehicles) return false
        return activeTrips.none { it.vehicleId == id && !it.isComplete(nowMs) }
    }

    fun addToInventory(location: Location, stack: ItemStack) {
        if (stack.quantity <= 0) return
        val updated = inventoryAt(location).add(stack)
        _inventories[location] = updated
        db.inventory().replaceForLocation(location.name, updated.toEntities(location))
        bump()
    }

    fun setInventory(location: Location, inventory: Inventory) {
        _inventories[location] = inventory
        db.inventory().replaceForLocation(location.name, inventory.toEntities(location))
        bump()
    }

    /** Single-item dispatch — convenience wrapper around [shipMultiple]. */
    fun ship(
        from: Location,
        to: Location,
        type: ItemType,
        amount: Int,
        vehicleId: Long,
        nowMs: Long,
    ): Trip? = shipMultiple(from, to, mapOf(type to amount), vehicleId, nowMs)

    /**
     * Multi-item dispatch: load several different ItemTypes onto one
     * vehicle instance, all bound for [to]. Returns null if the
     * vehicle is busy, the total quantity exceeds capacity, or any item
     * has fewer units in stock than requested.
     */
    fun shipMultiple(
        from: Location,
        to: Location,
        cargo: Map<ItemType, Int>,
        vehicleId: Long,
        nowMs: Long,
    ): Trip? {
        val owned = _ownedVehicles[vehicleId] ?: return null
        if (!vehicleAvailable(vehicleId, nowMs)) return null
        val cleaned = cargo.filterValues { it > 0 }
        if (cleaned.isEmpty()) return null
        val total = cleaned.values.sum()
        if (total > owned.type.capacityLbs) return null

        var inventory = inventoryAt(from)
        val pulled = mutableListOf<ItemStack>()
        for ((type, amount) in cleaned) {
            val (after, taken) = inventory.remove(type, amount) ?: return null
            inventory = after
            pulled += taken
        }

        val tripId = nextTripId++
        val trip = Trip(
            id = tripId,
            vehicleId = vehicleId,
            vehicleType = owned.type,
            origin = from,
            destination = to,
            cargo = pulled,
            startMs = nowMs,
            durationMs = owned.type.tripDurationMs,
        )
        db.runInTransaction {
            db.inventory().replaceForLocation(from.name, inventory.toEntities(from))
            db.trips().insert(trip.toEntity())
            db.systemMeta().put(SystemMetaKeys.NEXT_TRIP_ID, nextTripId.toString())
        }
        _inventories[from] = inventory
        activeTrips += trip
        bump()
        return trip
    }

    /**
     * Process completed trips: drain their cargo into the destination
     * inventory and remove them from the active list. Idempotent.
     */
    fun tick(nowMs: Long) {
        val done = activeTrips.filter { it.isComplete(nowMs) }
        if (done.isEmpty()) return
        val byDest: Map<Location, List<ItemStack>> = done
            .groupBy { it.destination }
            .mapValues { (_, trips) -> trips.flatMap { it.cargo } }
        db.runInTransaction {
            byDest.forEach { (dest, stacks) ->
                var inv = inventoryAt(dest)
                stacks.forEach { inv = inv.add(it) }
                _inventories[dest] = inv
                db.inventory().replaceForLocation(dest.name, inv.toEntities(dest))
            }
            done.forEach { db.trips().deleteById(it.id) }
        }
        activeTrips.removeAll(done)
        bump()
    }

    /** Drop in-memory state and re-read from a freshly wiped or
     *  re-seeded DB. Used by the full-game reset. */
    fun reload() {
        _inventories.clear()
        _ownedVehicles.clear()
        activeTrips.clear()
        nextTripId = 1L
        load()
        ensureStarterWheelbarrow()
        backfillCustomNames()
        bump()
    }

    /**
     * Buy a fresh vehicle of [type] with the given [customName] and
     * paint [colorArgb]. Returns the new instance with its assigned id,
     * or null if the insert failed (which shouldn't happen in practice).
     */
    fun unlockVehicle(
        type: VehicleType,
        customName: String = defaultNameFor(type),
        colorArgb: Int = VehicleOwnedEntity.DEFAULT_VEHICLE_COLOR,
    ): OwnedVehicle? {
        val nameToUse = customName.ifBlank { defaultNameFor(type) }
        val newId = db.vehicles().insert(
            VehicleOwnedEntity(
                vehicle = type.name,
                customName = nameToUse,
                colorArgb = colorArgb,
            )
        )
        if (newId <= 0) return null
        val owned = OwnedVehicle(newId, type, nameToUse, colorArgb)
        _ownedVehicles[newId] = owned
        bump()
        return owned
    }

    /** Sell an owned vehicle for 50% of its unlock cost. The starter
     *  wheelbarrow (the only free one) and vehicles already in transit
     *  cannot be sold. Returns the payout, or null on failure. */
    fun sellVehicle(id: Long, nowMs: Long): Int? {
        val owned = _ownedVehicles[id] ?: return null
        // Don't let the player sell their last free wheelbarrow.
        if (owned.type == VehicleType.WHEELBARROW &&
            _ownedVehicles.values.count { it.type == VehicleType.WHEELBARROW } == 1) {
            return null
        }
        if (!vehicleAvailable(id, nowMs)) return null
        _ownedVehicles.remove(id)
        db.vehicles().deleteById(id)
        bump()
        return owned.type.unlockCost / 2
    }

    fun setColor(id: Long, colorArgb: Int) {
        val owned = _ownedVehicles[id] ?: return
        _ownedVehicles[id] = owned.copy(colorArgb = colorArgb)
        db.vehicles().updateColor(id, colorArgb)
        bump()
    }

    fun renameVehicle(id: Long, newName: String) {
        val owned = _ownedVehicles[id] ?: return
        val nameToUse = newName.ifBlank { defaultNameFor(owned.type) }
        _ownedVehicles[id] = owned.copy(customName = nameToUse)
        db.vehicles().updateName(id, nameToUse)
        bump()
    }

    /** Suggested name for the next vehicle of [type] purchased — picks
     *  the smallest positive integer that isn't already in use as the
     *  trailing number, so renaming or selling a middle entry doesn't
     *  leave gaps. */
    fun defaultNameFor(type: VehicleType): String {
        val prefix = type.displayName
        val takenIndices = _ownedVehicles.values
            .filter { it.type == type }
            .mapNotNull { it.customName.removePrefix("$prefix ").toIntOrNull() }
            .toSet()
        var n = 1
        while (n in takenIndices) n += 1
        return "$prefix $n"
    }

    private fun ensureStarterWheelbarrow() {
        if (_ownedVehicles.values.any { it.type == VehicleType.WHEELBARROW }) return
        unlockVehicle(VehicleType.WHEELBARROW)
    }

    private fun backfillCustomNames() {
        // Rows imported from MIGRATION_8_9 have customName == "". Give
        // each one the canonical "{Type} {N}" so the UI shows something
        // sensible right away. Numbering is per-type, in id order.
        val grouped = _ownedVehicles.values
            .filter { it.customName.isBlank() }
            .groupBy { it.type }
        if (grouped.isEmpty()) return
        for ((type, instances) in grouped) {
            val sorted = instances.sortedBy { it.id }
            sorted.forEachIndexed { idx, owned ->
                val name = "${type.displayName} ${idx + 1}"
                _ownedVehicles[owned.id] = owned.copy(customName = name)
                db.vehicles().updateName(owned.id, name)
            }
        }
        bump()
    }

    private fun bump() { revisionTick = revisionTick + 1 }

    private fun load() {
        // Inventories: one query, partition by location.
        db.inventory().getAll().groupBy { it.location }.forEach { (locName, rows) ->
            val location = runCatching { Location.valueOf(locName) }.getOrNull() ?: return@forEach
            val stacks = rows.mapNotNull { it.toItemStackOrNull() }
            _inventories[location] = Inventory(stacks)
        }
        // Vehicle instances
        db.vehicles().getAll().forEach { entity ->
            val type = runCatching { VehicleType.valueOf(entity.vehicle) }.getOrNull() ?: return@forEach
            _ownedVehicles[entity.id] = OwnedVehicle(
                id = entity.id,
                type = type,
                customName = entity.customName,
                colorArgb = entity.colorArgb,
            )
        }
        // Trips
        db.trips().getAll().forEach { row ->
            row.toTripOrNull()?.let { activeTrips += it }
        }
        // Counter
        nextTripId = db.systemMeta().get(SystemMetaKeys.NEXT_TRIP_ID)
            ?.toLongOrNull()
            ?.coerceAtLeast(1L)
            ?: ((activeTrips.maxOfOrNull { it.id } ?: 0L) + 1L).coerceAtLeast(1L)
    }

    // -- Entity <-> domain mapping --------------------------------------

    private fun Inventory.toEntities(location: Location): List<InventoryStackEntity> =
        stacks.map { stack ->
            InventoryStackEntity(
                location = location.name,
                type = stack.type.name,
                quantity = stack.quantity,
                score = stack.score,
                tier = stack.tier.name,
                createdMs = stack.createdMs,
            )
        }

    private fun InventoryStackEntity.toItemStackOrNull(): ItemStack? {
        val type = ItemType.valueOfOrNull(type) ?: return null
        if (quantity <= 0) return null
        val tier = runCatching { ItemTier.valueOf(tier) }.getOrDefault(ItemTier.NORMAL)
        return ItemStack(
            type = type,
            quantity = quantity,
            score = score.coerceIn(0, 100),
            tier = tier,
            createdMs = createdMs,
        )
    }

    private fun Trip.toEntity(): TransportTripEntity {
        val cargoJson = JSONArray().apply {
            cargo.forEach { stack ->
                put(JSONObject()
                    .put("type", stack.type.name)
                    .put("quantity", stack.quantity)
                    .put("score", stack.score)
                    .put("tier", stack.tier.name)
                    .put("createdMs", stack.createdMs))
            }
        }
        return TransportTripEntity(
            id = id,
            vehicle = vehicleType.name,
            vehicleId = vehicleId,
            origin = origin.name,
            destination = destination.name,
            cargoJson = cargoJson.toString(),
            startMs = startMs,
            durationMs = durationMs,
        )
    }

    private fun TransportTripEntity.toTripOrNull(): Trip? {
        val veh = runCatching { VehicleType.valueOf(vehicle) }.getOrNull() ?: return null
        val orig = runCatching { Location.valueOf(origin) }.getOrNull() ?: return null
        val dest = runCatching { Location.valueOf(destination) }.getOrNull() ?: return null
        val cargoArr = runCatching { JSONArray(cargoJson) }.getOrNull() ?: return null
        val cargo = mutableListOf<ItemStack>()
        for (i in 0 until cargoArr.length()) {
            val s = cargoArr.optJSONObject(i) ?: continue
            val type = ItemType.valueOfOrNull(s.optString("type")) ?: continue
            val qty = s.optInt("quantity", 0)
            if (qty <= 0) continue
            cargo += ItemStack(
                type = type,
                quantity = qty,
                score = s.optInt("score", 50),
                tier = runCatching { ItemTier.valueOf(s.optString("tier", "NORMAL")) }
                    .getOrDefault(ItemTier.NORMAL),
                createdMs = s.optLong("createdMs", 0L),
            )
        }
        return Trip(
            id = id,
            vehicleId = vehicleId,
            vehicleType = veh,
            origin = orig,
            destination = dest,
            cargo = cargo,
            startMs = startMs,
            durationMs = durationMs,
        )
    }

    companion object {
        @Volatile private var instance: TransportService? = null

        fun get(context: Context): TransportService {
            val existing = instance
            if (existing != null) return existing
            return synchronized(this) {
                instance ?: TransportService(context.applicationContext).also { instance = it }
            }
        }
    }
}
