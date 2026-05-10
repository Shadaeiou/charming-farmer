package com.shadaeiou.charmingfarmer.ui

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadaeiou.charmingfarmer.data.BeerRecipe
import com.shadaeiou.charmingfarmer.data.BjcpScore
import com.shadaeiou.charmingfarmer.data.BrewBatch
import com.shadaeiou.charmingfarmer.data.BrewStage
import com.shadaeiou.charmingfarmer.data.Brewery
import com.shadaeiou.charmingfarmer.data.FarmGame
import com.shadaeiou.charmingfarmer.data.ItemGrade
import com.shadaeiou.charmingfarmer.data.ItemType
import com.shadaeiou.charmingfarmer.data.Location
import com.shadaeiou.charmingfarmer.data.TransportService
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreweryScreen(onBack: () -> Unit, onOpenMap: () -> Unit) {
    val ctx = LocalContext.current
    val game = remember { FarmGame(ctx.applicationContext) }
    val transport = remember { TransportService.get(ctx.applicationContext) }
    val brewery = remember { Brewery.get(ctx.applicationContext) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var transportOpen by remember { mutableStateOf(false) }
    var batchToInspect by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            game.tick()
            transport.tick(System.currentTimeMillis())
            brewery.tick(transport, System.currentTimeMillis())
            nowMs = System.currentTimeMillis()
            delay(500)
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
    @Suppress("UNUSED_EXPRESSION") brewery.revisionTick
    @Suppress("UNUSED_EXPRESSION") transport.revisionTick

    val brewInv = transport.inventoryAt(Location.BREWERY)
    val maltStacks = brewInv.stacks.filter { it.type.name.startsWith("MALT_") }
    val bottleStacks = brewInv.stacks.filter { it.type.name.startsWith("BEER_") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🍺 Brewery", fontWeight = FontWeight.Bold) },
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
                text = brewery.feedback ?: "Pick a recipe, brew, ferment, bottle, sell. Higher-grade malt = better beer.",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                textAlign = TextAlign.Center,
                color = if (brewery.feedbackBad) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))

            // Active brews
            BrewerySection("⚗️ In progress (${brewery.activeBatches.count { it.stage != BrewStage.BOTTLED }}/${brewery.tier.brewSlots})") {
                val brewing = brewery.activeBatches.filter { it.stage != BrewStage.BOTTLED }
                if (brewing.isEmpty()) {
                    Text(
                        "No active brews. Start one below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    brewing.forEach { batch ->
                        BrewBatchRow(batch, nowMs)
                        Spacer(Modifier.height(6.dp))
                    }
                }
                Text(
                    "Equipment: ${brewery.tier.displayName} (cap ${brewery.tier.qualityCap})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Spacer(Modifier.height(10.dp))

            BrewerySection("🌾 Malt in cellar") {
                if (maltStacks.isEmpty()) {
                    Text(
                        "No malt yet. Ship pale, crystal, chocolate, black, or Munich malt from the malthouse.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    val grouped = maltStacks.groupBy { it.type }
                    grouped.forEach { (type, stacks) ->
                        val totalQty = stacks.sumOf { it.quantity }
                        val avg = stacks.sumOf { it.score.toLong() * it.quantity } / totalQty.coerceAtLeast(1)
                        InventoryRowRO(
                            emoji = type.emoji,
                            name = "${type.displayName} × $totalQty",
                            grade = ItemGrade.fromScore(avg.toInt()),
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            BrewerySection("📜 Recipes") {
                BeerRecipe.entries.forEach { recipe ->
                    val canBrew = brewery.availableSlots() > 0 &&
                        canSatisfyMalts(brewInv, recipe) &&
                        state.coins >= recipe.totalCoinCost &&
                        state.energy.toInt() >= recipe.brewEnergy
                    RecipeRow(
                        recipe = recipe,
                        enabled = canBrew,
                        onBrew = {
                            brewery.startBrew(
                                transport = transport,
                                recipe = recipe,
                                nowMs = System.currentTimeMillis(),
                                spendEnergy = { game.spendEnergy(it) },
                                spendCoins = { amount ->
                                    if (state.coins < amount) false else {
                                        // Direct coin deduction via FarmGame.addCoins(-amount)
                                        // because there's no public spendCoins yet.
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

            Spacer(Modifier.height(10.dp))

            BrewerySection("🍺 Bottled & ready to sell") {
                val bottled = brewery.activeBatches.filter { it.stage == BrewStage.BOTTLED }
                if (bottled.isEmpty()) {
                    Text(
                        "No bottles yet — finish a brew to fill the cellar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    bottled.forEach { batch ->
                        BottledRow(
                            batch = batch,
                            onSell = {
                                brewery.sellBottle(
                                    transport = transport,
                                    batchId = batch.id,
                                    addCoins = { game.addCoins(it) },
                                )
                            },
                            onInspect = { batchToInspect = batch.id },
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }
                if (bottleStacks.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Cellar inventory:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    bottleStacks.forEach { stack ->
                        InventoryRowRO(
                            emoji = stack.type.emoji,
                            name = "${stack.type.displayName} × ${stack.quantity}",
                            grade = ItemGrade.fromScore(stack.score),
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    // BJCP score reveal dialog
    val inspectBatch = batchToInspect?.let { id ->
        brewery.activeBatches.firstOrNull { it.id == id }
    }
    if (inspectBatch != null) {
        BjcpDialog(
            batch = inspectBatch,
            score = brewery.bjcpFor(inspectBatch),
            offFlavors = brewery.diagnoseOffFlavors(inspectBatch),
            onDismiss = { batchToInspect = null },
        )
    }
}

@Composable
private fun BrewerySection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
private fun BrewBatchRow(batch: BrewBatch, nowMs: Long) {
    val frac = batch.stageProgress(nowMs)
    val remainingS = (batch.stageRemainingMs(nowMs) / 1000).toInt()
    val stageEmoji = when (batch.stage) {
        BrewStage.MASH -> "♨️"
        BrewStage.BOIL -> "🔥"
        BrewStage.FERMENT -> "🫧"
        BrewStage.BOTTLED -> "🍾"
        BrewStage.SOLD -> "✅"
    }
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stageEmoji, fontSize = 18.sp)
            Spacer(Modifier.padding(end = 6.dp))
            Text(
                "${batch.recipe.displayName} — ${batch.stage.name.lowercase().replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (remainingS > 0) "${remainingS}s" else "advancing",
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
private fun RecipeRow(recipe: BeerRecipe, enabled: Boolean, onBrew: () -> Unit) {
    val bg = if (enabled) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val borderColor = if (enabled) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val labelColor = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(2.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled) { onBrew() }
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
            Text(
                "⚡${recipe.brewEnergy} · 🪙${recipe.totalCoinCost}",
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            recipe.description,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Needs: " + recipe.maltBill.entries.joinToString { "${it.value}× ${it.key.displayName}" },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = labelColor,
        )
        Text(
            "Stages: ${prettyMs(recipe.mashMs)} mash → ${prettyMs(recipe.boilMs)} boil → ${prettyMs(recipe.fermentMs)} ferment",
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
        )
    }
}

@Composable
private fun BottledRow(batch: BrewBatch, onSell: () -> Unit, onInspect: () -> Unit) {
    val score = batch.finalScore ?: 0
    val grade = ItemGrade.fromScore(score)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(batch.recipe.outputType.emoji, fontSize = 18.sp)
        Spacer(Modifier.padding(end = 6.dp))
        Column(Modifier.weight(1f)) {
            Text(
                batch.recipe.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Score $score · Grade ${grade.display}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(grade.color),
                fontWeight = FontWeight.Bold,
            )
        }
        TextButton(onClick = onInspect) { Text("Notes") }
        Button(onClick = onSell) { Text("Sell") }
    }
}

@Composable
private fun BjcpDialog(
    batch: BrewBatch,
    score: BjcpScore,
    offFlavors: List<String>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = {
            Text("${batch.recipe.outputType.emoji} ${batch.recipe.displayName}",
                fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    "BJCP-style breakdown",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                BjcpRow("Aroma", score.aroma, 24)
                BjcpRow("Appearance", score.appearance, 6)
                BjcpRow("Flavor", score.flavor, 40)
                BjcpRow("Mouthfeel", score.mouthfeel, 10)
                BjcpRow("Overall", score.overall, 20)
                Spacer(Modifier.height(8.dp))
                val grade = ItemGrade.fromScore(score.total)
                Text(
                    "Total: ${score.total}/100 · Grade ${grade.display}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(grade.color),
                )
                if (offFlavors.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Off-flavor notes",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    offFlavors.forEach { flaw ->
                        Text("• $flaw", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
    )
}

@Composable
private fun BjcpRow(label: String, value: Int, max: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(
            "$value / $max",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun InventoryRowRO(emoji: String, name: String, grade: ItemGrade) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 16.sp)
        Spacer(Modifier.padding(end = 6.dp))
        Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            "Grade ${grade.display}",
            style = MaterialTheme.typography.labelSmall,
            color = Color(grade.color),
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun canSatisfyMalts(
    inv: com.shadaeiou.charmingfarmer.data.Inventory,
    recipe: BeerRecipe,
): Boolean = recipe.maltBill.all { (type, kg) -> inv.totalOf(type) >= kg }

private fun prettyMs(ms: Long): String {
    val s = ms / 1000
    return when {
        s < 60 -> "${s}s"
        else -> "${s / 60}m"
    }
}
