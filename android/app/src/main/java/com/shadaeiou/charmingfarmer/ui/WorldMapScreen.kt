package com.shadaeiou.charmingfarmer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadaeiou.charmingfarmer.data.Biome
import com.shadaeiou.charmingfarmer.data.FarmGame
import com.shadaeiou.charmingfarmer.data.LandService
import com.shadaeiou.charmingfarmer.data.LandTile
import com.shadaeiou.charmingfarmer.data.Location
import com.shadaeiou.charmingfarmer.data.StructureType
import com.shadaeiou.charmingfarmer.data.TransportService
import com.shadaeiou.charmingfarmer.data.biomeAt
import kotlinx.coroutines.delay

/**
 * Pixel-style world map. Replaces the old atlas. Renders a fixed 7x7
 * viewport centered on the House at (0, 0) for now — pan/zoom comes
 * in the next commit, infinite-world tile generation in the one after.
 *
 * Tap an owned tile with a structure → navigate to that screen.
 * Tap an owned-but-empty tile → build menu (placeholder feedback for
 * now; commit 5 wires the build flow).
 * Tap an unowned tile → buy dialog (placeholder for now; commit 4).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldMapScreen(
    onNavigate: (route: String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val ctx = LocalContext.current
    val land = remember { LandService.get(ctx.applicationContext) }
    val game = remember { FarmGame(ctx.applicationContext) }
    val transport = remember { TransportService.get(ctx.applicationContext) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var transportOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            game.tick()
            land.tick(System.currentTimeMillis())
            transport.tick(System.currentTimeMillis())
            nowMs = System.currentTimeMillis()
            delay(500)
        }
    }
    DisposableEffect(Unit) { onDispose { game.save() } }

    if (transportOpen) {
        // The panel itself owns the source + destination pickers so the
        // dialog looks identical whether opened from the world map, a
        // building screen, etc.
        TransportPanel(
            transport = transport,
            onDismiss = { transportOpen = false },
        )
    }

    // Subscribe to land service revisions for recomposition when tiles change.
    @Suppress("UNUSED_EXPRESSION") land.revisionTick

    val state = game.state

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🗺️  Your Land", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { transportOpen = true }) {
                        Icon(Icons.Filled.LocalShipping, contentDescription = "Transport")
                    }
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
        containerColor = SkyColor,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(8.dp),
        ) {
            EnergyCoinsBar(state.energy.toInt(), state.maxEnergy, state.coins)
            Spacer(Modifier.height(8.dp))

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                // Fit a 7x7 grid into the smaller of width/height.
                val tileSize: Dp = minOf(maxWidth, maxHeight) / VIEWPORT_TILES.toFloat()
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    // Y axis: positive Y is "north" — render top-down so the
                    // world matches the player's mental model (look up to see
                    // farther north).
                    for (yIndex in (VIEWPORT_TILES / 2) downTo -(VIEWPORT_TILES / 2)) {
                        Row {
                            for (xIndex in -(VIEWPORT_TILES / 2)..(VIEWPORT_TILES / 2)) {
                                WorldTileView(
                                    x = xIndex,
                                    y = yIndex,
                                    tile = land.tileAt(xIndex, yIndex),
                                    nowMs = nowMs,
                                    size = tileSize,
                                    onTap = { handleTap(land.tileAt(xIndex, yIndex), xIndex, yIndex) {
                                        msg, route -> if (route != null) onNavigate(route) else feedback = msg
                                    } },
                                )
                            }
                        }
                    }
                }
            }

            feedback?.let { msg ->
                Text(
                    msg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, start = 4.dp, end = 4.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private const val VIEWPORT_TILES = 7

private val SkyColor = Color(0xFF1F2A36)

@Composable
private fun EnergyCoinsBar(energy: Int, maxEnergy: Int, coins: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "⚡ $energy / $maxEnergy",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
        Box(
            modifier = Modifier
                .height(6.dp)
                .weight(1f)
                .clip(RoundedCornerShape(3.dp)),
        ) {
            LinearProgressIndicator(
                progress = { (energy.toFloat() / maxEnergy.coerceAtLeast(1)).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
        Text(
            "🪙 $coins",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun handleTap(
    tile: LandTile?,
    x: Int,
    y: Int,
    callback: (msg: String?, route: String?) -> Unit,
) {
    when {
        tile == null -> callback("Locked land at ($x, $y) — buy flow lands in the next update.", null)
        tile.isBuilding -> callback("Construction in progress at ($x, $y).", null)
        tile.structure != null -> callback(null, tile.structure.routeId)
        else -> callback("Empty plot at ($x, $y) — build menu lands in the next update.", null)
    }
}

@Composable
private fun WorldTileView(
    x: Int,
    y: Int,
    tile: LandTile?,
    nowMs: Long,
    size: Dp,
    onTap: () -> Unit,
) {
    // Owned-and-empty tiles render as cleared dirt; owned-with-structure
    // also use dirt as the underlay; unowned use the wild biome.
    val biome: Biome = when {
        tile == null -> biomeAt(x, y)
        tile.structure == null -> Biome.DIRT
        else -> Biome.DIRT
    }
    val borderColor = when {
        tile == null -> Color.Transparent
        tile.isBuilding -> ConstructionAccent
        tile.structure != null -> OwnedAccent
        else -> Color(0xFFCAA468)
    }

    Box(
        modifier = Modifier
            .size(size)
            .clickable { onTap() },
    ) {
        PixelTerrain(biome = biome, x = x, y = y, modifier = Modifier.fillMaxSize())

        if (tile != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(width = 2.dp, color = borderColor),
            )
        }

        // Building icon centered. BARN and MARKET get hand-drawn pixel-art
        // icons sized to match the emoji glyphs used by every other tile;
        // anything else falls back to the structure's emoji at 0.55× size.
        tile?.structure?.let { struct ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                when (struct) {
                    StructureType.BARN -> BarnMapIcon(modifier = Modifier.size(size * 0.6f))
                    StructureType.MARKET -> MarketMapIcon(modifier = Modifier.size(size * 0.6f))
                    else -> Text(
                        text = struct.emoji,
                        fontSize = (size.value * 0.55f).sp,
                    )
                }
            }
            if (tile.isBuilding) {
                Text(
                    "🔨",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp),
                    fontSize = 12.sp,
                )
            }
        }

        // Unowned land gets a tiny lock so the visual difference is unmistakable.
        if (tile == null) {
            Text(
                "🔒",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 2.dp, bottom = 2.dp),
                fontSize = 10.sp,
            )
        }
    }
}

private val OwnedAccent = Color(0xFFE5BC5B)
private val ConstructionAccent = Color(0xFFE57F3D)

/**
 * Canvas-drawn pixel-style terrain. 8×8 grid of "pixels" per tile,
 * deterministic per-coordinate noise so the same coord always renders
 * the same texture. Naive — real artistry comes from sprite sheets
 * later; the rendering call site is this one Composable so swapping
 * is a one-line change.
 */
@Composable
private fun PixelTerrain(biome: Biome, x: Int, y: Int, modifier: Modifier) {
    val palette = paletteFor(biome)
    Canvas(modifier = modifier) {
        val grid = 8
        val pixelSize = size.width / grid
        for (py in 0 until grid) {
            for (px in 0 until grid) {
                val seed = ((x.toLong() * 73856093L) xor
                    (y.toLong() * 19349663L) xor
                    (px.toLong() * 83492791L) xor
                    (py.toLong() * 12379561L)) and 0x7fffffffL
                val idx = (seed % palette.size).toInt()
                drawRect(
                    color = palette[idx],
                    topLeft = Offset(px * pixelSize, py * pixelSize),
                    size = Size(pixelSize, pixelSize),
                )
            }
        }
    }
}

/**
 * Pixel-art top-down icon of a red barn with a grey silo. Drawn on a
 * 16×16 virtual grid so it looks crisp at every tile size. Internal so
 * the barn screen can reuse it as a header glyph.
 */
@Composable
internal fun BarnMapIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cols = 16f
        val rows = 16f
        val px = size.width / cols
        val py = size.height / rows
        fun r(cx: Float, cy: Float, cw: Float, ch: Float, color: Color) {
            drawRect(color = color, topLeft = Offset(cx * px, cy * py),
                size = Size(cw * px, ch * py))
        }

        val barnRed      = Color(0xFFB22222)
        val barnRedDark  = Color(0xFF8B0000)
        val roofRed      = Color(0xFF7A1010)
        val barnWhite    = Color(0xFFF5F0E8)
        val siloGrey     = Color(0xFF9E9E9E)
        val siloGreyDark = Color(0xFF757575)
        val siloDome     = Color(0xFFBDBDBD)
        val ground       = Color(0xFF5C8A34)
        val groundShadow = Color(0xFF3E6225)
        val doorBrown    = Color(0xFF5C3317)
        val windowYellow = Color(0xFFFFE082)

        // Ground strip at the base
        r(0f, 13.5f, 16f, 2.5f, ground)
        r(0f, 14.5f, 16f, 1.5f, groundShadow)

        // ── Silo (right side) ──────────────────────────────────────
        // Body
        r(11f, 4f, 3.5f, 10f, siloGrey)
        r(11f, 4f, 0.6f, 10f, siloGreyDark)   // left shadow stripe
        r(14f, 4f, 0.5f, 10f, siloGreyDark)   // right shadow stripe
        // Horizontal band rings
        r(11f, 7f, 3.5f, 0.4f, siloGreyDark)
        r(11f, 10f, 3.5f, 0.4f, siloGreyDark)
        // Dome cap
        r(11.3f, 2.5f, 2.9f, 1.8f, siloDome)
        r(11.6f, 1.8f, 2.3f, 0.9f, siloDome)
        r(12f, 1.2f, 1.5f, 0.8f, siloDome)

        // ── Barn body ──────────────────────────────────────────────
        r(1f, 6f, 10f, 8f, barnRed)
        // Right edge shadow
        r(10f, 6f, 1f, 8f, barnRedDark)
        // Left edge highlight
        r(1f, 6f, 0.7f, 8f, Color(0xFFCC2222))

        // ── Roof (triangle simulated with layered rects) ───────────
        r(0f, 3.5f, 12f, 1f, roofRed)
        r(0.5f, 2.5f, 11f, 1.2f, roofRed)
        r(1.2f, 1.5f, 9.5f, 1.2f, roofRed)
        r(2.2f, 0.5f, 7.5f, 1.2f, roofRed)
        r(3.5f, -0.2f, 5f, 1f, roofRed)
        // Ridge cap (peak)
        r(4.8f, -0.3f, 2.3f, 0.6f, barnRedDark)

        // ── Barn door (double doors, centered) ────────────────────
        r(3.5f, 9f, 4f, 5f, doorBrown)
        // Door gap (vertical split)
        r(5.3f, 9f, 0.3f, 5f, barnRedDark)
        // Door arch top
        r(3.5f, 8.3f, 4f, 0.8f, doorBrown)
        r(4f, 7.9f, 3f, 0.6f, doorBrown)
        // Horizontal bar across door
        r(3.5f, 10.5f, 4f, 0.3f, barnRedDark)

        // ── Small loft window above door ──────────────────────────
        r(4.8f, 6.3f, 2.4f, 1.8f, windowYellow)
        r(5.8f, 6.3f, 0.3f, 1.8f, barnRedDark)
        r(4.8f, 7.1f, 2.4f, 0.3f, barnRedDark)

        // White trim strips (fascia)
        r(1f, 5.8f, 10f, 0.4f, barnWhite)
        r(1f, 6f, 0.4f, 8f, barnWhite)
    }
}

/**
 * Pixel-art top-down icon of a market stall — red-and-white striped
 * awning over a wooden counter with produce on display. Matches the
 * pattern used in [MarketScreen]'s background art so the icon and the
 * destination feel like the same place.
 */
@Composable
internal fun MarketMapIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cols = 16f
        val rows = 16f
        val px = size.width / cols
        val py = size.height / rows
        fun r(cx: Float, cy: Float, cw: Float, ch: Float, color: Color) {
            drawRect(color = color, topLeft = Offset(cx * px, cy * py),
                size = Size(cw * px, ch * py))
        }

        val awningRed   = Color(0xFFC62828)
        val awningWhite = Color(0xFFF5F5F5)
        val awningDark  = Color(0xFF8B1A1A)
        val woodPost    = Color(0xFF795548)
        val woodPostDark= Color(0xFF4E342E)
        val counterWood = Color(0xFF8D6E63)
        val counterDark = Color(0xFF5D4037)
        val ground      = Color(0xFF5C8A34)
        val groundShade = Color(0xFF3E6225)
        val produceRed  = Color(0xFFE53935)
        val produceGold = Color(0xFFFFB300)
        val produceGrn  = Color(0xFF43A047)

        // Ground strip
        r(0f, 14f, 16f, 2f, ground)
        r(0f, 15f, 16f, 1f, groundShade)

        // ── Wooden support posts ─────────────────────────────────────
        r(1.5f, 4f, 1.2f, 10f, woodPost)
        r(1.5f, 4f, 0.4f, 10f, woodPostDark)
        r(13.3f, 4f, 1.2f, 10f, woodPost)
        r(13.3f, 4f, 0.4f, 10f, woodPostDark)

        // ── Striped awning ────────────────────────────────────────────
        // Base red layer
        r(1f, 2f, 14f, 3.5f, awningRed)
        // White stripes (3 of them)
        r(3f, 2f, 2.5f, 3.5f, awningWhite)
        r(7f, 2f, 2.5f, 3.5f, awningWhite)
        r(11f, 2f, 2.5f, 3.5f, awningWhite)
        // Awning peak (curve simulated with stepped rects)
        r(2f, 1.2f, 12f, 0.9f, awningRed)
        r(4f, 0.5f, 8f, 0.9f, awningRed)
        r(6f, 0f, 4f, 0.7f, awningDark)
        // Bottom scalloped fringe (alternating rects)
        for (i in 0 until 7) {
            val fx = 1f + i * 2f
            val fc = if (i % 2 == 0) awningRed else awningWhite
            r(fx, 5.5f, 1.8f, 1f, fc)
        }
        r(1f, 6.3f, 14f, 0.5f, awningDark)

        // ── Counter ───────────────────────────────────────────────────
        r(2f, 9.5f, 12f, 4f, counterWood)
        r(2f, 9.5f, 12f, 0.5f, counterDark)
        r(2f, 13.2f, 12f, 0.6f, counterDark)
        // Counter face vertical seams
        for (s in 1 until 4) {
            r(2f + s * 3f, 9.5f, 0.3f, 4f, counterDark)
        }

        // ── Produce on counter ────────────────────────────────────────
        // Red apples
        r(3f, 8f, 1.4f, 1.4f, produceRed)
        r(4.6f, 8f, 1.4f, 1.4f, produceRed)
        r(3.4f, 7.5f, 0.4f, 0.6f, woodPostDark)
        r(5f, 7.5f, 0.4f, 0.6f, woodPostDark)
        // Golden grain mounds
        r(7f, 8f, 1.4f, 1.4f, produceGold)
        r(8.6f, 8f, 1.4f, 1.4f, produceGold)
        // Green cabbages
        r(10.4f, 8f, 1.4f, 1.4f, produceGrn)
        r(12f, 8f, 1.4f, 1.4f, produceGrn)
    }
}

private fun paletteFor(biome: Biome): List<Color> = when (biome) {
    Biome.GRASS -> listOf(
        Color(0xFF7BAE4D), Color(0xFF8FCB5C), Color(0xFF6A9D40), Color(0xFF5E8A3D),
    )
    Biome.DIRT -> listOf(
        Color(0xFF8B6845), Color(0xFFA17D52), Color(0xFF785738), Color(0xFF6E4F2F),
    )
    Biome.WATER -> listOf(
        Color(0xFF4A90C2), Color(0xFF5FA8D9), Color(0xFF3A78A6), Color(0xFF2D6688),
    )
    Biome.HILL -> listOf(
        Color(0xFF8B8275), Color(0xFFA8A092), Color(0xFF6F685C), Color(0xFF55504A),
    )
    Biome.FOREST -> listOf(
        Color(0xFF3F6B33), Color(0xFF548A40), Color(0xFF2A4A21), Color(0xFF22381B),
    )
}
