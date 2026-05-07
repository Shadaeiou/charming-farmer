package com.shadaeiou.charmingfarmer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseScreen(onBack: () -> Unit, onOpenMap: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🏡 Home", fontWeight = FontWeight.Bold) },
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
                .background(Color(0xFF1A0F08)),
        ) {
            LivingRoomBackground(modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "🛋️  Living Room",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "More rooms coming soon...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBB89A),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private val WallLight = Color(0xFFE8C8A0)
private val WallDark = Color(0xFFC9A57E)
private val WallTrim = Color(0xFF6E4A2A)
private val FloorLight = Color(0xFF8B5A3C)
private val FloorDark = Color(0xFF5E3A22)
private val FloorPlank = Color(0xFF3F2616)
private val SofaBody = Color(0xFF5A6E8C)
private val SofaShade = Color(0xFF3F4F66)
private val SofaCushion = Color(0xFF7C92B0)
private val TableTop = Color(0xFF6B4A2D)
private val TableLeg = Color(0xFF3D2818)
private val LampShade = Color(0xFFFFE9A8)
private val LampBase = Color(0xFF3F2818)
private val LampGlow = Color(0x66FFE9A8)
private val FrameWood = Color(0xFF3F2616)
private val FrameMat = Color(0xFFEFE3CE)
private val FramePaint = Color(0xFF3D7B5C)
private val FrameSky = Color(0xFFA8C8E0)
private val WindowFrame = Color(0xFF6E4A2A)
private val WindowSky = Color(0xFF89C0E5)
private val WindowSkyDeep = Color(0xFF5E9DC9)
private val Curtain = Color(0xFFB04A40)
private val CurtainShade = Color(0xFF7E2E26)
private val PlantPot = Color(0xFF8B4A2A)
private val PlantPotShade = Color(0xFF5E2F16)
private val PlantLeaf = Color(0xFF3D7B3F)
private val PlantLeafDark = Color(0xFF265528)
private val RugRed = Color(0xFF9C3A3F)
private val RugCream = Color(0xFFE6D2A8)
private val Skirting = Color(0xFF5A3820)

/**
 * Pixel-art living room. Drawn with a fixed virtual grid so the same
 * scene composes at any screen size. The wall takes the top ~62% of
 * the canvas, the floor the bottom ~38%; furniture sits relative to
 * those bands.
 */
@Composable
private fun LivingRoomBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Virtual grid: 64 wide × 40 tall pixel cells.
        val cols = 64f
        val rows = 40f
        val px = w / cols
        val py = h / rows
        fun rect(cx: Float, cy: Float, cw: Float, ch: Float, color: Color) {
            drawRect(color = color,
                topLeft = Offset(cx * px, cy * py),
                size = Size(cw * px, ch * py))
        }

        val floorTop = 25f

        // Wall — vertical gradient via two bands
        rect(0f, 0f, cols, floorTop * 0.55f, WallLight)
        rect(0f, floorTop * 0.55f, cols, floorTop - floorTop * 0.55f, WallDark)
        // Wall trim line just above the floor
        rect(0f, floorTop - 1f, cols, 1f, WallTrim)

        // Floor — alternating planks
        rect(0f, floorTop, cols, rows - floorTop, FloorDark)
        for (band in 0 until 4) {
            val y = floorTop + band * 4f
            rect(0f, y, cols, 2f, FloorLight)
            rect(0f, y + 2f, cols, 1f, FloorPlank)
        }
        // Skirting board between wall and floor
        rect(0f, floorTop, cols, 1f, Skirting)

        // Window — top-right corner
        val winX = 38f; val winY = 3f; val winW = 18f; val winH = 11f
        rect(winX, winY, winW, winH, WindowFrame)
        // Sky inside window (split for depth)
        rect(winX + 1f, winY + 1f, winW - 2f, (winH - 2f) * 0.55f, WindowSky)
        rect(winX + 1f, winY + 1f + (winH - 2f) * 0.55f, winW - 2f, (winH - 2f) * 0.45f, WindowSkyDeep)
        // Mullions (cross bars)
        rect(winX + winW / 2f - 0.5f, winY + 1f, 1f, winH - 2f, WindowFrame)
        rect(winX + 1f, winY + winH / 2f - 0.5f, winW - 2f, 1f, WindowFrame)
        // Curtains hanging from a rod above the window
        rect(winX - 2f, winY - 1f, winW + 4f, 1f, WallTrim)
        rect(winX - 3f, winY, 3f, winH + 2f, Curtain)
        rect(winX - 3f, winY, 1f, winH + 2f, CurtainShade)
        rect(winX + winW, winY, 3f, winH + 2f, Curtain)
        rect(winX + winW + 2f, winY, 1f, winH + 2f, CurtainShade)

        // Picture frame — top-left, landscape painting
        val frX = 6f; val frY = 4f; val frW = 16f; val frH = 10f
        rect(frX, frY, frW, frH, FrameWood)
        rect(frX + 1f, frY + 1f, frW - 2f, frH - 2f, FrameMat)
        rect(frX + 2f, frY + 2f, frW - 4f, (frH - 4f) * 0.5f, FrameSky)
        rect(frX + 2f, frY + 2f + (frH - 4f) * 0.5f, frW - 4f, (frH - 4f) * 0.5f, FramePaint)
        // A tiny sun in the painting
        rect(frX + frW - 4f, frY + 3f, 1f, 1f, LampShade)
        // Hooks above the frame
        rect(frX + frW / 2f - 0.5f, frY - 1f, 1f, 1f, FrameWood)

        // Rug centered on the floor
        val rugX = 14f; val rugY = floorTop + 5f; val rugW = 36f; val rugH = 9f
        rect(rugX, rugY, rugW, rugH, RugRed)
        rect(rugX + 1f, rugY + 1f, rugW - 2f, rugH - 2f, RugCream)
        rect(rugX + 3f, rugY + 3f, rugW - 6f, rugH - 6f, RugRed)
        // Rug fringe (top + bottom)
        for (i in 0 until rugW.toInt() step 2) {
            rect(rugX + i.toFloat(), rugY - 0.5f, 1f, 0.5f, RugCream)
            rect(rugX + i.toFloat(), rugY + rugH, 1f, 0.5f, RugCream)
        }

        // Sofa — sitting on the rug, centered
        val sfX = 18f; val sfY = floorTop + 1f; val sfW = 22f; val sfH = 8f
        // Back of sofa
        rect(sfX, sfY, sfW, 4f, SofaBody)
        rect(sfX, sfY, sfW, 1f, SofaShade)
        // Seat base
        rect(sfX, sfY + 4f, sfW, 4f, SofaShade)
        // Cushions on the seat
        rect(sfX + 1f, sfY + 4f, (sfW - 3f) / 2f, 3f, SofaCushion)
        rect(sfX + 2f + (sfW - 3f) / 2f, sfY + 4f, (sfW - 3f) / 2f, 3f, SofaCushion)
        // Armrests
        rect(sfX - 1f, sfY + 2f, 2f, 6f, SofaShade)
        rect(sfX + sfW - 1f, sfY + 2f, 2f, 6f, SofaShade)
        // Throw pillows on top of cushions
        rect(sfX + 2f, sfY + 4f, 3f, 2f, RugCream)
        rect(sfX + sfW - 5f, sfY + 4f, 3f, 2f, Curtain)

        // Coffee table in front of sofa
        val tbX = 24f; val tbY = floorTop + 11f; val tbW = 12f; val tbH = 2f
        rect(tbX, tbY, tbW, tbH, TableTop)
        rect(tbX, tbY + tbH, 1f, 2f, TableLeg)
        rect(tbX + tbW - 1f, tbY + tbH, 1f, 2f, TableLeg)

        // Floor lamp — right side, glow on wall behind
        val lpX = 50f; val lpY = 10f
        rect(lpX - 3f, lpY - 2f, 9f, 7f, LampGlow)
        rect(lpX, lpY, 3f, 4f, LampShade)
        rect(lpX + 1f, lpY + 4f, 1f, 11f, LampBase)
        rect(lpX - 1f, lpY + 15f, 5f, 1f, LampBase)

        // Potted plant — left of sofa
        val plX = 8f; val plY = floorTop + 5f
        // Pot
        rect(plX, plY + 4f, 5f, 4f, PlantPot)
        rect(plX, plY + 4f, 1f, 4f, PlantPotShade)
        rect(plX, plY + 7f, 5f, 1f, PlantPotShade)
        // Leaves
        rect(plX + 1f, plY, 3f, 5f, PlantLeaf)
        rect(plX, plY + 1f, 5f, 3f, PlantLeaf)
        rect(plX + 2f, plY + 1f, 1f, 1f, PlantLeafDark)
        rect(plX + 1f, plY + 2f, 1f, 1f, PlantLeafDark)
        rect(plX + 3f, plY + 3f, 1f, 1f, PlantLeafDark)
    }
}
