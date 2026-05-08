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
import com.shadaeiou.charmingfarmer.data.ItemType
import com.shadaeiou.charmingfarmer.data.Location
import com.shadaeiou.charmingfarmer.data.TransportService
import com.shadaeiou.charmingfarmer.data.Trip
import com.shadaeiou.charmingfarmer.data.VehicleType
import kotlinx.coroutines.delay

/**
 * Custom-load a single vehicle for a single destination, with mixed
 * cargo. Flow:
 *   1. Pick a vehicle from your idle fleet (in-transit are dimmed).
 *   2. Pick a destination — only ones that accept at least one item
 *      from your inventory are listed.
 *   3. Load up to capacity. Each cargo row has fast quantity controls:
 *      ±1, +10, +100, ½ (load half your stock), Max (fill until either
 *      stock or remaining capacity runs out).
 *   4. Ship. The vehicle goes on a trip carrying everything you loaded.
 *
 * Multi-stop routes are a planned follow-up — the model already
 * supports mixed cargo per trip, so a Route -> List<Stop> structure
 * can layer on top without changing this panel's UX.
 */
@Composable
fun TransportPanel(
    transport: TransportService,
    origin: Location,
    allowedDestinations: List<Location>,
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

    val origInv = transport.inventoryAt(origin)
    val grouped = origInv.stacks.groupBy { it.type }
    val activeTrips = transport.activeTrips.toList()
    @Suppress("UNUSED_EXPRESSION") transport.revisionTick

    val ownedVehicles = VehicleType.entries.filter { it in transport.vehiclesOwned() }
    var selectedVehicle by remember(ownedVehicles.size) {
        mutableStateOf(
            ownedVehicles.firstOrNull { transport.vehicleAvailable(it, System.currentTimeMillis()) }
                ?: ownedVehicles.firstOrNull()
        )
    }
    var selectedDestination by remember(allowedDestinations) {
        mutableStateOf(allowedDestinations.firstOrNull())
    }
    val cargoLoad = remember { mutableStateMapOf<ItemType, Int>() }

    // Reset cargo if vehicle/destination changes (item filters or
    // capacity might shrink and the previous load could be invalid).
    LaunchedEffect(selectedVehicle, selectedDestination) { cargoLoad.clear() }

    val capacity = selectedVehicle?.capacityKg ?: 0
    val loadedTotal = cargoLoad.values.sum()
    val remainingCap = (capacity - loadedTotal).coerceAtLeast(0)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = {
            Text(
                "🚚 Ship from ${origin.emoji} ${origin.displayName}",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (activeTrips.isNotEmpty()) {
                    SectionLabel("In transit")
                    activeTrips.forEach { trip ->
                        TripCard(trip, nowMs)
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
                        items(ownedVehicles, key = { it.name }) { v ->
                            val available = transport.vehicleAvailable(v, nowMs)
                            VehicleChip(
                                vehicle = v,
                                tint = Color(transport.colorOf(v)),
                                selected = v == selectedVehicle,
                                disabled = !available,
                                onTap = { if (available) selectedVehicle = v },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                SectionLabel("Destination")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(allowedDestinations, key = { it.name }) { dest ->
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
                            "$loadedTotal / ${veh.capacityKg} kg",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (loadedTotal >= veh.capacityKg)
                                MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    val frac = (loadedTotal.toFloat() / veh.capacityKg.coerceAtLeast(1))
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

                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { cargoLoad.clear() },
                                enabled = cargoLoad.isNotEmpty(),
                                modifier = Modifier.weight(1f),
                            ) { Text("Clear") }
                            Button(
                                onClick = {
                                    val trip = transport.shipMultiple(
                                        from = origin,
                                        to = dest,
                                        cargo = cargoLoad.toMap(),
                                        vehicle = veh,
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
                                enabled = loadedTotal > 0 &&
                                    transport.vehicleAvailable(veh, nowMs),
                                modifier = Modifier.weight(2f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                ),
                            ) {
                                Text(
                                    "Ship $loadedTotal → ${dest.emoji}",
                                    fontWeight = FontWeight.Bold,
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
    vehicle: VehicleType,
    tint: Color,
    selected: Boolean,
    disabled: Boolean,
    onTap: () -> Unit,
) {
    val bg = when {
        selected -> tint.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    }
    val border = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(2.dp, border, RoundedCornerShape(8.dp))
            .alpha(if (disabled) 0.45f else 1f)
            .clickable(enabled = !disabled) { onTap() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(vehicle.emoji, fontSize = 22.sp)
        Text(
            vehicle.displayName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "${vehicle.capacityKg}kg",
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
private fun TripCard(trip: Trip, nowMs: Long) {
    val frac = trip.progress(nowMs)
    val remainingS = (trip.remainingMs(nowMs) / 1000).toInt()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(trip.vehicle.emoji, fontSize = 16.sp)
                Spacer(Modifier.padding(end = 6.dp))
                Text(
                    "${trip.origin.emoji} → ${trip.destination.emoji}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                )
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
            val cargoSummary = trip.cargo.joinToString { "${it.type.emoji}×${it.quantity}" }
            Text(
                cargoSummary.ifBlank { "empty" },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
