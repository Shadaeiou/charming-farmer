package com.shadaeiou.charmingfarmer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadaeiou.charmingfarmer.data.ItemGrade
import com.shadaeiou.charmingfarmer.data.ItemStack
import com.shadaeiou.charmingfarmer.data.ItemType
import com.shadaeiou.charmingfarmer.data.Location
import com.shadaeiou.charmingfarmer.data.OwnedVehicle
import com.shadaeiou.charmingfarmer.data.TransportService
import com.shadaeiou.charmingfarmer.data.Trip
import kotlinx.coroutines.delay

/**
 * Custom-load a single vehicle for a single destination, with mixed
 * cargo. The Ship button is sticky at the bottom so it's always
 * reachable no matter how long the cargo list grows.
 *
 * The panel is identical no matter which screen it's opened from —
 * the player picks both the origin silo and the destination from
 * inside the dialog. Default origin is the first silo with any items
 * (FARM if everything is empty).
 */
@Composable
fun TransportPanel(
    transport: TransportService,
    onDismiss: () -> Unit,
) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            transport.tick(System.currentTimeMillis())
            nowMs = System.currentTimeMillis()
            delay(500)
        }
    }
    var feedback by remember { mutableStateOf<String?>(null) }
    var feedbackBad by remember { mutableStateOf(false) }

    @Suppress("UNUSED_EXPRESSION") transport.revisionTick

    val sources = Location.entries.toList()
    val initialSource = remember {
        sources.firstOrNull { transport.inventoryAt(it).stacks.isNotEmpty() }
            ?: Location.FARM
    }
    var selectedSource by remember { mutableStateOf(initialSource) }
    val origInv = transport.inventoryAt(selectedSource)
    val grouped = origInv.stacks.groupBy { it.type }
    val activeTrips = transport.activeTrips.toList()

    val destinations = sources.filter { it != selectedSource }
    var selectedDestination by remember(selectedSource) {
        mutableStateOf(destinations.firstOrNull())
    }

    val ownedVehicles = transport.vehiclesOwned()
    var selectedVehicleId by remember(ownedVehicles.size) {
        mutableStateOf(
            ownedVehicles.firstOrNull {
                transport.vehicleAvailable(it.id, System.currentTimeMillis())
            }?.id
                ?: ownedVehicles.firstOrNull()?.id
        )
    }
    val cargoLoad = remember { mutableStateMapOf<ItemType, Int>() }

    LaunchedEffect(selectedVehicleId, selectedDestination, selectedSource) { cargoLoad.clear() }

    val selectedVehicle = selectedVehicleId?.let { transport.vehicleById(it) }
    val capacity = selectedVehicle?.type?.capacityLbs ?: 0
    val loadedTotal = cargoLoad.values.sum()
    val remainingCap = (capacity - loadedTotal).coerceAtLeast(0)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            // Sticky Ship button: lives in the AlertDialog's confirm
            // slot, always at the bottom regardless of how tall the
            // cargo list scrolls. Falls back to a Close action when
            // there's nothing to ship.
            val veh = selectedVehicle
            val dest = selectedDestination
            if (veh != null && dest != null && loadedTotal > 0) {
                Button(
                    onClick = {
                        val trip = transport.shipMultiple(
                            from = selectedSource,
                            to = dest,
                            cargo = cargoLoad.toMap(),
                            vehicleId = veh.id,
                            nowMs = System.currentTimeMillis(),
                        )
                        if (trip == null) {
                            feedback = "Couldn't dispatch — recheck vehicle / capacity"
                            feedbackBad = true
                        } else {
                            feedback =
                                "🚚 Sent ${trip.cargo.sumOf { it.quantity }} items → ${dest.displayName}"
                            feedbackBad = false
                            cargoLoad.clear()
                        }
                    },
                    enabled = transport.vehicleAvailable(veh.id, nowMs),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text(
                        "Ship $loadedTotal lbs → ${dest.emoji}",
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
        dismissButton = {
            if (cargoLoad.isNotEmpty()) {
                TextButton(onClick = { cargoLoad.clear() }) { Text("Clear") }
            } else {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
        title = {
            Text(
                "🚚 Ship from ${selectedSource.emoji} ${selectedSource.displayName}",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (activeTrips.isNotEmpty()) {
                    SectionLabel("In transit")
                    activeTrips.forEach { trip ->
                        TripCard(trip, transport, nowMs)
                        Spacer(Modifier.height(4.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                }

                SectionLabel("Pick a vehicle")
                if (ownedVehicles.isEmpty()) {
                    Text(
                        "No vehicles owned. Buy one in the Garage.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(ownedVehicles, key = { it.id }) { v ->
                            val available = transport.vehicleAvailable(v.id, nowMs)
                            VehicleChip(
                                vehicle = v,
                                selected = v.id == selectedVehicleId,
                                disabled = !available,
                                onTap = { if (available) selectedVehicleId = v.id },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                SectionLabel("Source")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(sources, key = { it.name }) { src ->
                        val stockCount = transport.inventoryAt(src).stacks.sumOf { it.quantity }
                        DestinationChip(
                            location = src,
                            selected = src == selectedSource,
                            stockCount = stockCount,
                            onTap = { selectedSource = src },
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                SectionLabel("Destination")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(destinations, key = { it.name }) { dest ->
                        DestinationChip(
                            location = dest,
                            selected = dest == selectedDestination,
                            onTap = { selectedDestination = dest },
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                val veh = selectedVehicle
                val dest = selectedDestination
                if (veh != null && dest != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Cargo manifest",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "$loadedTotal / ${veh.type.capacityLbs} lbs",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (loadedTotal >= veh.type.capacityLbs)
                                MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    val frac = (loadedTotal.toFloat() / veh.type.capacityLbs.coerceAtLeast(1))
                        .coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { frac },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))

                    val acceptable = grouped.filterKeys { dest.accepts(it) }
                    if (acceptable.isEmpty()) {
                        Text(
                            "Nothing here can be sent to ${dest.displayName}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            acceptable.forEach { (type, stacks) ->
                                val available = stacks.sumOf { it.quantity }
                                val avgScore = stacks.sumOf { it.score.toLong() * it.quantity } /
                                    available.coerceAtLeast(1)
                                val loaded = cargoLoad[type] ?: 0
                                CargoRow(
                                    type = type,
                                    loaded = loaded,
                                    available = available,
                                    avgScore = avgScore.toInt(),
                                    remainingCapacity = remainingCap,
                                    onChange = { newQty ->
                                        val capped = newQty
                                            .coerceAtLeast(0)
                                            .coerceAtMost(available)
                                            .coerceAtMost(loaded + remainingCap)
                                        if (capped == 0) cargoLoad.remove(type)
                                        else cargoLoad[type] = capped
                                    },
                                )
                            }
                        }
                    }
                }

                feedback?.let { msg ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (feedbackBad) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun VehicleChip(
    vehicle: OwnedVehicle,
    selected: Boolean,
    disabled: Boolean,
    onTap: () -> Unit,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    val border = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(2.dp, border, RoundedCornerShape(8.dp))
            .alpha(if (disabled) 0.45f else 1f)
            .clickable(enabled = !disabled) { onTap() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        VehicleIcon(
            vehicle = vehicle.type,
            tint = Color(vehicle.colorArgb),
            modifier = Modifier.size(36.dp),
        )
        Text(
            vehicle.displayName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "${vehicle.type.capacityLbs} lbs",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (disabled) {
            Text(
                "in transit",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

@Composable
private fun DestinationChip(
    location: Location,
    selected: Boolean,
    onTap: () -> Unit,
    stockCount: Int? = null,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    val border = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(2.dp, border, RoundedCornerShape(8.dp))
            .clickable(onClick = onTap)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(location.emoji, fontSize = 14.sp)
        Spacer(Modifier.padding(end = 4.dp))
        Text(
            location.displayName,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
        if (stockCount != null && stockCount > 0) {
            Spacer(Modifier.padding(end = 4.dp))
            Text(
                "×$stockCount",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CargoRow(
    type: ItemType,
    loaded: Int,
    available: Int,
    avgScore: Int,
    remainingCapacity: Int,
    onChange: (Int) -> Unit,
) {
    val grade = ItemGrade.fromScore(avgScore)
    val canAddMore = remainingCapacity > 0 && loaded < available
    val canRemove = loaded > 0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(type.emoji, fontSize = 18.sp)
            Spacer(Modifier.padding(end = 6.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    type.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Avg grade ${grade.display} · stock $available",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(grade.color),
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (loaded > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        else Color.Transparent
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    "Loaded $loaded",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (loaded > 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            QuickStep("−", canRemove) { onChange(loaded - 1) }
            QuickStep("+1", canAddMore) { onChange(loaded + 1) }
            QuickStep("+10", canAddMore) { onChange(loaded + 10) }
            QuickStep("+100", canAddMore) { onChange(loaded + 100) }
            QuickStep("½", available >= 2) { onChange(available / 2) }
            QuickStep("Max", canAddMore) { onChange(loaded + remainingCapacity) }
        }
    }
}

@Composable
private fun QuickStep(label: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
        modifier = Modifier.heightIn(min = 30.dp),
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TripCard(trip: Trip, transport: TransportService, nowMs: Long) {
    val frac = trip.progress(nowMs)
    val remainingS = (trip.remainingMs(nowMs) / 1000).toInt()
    val owned = transport.vehicleById(trip.vehicleId)
    val tint = owned?.colorArgb?.let { Color(it) }
        ?: Color(com.shadaeiou.charmingfarmer.data.room.VehicleOwnedEntity.DEFAULT_VEHICLE_COLOR)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                VehicleIcon(
                    vehicle = trip.vehicleType,
                    tint = tint,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.padding(end = 6.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        owned?.displayName ?: trip.vehicleType.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "${trip.origin.emoji} → ${trip.destination.emoji}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    if (remainingS > 0) "${remainingS}s" else "arriving",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { frac },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            // Group identical item types so the manifest stays compact
            // even on multi-stack mixed-cargo trips.
            val cargoSummary = trip.cargo
                .groupBy { it.type }
                .map { (type, stacks) ->
                    "${type.emoji}×${stacks.sumOf { it.quantity }}"
                }
                .joinToString("  ")
            Text(
                cargoSummary.ifBlank { "empty" },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
