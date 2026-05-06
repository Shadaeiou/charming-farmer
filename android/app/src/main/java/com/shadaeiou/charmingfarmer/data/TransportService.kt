package com.shadaeiou.charmingfarmer.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
 * App-wide singleton. Holds the transport state — owned vehicles, in-flight
 * trips, and per-location inventories. Persists to its own SharedPreferences
 * key (`charming-farmer-transport-v1`) so it grows independently of the
 * existing FarmGame state.
 *
 * Inventories live here too because they're the things the transport service
 * moves between. Any screen reads/writes them via this service.
 */
class TransportService private constructor(appContext: Context) {

    private val prefs = appContext.getSharedPreferences(
        "charming-farmer-transport-v1",
        Context.MODE_PRIVATE,
    )

    private val _inventories: MutableMap<Location, Inventory> = mutableMapOf()
    private val _vehiclesOwned: MutableSet<VehicleType> = mutableSetOf(VehicleType.WHEELBARROW)
    val activeTrips = mutableStateListOf<Trip>()
    private var nextTripId: Long = 1L

    var revisionTick: Int by mutableStateOf(0)
        private set

    init {
        load()
    }

    fun inventoryAt(location: Location): Inventory =
        _inventories[location] ?: Inventory()

    fun vehiclesOwned(): Set<VehicleType> = _vehiclesOwned.toSet()

    fun vehicleAvailable(vehicle: VehicleType, nowMs: Long): Boolean {
        if (vehicle !in _vehiclesOwned) return false
        // A vehicle can only be on one trip at a time. Once the trip is
        // complete it auto-returns instantly for simplicity (could become
        // a return leg later).
        return activeTrips.none { it.vehicle == vehicle && !it.isComplete(nowMs) }
    }

    fun addToInventory(location: Location, stack: ItemStack) {
        if (stack.quantity <= 0) return
        _inventories[location] = inventoryAt(location).add(stack)
        bump()
        save()
    }

    fun setInventory(location: Location, inventory: Inventory) {
        _inventories[location] = inventory
        bump()
        save()
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
        _inventories[from] = afterRemove
        val trip = Trip(
            id = nextTripId++,
            vehicle = vehicle,
            origin = from,
            destination = to,
            cargo = pulled,
            startMs = nowMs,
            durationMs = vehicle.tripDurationMs,
        )
        activeTrips += trip
        bump()
        save()
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
        for (trip in done) {
            for (stack in trip.cargo) {
                _inventories[trip.destination] = inventoryAt(trip.destination).add(stack)
            }
        }
        activeTrips.removeAll(done)
        bump()
        save()
    }

    fun unlockVehicle(vehicle: VehicleType): Boolean {
        if (vehicle in _vehiclesOwned) return false
        _vehiclesOwned += vehicle
        bump()
        save()
        return true
    }

    private fun bump() { revisionTick = revisionTick + 1 }

    private fun save() {
        val invJson = JSONObject()
        for ((loc, inv) in _inventories) {
            invJson.put(loc.name, inv.toJson())
        }
        val vehJson = JSONArray().apply {
            _vehiclesOwned.forEach { put(it.name) }
        }
        val tripsJson = JSONArray()
        for (t in activeTrips) {
            tripsJson.put(JSONObject()
                .put("id", t.id)
                .put("vehicle", t.vehicle.name)
                .put("origin", t.origin.name)
                .put("destination", t.destination.name)
                .put("startMs", t.startMs)
                .put("durationMs", t.durationMs)
                .put("cargo", JSONArray().also { c -> t.cargo.forEach { stack ->
                    c.put(JSONObject()
                        .put("type", stack.type.name)
                        .put("quantity", stack.quantity)
                        .put("score", stack.score)
                        .put("tier", stack.tier.name)
                        .put("createdMs", stack.createdMs))
                }}))
        }
        val root = JSONObject()
            .put("inventories", invJson)
            .put("vehicles", vehJson)
            .put("trips", tripsJson)
            .put("nextTripId", nextTripId)
        prefs.edit().putString("state", root.toString()).apply()
    }

    private fun load() {
        val raw = prefs.getString("state", null) ?: return
        runCatching {
            val root = JSONObject(raw)
            val invs = root.optJSONObject("inventories")
            if (invs != null) {
                val keys = invs.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val loc = runCatching { Location.valueOf(k) }.getOrNull() ?: continue
                    _inventories[loc] = Inventory.fromJson(invs.optJSONArray(k))
                }
            }
            val vehs = root.optJSONArray("vehicles")
            if (vehs != null) {
                for (i in 0 until vehs.length()) {
                    val name = vehs.optString(i)
                    runCatching { VehicleType.valueOf(name) }.getOrNull()?.let { _vehiclesOwned += it }
                }
            }
            val trips = root.optJSONArray("trips")
            if (trips != null) {
                for (i in 0 until trips.length()) {
                    val o = trips.optJSONObject(i) ?: continue
                    val vehicle = runCatching { VehicleType.valueOf(o.optString("vehicle")) }
                        .getOrNull() ?: continue
                    val origin = runCatching { Location.valueOf(o.optString("origin")) }
                        .getOrNull() ?: continue
                    val destination = runCatching { Location.valueOf(o.optString("destination")) }
                        .getOrNull() ?: continue
                    activeTrips += Trip(
                        id = o.optLong("id"),
                        vehicle = vehicle,
                        origin = origin,
                        destination = destination,
                        cargo = Inventory.fromJson(o.optJSONArray("cargo")).stacks,
                        startMs = o.optLong("startMs"),
                        durationMs = o.optLong("durationMs"),
                    )
                }
            }
            nextTripId = root.optLong("nextTripId", 1L).coerceAtLeast(1L)
        }
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
