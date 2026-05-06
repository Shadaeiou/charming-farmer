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
    MALTHOUSE("Malthouse", "🌾"),
    BREWERY("Brewery", "🍺"),
    KITCHEN("Kitchen", "🍳"),
    MARKET("Market", "🏪"),
    CELLAR("Cellar", "🍷"),
}

enum class VehicleType(
    val displayName: String,
    val emoji: String,
    val capacityKg: Int,
    val tripDurationMs: Long,
    val energyCost: Int,
    val coinCost: Int,
    val unlockCost: Int,
) {
    WHEELBARROW("Wheelbarrow", "🛒", 5, 5 * 60_000L, 2, 0, 0),
    HORSE_CART("Horse Cart", "🐎", 25, 8 * 60_000L, 3, 0, 600),
    TRUCK("Truck", "🚜", 100, 4 * 60_000L, 5, 5, 6_000),
    TRAIN("Train", "🚂", 500, 2 * 60_000L, 8, 25, 80_000),
}

data class Trip(
    val id: Long,
    val vehicle: VehicleType,
    val origin: Location,
    val destination: Location,
    val cargo: List<ItemStack>,
    val startMs: Long,
    val durationMs: Long,
) {
    fun progress(nowMs: Long): Float =
        ((nowMs - startMs).toFloat() / durationMs).coerceIn(0f, 1f)

    fun isComplete(nowMs: Long): Boolean = nowMs - startMs >= durationMs

    fun remainingMs(nowMs: Long): Long = (startMs + durationMs - nowMs).coerceAtLeast(0)
}

/**
 * App-wide singleton, now backed by Room. State that this service
 * owns:
 *   - Per-location inventories (one row per ItemStack in inventory_stacks)
 *   - Owned vehicles (vehicles_owned)
 *   - In-flight trips (transport_trips, cargo serialized as JSON since
 *     it's read/written as a unit)
 *   - nextTripId counter (system_meta)
 *
 * Inventory edits use Room's @Transaction-backed replaceForLocation so
 * a ship() that subtracts from origin and adds to destination cannot
 * land half-applied even if interrupted.
 */
class TransportService private constructor(appContext: Context) {

    private val db = AppDatabase.get(appContext).also {
        LegacyMigrator.migrateIfNeeded(appContext, it)
    }

    private val _inventories: MutableMap<Location, Inventory> = mutableMapOf()
    private val _vehiclesOwned: MutableSet<VehicleType> = mutableSetOf()
    val activeTrips = mutableStateListOf<Trip>()
    private var nextTripId: Long = 1L

    var revisionTick: Int by mutableStateOf(0)
        private set

    init {
        load()
        // Wheelbarrow is always free; ensure it's persisted for new users.
        if (VehicleType.WHEELBARROW !in _vehiclesOwned) {
            _vehiclesOwned += VehicleType.WHEELBARROW
            db.vehicles().insert(VehicleOwnedEntity(VehicleType.WHEELBARROW.name))
        }
    }

    fun inventoryAt(location: Location): Inventory =
        _inventories[location] ?: Inventory()

    fun vehiclesOwned(): Set<VehicleType> = _vehiclesOwned.toSet()

    fun vehicleAvailable(vehicle: VehicleType, nowMs: Long): Boolean {
        if (vehicle !in _vehiclesOwned) return false
        return activeTrips.none { it.vehicle == vehicle && !it.isComplete(nowMs) }
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

    /**
     * Try to move [amount] of [type] (lowest-quality first) from [from] to
     * [to] using [vehicle]. Returns the new trip on success, null if the
     * vehicle isn't available, the inventory is short, or the cargo would
     * exceed vehicle capacity.
     */
    fun ship(
        from: Location,
        to: Location,
        type: ItemType,
        amount: Int,
        vehicle: VehicleType,
        nowMs: Long,
    ): Trip? {
        if (!vehicleAvailable(vehicle, nowMs)) return null
        if (amount <= 0 || amount > vehicle.capacityKg) return null
        val (afterRemove, pulled) = inventoryAt(from).remove(type, amount) ?: return null
        val tripId = nextTripId++
        val trip = Trip(
            id = tripId,
            vehicle = vehicle,
            origin = from,
            destination = to,
            cargo = pulled,
            startMs = nowMs,
            durationMs = vehicle.tripDurationMs,
        )
        // Origin inventory + active trips list + next-id counter all go
        // in one transaction so a half-ship can't strand cargo nowhere.
        db.runInTransaction {
            db.inventory().replaceForLocation(from.name, afterRemove.toEntities(from))
            db.trips().insert(trip.toEntity())
            db.systemMeta().put(SystemMetaKeys.NEXT_TRIP_ID, nextTripId.toString())
        }
        _inventories[from] = afterRemove
        activeTrips += trip
        bump()
        return trip
    }

    /**
     * Process completed trips: drain their cargo into the destination
     * inventory and remove them from the active list. Idempotent — call
     * from a tick loop.
     */
    fun tick(nowMs: Long) {
        val done = activeTrips.filter { it.isComplete(nowMs) }
        if (done.isEmpty()) return
        // Group cargo by destination so the per-location inventory write
        // happens once per destination instead of per-stack.
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

    fun unlockVehicle(vehicle: VehicleType): Boolean {
        if (vehicle in _vehiclesOwned) return false
        _vehiclesOwned += vehicle
        db.vehicles().insert(VehicleOwnedEntity(vehicle.name))
        bump()
        return true
    }

    private fun bump() { revisionTick = revisionTick + 1 }

    private fun load() {
        // Inventories: one query, partition by location.
        db.inventory().getAll().groupBy { it.location }.forEach { (locName, rows) ->
            val location = runCatching { Location.valueOf(locName) }.getOrNull() ?: return@forEach
            val stacks = rows.mapNotNull { it.toItemStackOrNull() }
            _inventories[location] = Inventory(stacks)
        }
        // Vehicles
        db.vehicles().getAll().forEach { name ->
            runCatching { VehicleType.valueOf(name) }.getOrNull()?.let { _vehiclesOwned += it }
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
            vehicle = vehicle.name,
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
            vehicle = veh,
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
