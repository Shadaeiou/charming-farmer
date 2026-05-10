package com.shadaeiou.charmingfarmer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadaeiou.charmingfarmer.data.Appliance
import com.shadaeiou.charmingfarmer.data.CookStage
import com.shadaeiou.charmingfarmer.data.CookingRun
import com.shadaeiou.charmingfarmer.data.FarmGame
import com.shadaeiou.charmingfarmer.data.Inventory
import com.shadaeiou.charmingfarmer.data.ItemType
import com.shadaeiou.charmingfarmer.data.Kitchen
import com.shadaeiou.charmingfarmer.data.KitchenRecipe
import com.shadaeiou.charmingfarmer.data.Location
import com.shadaeiou.charmingfarmer.data.TransportService
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitchenScreen(onBack: () -> Unit, onOpenMap: () -> Unit) {
    val ctx = LocalContext.current
    val game = remember { FarmGame(ctx.applicationContext) }
    val transport = remember { TransportService.get(ctx.applicationContext) }
    val kitchen = remember { Kitchen.get(ctx.applicationContext) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var transportOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            game.tick()
            transport.tick(System.currentTimeMillis())
            kitchen.tick(transport, System.currentTimeMillis())
            nowMs = System.currentTimeMillis()
            delay(500)
        }
    }
    DisposableEffect(Unit) { onDispose { game.save() } }

    @Suppress("UNUSED_EXPRESSION") kitchen.revisionTick
    @Suppress("UNUSED_EXPRESSION") transport.revisionTick

    if (transportOpen) {
        TransportPanel(
            transport = transport,
            onDismiss = { transportOpen = false },
        )
    }

    val state = game.state
    val farmInventory = transport.inventoryAt(Location.FARM)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🍳 Kitchen", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { transportOpen = true }) {
                        Icon(Icons.Filled.LocalShipping, contentDescription = "Ship dishes")
                    }
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
                .background(Color(0xFF120A06)),
        ) {
            KitchenBackground(modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                EnergyAndCoinsRow(state.energy.toInt(), state.maxEnergy, state.coins)
                Spacer(Modifier.height(8.dp))

                Text(
                    text = kitchen.feedback ?: "Pick a recipe — chop, dice, knead, then cook on the right appliance.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xCC1F140C))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    textAlign = TextAlign.Center,
                    color = if (kitchen.feedbackBad) MaterialTheme.colorScheme.error
                        else Color(0xFFF5E6D0),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))

                KitchenSection("🍽️ In the kitchen") {
                    if (kitchen.activeRuns.isEmpty()) {
                        Text(
                            "Idle. Pick a recipe to start prep.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        kitchen.activeRuns.forEach { run ->
                            CookingRow(run, nowMs)
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                    Text(
                        "Equipment: ${kitchen.tier.displayName} (cap ${kitchen.tier.qualityCap})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                Spacer(Modifier.height(10.dp))

                KitchenSection("📜 Recipes") {
                    // Cheapest first; the order is stable regardless of
                    // affordability, so unaffordable recipes stay greyed
                    // out in place rather than jumping around.
                    KitchenRecipe.entries.sortedBy { it.ingredientCoinCost }.forEach { recipe ->
                        val needCoins = state.coins < recipe.ingredientCoinCost
                        val needEnergy = state.energy.toInt() < recipe.cookEnergy
                        val prepBusy = !kitchen.prepSlotFree()
                        val applianceBusy = !kitchen.applianceFree(recipe.appliance)
                        val canCook = !needCoins && !needEnergy && !prepBusy && !applianceBusy
                        RecipeRow(
                            recipe = recipe,
                            enabled = canCook,
                            shortCoins = needCoins,
                            shortEnergy = needEnergy,
                            applianceBusy = applianceBusy,
                            farmInventory = farmInventory,
                            onCook = {
                                kitchen.startCooking(
                                    recipe = recipe,
                                    nowMs = System.currentTimeMillis(),
                                    spendEnergy = { game.spendEnergy(it) },
                                    spendCoins = { amount ->
                                        if (state.coins < amount) false else {
                                            game.addCoins(-amount)
                                            true
                                        }
                                    },
                                )
                            },
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun KitchenSection(title: String, content: @Composable () -> Unit) {
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
private fun CookingRow(run: CookingRun, nowMs: Long) {
    val frac = run.progress(nowMs)
    val remainingS = (run.remainingMs(nowMs) / 1000).toInt()
    val (stageEmoji, stageLabel, barColor) = when (run.stage) {
        CookStage.PREP -> Triple(run.recipe.prepEmoji, "${run.recipe.prepLabel} (prep)", Color(0xFF8AB4F8))
        CookStage.COOK -> Triple(run.recipe.appliance.emoji, "${run.recipe.appliance.displayName} (cook)", Color(0xFFFFB74D))
    }
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(run.recipe.outputType.emoji, fontSize = 18.sp)
            Spacer(Modifier.padding(end = 6.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    run.recipe.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "$stageEmoji $stageLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                if (remainingS > 0) prettyTime(remainingS) else "plating",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { frac },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
private fun RecipeRow(
    recipe: KitchenRecipe,
    enabled: Boolean,
    shortCoins: Boolean,
    shortEnergy: Boolean,
    applianceBusy: Boolean,
    farmInventory: Inventory,
    onCook: () -> Unit,
) {
    val bg = if (enabled) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val borderColor = if (enabled) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val labelColor = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant
    val errorColor = MaterialTheme.colorScheme.error
    val energyColor = if (shortEnergy) errorColor else labelColor
    val coinColor = if (shortCoins) errorColor else labelColor

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(2.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled) { onCook() }
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(recipe.outputType.emoji, fontSize = 22.sp)
            Spacer(Modifier.padding(end = 8.dp))
            Text(
                recipe.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = labelColor,
                modifier = Modifier.weight(1f),
            )
            Row {
                Text(
                    "⚡${recipe.cookEnergy}",
                    style = MaterialTheme.typography.labelSmall,
                    color = energyColor,
                    fontWeight = if (shortEnergy) FontWeight.Bold else FontWeight.Normal,
                )
                Text(" · ", style = MaterialTheme.typography.labelSmall, color = labelColor)
                Text(
                    "🪙${prettyCoins(recipe.ingredientCoinCost)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = coinColor,
                    fontWeight = if (shortCoins) FontWeight.Bold else FontWeight.Normal,
                )
                Text(" · ", style = MaterialTheme.typography.labelSmall, color = labelColor)
                Text(
                    "🕒 ${prettyMinutes(recipe.totalDurationMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${recipe.prepEmoji} ${recipe.prepLabel}",
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
            )
            Text(" → ", style = MaterialTheme.typography.labelSmall, color = labelColor)
            Text(
                "${recipe.appliance.emoji} ${recipe.appliance.displayName}",
                style = MaterialTheme.typography.labelSmall,
                color = if (applianceBusy && !enabled) errorColor else labelColor,
                fontWeight = if (applianceBusy && !enabled) FontWeight.Bold else FontWeight.Normal,
            )
        }
        Spacer(Modifier.height(4.dp))
        // One row per ingredient — text turns red when the player's
        // FARM silo doesn't hold enough of that crop. Coins still pay
        // for the recipe (Phase 1), but the red flag is a hint that
        // the player should grow more.
        Column {
            recipe.ingredients.forEach { ing ->
                val cropItem = ItemType.valueOfOrNull("CROP_${ing.crop.name}")
                val onHand = if (cropItem != null) farmInventory.totalOf(cropItem) else 0
                val outOfStock = onHand < ing.quantity
                Text(
                    "${ing.quantity}× ${ing.crop.emoji} ${ing.crop.displayName}  (have $onHand)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (outOfStock) FontWeight.Bold else FontWeight.Normal,
                    color = if (outOfStock) errorColor else labelColor,
                )
            }
        }
    }
}

private fun prettyMinutes(ms: Long): String {
    val s = ms / 1000
    return when {
        s < 60 -> "${s}s"
        else -> "${s / 60}m"
    }
}

private fun prettyTime(seconds: Int): String = when {
    seconds < 60 -> "${seconds}s"
    else -> "${seconds / 60}m ${seconds % 60}s"
}

private fun prettyCoins(n: Int): String = when {
    n >= 1_000_000 -> {
        val m = n / 1_000_000.0
        if (m == m.toLong().toDouble()) "${m.toLong()}m" else "%.1fm".format(m)
    }
    n >= 1_000 -> "${n / 1_000}k"
    else -> "$n"
}

private val WallTile = Color(0xFFEFE0C2)
private val WallTileShadow = Color(0xFFC9B488)
private val WallGrout = Color(0xFF8E7855)
private val Counter = Color(0xFF8B5A3C)
private val CounterEdge = Color(0xFF5E3A22)
private val CabinetWood = Color(0xFF6E4423)
private val CabinetShadow = Color(0xFF3F2616)
private val CabinetHandle = Color(0xFFD9B45A)
private val FloorTileLight = Color(0xFFA0866A)
private val FloorTileDark = Color(0xFF6F5A42)
private val Stove = Color(0xFF2E2A28)
private val StoveTrim = Color(0xFF4A4644)
private val StoveBurner = Color(0xFF181412)
private val StoveBurnerHot = Color(0xFFE5421C)
private val StoveDial = Color(0xFFC9C5BE)
private val OvenWindow = Color(0xFF1B0E08)
private val OvenGlow = Color(0xFFFFB94A)
private val SinkBasin = Color(0xFFC8CDD0)
private val SinkBasinShade = Color(0xFF8C9094)
private val SinkRim = Color(0xFF6F7378)
private val SinkFaucet = Color(0xFFA0A4A8)
private val FridgeBody = Color(0xFFF1F1ED)
private val FridgeShadow = Color(0xFFC8C8C0)
private val FridgeHandle = Color(0xFF8A8A82)
private val FridgeSeal = Color(0xFF555048)
private val FridgeMagnet = Color(0xFFD9472E)

/**
 * Pixel-art kitchen background. Same approach as the living room —
 * everything draws on a fixed 64×40 virtual grid so layout is stable
 * across screen sizes. The interactive recipe list overlays this so
 * the player still sees the room while picking dishes.
 */
@Composable
private fun KitchenBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cols = 64f
        val rows = 40f
        val px = w / cols
        val py = h / rows
        fun rect(cx: Float, cy: Float, cw: Float, ch: Float, color: Color) {
            drawRect(color = color,
                topLeft = Offset(cx * px, cy * py),
                size = Size(cw * px, ch * py))
        }

        val floorTop = 28f

        // Tiled wall — pale ceramic squares
        rect(0f, 0f, cols, floorTop, WallTile)
        for (row in 0 until floorTop.toInt() step 4) {
            for (col in 0 until cols.toInt() step 4) {
                rect(col + 0.4f, row + 0.4f, 3.2f, 1f, WallTileShadow)
            }
        }
        for (row in 0..floorTop.toInt() step 4) {
            rect(0f, row.toFloat() - 0.2f, cols, 0.4f, WallGrout)
        }
        for (col in 0..cols.toInt() step 4) {
            rect(col.toFloat() - 0.2f, 0f, 0.4f, floorTop, WallGrout)
        }

        rect(0f, floorTop, cols, rows - floorTop, FloorTileDark)
        val tile = 4f
        var rowI = 0
        var fy = floorTop
        while (fy < rows) {
            var fx = if (rowI % 2 == 0) 0f else tile / 2f
            while (fx < cols) {
                rect(fx, fy, tile, tile, FloorTileLight)
                fx += tile * 2f
            }
            rowI += 1
            fy += tile
        }
        rect(0f, floorTop, cols, 0.6f, CabinetShadow)

        rect(2f, 6f, 26f, 8f, CabinetWood)
        rect(2f, 6f, 26f, 0.6f, CabinetShadow)
        rect(2f, 13.4f, 26f, 0.6f, CabinetShadow)
        for (split in intArrayOf(8, 14, 20, 26)) {
            rect(split.toFloat() - 0.2f, 6f, 0.4f, 8f, CabinetShadow)
        }
        for (cx in intArrayOf(5, 11, 17, 23)) {
            rect(cx.toFloat(), 12.5f, 1f, 0.5f, CabinetHandle)
        }

        rect(46f, 6f, 16f, 6f, CabinetWood)
        rect(46f, 6f, 16f, 0.6f, CabinetShadow)
        rect(46f, 11.4f, 16f, 0.6f, CabinetShadow)
        for (split in intArrayOf(50, 54, 58)) {
            rect(split.toFloat() - 0.2f, 6f, 0.4f, 6f, CabinetShadow)
        }

        val counterY = 19f
        rect(2f, counterY, 42f, 2f, Counter)
        rect(2f, counterY, 42f, 0.4f, CabinetShadow)
        rect(2f, counterY + 1.6f, 42f, 0.4f, CounterEdge)
        rect(2f, counterY + 2f, 42f, 7f, CabinetWood)
        for (split in intArrayOf(8, 14, 20, 26, 32, 38)) {
            rect(split.toFloat() - 0.2f, counterY + 2f, 0.4f, 7f, CabinetShadow)
        }
        for (cx in intArrayOf(5, 11, 17, 23, 29, 35, 41)) {
            rect(cx.toFloat(), counterY + 5f, 1f, 0.5f, CabinetHandle)
        }

        val stX = 4f
        val stY = counterY - 11f
        rect(stX, stY, 12f, 4f, StoveTrim)
        rect(stX + 1f, stY + 4f, 10f, 1f, Stove)
        rect(stX, stY + 5f, 12f, 4f, Stove)
        rect(stX + 1f, stY + 6f, 4f, 2f, StoveBurner)
        rect(stX + 7f, stY + 6f, 4f, 2f, StoveBurner)
        rect(stX + 2f, stY + 6.5f, 2f, 1f, StoveBurnerHot)
        rect(stX, stY + 9f, 12f, 1.5f, StoveTrim)
        for (dx in intArrayOf(2, 5, 8, 11)) {
            rect(stX + dx - 0.5f, stY + 9.4f, 0.7f, 0.7f, StoveDial)
        }
        rect(stX, counterY + 2f, 12f, 7f, Stove)
        rect(stX + 1f, counterY + 3f, 10f, 4f, OvenWindow)
        rect(stX + 2f, counterY + 4f, 8f, 2f, OvenGlow)
        rect(stX + 1f, counterY + 7.5f, 10f, 0.6f, StoveTrim)
        rect(stX + 0.5f, counterY + 8.5f, 11f, 0.4f, StoveTrim)

        val skX = 18f
        val skY = counterY - 0.2f
        rect(skX, skY, 12f, 2.4f, SinkRim)
        rect(skX + 1f, skY + 0.4f, 10f, 1.6f, SinkBasin)
        rect(skX + 1f, skY + 0.4f, 10f, 0.4f, SinkBasinShade)
        rect(skX + 5.5f, counterY - 5f, 1f, 4f, SinkFaucet)
        rect(skX + 5.5f, counterY - 5f, 4f, 0.7f, SinkFaucet)
        rect(skX + 9f, counterY - 4f, 0.6f, 2f, SinkFaucet)
        rect(skX + 4.5f, counterY - 5.6f, 3f, 0.7f, SinkFaucet)

        rect(34f, counterY - 0.8f, 6f, 0.8f, CabinetWood)
        rect(35f, counterY - 1.5f, 4f, 0.7f, SinkBasin)

        // Air fryer on the counter, right of the cutting board
        val afX = 41f
        val afY = counterY - 4.5f
        rect(afX, afY, 4.5f, 4.5f, Stove)
        rect(afX + 0.5f, afY + 0.5f, 3.5f, 3f, OvenWindow)
        rect(afX + 1f, afY + 1.2f, 2.5f, 1.6f, OvenGlow)
        rect(afX + 0.5f, afY + 3.8f, 3.5f, 0.4f, StoveTrim)

        val frX = 48f
        val frY = counterY - 14f
        rect(frX, frY, 12f, 26f, FridgeShadow)
        rect(frX, frY, 11f, 26f, FridgeBody)
        rect(frX, frY + 7f, 11f, 0.5f, FridgeSeal)
        rect(frX + 9f, frY + 1.5f, 0.6f, 4.5f, FridgeHandle)
        rect(frX + 9f, frY + 9f, 0.6f, 14f, FridgeHandle)
        rect(frX, frY, 0.4f, 26f, FridgeSeal)
        rect(frX + 2f, frY + 10f, 1.2f, 1.2f, FridgeMagnet)
        rect(frX + 4.5f, frY + 12f, 1.2f, 1.2f, OvenGlow)
        rect(frX + 6.5f, frY + 10.5f, 1.2f, 1.2f, FramePaintGreen)
        rect(frX + 4f, frY - 2.5f, 4f, 2.5f, CabinetWood)
        rect(frX + 3.5f, frY - 5f, 5f, 3f, FramePaintGreen)
        rect(frX + 5f, frY - 6f, 1f, 1.5f, FramePaintGreen)
    }
}

private val FramePaintGreen = Color(0xFF3D7B5C)
