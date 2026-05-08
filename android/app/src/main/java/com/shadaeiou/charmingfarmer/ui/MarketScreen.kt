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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.shadaeiou.charmingfarmer.data.CropType
import com.shadaeiou.charmingfarmer.data.FarmGame
import com.shadaeiou.charmingfarmer.data.Inventory
import com.shadaeiou.charmingfarmer.data.ItemGrade
import com.shadaeiou.charmingfarmer.data.ItemStack
import com.shadaeiou.charmingfarmer.data.ItemType
import com.shadaeiou.charmingfarmer.data.KitchenRecipe
import com.shadaeiou.charmingfarmer.data.Location
import com.shadaeiou.charmingfarmer.data.TransportService
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(onBack: () -> Unit, onOpenMap: () -> Unit) {
    val ctx = LocalContext.current
    val game = remember { FarmGame(ctx.applicationContext) }
    val transport = remember { TransportService.get(ctx.applicationContext) }
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

    val state = game.state
    val marketInv = transport.inventoryAt(Location.MARKET)
    val grouped = marketInv.stacks.groupBy { it.type }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🏪  Market", fontWeight = FontWeight.Bold) },
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
                .background(Color(0xFF0E1A0A)),
        ) {
            MarketBackground(modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                EnergyAndCoinsRow(state.energy.toInt(), state.maxEnergy, state.coins)
                Spacer(Modifier.height(8.dp))

                val totalItems = marketInv.stacks.sumOf { it.quantity }
                Text(
                    text = if (totalItems == 0)
                        "Nothing for sale yet. Ship goods here from the Barn."
                    else
                        "$totalItems items ready to sell — tap to cash out.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xCC0A1500))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    textAlign = TextAlign.Center,
                    color = Color(0xFFD4E8C2),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))

                if (grouped.isEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Ship items here from the Barn using the 🚚 truck.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFAAC89A),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    // "Sell all" button when there are items
                    val totalPayout = marketInv.stacks.sumOf {
                        it.unitSellPrice(basePriceFor(it.type)).toLong() * it.quantity
                    }.toInt()
                    Button(
                        onClick = {
                            transport.setInventory(Location.MARKET, Inventory())
                            game.addCoins(totalPayout)
                            game.save()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32),
                        ),
                    ) {
                        Text(
                            "Sell Everything  🪙 $totalPayout",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                    }

                    grouped.forEach { (type, stacks) ->
                        MarketItemCard(
                            type = type,
                            stacks = stacks,
                            onSell = { sellStacks ->
                                val payout = sellStacks.sumOf {
                                    it.unitSellPrice(basePriceFor(it.type)).toLong() * it.quantity
                                }.toInt()
                                val typesToRemove = sellStacks.map { it.type }.toSet()
                                val remaining = marketInv.stacks.filter { it.type !in typesToRemove }
                                transport.setInventory(Location.MARKET, Inventory(remaining))
                                game.addCoins(payout)
                                game.save()
                            },
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "Prices scale quadratically with quality — a Grade S item sells for much more than a Grade C.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF88A878),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun MarketItemCard(
    type: ItemType,
    stacks: List<ItemStack>,
    onSell: (List<ItemStack>) -> Unit,
) {
    val totalQty = stacks.sumOf { it.quantity }
    val weighted = stacks.sumOf { it.score.toLong() * it.quantity }
    val avg = (weighted / totalQty.coerceAtLeast(1)).toInt()
    val grade = ItemGrade.fromScore(avg)
    val base = basePriceFor(type)
    val unitPrice = stacks.sumOf { it.unitSellPrice(base).toLong() * it.quantity }.toInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.93f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(type.emoji, fontSize = 22.sp)
            Column(Modifier.weight(1f)) {
                Text(
                    "${type.displayName} × $totalQty",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Avg grade ${grade.display}  ·  base 🪙$base",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = { onSell(stacks) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    "Sell\n🪙$unitPrice",
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/**
 * Base sale price for each item type. Dishes come from [KitchenRecipe];
 * beers match [Brewery.basePriceFor]; malts and raw grains use fixed
 * reference prices that put them in the right relative ballpark.
 */
private fun basePriceFor(type: ItemType): Int {
    // Dishes (artisan goods from the kitchen)
    KitchenRecipe.values().firstOrNull { it.outputType == type }?.let { return it.basePrice }
    // Raw farm crops shipped from the silo. The CROP_X ItemType name
    // maps directly to CropType.X by suffix; we charge ~3× the bare
    // sellPrice as the basePrice so a Grade B crop sold via the market
    // roughly matches direct-harvest revenue and Grade S clears more.
    if (type.name.startsWith("CROP_")) {
        val cropName = type.name.removePrefix("CROP_")
        val crop = runCatching { CropType.valueOf(cropName) }.getOrNull()
        if (crop != null) return (crop.sellPrice * 3).coerceAtLeast(10)
    }
    // Beers
    return when (type) {
        ItemType.BEER_PALE_ALE   -> 220
        ItemType.BEER_IPA        -> 320
        ItemType.BEER_STOUT      -> 280
        ItemType.BEER_HEFEWEIZEN -> 260
        // Malts
        ItemType.MALT_PALE       -> 80
        ItemType.MALT_MUNICH     -> 100
        ItemType.MALT_CRYSTAL    -> 130
        ItemType.MALT_CHOCOLATE  -> 160
        ItemType.MALT_BLACK      -> 190
        // Yeasts
        ItemType.YEAST_ENGLISH_ALE   -> 50
        ItemType.YEAST_AMERICAN_ALE  -> 50
        ItemType.YEAST_GERMAN_LAGER  -> 50
        ItemType.YEAST_GERMAN_WHEAT  -> 50
        // Raw hops
        ItemType.HOPS           -> 30
        ItemType.HOPS_CASCADE   -> 45
        ItemType.HOPS_SAAZ      -> 45
        ItemType.HOPS_FUGGLE    -> 40
        ItemType.HOPS_CITRA     -> 50
        // Raw grain
        ItemType.BARLEY         -> 20
        ItemType.WHEAT_GRAIN    -> 18
        ItemType.OATS           -> 16
        ItemType.RYE            -> 22
        else                    -> 10
    }
}

// ── Pixel-art market background ──────────────────────────────────────────

private val StoneLight  = Color(0xFF8E8E7A)
private val StoneMid    = Color(0xFF6E6E5A)
private val StoneDark   = Color(0xFF4A4A3A)
private val StoneGrout  = Color(0xFF3A3A2A)
private val AwningRed   = Color(0xFFC62828)
private val AwningWhite = Color(0xFFF5F5F5)
private val AwningStripe= Color(0xFFB71C1C)
private val WoodBeam    = Color(0xFF795548)
private val WoodBeamDark= Color(0xFF4E342E)
private val CounterWood = Color(0xFF8D6E63)
private val CounterShade= Color(0xFF5D4037)
private val SignYellow  = Color(0xFFFFF176)
private val SignBrown   = Color(0xFF5D4037)
private val CoinGold    = Color(0xFFFFD700)
private val ProdRed     = Color(0xFFE53935)
private val ProdGreen   = Color(0xFF43A047)
private val ProdYellow  = Color(0xFFFFB300)
private val SkyBlue     = Color(0xFF1A3D1A)
private val GrassGreen  = Color(0xFF2E5A1C)
private val PathGrey    = Color(0xFF5C5C48)

@Composable
private fun MarketBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cols = 64f
        val rows = 40f
        val px = w / cols
        val py = h / rows
        fun rect(cx: Float, cy: Float, cw: Float, ch: Float, color: Color) {
            drawRect(color = color, topLeft = Offset(cx * px, cy * py),
                size = Size(cw * px, ch * py))
        }

        val floorY = 28f

        // Sky / backdrop (dark green foliage suggestion)
        rect(0f, 0f, cols, floorY, SkyBlue)
        // Grass patches behind stall
        rect(0f, floorY - 3f, 15f, 3f, GrassGreen)
        rect(49f, floorY - 3f, 15f, 3f, GrassGreen)

        // Stone floor — rows of cobbles
        rect(0f, floorY, cols, rows - floorY, StoneMid)
        // Horizontal grout lines
        for (row in 0 until 4) {
            val gy = floorY + row * 3f
            rect(0f, gy, cols, 0.3f, StoneGrout)
        }
        // Vertical grout lines (staggered)
        for (col in 0 until 11) {
            val gx = col * 6f
            rect(gx, floorY, 0.3f, 3f, StoneGrout)
            rect(gx + 3f, floorY + 3f, 0.3f, 3f, StoneGrout)
        }
        // Stone colour variation
        for (row in 0 until 4) {
            for (col in 0 until 11) {
                val sx = col * 6f + (if (row % 2 == 0) 0f else 3f)
                val sy = floorY + row * 3f
                if ((col + row) % 3 == 0) rect(sx, sy, 5.7f, 2.7f, StoneLight)
                else if ((col + row) % 3 == 1) rect(sx, sy, 5.7f, 2.7f, StoneDark)
            }
        }
        // Centre path strip
        rect(20f, floorY, 24f, rows - floorY, PathGrey)
        rect(20f, floorY, 24f, 0.3f, StoneGrout)

        // ── Wooden support beams ─────────────────────────────────
        // Left post
        rect(4f, 8f, 1.5f, floorY - 8f, WoodBeam)
        rect(4f, 8f, 0.4f, floorY - 8f, WoodBeamDark)
        // Right post
        rect(58f, 8f, 1.5f, floorY - 8f, WoodBeam)
        rect(58f, 8f, 0.4f, floorY - 8f, WoodBeamDark)
        // Horizontal beam across the top
        rect(3.5f, 7.5f, 57f, 1.8f, WoodBeam)
        rect(3.5f, 7.5f, 57f, 0.5f, WoodBeamDark)

        // ── Striped awning ────────────────────────────────────────
        // Base awning fill
        rect(4f, 8f, 56f, 5f, AwningRed)
        // White stripes
        for (i in 0 until 8) {
            val sx = 4f + i * 7f
            rect(sx, 8f, 3f, 5f, AwningWhite)
        }
        // Bottom fringe (zig-zag effect with rects)
        for (i in 0 until 28) {
            val fx = 4f + i * 2f
            val fh = if (i % 2 == 0) 1.5f else 0.8f
            val fc = if (i % 4 < 2) AwningRed else AwningWhite
            rect(fx, 13f, 1.8f, fh, fc)
        }
        // Awning shadow on back wall
        rect(4f, 13f, 56f, 1.5f, Color(0x44000000))

        // ── Market counter ────────────────────────────────────────
        rect(6f, floorY - 6f, 52f, 5.5f, CounterWood)
        rect(6f, floorY - 6f, 52f, 0.5f, Color(0xFF4E342E)) // top edge shade
        rect(6f, floorY - 0.5f, 52f, 0.5f, CounterShade)     // bottom edge
        // Counter front face
        rect(6f, floorY - 0.5f, 52f, 1f, CounterShade)
        // Divider lines on counter
        for (d in 1 until 5) {
            val dx = 6f + d * 10.4f
            rect(dx, floorY - 6f, 0.4f, 5.5f, CounterShade)
        }

        // ── Produce on the counter ────────────────────────────────
        // Red apples
        for (i in 0 until 3) {
            rect(8f + i * 1.8f, floorY - 5f, 1.5f, 1.5f, ProdRed)
            rect(8.6f + i * 1.8f, floorY - 5.6f, 0.3f, 0.6f, WoodBeamDark)
        }
        // Green cabbages
        for (i in 0 until 3) {
            rect(19f + i * 1.8f, floorY - 5f, 1.5f, 1.5f, ProdGreen)
        }
        // Golden grain sacks (small mounds)
        for (i in 0 until 4) {
            rect(29.5f + i * 2f, floorY - 5f, 1.5f, 1.5f, ProdYellow)
            rect(30f + i * 2f, floorY - 5.4f, 0.5f, 0.5f, ProdYellow)
        }
        // Beer bottles (right section)
        for (i in 0 until 4) {
            val bx = 42f + i * 2.5f
            rect(bx + 0.5f, floorY - 5.8f, 0.7f, 0.8f, Color(0xFF5D4037))
            rect(bx, floorY - 5f, 1.7f, 1.5f, Color(0xFF6D9A3F))
        }

        // ── Price sign above centre ───────────────────────────────
        val sx = 25f; val sy = 2.5f
        rect(sx, sy, 14f, 5f, SignBrown)
        rect(sx + 0.5f, sy + 0.5f, 13f, 4f, SignYellow)
        // Sign text stand (pole)
        rect(31.5f, sy + 5f, 1f, 3f, SignBrown)
        // Coin icon on sign
        for (row in 0 until 3) {
            for (col in 0 until 3) {
                if ((row + col) % 2 == 0) {
                    rect(sx + 1.5f + col * 1.5f, sy + 1f + row * 1.2f, 1.2f, 1f, CoinGold)
                }
            }
        }
    }
}
