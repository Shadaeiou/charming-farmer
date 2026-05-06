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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadaeiou.charmingfarmer.data.FarmGame
import com.shadaeiou.charmingfarmer.data.ItemGrade
import com.shadaeiou.charmingfarmer.data.ItemStack
import com.shadaeiou.charmingfarmer.data.ItemType
import com.shadaeiou.charmingfarmer.data.KilnProfile
import com.shadaeiou.charmingfarmer.data.KilnRun
import com.shadaeiou.charmingfarmer.data.Location
import com.shadaeiou.charmingfarmer.data.Malthouse
import com.shadaeiou.charmingfarmer.data.TransportService
import kotlinx.coroutines.delay

private val MALTABLE_GRAINS = listOf(
    ItemType.BARLEY,
    ItemType.WHEAT_GRAIN,
    ItemType.OATS,
    ItemType.RYE,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MalthouseScreen(onBack: () -> Unit, onOpenMap: () -> Unit) {
    val ctx = LocalContext.current
    val game = remember { FarmGame(ctx.applicationContext) }
    val transport = remember { TransportService.get(ctx.applicationContext) }
    val malthouse = remember { Malthouse.get(ctx.applicationContext) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            game.tick()
            transport.tick(System.currentTimeMillis())
            malthouse.tick(transport, System.currentTimeMillis())
            nowMs = System.currentTimeMillis()
            delay(500)
        }
    }
    DisposableEffect(Unit) { onDispose { game.save() } }

    var feedback by remember { mutableStateOf<String?>(null) }
    var feedbackBad by remember { mutableStateOf(false) }
    var selectedGrain by remember { mutableStateOf<ItemType?>(null) }
    var transportOpen by remember { mutableStateOf(false) }

    val state = game.state
    @Suppress("UNUSED_EXPRESSION") transport.revisionTick
    @Suppress("UNUSED_EXPRESSION") malthouse.revisionTick

    val mhInv = transport.inventoryAt(Location.MALTHOUSE)
    val grainStacks = mhInv.stacks.filter { it.type in MALTABLE_GRAINS }
    val maltStacks = mhInv.stacks.filter { it.type.name.startsWith("MALT_") }

    if (transportOpen) {
        TransportPanel(
            transport = transport,
            origin = Location.MALTHOUSE,
            allowedDestinations = listOf(Location.BREWERY, Location.FARM),
            onDismiss = { transportOpen = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🌾 Malthouse", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { transportOpen = true }) {
                        Icon(Icons.Filled.LocalShipping, contentDescription = "Transport")
                    }
                    IconButton(onClick = onOpenMap) {
                        Icon(Icons.Filled.Map, contentDescription = "Map")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            EnergyAndCoinsRow(state.energy.toInt(), state.maxEnergy, state.coins)

            Spacer(Modifier.height(8.dp))

            Text(
                text = feedback ?: "Pick a grain, choose a kilning profile, drop it in a kiln.",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                textAlign = TextAlign.Center,
                color = if (feedbackBad) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(Modifier.height(8.dp))

            // Active kilns
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(Modifier.padding(10.dp)) {
                    Text(
                        "🔥 Kilns (${malthouse.activeRuns.size}/${malthouse.tier.kilnSlots})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    if (malthouse.activeRuns.isEmpty()) {
                        Text(
                            "No kilns running.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        malthouse.activeRuns.forEach { run ->
                            KilnRunRow(run, nowMs)
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                    Text(
                        "Equipment: ${malthouse.tier.displayName} (cap ${malthouse.tier.qualityCap})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Raw grain inventory
            SectionCard(title = "🌾 Raw grain in malthouse") {
                if (grainStacks.isEmpty()) {
                    Text(
                        "Ship grain from the farm to start malting.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    val grouped = grainStacks.groupBy { it.type }
                    grouped.forEach { (type, stacks) ->
                        val totalQty = stacks.sumOf { it.quantity }
                        val avg = stacks.sumOf { it.score.toLong() * it.quantity } / totalQty.coerceAtLeast(1)
                        InventoryRow(
                            emoji = type.emoji,
                            name = "${type.displayName} × $totalQty",
                            grade = ItemGrade.fromScore(avg.toInt()),
                            selected = selectedGrain == type,
                            onClick = { selectedGrain = if (selectedGrain == type) null else type },
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Kilning profiles
            SectionCard(title = "🔥 Kilning profiles") {
                if (selectedGrain == null) {
                    Text(
                        "Tap a grain above to choose a profile.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    KilnProfile.entries.forEach { profile ->
                        ProfileRow(
                            profile = profile,
                            enabled = malthouse.availableSlots() > 0 &&
                                state.energy.toInt() >= profile.energyCost,
                            onClick = {
                                val grain = selectedGrain ?: return@ProfileRow
                                val started = malthouse.startKiln(
                                    transport = transport,
                                    inputType = grain,
                                    profile = profile,
                                    nowMs = System.currentTimeMillis(),
                                ) { amt -> game.spendEnergy(amt) }
                                if (started == null) {
                                    feedback = if (malthouse.availableSlots() == 0)
                                        "All kilns busy" else "Couldn't start kiln"
                                    feedbackBad = true
                                } else {
                                    feedback = "Kilning ${grain.displayName} → ${profile.displayName}"
                                    feedbackBad = false
                                    selectedGrain = null
                                }
                            },
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Finished malt inventory
            SectionCard(title = "🟡 Malt ready to ship") {
                if (maltStacks.isEmpty()) {
                    Text(
                        "No malt yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    val grouped = maltStacks.groupBy { it.type }
                    grouped.forEach { (type, stacks) ->
                        val totalQty = stacks.sumOf { it.quantity }
                        val avg = stacks.sumOf { it.score.toLong() * it.quantity } / totalQty.coerceAtLeast(1)
                        InventoryRow(
                            emoji = type.emoji,
                            name = "${type.displayName} × $totalQty",
                            grade = ItemGrade.fromScore(avg.toInt()),
                            selected = false,
                            onClick = {},
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun KilnRunRow(run: KilnRun, nowMs: Long) {
    val frac = run.progress(nowMs)
    val remainingS = (run.remainingMs(nowMs) / 1000).toInt()
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(run.profile.emoji, fontSize = 16.sp)
            Spacer(Modifier.padding(end = 6.dp))
            Text(
                "${run.inputType.displayName} → ${run.profile.outputType.displayName}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (remainingS > 0) "${remainingS}s" else "done",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { frac },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
private fun ProfileRow(profile: KilnProfile, enabled: Boolean, onClick: () -> Unit) {
    val bg = if (enabled) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val borderColor = if (enabled) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(profile.emoji, fontSize = 18.sp)
        Spacer(Modifier.padding(end = 8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                profile.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "${prettyMin(profile.durationMs)} · ⚡${profile.energyCost} · " +
                    if (profile.scoreOffset >= 0) "+${profile.scoreOffset} score"
                    else "${profile.scoreOffset} score",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InventoryRow(
    emoji: String,
    name: String,
    grade: ItemGrade,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer
        else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 16.sp)
        Spacer(Modifier.padding(end = 6.dp))
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            "Grade ${grade.display}",
            style = MaterialTheme.typography.labelSmall,
            color = Color(grade.color),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
internal fun EnergyAndCoinsRow(energy: Int, maxEnergy: Int, coins: Int) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.5f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(Modifier.padding(10.dp)) {
                Text("⚡ Energy", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$energy / $maxEnergy", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
                LinearProgressIndicator(
                    progress = { (energy.toFloat() / maxEnergy.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(Modifier.padding(10.dp)) {
                Text("🪙 Coins", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$coins", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun prettyMin(ms: Long): String {
    val s = ms / 1000
    return when {
        s < 60 -> "${s}s"
        else -> "${s / 60}m"
    }
}
