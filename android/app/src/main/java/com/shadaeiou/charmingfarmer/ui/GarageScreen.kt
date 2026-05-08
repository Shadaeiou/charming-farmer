package com.shadaeiou.charmingfarmer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadaeiou.charmingfarmer.data.FarmGame
import com.shadaeiou.charmingfarmer.data.OwnedVehicle
import com.shadaeiou.charmingfarmer.data.TransportService
import com.shadaeiou.charmingfarmer.data.VehicleType
import com.shadaeiou.charmingfarmer.data.room.VehicleOwnedEntity
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageScreen(onBack: () -> Unit, onOpenMap: () -> Unit) {
    val ctx = LocalContext.current
    val game = remember { FarmGame(ctx.applicationContext) }
    val transport = remember { TransportService.get(ctx.applicationContext) }
    @Suppress("UNUSED_EXPRESSION", "AssignedValueIsNeverRead")
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var feedbackBad by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            game.tick()
            transport.tick(System.currentTimeMillis())
            nowMs = System.currentTimeMillis()
            delay(500)
        }
    }
    DisposableEffect(Unit) { onDispose { game.save() } }

    @Suppress("UNUSED_EXPRESSION") transport.revisionTick

    val state = game.state
    val owned = transport.vehiclesOwned()
    var pendingBuy by remember { mutableStateOf<VehicleType?>(null) }
    var pendingEdit by remember { mutableStateOf<OwnedVehicle?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🚗  Garage", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenMap) {
                        Icon(Icons.Filled.Map, contentDescription = "Map")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF1A1A1A)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                EnergyAndCoinsRow(state.energy.toInt(), state.maxEnergy, state.coins)
                Spacer(Modifier.height(8.dp))

                feedback?.let { msg ->
                    Text(
                        msg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xCC111111))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        textAlign = TextAlign.Center,
                        color = if (feedbackBad) MaterialTheme.colorScheme.error
                            else Color(0xFFD4E8C2),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                }

                GarageSection("🚗 Your fleet (${owned.size})") {
                    if (owned.isEmpty()) {
                        Text("No vehicles owned.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        owned.forEach { ov ->
                            val available = transport.vehicleAvailable(ov.id, System.currentTimeMillis())
                            OwnedVehicleRow(
                                vehicle = ov,
                                inUse = !available,
                                onEdit = { pendingEdit = ov },
                                onSell = {
                                    val payout = transport.sellVehicle(ov.id, System.currentTimeMillis())
                                    if (payout == null) {
                                        feedback = "Can't sell ${ov.displayName} (in transit or last wheelbarrow?)"
                                        feedbackBad = true
                                    } else {
                                        game.addCoins(payout)
                                        game.save()
                                        feedback = "Sold ${ov.displayName} for 🪙$payout"
                                        feedbackBad = false
                                    }
                                },
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                GarageSection("🏷️ Buy more vehicles") {
                    // Every type is buyable any number of times now.
                    val forSale = VehicleType.entries.filter { it.unlockCost > 0 || it == VehicleType.WHEELBARROW }
                    forSale.forEach { v ->
                        BuyVehicleRow(
                            vehicle = v,
                            ownedCount = owned.count { it.type == v },
                            canAfford = state.coins >= v.unlockCost,
                            onBuy = { pendingBuy = v },
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "Selling a vehicle returns 50% of its purchase price. Your last Wheelbarrow stays put. Vehicles in transit can't be sold or repainted.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF999999),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
                )
            }
        }
    }

    pendingBuy?.let { v ->
        VehicleSetupDialog(
            title = "Buy ${v.displayName}",
            subtitle = "${v.capacityLbs} lbs · 🪙${v.unlockCost}",
            initialName = transport.defaultNameFor(v),
            initialColor = VehicleOwnedEntity.DEFAULT_VEHICLE_COLOR,
            previewType = v,
            confirmLabel = if (state.coins >= v.unlockCost) "Buy 🪙${v.unlockCost}" else "Not enough coins",
            confirmEnabled = state.coins >= v.unlockCost,
            onCancel = { pendingBuy = null },
            onConfirm = { name, argb ->
                game.addCoins(-v.unlockCost)
                transport.unlockVehicle(v, name, argb)
                game.save()
                feedback = "Bought $name!"
                feedbackBad = false
                pendingBuy = null
            },
        )
    }

    pendingEdit?.let { ov ->
        VehicleSetupDialog(
            title = "Edit ${ov.displayName}",
            subtitle = "${ov.type.capacityLbs} lbs · ${ov.type.displayName}",
            initialName = ov.displayName,
            initialColor = ov.colorArgb,
            previewType = ov.type,
            confirmLabel = "Save",
            confirmEnabled = true,
            onCancel = { pendingEdit = null },
            onConfirm = { name, argb ->
                transport.renameVehicle(ov.id, name)
                transport.setColor(ov.id, argb)
                feedback = "Updated $name"
                feedbackBad = false
                pendingEdit = null
            },
        )
    }
}

@Composable
private fun GarageSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun OwnedVehicleRow(
    vehicle: OwnedVehicle,
    inUse: Boolean,
    onEdit: () -> Unit,
    onSell: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        VehicleIcon(
            vehicle = vehicle.type,
            tint = Color(vehicle.colorArgb),
            modifier = Modifier.size(44.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                vehicle.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (inUse) "In transit · ${vehicle.type.capacityLbs} lbs"
                else "Idle · ${vehicle.type.capacityLbs} lbs",
                style = MaterialTheme.typography.labelSmall,
                color = if (inUse) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(onClick = onEdit, enabled = !inUse) { Text("Edit") }
        OutlinedButton(
            onClick = onSell,
            enabled = !inUse,
        ) { Text("🪙${vehicle.type.unlockCost / 2}") }
    }
}

@Composable
private fun BuyVehicleRow(
    vehicle: VehicleType,
    ownedCount: Int,
    canAfford: Boolean,
    onBuy: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        VehicleIcon(
            vehicle = vehicle,
            tint = Color(VehicleOwnedEntity.DEFAULT_VEHICLE_COLOR),
            modifier = Modifier.size(40.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                vehicle.displayName + (if (ownedCount > 0) "  (own $ownedCount)" else ""),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "${vehicle.capacityLbs} lbs · ${vehicle.tripDurationMs / 60_000}min/trip",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(
            onClick = onBuy,
            enabled = canAfford,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text(
                "🪙${vehicle.unlockCost}",
                fontWeight = FontWeight.Bold,
                color = if (canAfford) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
            )
        }
    }
}

/**
 * Combined name + RGB color picker dialog. Used for both the buy flow
 * (initial setup) and the edit flow (rename + repaint). Live preview
 * shows the pixel-art icon retinted as the player slides RGB.
 */
@Composable
private fun VehicleSetupDialog(
    title: String,
    subtitle: String,
    initialName: String,
    initialColor: Int,
    previewType: VehicleType,
    confirmLabel: String,
    confirmEnabled: Boolean,
    onCancel: () -> Unit,
    onConfirm: (name: String, argb: Int) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    val initial = Color(initialColor)
    var r by remember { mutableStateOf((initial.red * 255).toInt().toFloat()) }
    var g by remember { mutableStateOf((initial.green * 255).toInt().toFloat()) }
    var b by remember { mutableStateOf((initial.blue * 255).toInt().toFloat()) }
    val swatch = Color(
        red = r.toInt().coerceIn(0, 255) / 255f,
        green = g.toInt().coerceIn(0, 255) / 255f,
        blue = b.toInt().coerceIn(0, 255) / 255f,
        alpha = 1f,
    )

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = { Text("Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF222222)),
                    contentAlignment = Alignment.Center,
                ) {
                    VehicleIcon(
                        vehicle = previewType,
                        tint = swatch,
                        modifier = Modifier.size(64.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "RGB(${r.toInt()}, ${g.toInt()}, ${b.toInt()})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                ChannelSlider("R", r, Color(0xFFE53935)) { r = it }
                ChannelSlider("G", g, Color(0xFF43A047)) { g = it }
                ChannelSlider("B", b, Color(0xFF1E88E5)) { b = it }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), swatch.toArgb()) },
                enabled = confirmEnabled,
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        },
    )
}

@Composable
private fun ChannelSlider(label: String, value: Float, accent: Color, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            modifier = Modifier.padding(end = 8.dp),
            fontWeight = FontWeight.Bold,
            color = accent,
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0f..255f,
            steps = 0,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
            ),
        )
        Text(
            "${value.toInt()}",
            modifier = Modifier.padding(start = 8.dp),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
