package com.shadaeiou.charmingfarmer.ui

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.shadaeiou.charmingfarmer.data.FARM_GOALS
import com.shadaeiou.charmingfarmer.data.FarmGame
import com.shadaeiou.charmingfarmer.data.FarmState
import com.shadaeiou.charmingfarmer.data.Goal
import com.shadaeiou.charmingfarmer.data.Plot
import com.shadaeiou.charmingfarmer.data.PlotKind
import com.shadaeiou.charmingfarmer.data.Season
import com.shadaeiou.charmingfarmer.data.TreeType
import com.shadaeiou.charmingfarmer.data.UPGRADES
import com.shadaeiou.charmingfarmer.data.Upgrade
import kotlin.math.cos
import kotlin.math.sin
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
    var currentSeason by remember { mutableStateOf(game.currentSeason()) }
    var seasonCycleProgress by remember { mutableFloatStateOf(game.seasonCycleProgress()) }
    var msUntilNextSeason by remember { mutableLongStateOf(game.msUntilNextSeason()) }
    var transportOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            game.tick()
            transport.tick(System.currentTimeMillis())
            nowMs = System.currentTimeMillis()
            currentSeason = game.currentSeason(nowMs)
            seasonCycleProgress = game.seasonCycleProgress(nowMs)
            msUntilNextSeason = game.msUntilNextSeason(nowMs)
            delay(250)
        }
    }
    DisposableEffect(Unit) { onDispose { game.save() } }

    if (transportOpen) {
        TransportPanel(
            transport = transport,
            onDismiss = { transportOpen = false },
        )
    }

    val state = game.state

    Box(modifier = Modifier.fillMaxSize()) {
        SeasonBackground(season = currentSeason, modifier = Modifier.fillMaxSize())
        Scaffold(
            containerColor = Color.Transparent,
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
                StatCards(
                    s = state,
                    cycleProgress = seasonCycleProgress,
                    currentSeason = currentSeason,
                    msUntilNextSeason = msUntilNextSeason,
                )
                Spacer(Modifier.height(4.dp))
                SeasonBanner(currentSeason)
                Spacer(Modifier.height(2.dp))
                FeedbackText(game.feedback, game.feedbackBad)
                Spacer(Modifier.height(2.dp))
                FarmGrid(state, nowMs, currentSeason, modifier = Modifier.weight(1f), onPlotClick = { game.clickPlot(it) })
                Spacer(Modifier.height(4.dp))
                SeedShelf(state, currentSeason, onSelect = { game.selectSeed(it) })
                Spacer(Modifier.height(2.dp))
                TreeNursery(state, currentSeason, onSelect = { game.selectTree(it) })
                Spacer(Modifier.height(4.dp))
                UpgradesRow(state, costFn = game::upgradeCost, onBuy = { game.buyUpgrade(it) })
                Spacer(Modifier.height(2.dp))
                GoalsSection(state, onClaim = { game.completeGoal(it) })
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}

private fun formatRemaining(ms: Long): String {
    val secs = (ms / 1000L).coerceAtLeast(0L)
    return if (secs < 60L) "${secs}s" else "${secs / 60L}m ${secs % 60L}s"
}

@Composable
private fun StatCards(
    s: FarmState,
    cycleProgress: Float,
    currentSeason: Season,
    msUntilNextSeason: Long,
) {
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Card(
            modifier = Modifier.weight(1f).fillMaxHeight(),
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
            modifier = Modifier.weight(1f).fillMaxHeight(),
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
        // Season clock — third card, square, fills the same height as the other two.
        // Long-press to see how long until the season changes; the tooltip
        // sits right below the clock and disappears the moment you let go.
        var showSeasonTooltip by remember { mutableStateOf(false) }
        Box(modifier = Modifier.width(72.dp).fillMaxHeight()) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            val upBeforeTimeout = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                waitForUpOrCancellation()
                            }
                            if (upBeforeTimeout == null) {
                                showSeasonTooltip = true
                                waitForUpOrCancellation()
                                showSeasonTooltip = false
                            }
                        }
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Box(Modifier.fillMaxSize().padding(8.dp)) {
                    SeasonClock(cycleProgress, Modifier.fillMaxSize())
                }
            }
            if (showSeasonTooltip) {
                val nextSeason = Season.next(currentSeason)
                BelowAnchorTooltip(
                    "Next: ${nextSeason.emoji} ${nextSeason.displayName} in ${formatRemaining(msUntilNextSeason)}"
                )
            }
        }
    }
}

@Composable
private fun SeasonBanner(season: Season) {
    val (bg, fg) = when (season) {
        Season.SPRING -> Color(0xFFD0F0B0) to Color(0xFF2D6A00)
        Season.SUMMER -> Color(0xFF2E7D32) to Color.White
        Season.FALL -> Color(0xFFBF360C) to Color.White
        Season.WINTER -> Color(0xFF1565C0) to Color.White
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "${season.emoji} ${season.displayName}",
            color = fg,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun FeedbackText(msg: String?, bad: Boolean) {
    val text = msg ?: "Tap grass to till. Tap tilled soil to plant your selected seed."
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 22.dp)
            .padding(horizontal = 4.dp),
        textAlign = TextAlign.Center,
        color = if (bad) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun FarmGrid(
    s: FarmState,
    nowMs: Long,
    currentSeason: Season,
    modifier: Modifier = Modifier,
    onPlotClick: (Int) -> Unit,
) {
    val gridCols = when {
        s.plotCount <= 16 -> 4
        s.plotCount <= 25 -> 5
        s.plotCount <= 36 -> 6
        s.plotCount <= 42 -> 7
        else -> 8
    }
    val gridRows = (s.plotCount + gridCols - 1) / gridCols

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val targetAspect = gridCols.toFloat() / gridRows.toFloat()
        val widthFromHeight = maxHeight * targetAspect
        val gridW = minOf(maxWidth, widthFromHeight)
        val gridH = gridW / targetAspect
        Box(
            modifier = Modifier
                .width(gridW)
                .height(gridH)
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
                for (r in 0 until gridRows) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        for (c in 0 until gridCols) {
                            val idx = r * gridCols + c
                            PlotCell(
                                plot = s.plots.getOrElse(idx) { com.shadaeiou.charmingfarmer.data.Plot() },
                                nowMs = nowMs,
                                currentSeason = currentSeason,
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
private fun PlotCell(plot: Plot, nowMs: Long, currentSeason: Season, modifier: Modifier, onClick: () -> Unit) {
    val treeDead = plot.kind == PlotKind.TREE && plot.treeIsDead(nowMs)
    val treeWrongSeason = plot.kind == PlotKind.TREE && !treeDead && plot.treeReadyWrongSeason(nowMs, currentSeason)
    val treeReady = plot.kind == PlotKind.TREE && !treeDead && !treeWrongSeason && plot.treeHarvestReady(nowMs)
    val cropDead = plot.kind == PlotKind.PLANTED && plot.isCropDead(currentSeason)
    val (bg, borderColor) = when {
        treeDead -> Color(0xFF4A3020) to Color(0xFF2A180A)
        cropDead -> Color(0xFF2A1A0A) to Color(0xFF140D05)
        // Tree harvest ready → bright green outline so the plot pops
        // visually even on a busy farm grid.
        treeReady -> Color(0xFF5B3E1F) to Color(0xFF4CAF50)
        plot.kind == PlotKind.TREE -> Color(0xFF5B3E1F) to SoilDarkColor
        plot.kind == PlotKind.GRASS -> GrassColor to GrassEdgeColor
        else -> SoilTilledColor to SoilDarkColor
    }
    val frac = plot.growthFraction(nowMs)
    val ready = plot.kind == PlotKind.PLANTED && !cropDead && frac >= 1f

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
                    val showFruit = treeReady || treeWrongSeason
                    Text(
                        text = if (showFruit) tree.fruitEmoji else tree.treeEmoji,
                        fontSize = 26.sp,
                        modifier = Modifier
                            .scale(bounceScale)
                            .alpha(if (treeWrongSeason) 0.5f else 1f),
                    )
                    if (treeWrongSeason) {
                        // Show the first harvest season emoji as a hint
                        val hintSeason = tree.harvestSeasons
                            .minByOrNull { listOf(Season.SPRING, Season.SUMMER, Season.FALL, Season.WINTER).indexOf(it) }
                        Text(
                            "${hintSeason?.emoji ?: ""}⏳",
                            fontSize = 10.sp,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp),
                        )
                    }
                    Text(
                        if (showFruit) tree.treeEmoji else tree.fruitEmoji,
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.TopStart).padding(2.dp),
                    )
                    Text(
                        "${plot.harvestCount}/${tree.maxHarvests}",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.align(Alignment.TopEnd).padding(2.dp),
                    )
                    val allHarvestsDone = plot.harvestCount >= tree.maxHarvests
                    if (!allHarvestsDone) {
                        val harvestFrac = plot.treeHarvestIntervalFraction(nowMs)
                        val animHarvestFrac by animateFloatAsState(harvestFrac, tween(300), label = "harvest")
                        // Yellow while the tree is still growing toward
                        // the next harvest; bright green the moment a
                        // fruit is ready to pick.
                        val barColor = when {
                            treeWrongSeason -> Color(0xFF9E9E9E)
                            treeReady -> Color(0xFF4CAF50)
                            else -> Color(0xFFFFC107)
                        }
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
                                    .fillMaxWidth(if (treeReady) 1f else animHarvestFrac)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(barColor),
                            )
                        }
                    }
                }
            }
            PlotKind.PLANTED -> {
                if (cropDead) {
                    Text("💀", fontSize = 26.sp)
                    plot.crop?.let {
                        Text(
                            it.emoji,
                            fontSize = 11.sp,
                            modifier = Modifier.align(Alignment.TopStart).padding(2.dp),
                        )
                    }
                } else {
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
            }
            else -> Unit
        }
    }
}

@Composable
private fun SeedShelf(state: FarmState, currentSeason: Season, onSelect: (CropType) -> Unit) {
    Column {
        Text("🌱 Seeds", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            for (c in CropType.entries) {
                SeedButton(
                    crop = c,
                    selected = state.selectedTree == null && c == state.selectedSeed,
                    coins = state.coins,
                    currentSeason = currentSeason,
                    modifier = Modifier.width(62.dp),
                    onClick = { onSelect(c) },
                )
            }
        }
    }
}

@Composable
private fun TreeNursery(state: FarmState, currentSeason: Season, onSelect: (TreeType) -> Unit) {
    Column {
        Text("🌳 Trees", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            for (t in TreeType.entries) {
                TreeButton(
                    tree = t,
                    selected = t == state.selectedTree,
                    coins = state.coins,
                    currentSeason = currentSeason,
                    modifier = Modifier.width(70.dp),
                    onClick = { onSelect(t) },
                )
            }
        }
    }
}

@Composable
private fun TreeButton(tree: TreeType, selected: Boolean, coins: Int, currentSeason: Season, modifier: Modifier, onClick: () -> Unit) {
    val canAfford = coins >= tree.coinCost
    val inHarvestSeason = currentSeason in tree.harvestSeasons
    var showTooltip by remember { mutableStateOf(false) }
    val revenuePerMin = tree.maxHarvests * tree.sellPrice * 60_000.0 / tree.lifeMs
    val borderColor = if (selected) MaterialTheme.colorScheme.tertiary
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val bg = if (selected) MaterialTheme.colorScheme.tertiaryContainer
        else MaterialTheme.colorScheme.surface
    val contentAlpha = if (canAfford) 1f else 0.38f
    val seasonOrder = listOf(Season.SPRING, Season.SUMMER, Season.FALL, Season.WINTER)
    val harvestSeasonStr = tree.harvestSeasons
        .sortedBy { seasonOrder.indexOf(it) }
        .joinToString("") { it.emoji }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(contentAlpha)
                .clip(RoundedCornerShape(8.dp))
                .background(bg)
                .border(if (selected) 3.dp else 1.dp, borderColor, RoundedCornerShape(8.dp))
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
                .padding(vertical = 3.dp, horizontal = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("${tree.treeEmoji}${tree.fruitEmoji}", fontSize = 14.sp)
            Text(
                tree.displayName,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 11.sp,
            )
            Text(
                "🪙${prettyCoins(tree.coinCost)} ${if (inHarvestSeason) "$harvestSeasonStr✓" else harvestSeasonStr}",
                fontSize = 8.sp,
                color = if (inHarvestSeason) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 10.sp,
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
    currentSeason: Season,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val canAfford = coins >= crop.coinCost
    val inSeason = currentSeason in crop.plantSeasons
    var showTooltip by remember { mutableStateOf(false) }
    val revenuePerMin = crop.sellPrice * 60_000.0 / crop.growthMs
    val borderColor = if (selected) MaterialTheme.colorScheme.secondary
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val bg = if (selected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surface
    val contentAlpha = when {
        !canAfford -> 0.38f
        !inSeason -> 0.55f
        else -> 1f
    }
    val seasonOrder = listOf(Season.SPRING, Season.SUMMER, Season.FALL, Season.WINTER)
    val plantSeasonStr = crop.plantSeasons
        .sortedBy { seasonOrder.indexOf(it) }
        .joinToString("") { it.emoji }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(contentAlpha)
                .clip(RoundedCornerShape(8.dp))
                .background(bg)
                .border(if (selected) 3.dp else 1.dp, borderColor, RoundedCornerShape(8.dp))
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
                .padding(vertical = 3.dp, horizontal = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(crop.emoji, fontSize = 15.sp)
            Text(
                crop.displayName,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 11.sp,
            )
            Text(
                "🪙${prettyCoins(crop.coinCost)} ${if (inSeason) "$plantSeasonStr✓" else "❄︎"}",
                fontSize = 8.sp,
                color = if (inSeason) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 10.sp,
            )
        }
        if (showTooltip) {
            AboveAnchorTooltip("${"%.1f".format(revenuePerMin)} coins/min")
        }
    }
}

@Composable
private fun BelowAnchorTooltip(text: String) {
    val density = LocalDensity.current
    Popup(
        popupPositionProvider = object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val gap = with(density) { 6.dp.roundToPx() }
                val centeredX = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
                val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
                val x = centeredX.coerceIn(0, maxX)
                val belowY = anchorBounds.bottom + gap
                val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
                val y = if (belowY <= maxY) belowY
                    else (anchorBounds.top - popupContentSize.height - gap).coerceAtLeast(0)
                return IntOffset(x, y)
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
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(3.dp))
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
                        "🪙${prettyCoins(cost)} · lv $lvl",
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor,
                    )
                }
            }
        }
    }
}

/** 4-quadrant clock: Spring=top-right, Summer=bottom-right, Fall=bottom-left, Winter=top-left. */
@Composable
private fun SeasonClock(cycleProgress: Float, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = minOf(size.width, size.height) / 2f - 2f
            val arcRect = androidx.compose.ui.geometry.Rect(cx - r, cy - r, cx + r, cy + r)

            // Semi-opaque dark backing
            drawCircle(Color(0xCC111111), radius = r + 2f, center = Offset(cx, cy))

            // Season quadrants (muted colours matching backgrounds)
            // Spring  12→3 o'clock = startAngle -90, sweep 90
            drawArc(Color(0xFF1B3A12), -90f, 90f, useCenter = true,
                topLeft = Offset(arcRect.left, arcRect.top),
                size = Size(arcRect.width, arcRect.height))
            // Summer  3→6 o'clock
            drawArc(Color(0xFF2B3A00), 0f, 90f, useCenter = true,
                topLeft = Offset(arcRect.left, arcRect.top),
                size = Size(arcRect.width, arcRect.height))
            // Fall    6→9 o'clock
            drawArc(Color(0xFF3A1500), 90f, 90f, useCenter = true,
                topLeft = Offset(arcRect.left, arcRect.top),
                size = Size(arcRect.width, arcRect.height))
            // Winter  9→12 o'clock
            drawArc(Color(0xFF0A1E3A), 180f, 90f, useCenter = true,
                topLeft = Offset(arcRect.left, arcRect.top),
                size = Size(arcRect.width, arcRect.height))

            // Quadrant divider lines
            val div = Color(0x55FFFFFF)
            drawLine(div, Offset(cx, cy - r), Offset(cx, cy + r), 1f)
            drawLine(div, Offset(cx - r, cy), Offset(cx + r, cy), 1f)

            // Outer ring
            drawCircle(Color(0x44FFFFFF), radius = r, center = Offset(cx, cy),
                style = Stroke(1.5f))

            // Clock hand
            val handAngleDeg = cycleProgress * 360f - 90f
            val handAngleRad = Math.toRadians(handAngleDeg.toDouble()).toFloat()
            val handLen = r * 0.72f
            drawLine(
                Color.White,
                Offset(cx, cy),
                Offset(cx + handLen * cos(handAngleRad), cy + handLen * sin(handAngleRad)),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round,
            )
            // Centre pivot
            drawCircle(Color.White, radius = 3f, center = Offset(cx, cy))
        }
        // Season emojis at each corner quadrant
        Box(modifier = Modifier.fillMaxSize()) {
            Text(Season.SPRING.emoji, fontSize = 13.sp,
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 2.dp, top = 2.dp))
            Text(Season.SUMMER.emoji, fontSize = 13.sp,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 2.dp, bottom = 2.dp))
            Text(Season.FALL.emoji, fontSize = 13.sp,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 2.dp, bottom = 2.dp))
            Text(Season.WINTER.emoji, fontSize = 13.sp,
                modifier = Modifier.align(Alignment.TopStart).padding(start = 2.dp, top = 2.dp))
        }
    }
}

@Composable
private fun SeasonBackground(season: Season, modifier: Modifier = Modifier) {
    when (season) {
        Season.FALL -> FallBackground(modifier)
        Season.WINTER -> WinterBackground(modifier)
        Season.SPRING -> SpringBackground(modifier)
        Season.SUMMER -> SummerBackground(modifier)
    }
}

/** Dark reddish-brown with alternating up/down triangle tessellation. */
@Composable
private fun FallBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(Color(0xFF180800))
        val step = 64f
        val rows = (size.height / step).toInt() + 3
        val cols = (size.width / step).toInt() + 2
        val fill = Color(0xFF2C1005)
        val stroke = Color(0xFF3D1A08)
        for (row in -1..rows) {
            for (col in -1..cols) {
                val xOff = if (row % 2 == 0) 0f else step / 2
                val cx = col * step + xOff
                val cy = row * step
                val path = Path()
                if ((row + col) % 2 == 0) {
                    // upward triangle
                    path.moveTo(cx, cy - step * 0.5f)
                    path.lineTo(cx - step * 0.5f, cy + step * 0.35f)
                    path.lineTo(cx + step * 0.5f, cy + step * 0.35f)
                } else {
                    // downward triangle
                    path.moveTo(cx, cy + step * 0.5f)
                    path.lineTo(cx - step * 0.5f, cy - step * 0.35f)
                    path.lineTo(cx + step * 0.5f, cy - step * 0.35f)
                }
                path.close()
                drawPath(path, fill)
                drawPath(path, stroke, style = Stroke(1f))
            }
        }
    }
}

/** Near-black navy with a grid of small muted diamonds. */
@Composable
private fun WinterBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(Color(0xFF030C1A))
        val step = 52f
        val fill = Color(0xFF0A1D30)
        val stroke = Color(0xFF122540)
        val rows = (size.height / step).toInt() + 3
        val cols = (size.width / step).toInt() + 2
        val ds = 10f  // half-size of diamond
        for (row in -1..rows) {
            val xOff = if (row % 2 == 0) 0f else step / 2
            for (col in -1..cols) {
                val cx = col * step + xOff
                val cy = row * step
                val path = Path()
                path.moveTo(cx, cy - ds)
                path.lineTo(cx + ds, cy)
                path.lineTo(cx, cy + ds)
                path.lineTo(cx - ds, cy)
                path.close()
                drawPath(path, fill)
                drawPath(path, stroke, style = Stroke(1f))
            }
        }
    }
}

/** Very dark green with a honeycomb of hexagon outlines. */
@Composable
private fun SpringBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(Color(0xFF050F02))
        val r = 28f
        val hexW = r * 1.732f   // sqrt(3) * r
        val hexH = r * 2f
        val cols = (size.width / hexW).toInt() + 3
        val rows = (size.height / (hexH * 0.75f)).toInt() + 3
        val stroke = Color(0xFF0F2A08)
        for (row in -1..rows) {
            for (col in -1..cols) {
                val xOff = if (row % 2 == 0) 0f else hexW / 2
                val cx = col * hexW + xOff
                val cy = row * hexH * 0.75f
                val path = Path()
                for (i in 0 until 6) {
                    val angle = Math.toRadians((60.0 * i - 30)).toFloat()
                    val x = cx + r * cos(angle)
                    val y = cy + r * sin(angle)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(path, Color(0xFF0A1F05))
                drawPath(path, stroke, style = Stroke(1.2f))
            }
        }
    }
}

/** Very dark green with diagonal stripe bands. */
@Composable
private fun SummerBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(Color(0xFF021200))
        val stripeWidth = 18f
        val gap = 44f
        val diagLen = size.width + size.height
        var offset = -diagLen
        while (offset < size.width + gap) {
            val path = Path()
            path.moveTo(offset, 0f)
            path.lineTo(offset + stripeWidth, 0f)
            path.lineTo(offset + stripeWidth + size.height, size.height)
            path.lineTo(offset + size.height, size.height)
            path.close()
            drawPath(path, Color(0xFF072800))
            offset += gap
        }
    }
}

@Composable
private fun GoalsSection(s: FarmState, onClaim: (String) -> Unit) {
    val nextGoal = FARM_GOALS.firstOrNull { it.id !in s.completedGoals } ?: return
    val coinsOk = s.coins >= nextGoal.coinsRequired
    val harvestsOk = s.harvested >= nextGoal.harvestsRequired
    val canClaim = coinsOk && harvestsOk
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "🎯 ${nextGoal.title}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (canClaim) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { onClaim(nextGoal.id) }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            "Claim!",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "🪙 ${prettyCoins(s.coins)} / ${prettyCoins(nextGoal.coinsRequired)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (coinsOk) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LinearProgressIndicator(
                        progress = { (s.coins.toFloat() / nextGoal.coinsRequired).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                        color = if (coinsOk) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "🌾 ${s.harvested} / ${nextGoal.harvestsRequired}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (harvestsOk) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LinearProgressIndicator(
                        progress = { (s.harvested.toFloat() / nextGoal.harvestsRequired).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                        color = if (harvestsOk) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        }
    }
}

private fun prettyCoins(n: Int): String = when {
    n >= 1_000_000 -> {
        val m = n / 1_000_000.0
        if (m == m.toLong().toDouble()) "${m.toLong()}m" else "${"%.1f".format(m)}m"
    }
    n >= 1_000 -> "${n / 1_000}k"
    else -> "$n"
}

private fun prettyTime(ms: Long): String {
    val s = ms / 1000
    return when {
        s < 60 -> "${s}s"
        s < 3600 -> "${s / 60}m"
        else -> "${s / 3600}h"
    }
}
