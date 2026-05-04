package com.shadaeiou.charmingfarmer.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadaeiou.charmingfarmer.data.CropType
import com.shadaeiou.charmingfarmer.data.FarmGame
import com.shadaeiou.charmingfarmer.data.FarmState
import com.shadaeiou.charmingfarmer.data.Plot
import com.shadaeiou.charmingfarmer.data.PlotKind
import com.shadaeiou.charmingfarmer.data.UPGRADES
import com.shadaeiou.charmingfarmer.data.Upgrade
import kotlinx.coroutines.delay

private val SoilColor = Color(0xFF8B5A3C)
private val SoilDarkColor = Color(0xFF4A2D18)
private val SoilTilledColor = Color(0xFF6B4226)
private val GrassColor = Color(0xFF7CC36B)
private val GrassEdgeColor = Color(0xFF4A8F3F)
private val ReadyColor = Color(0xFFFFD24A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOpenSettings: () -> Unit) {
    val ctx = LocalContext.current
    val game = remember { FarmGame(ctx.applicationContext) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            game.tick()
            nowMs = System.currentTimeMillis()
            delay(250)
        }
    }
    DisposableEffect(Unit) { onDispose { game.save() } }

    val state = game.state

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🌾 Charming Farmer", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
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
                .padding(12.dp),
        ) {
            StatCards(state)
            Spacer(Modifier.height(8.dp))
            FeedbackText(game.feedback, game.feedbackBad)
            Spacer(Modifier.height(8.dp))
            FarmGrid(state, nowMs, onPlotClick = { game.clickPlot(it) })
            Spacer(Modifier.height(14.dp))
            SeedShelf(state.selectedSeed, onSelect = { game.selectSeed(it) })
            Spacer(Modifier.height(14.dp))
            UpgradesRow(state, costFn = game::upgradeCost, onBuy = { game.buyUpgrade(it) })
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatCards(s: FarmState) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(Modifier.padding(10.dp)) {
                Text(
                    "⚡ Energy",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${s.energy.toInt()} / ${s.maxEnergy}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                LinearProgressIndicator(
                    progress = { (s.energy / s.maxEnergy).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(Modifier.padding(10.dp)) {
                Text(
                    "🪙 Coins",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${s.coins}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Harvested: ${s.harvested}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun FeedbackText(msg: String?, bad: Boolean) {
    val text = msg ?: "Tap grass to till. Tap tilled soil to plant your selected seed."
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp)
            .padding(horizontal = 4.dp),
        textAlign = TextAlign.Center,
        color = if (bad) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun FarmGrid(s: FarmState, nowMs: Long, onPlotClick: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SoilColor)
            .border(4.dp, SoilDarkColor, RoundedCornerShape(16.dp))
            .padding(8.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (r in 0 until 4) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (c in 0 until 4) {
                        val idx = r * 4 + c
                        PlotCell(
                            plot = s.plots[idx],
                            nowMs = nowMs,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            onClick = { onPlotClick(idx) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlotCell(plot: Plot, nowMs: Long, modifier: Modifier, onClick: () -> Unit) {
    val (bg, borderColor) = when (plot.kind) {
        PlotKind.GRASS -> GrassColor to GrassEdgeColor
        PlotKind.TILLED, PlotKind.PLANTED -> SoilTilledColor to SoilDarkColor
    }
    val frac = plot.growthFraction(nowMs)
    val ready = plot.kind == PlotKind.PLANTED && frac >= 1f

    val bounceScale = if (ready) {
        val transition = rememberInfiniteTransition(label = "ready-bounce")
        val scale by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 600),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "ready-scale",
        )
        scale
    } else 1f

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(2.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        val content = when {
            plot.kind != PlotKind.PLANTED -> ""
            frac >= 1f -> plot.crop?.emoji ?: ""
            frac > 0.5f -> plot.crop?.sprout ?: "🌱"
            else -> "🌱"
        }
        if (content.isNotEmpty()) {
            Text(
                text = content,
                fontSize = 30.sp,
                modifier = Modifier.scale(bounceScale),
            )
        }
        if (plot.kind == PlotKind.PLANTED && plot.watered && frac < 1f) {
            Text(
                "💧",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp),
                fontSize = 11.sp,
            )
        }
        if (plot.kind == PlotKind.PLANTED) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x66000000)),
            ) {
                val animFrac by animateFloatAsState(
                    targetValue = frac,
                    animationSpec = tween(300),
                    label = "growth",
                )
                Box(
                    Modifier
                        .fillMaxWidth(animFrac)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (frac >= 1f) ReadyColor else GrassColor),
                )
            }
        }
    }
}

@Composable
private fun SeedShelf(selected: CropType, onSelect: (CropType) -> Unit) {
    Column {
        Text(
            "🌱 Seed Shop",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (c in CropType.entries) {
                SeedButton(
                    crop = c,
                    selected = c == selected,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(c) },
                )
            }
        }
    }
}

@Composable
private fun SeedButton(
    crop: CropType,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.secondary
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val bg = if (selected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surface
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(if (selected) 3.dp else 2.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(crop.emoji, fontSize = 28.sp)
        Text(
            crop.displayName,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "🪙${crop.coinCost} → 🪙${crop.sellPrice}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "⚡${crop.plantEnergy} · ${prettyTime(crop.growthMs)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun UpgradesRow(s: FarmState, costFn: (Upgrade) -> Int, onBuy: (Upgrade) -> Unit) {
    Column {
        Text(
            "🛠️ Upgrades",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (up in UPGRADES) {
                val cost = costFn(up)
                val lvl = s.upgradeLevels[up.key] ?: 0
                val affordable = s.coins >= cost
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (affordable) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(
                            2.dp,
                            if (affordable) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            RoundedCornerShape(10.dp),
                        )
                        .clickable(enabled = affordable) { onBuy(up) }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val labelColor = if (affordable) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    Text(
                        up.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = labelColor,
                    )
                    Text(
                        up.desc,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = labelColor,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "🪙$cost · lv $lvl",
                        style = MaterialTheme.typography.labelMedium,
                        color = labelColor,
                    )
                }
            }
        }
    }
}

private fun prettyTime(ms: Long): String {
    val s = ms / 1000
    return when {
        s < 60 -> "${s}s"
        s < 3600 -> "${s / 60}m"
        else -> "${s / 3600}h"
    }
}
