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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import com.shadaeiou.charmingfarmer.data.CropType
import com.shadaeiou.charmingfarmer.data.FarmGame
import com.shadaeiou.charmingfarmer.data.FarmState
import com.shadaeiou.charmingfarmer.data.Plot
import com.shadaeiou.charmingfarmer.data.PlotKind
import com.shadaeiou.charmingfarmer.data.TreeType
import com.shadaeiou.charmingfarmer.data.UPGRADES
import com.shadaeiou.charmingfarmer.data.Upgrade
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

private val SoilColor = Color(0xFF8B5A3C)
private val SoilDarkColor = Color(0xFF4A2D18)
private val SoilTilledColor = Color(0xFF6B4226)
private val GrassColor = Color(0xFF7CC36B)
private val GrassEdgeColor = Color(0xFF4A8F3F)
private val ReadyColor = Color(0xFFFFD24A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOpenSettings: () -> Unit, onOpenMap: () -> Unit) {
    val ctx = LocalContext.current
    val game = remember { FarmGame(ctx.applicationContext) }
    val transport = remember { com.shadaeiou.charmingfarmer.data.TransportService.get(ctx.applicationContext) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var transportOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            game.tick()
            transport.tick(System.currentTimeMillis())
            nowMs = System.currentTimeMillis()
            delay(250)
        }
    }
    DisposableEffect(Unit) { onDispose { game.save() } }

    if (transportOpen) {
        TransportPanel(
            transport = transport,
            origin = com.shadaeiou.charmingfarmer.data.Location.FARM,
            allowedDestinations = listOf(
                com.shadaeiou.charmingfarmer.data.Location.MALTHOUSE,
                com.shadaeiou.charmingfarmer.data.Location.BREWERY,
            ),
            onDismiss = { transportOpen = false },
        )
    }

    val state = game.state

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🌾 Charming Farmer", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { transportOpen = true }) {
                        Icon(Icons.Filled.LocalShipping, contentDescription = "Transport")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            StatCards(state)
            Spacer(Modifier.height(4.dp))
            FeedbackText(game.feedback, game.feedbackBad)
            Spacer(Modifier.height(4.dp))
            FarmGrid(state, nowMs, modifier = Modifier.weight(1f), onPlotClick = { game.clickPlot(it) })
            Spacer(Modifier.height(6.dp))
            SeedShelf(state, onSelect = { game.selectSeed(it) })
            Spacer(Modifier.height(4.dp))
            TreeNursery(state, onSelect = { game.selectTree(it) })
            Spacer(Modifier.height(6.dp))
            UpgradesRow(state, costFn = game::upgradeCost, onBuy = { game.buyUpgrade(it) })
            Spacer(Modifier.height(4.dp))
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
private fun FarmGrid(s: FarmState, nowMs: Long, modifier: Modifier = Modifier, onPlotClick: (Int) -> Unit) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val gridSize = minOf(maxWidth, maxHeight)
        Box(
            modifier = Modifier
                .size(gridSize)
                .align(Alignment.TopCenter)
                .clip(RoundedCornerShape(16.dp))
                .background(SoilColor)
                .border(4.dp, SoilDarkColor, RoundedCornerShape(16.dp))
                .padding(8.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (r in 0 until 4) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        for (c in 0 until 4) {
                            val idx = r * 4 + c
                            PlotCell(
                                plot = s.plots[idx],
                                nowMs = nowMs,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                onClick = { onPlotClick(idx) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlotCell(plot: Plot, nowMs: Long, modifier: Modifier, onClick: () -> Unit) {
    val treeDead = plot.kind == PlotKind.TREE && plot.treeIsDead(nowMs)
    val treeReady = plot.kind == PlotKind.TREE && !treeDead && plot.treeHarvestReady(nowMs)
    val (bg, borderColor) = when {
        treeDead -> Color(0xFF4A3020) to Color(0xFF2A180A)
        plot.kind == PlotKind.TREE -> Color(0xFF5B3E1F) to SoilDarkColor
        plot.kind == PlotKind.GRASS -> GrassColor to GrassEdgeColor
        else -> SoilTilledColor to SoilDarkColor
    }
    val frac = plot.growthFraction(nowMs)
    val ready = plot.kind == PlotKind.PLANTED && frac >= 1f

    val transition = rememberInfiniteTransition(label = "bounce")
    val pulseScale by transition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "pulse",
    )
    val bounceScale = if (ready || treeReady) pulseScale else 1f

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(2.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        when (plot.kind) {
            PlotKind.TREE -> {
                val tree = plot.tree!!
                if (treeDead) {
                    Text("🪵", fontSize = 26.sp)
                } else {
                    Text(
                        text = if (treeReady) tree.fruitEmoji else tree.treeEmoji,
                        fontSize = 26.sp,
                        modifier = Modifier.scale(bounceScale),
                    )
                    Text(
                        if (treeReady) tree.treeEmoji else tree.fruitEmoji,
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.TopStart).padding(2.dp),
                    )
                    Text(
                        "${plot.harvestCount}/${tree.maxHarvests}",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.align(Alignment.TopEnd).padding(2.dp),
                    )
                    val lifeFrac = plot.treeLifeFraction(nowMs)
                    val animLife by animateFloatAsState(lifeFrac, tween(300), label = "life")
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0x66000000)),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(1f - animLife)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (treeReady) ReadyColor else Color(0xFF7DB87D)),
                        )
                    }
                }
            }
            PlotKind.PLANTED -> {
                val content = when {
                    frac >= 1f -> plot.crop?.emoji ?: ""
                    frac > 0.5f -> plot.crop?.sprout ?: "🌱"
                    else -> "🌱"
                }
                if (content.isNotEmpty()) {
                    Text(text = content, fontSize = 30.sp, modifier = Modifier.scale(bounceScale))
                }
                if (frac < 1f && plot.crop != null) {
                    Text(
                        plot.crop.emoji,
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.TopStart).padding(2.dp),
                    )
                }
                if (plot.watered && frac < 1f) {
                    Text("💧", fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.TopEnd).padding(2.dp))
                }
                val animFrac by animateFloatAsState(frac, tween(300), label = "growth")
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .fillMaxWidth().height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0x66000000)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(animFrac).fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (frac >= 1f) ReadyColor else GrassColor),
                    )
                }
            }
            else -> Unit
        }
    }
}

@Composable
private fun SeedShelf(state: FarmState, onSelect: (CropType) -> Unit) {
    Column {
        Text("🌱 Seeds", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (c in CropType.entries) {
                SeedButton(
                    crop = c,
                    selected = state.selectedTree == null && c == state.selectedSeed,
                    coins = state.coins,
                    modifier = Modifier.width(80.dp),
                    onClick = { onSelect(c) },
                )
            }
        }
    }
}

@Composable
private fun TreeNursery(state: FarmState, onSelect: (TreeType) -> Unit) {
    Column {
        Text("🌳 Trees", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (t in TreeType.entries) {
                TreeButton(
                    tree = t,
                    selected = t == state.selectedTree,
                    coins = state.coins,
                    modifier = Modifier.width(88.dp),
                    onClick = { onSelect(t) },
                )
            }
        }
    }
}

@Composable
private fun TreeButton(tree: TreeType, selected: Boolean, coins: Int, modifier: Modifier, onClick: () -> Unit) {
    val canAfford = coins >= tree.coinCost
    var showTooltip by remember { mutableStateOf(false) }
    val revenuePerMin = tree.maxHarvests * tree.sellPrice * 60_000.0 / tree.lifeMs
    val borderColor = if (selected) MaterialTheme.colorScheme.tertiary
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val bg = if (selected) MaterialTheme.colorScheme.tertiaryContainer
        else MaterialTheme.colorScheme.surface

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (canAfford) 1f else 0.38f)
                .clip(RoundedCornerShape(10.dp))
                .background(bg)
                .border(if (selected) 3.dp else 2.dp, borderColor, RoundedCornerShape(10.dp))
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        val upBeforeTimeout = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                            waitForUpOrCancellation()
                        }
                        if (upBeforeTimeout != null) {
                            onClick()
                        } else {
                            showTooltip = true
                            waitForUpOrCancellation()
                            showTooltip = false
                        }
                    }
                }
                .padding(vertical = 4.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("${tree.treeEmoji}${tree.fruitEmoji}", fontSize = 18.sp)
            Text(
                tree.displayName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                "🪙${prettyCoins(tree.coinCost)}·${tree.maxHarvests}×${prettyCoins(tree.sellPrice)}·${prettyTime(tree.lifeMs)}",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (showTooltip) {
            AboveAnchorTooltip("${"%.1f".format(revenuePerMin)} coins/min")
        }
    }
}

@Composable
private fun SeedButton(
    crop: CropType,
    selected: Boolean,
    coins: Int,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val canAfford = coins >= crop.coinCost
    var showTooltip by remember { mutableStateOf(false) }
    val revenuePerMin = crop.sellPrice * 60_000.0 / crop.growthMs
    val borderColor = if (selected) MaterialTheme.colorScheme.secondary
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val bg = if (selected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surface

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (canAfford) 1f else 0.38f)
                .clip(RoundedCornerShape(10.dp))
                .background(bg)
                .border(if (selected) 3.dp else 2.dp, borderColor, RoundedCornerShape(10.dp))
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        val upBeforeTimeout = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                            waitForUpOrCancellation()
                        }
                        if (upBeforeTimeout != null) {
                            onClick()
                        } else {
                            showTooltip = true
                            waitForUpOrCancellation()
                            showTooltip = false
                        }
                    }
                }
                .padding(vertical = 4.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(crop.emoji, fontSize = 22.sp)
            Text(
                crop.displayName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                "🪙${prettyCoins(crop.coinCost)}→${prettyCoins(crop.sellPrice)} ⚡${crop.plantEnergy}",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (showTooltip) {
            AboveAnchorTooltip("${"%.1f".format(revenuePerMin)} coins/min")
        }
    }
}

@Composable
private fun AboveAnchorTooltip(text: String) {
    val density = LocalDensity.current
    Popup(
        popupPositionProvider = object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
                val y = anchorBounds.top - popupContentSize.height - with(density) { 8.dp.roundToPx() }
                return IntOffset(
                    x.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
                    y.coerceAtLeast(0),
                )
            }
        },
    ) {
        Card(
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseSurface),
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(
                text,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.inverseOnSurface,
            )
        }
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
        val affordable = UPGRADES.filter { s.coins >= costFn(it) }
        val unavailable = UPGRADES.filter { s.coins < costFn(it) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (up in affordable + unavailable) {
                val cost = costFn(up)
                val lvl = s.upgradeLevels[up.key] ?: 0
                val canBuy = s.coins >= cost
                Column(
                    modifier = Modifier
                        .width(100.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (canBuy) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(
                            2.dp,
                            if (canBuy) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            RoundedCornerShape(10.dp),
                        )
                        .clickable(enabled = canBuy) { onBuy(up) }
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val labelColor = if (canBuy) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    Text(
                        up.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = labelColor,
                    )
                    Text(
                        up.desc,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = labelColor,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "🪙$cost · lv $lvl",
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor,
                    )
                }
            }
        }
    }
}

private fun prettyCoins(n: Int): String = if (n >= 1000) "${n / 1000}k" else "$n"

private fun prettyTime(ms: Long): String {
    val s = ms / 1000
    return when {
        s < 60 -> "${s}s"
        s < 3600 -> "${s / 60}m"
        else -> "${s / 3600}h"
    }
}
