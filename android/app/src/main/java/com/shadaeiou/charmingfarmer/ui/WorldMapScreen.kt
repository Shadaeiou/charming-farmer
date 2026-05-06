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
import com.shadaeiou.charmingfarmer.data.StructureType
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
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var feedback by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            game.tick()
            land.tick(System.currentTimeMillis())
            nowMs = System.currentTimeMillis()
            delay(500)
        }
    }
    DisposableEffect(Unit) { onDispose { game.save() } }

    // Subscribe to land service revisions for recomposition when tiles change.
    @Suppress("UNUSED_EXPRESSION") land.revisionTick

    val state = game.state

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🗺️  Your Land", fontWeight = FontWeight.Bold) },
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

        // Building emoji centered. During construction, show the structure
        // emoji at half opacity with a hammer overlay so the player can
        // tell what's being built.
        tile?.structure?.let { struct ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = struct.emoji,
                    fontSize = (size.value * 0.55f).sp,
                )
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
