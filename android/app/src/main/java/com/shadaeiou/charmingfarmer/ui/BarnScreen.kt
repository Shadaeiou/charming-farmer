package com.shadaeiou.charmingfarmer.ui

import androidx.compose.foundation.Canvas
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
import com.shadaeiou.charmingfarmer.data.FarmGame
import com.shadaeiou.charmingfarmer.data.ItemGrade
import com.shadaeiou.charmingfarmer.data.ItemStack
import com.shadaeiou.charmingfarmer.data.ItemTier
import com.shadaeiou.charmingfarmer.data.ItemType
import com.shadaeiou.charmingfarmer.data.Location
import com.shadaeiou.charmingfarmer.data.TransportService
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarnScreen(onBack: () -> Unit, onOpenMap: () -> Unit) {
    val ctx = LocalContext.current
    val game = remember { FarmGame(ctx.applicationContext) }
    val transport = remember { TransportService.get(ctx.applicationContext) }
    var transportOpen by remember { mutableStateOf(false) }
    @Suppress("UNUSED_EXPRESSION", "AssignedValueIsNeverRead")
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

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

    if (transportOpen) {
        TransportPanel(
            transport = transport,
            origin = Location.FARM,
            allowedDestinations = listOf(
                Location.MALTHOUSE,
                Location.BREWERY,
                Location.MARKET,
            ),
            onDismiss = { transportOpen = false },
        )
    }

    val state = game.state
    val farmInv = transport.inventoryAt(Location.FARM)
    val grouped = farmInv.stacks.groupBy { it.type }
    val raw = grouped.filterKeys { it.isRawCrop() }
    val processed = grouped.filterKeys { it.isProcessed() }
    val dishes = grouped.filterKeys { it.name.startsWith("DISH_") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🏚️  Storage Barn", fontWeight = FontWeight.Bold) },
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
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF15090A)),
        ) {
            BarnInteriorBackground(modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                EnergyAndCoinsRow(state.energy.toInt(), state.maxEnergy, state.coins)
                Spacer(Modifier.height(8.dp))

                val totalItems = farmInv.stacks.sumOf { it.quantity }
                val uniqueTypes = grouped.keys.size
                Text(
                    text = if (totalItems == 0)
                        "The barn is empty. Cooked dishes and harvested grain land here."
                    else
                        "$totalItems items across $uniqueTypes types in storage.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xCC1F0B07))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    textAlign = TextAlign.Center,
                    color = Color(0xFFF5E6D0),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))

                BarnSection("🍽️ Artisan dishes", dishes)
                Spacer(Modifier.height(10.dp))
                BarnSection("🟡 Malt & processed", processed)
                Spacer(Modifier.height(10.dp))
                BarnSection("🌾 Raw grain & hops", raw)
                Spacer(Modifier.height(8.dp))

                Text(
                    "Tap the 🚚 truck to ship from the barn to the malthouse, brewery, or market.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFCBB89A),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
                )
            }
        }
    }
}

private fun ItemType.isRawCrop(): Boolean = when (this) {
    ItemType.BARLEY, ItemType.WHEAT_GRAIN, ItemType.OATS, ItemType.RYE,
    ItemType.HOPS, ItemType.HOPS_CASCADE, ItemType.HOPS_SAAZ,
    ItemType.HOPS_FUGGLE, ItemType.HOPS_CITRA -> true
    else -> false
}

private fun ItemType.isProcessed(): Boolean = name.startsWith("MALT_") ||
    name.startsWith("YEAST_") || name.startsWith("BEER_")

@Composable
private fun BarnSection(title: String, group: Map<ItemType, List<ItemStack>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            if (group.isEmpty()) {
                Text(
                    "Nothing here yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Card
            }
            group.forEach { (type, stacks) ->
                StackRow(type, stacks)
            }
        }
    }
}

@Composable
private fun StackRow(type: ItemType, stacks: List<ItemStack>) {
    val totalQty = stacks.sumOf { it.quantity }
    val weighted = stacks.sumOf { it.score.toLong() * it.quantity }
    val avg = (weighted / totalQty.coerceAtLeast(1)).toInt()
    val grade = ItemGrade.fromScore(avg)
    val bestTier = stacks.maxByOrNull { it.tier.ordinal }?.tier ?: ItemTier.NORMAL

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(type.emoji, fontSize = 18.sp)
        Spacer(Modifier.padding(end = 6.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "${type.displayName} × $totalQty",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Avg score $avg" + if (bestTier != ItemTier.NORMAL)
                    " · best tier ${bestTier.displayName} ${bestTier.emojiSuffix}" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "Grade ${grade.display}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color(grade.color),
        )
    }
}

private val PlankLight = Color(0xFFA06A3F)
private val PlankMid = Color(0xFF7A4F2C)
private val PlankDark = Color(0xFF4F311A)
private val PlankSeam = Color(0xFF2E1B0C)
private val FloorPlank = Color(0xFF6B4226)
private val FloorPlankShade = Color(0xFF3E2614)
private val Hay = Color(0xFFE5C064)
private val HayShade = Color(0xFFB48E36)
private val HayString = Color(0xFF5E3F1A)
private val SackBurlap = Color(0xFFC9A36A)
private val SackBurlapShade = Color(0xFF8E703F)
private val SackTie = Color(0xFF5E3F1A)
private val CrateWood = Color(0xFF8B5A3C)
private val CrateShade = Color(0xFF5E3A22)
private val CrateMetal = Color(0xFF55504A)
private val LanternFrame = Color(0xFF2A1A0A)
private val LanternGlass = Color(0xFFFFE9A8)
private val LanternGlow = Color(0x66FFE9A8)
private val LoftDoor = Color(0xFF5E3A22)
private val LoftDoorShade = Color(0xFF2E1B0C)
private val PitchforkHandle = Color(0xFF8B6033)
private val PitchforkSteel = Color(0xFFB8B8B0)
private val Cobweb = Color(0x66FFFFFF)

/**
 * Pixel-art barn interior. Wooden plank back wall with a small loft
 * door near the apex, hay bales on the floor, sacks of grain stacked
 * against the wall, a wooden crate, a hanging lantern casting a warm
 * glow, a pitchfork leaned in the corner. Drawn on the same 64×40
 * virtual grid the kitchen/living-room scenes use.
 */
@Composable
private fun BarnInteriorBackground(modifier: Modifier = Modifier) {
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

        val floorTop = 30f

        // Plank back wall — horizontal banding to suggest stacked planks
        rect(0f, 0f, cols, floorTop, PlankMid)
        var bandY = 0f
        var bandIndex = 0
        while (bandY < floorTop) {
            val color = when (bandIndex % 3) {
                0 -> PlankLight
                1 -> PlankMid
                else -> PlankDark
            }
            rect(0f, bandY, cols, 2.5f, color)
            rect(0f, bandY + 2.5f, cols, 0.4f, PlankSeam)
            bandY += 2.9f
            bandIndex += 1
        }
        // Vertical seams between planks (every 8 cells)
        for (x in 0 until cols.toInt() step 8) {
            rect(x.toFloat() - 0.2f, 0f, 0.4f, floorTop, PlankSeam)
        }

        // Wooden floor — coarser planks running across
        rect(0f, floorTop, cols, rows - floorTop, FloorPlank)
        for (band in 0 until 3) {
            val y = floorTop + band * 4f + 1f
            rect(0f, y, cols, 0.3f, FloorPlankShade)
        }
        // Floor plank seams (verticals)
        for (x in 0 until cols.toInt() step 6) {
            rect(x.toFloat() - 0.15f, floorTop, 0.3f, rows - floorTop, FloorPlankShade)
        }

        // Hayloft door high on the wall
        val loftX = 27f; val loftY = 3f; val loftW = 10f; val loftH = 8f
        rect(loftX, loftY, loftW, loftH, LoftDoorShade)
        rect(loftX + 0.5f, loftY + 0.5f, loftW - 1f, loftH - 1f, LoftDoor)
        // Vertical plank seams in the loft door
        for (xs in 1 until 5) {
            rect(loftX + xs * 2f - 0.15f, loftY + 0.5f, 0.3f, loftH - 1f, LoftDoorShade)
        }
        // Cross brace (X pattern faintly)
        rect(loftX + 1f, loftY + loftH / 2f - 0.2f, loftW - 2f, 0.4f, LoftDoorShade)
        // A wisp of hay sticking out at the bottom of the loft door
        rect(loftX + 4f, loftY + loftH - 0.2f, 2.5f, 1f, Hay)
        rect(loftX + 4.5f, loftY + loftH + 0.6f, 1.5f, 0.5f, HayShade)

        // Hanging lantern from the rafters on the right
        val lnX = 50f; val lnY = 1f
        // Rope
        rect(lnX + 1.5f, lnY, 0.4f, 5f, LanternFrame)
        // Lantern body
        rect(lnX, lnY + 5f, 4f, 5f, LanternFrame)
        rect(lnX + 0.7f, lnY + 5.7f, 2.6f, 3.6f, LanternGlass)
        rect(lnX + 1.3f, lnY + 6.3f, 1.4f, 2.4f, LanternGlow)
        // Halo of glow on the wall
        rect(lnX - 4f, lnY + 4f, 12f, 9f, LanternGlow)

        // Cobweb in upper-left corner
        for (i in 0 until 5) {
            rect(0.5f + i * 0.7f, 0.5f + i * 0.5f, 4f - i * 0.5f, 0.15f, Cobweb)
        }
        for (i in 0 until 5) {
            rect(0.5f, 0.5f + i * 0.7f, 0.15f, 4f - i * 0.5f, Cobweb)
        }

        // Hay bales stacked along the floor (left)
        // Big bale: 8 wide × 5 tall
        val hb1X = 4f; val hb1Y = floorTop - 5f
        rect(hb1X, hb1Y, 8f, 5f, Hay)
        rect(hb1X, hb1Y, 8f, 0.5f, HayShade)
        rect(hb1X, hb1Y + 4.5f, 8f, 0.5f, HayShade)
        // Strings tying the bale
        rect(hb1X + 2.2f, hb1Y, 0.3f, 5f, HayString)
        rect(hb1X + 5.8f, hb1Y, 0.3f, 5f, HayString)
        // Hay strands texture
        for (i in 0 until 12) {
            val sx = hb1X + (i * 0.7f) % 7.5f
            val sy = hb1Y + 0.8f + (i * 0.5f) % 3.5f
            rect(sx, sy, 0.6f, 0.15f, HayShade)
        }
        // Stacked smaller bale on top
        val hb2X = 5f; val hb2Y = hb1Y - 3.5f
        rect(hb2X, hb2Y, 6f, 3.5f, Hay)
        rect(hb2X, hb2Y, 6f, 0.4f, HayShade)
        rect(hb2X, hb2Y + 3.1f, 6f, 0.4f, HayShade)
        rect(hb2X + 1.8f, hb2Y, 0.25f, 3.5f, HayString)
        rect(hb2X + 4f, hb2Y, 0.25f, 3.5f, HayString)

        // Sacks of grain — middle/right of floor
        // Sack 1
        val s1X = 18f; val s1Y = floorTop - 4f
        rect(s1X + 0.5f, s1Y, 4f, 4.5f, SackBurlap)
        rect(s1X, s1Y + 0.5f, 5f, 4f, SackBurlap)
        rect(s1X, s1Y + 0.5f, 1f, 4f, SackBurlapShade)
        rect(s1X + 1.5f, s1Y - 0.3f, 2f, 0.6f, SackTie)
        rect(s1X + 1.5f, s1Y - 1f, 2f, 0.7f, SackBurlap)
        // Sack 2 (slightly behind/right)
        val s2X = 22f; val s2Y = floorTop - 3.5f
        rect(s2X + 0.5f, s2Y, 4f, 4f, SackBurlap)
        rect(s2X, s2Y + 0.5f, 5f, 3.5f, SackBurlap)
        rect(s2X, s2Y + 0.5f, 1f, 3.5f, SackBurlapShade)
        rect(s2X + 1.5f, s2Y - 0.2f, 2f, 0.5f, SackTie)
        // Sack 3
        val s3X = 26f; val s3Y = floorTop - 4.5f
        rect(s3X + 0.5f, s3Y, 4f, 4.7f, SackBurlap)
        rect(s3X, s3Y + 0.5f, 5f, 4.2f, SackBurlap)
        rect(s3X, s3Y + 0.5f, 1f, 4.2f, SackBurlapShade)
        rect(s3X + 1.5f, s3Y - 0.3f, 2f, 0.6f, SackTie)
        rect(s3X + 1.5f, s3Y - 1f, 2f, 0.7f, SackBurlap)

        // Wooden crate on the right
        val crX = 38f; val crY = floorTop - 5f
        rect(crX, crY, 7f, 5f, CrateWood)
        rect(crX, crY, 7f, 0.5f, CrateShade)
        rect(crX, crY + 4.5f, 7f, 0.5f, CrateShade)
        rect(crX, crY, 0.5f, 5f, CrateShade)
        rect(crX + 6.5f, crY, 0.5f, 5f, CrateShade)
        // Metal banding around the crate
        rect(crX, crY + 1.5f, 7f, 0.4f, CrateMetal)
        rect(crX, crY + 3f, 7f, 0.4f, CrateMetal)
        // Cross plank
        rect(crX, crY + 2f, 7f, 0.3f, CrateShade)
        rect(crX + 3.4f, crY, 0.3f, 5f, CrateShade)

        // A second crate stacked next to the first
        val cr2X = 46f; val cr2Y = floorTop - 4f
        rect(cr2X, cr2Y, 6f, 4f, CrateWood)
        rect(cr2X, cr2Y, 6f, 0.5f, CrateShade)
        rect(cr2X, cr2Y + 3.5f, 6f, 0.5f, CrateShade)
        rect(cr2X, cr2Y, 0.5f, 4f, CrateShade)
        rect(cr2X + 5.5f, cr2Y, 0.5f, 4f, CrateShade)
        rect(cr2X + 2.85f, cr2Y, 0.3f, 4f, CrateShade)

        // Pitchfork leaned in the right corner
        val pfX = 58f
        rect(pfX, floorTop - 12f, 0.5f, 12f, PitchforkHandle)
        // Tines
        rect(pfX - 0.5f, floorTop - 14f, 0.4f, 2f, PitchforkSteel)
        rect(pfX + 0.5f, floorTop - 14f, 0.4f, 2f, PitchforkSteel)
        rect(pfX, floorTop - 14.5f, 0.4f, 2.5f, PitchforkSteel)
        rect(pfX - 0.5f, floorTop - 12f, 1.5f, 0.4f, PitchforkSteel)

        // Loose hay scattered on the floor
        for (i in 0 until 14) {
            val sx = (i * 4.6f) % cols
            val sy = floorTop + 0.2f + (i % 4) * 0.4f
            rect(sx, sy, 0.6f, 0.15f, Hay)
        }
    }
}
