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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadaeiou.charmingfarmer.data.ItemGrade
import com.shadaeiou.charmingfarmer.data.ItemStack
import com.shadaeiou.charmingfarmer.data.Location
import com.shadaeiou.charmingfarmer.data.TransportService
import com.shadaeiou.charmingfarmer.data.Trip
import com.shadaeiou.charmingfarmer.data.VehicleType
import kotlinx.coroutines.delay

/**
 * Reusable transport dialog. Shows:
 *   - Active trips with progress bars
 *   - Origin inventory grouped by item type
 *   - One row per item with destination buttons; tapping ships 1 unit
 *     using the smallest available vehicle that fits.
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
    @Suppress("UNUSED_EXPRESSION") transport.revisionTick // recompose on changes

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = {
            Text("🚚 Ship from ${origin.emoji} ${origin.displayName}", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
            ) {
                if (activeTrips.isNotEmpty()) {
                    Text(
                        "🚚 In transit",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    activeTrips.forEach { trip ->
                        TripCard(trip, nowMs)
                        Spacer(Modifier.height(4.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    "Available cargo",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                if (grouped.isEmpty()) {
                    Text(
                        "Nothing to ship from here.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(grouped.entries.toList(), key = { it.key.name }) { (type, stacks) ->
                        val totalQty = stacks.sumOf { it.quantity }
                        val avgScore = stacks.sumOf { it.score.toLong() * it.quantity } /
                            totalQty.coerceAtLeast(1)
                        var qty by remember(type) { mutableStateOf(1) }
                        // Cap qty by available stock and the largest owned vehicle.
                        val maxVehicleCap = transport.vehiclesOwned().maxOfOrNull { it.capacityKg } ?: 1
                        val maxAllowed = totalQty.coerceAtMost(maxVehicleCap).coerceAtLeast(1)
                        if (qty > maxAllowed) qty = maxAllowed
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(type.emoji, fontSize = 18.sp)
                                Spacer(Modifier.padding(end = 6.dp))
                                Text(
                                    "${type.displayName} × $totalQty",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                )
                                val grade = ItemGrade.fromScore(avgScore.toInt())
                                Text(
                                    "Grade ${grade.display}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(grade.color),
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            // Quantity stepper: -, count badge, +, max.
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    "Qty:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                OutlinedButton(
                                    onClick = { qty = (qty - 1).coerceAtLeast(1) },
                                    enabled = qty > 1,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                ) { Text("−") }
                                Text(
                                    "$qty",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                                OutlinedButton(
                                    onClick = { qty = (qty + 1).coerceAtMost(maxAllowed) },
                                    enabled = qty < maxAllowed,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                ) { Text("+") }
                                TextButton(
                                    onClick = { qty = maxAllowed },
                                    enabled = qty < maxAllowed,
                                ) { Text("Max ($maxAllowed)") }
                            }
                            Spacer(Modifier.height(6.dp))
                            // Filter destinations to only those that accept this
                            // item — no shipping hops to the malthouse.
                            val acceptedDestinations = allowedDestinations.filter { it.accepts(type) }
                            if (acceptedDestinations.isEmpty()) {
                                Text(
                                    "Nowhere to send this from here.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    acceptedDestinations.forEach { dest ->
                                        Button(
                                            onClick = {
                                                val veh = pickVehicle(transport, qty, nowMs)
                                                if (veh == null) {
                                                    feedback = "No idle vehicle big enough for $qty"
                                                    feedbackBad = true
                                                    return@Button
                                                }
                                                val trip = transport.ship(
                                                    from = origin,
                                                    to = dest,
                                                    type = type,
                                                    amount = qty,
                                                    vehicle = veh,
                                                    nowMs = System.currentTimeMillis(),
                                                )
                                                if (trip == null) {
                                                    feedback = "Couldn't ship that"
                                                    feedbackBad = true
                                                } else {
                                                    feedback = "Sent $qty ${type.displayName} → ${dest.displayName} (${veh.emoji})"
                                                    feedbackBad = false
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                            ),
                                        ) {
                                            Text(
                                                "Send $qty → ${dest.emoji}  ${dest.displayName}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                feedback?.let {
                    Text(
                        text = it,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (feedbackBad) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary,
                    )
                }
                val vehicles = transport.vehiclesOwned()
                if (vehicles.isNotEmpty()) {
                    Text(
                        "Fleet: " + vehicles.joinToString { "${it.emoji} ${it.displayName}" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        },
    )
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

private fun pickVehicle(
    transport: TransportService,
    requiredCapacity: Int,
    nowMs: Long,
): VehicleType? {
    return transport.vehiclesOwned()
        .filter { it.capacityKg >= requiredCapacity }
        .filter { transport.vehicleAvailable(it, nowMs) }
        .minByOrNull { it.tripDurationMs }
}
