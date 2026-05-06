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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadaeiou.charmingfarmer.data.FarmGame
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val FISHING_ENERGY_COST = 5
private const val MIN_CAST_WAIT_MS = 2_000L
private const val MAX_CAST_WAIT_MS = 6_000L
private const val BITE_WINDOW_MS = 1_500L

private val WaterColor = Color(0xFF3CA1D8)
private val WaterDeepColor = Color(0xFF1E5C82)
private val WaterEdgeColor = Color(0xFF13415E)
private val SandColor = Color(0xFFE7CB94)
private val SandEdgeColor = Color(0xFFB8975A)

private data class Fish(
    val name: String,
    val emoji: String,
    val coins: Int,
    val weight: Int,
)

private val FISH_TABLE = listOf(
    Fish("Minnow",     "🐟", 10,   3000),
    Fish("Trout",      "🐠", 30,   2500),
    Fish("Crab",       "🦀", 60,   1800),
    Fish("Shrimp",     "🦐", 90,   1200),
    Fish("Lobster",    "🦞", 200,  800),
    Fish("Pufferfish", "🐡", 350,  400),
    Fish("Squid",      "🦑", 700,  200),
    Fish("Octopus",    "🐙", 1_200, 70),
    Fish("Shark",      "🦈", 3_000, 25),
    Fish("Whale",      "🐳", 8_000, 5),
)
private val FISH_TOTAL_WEIGHT = FISH_TABLE.sumOf { it.weight }

private fun rollFish(): Fish {
    var roll = Random.nextInt(FISH_TOTAL_WEIGHT)
    for (f in FISH_TABLE) {
        if (roll < f.weight) return f
        roll -= f.weight
    }
    return FISH_TABLE.first()
}

private enum class CastPhase { IDLE, WAITING, BITING }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FishingScreen(onBack: () -> Unit, onOpenMap: () -> Unit) {
    val ctx = LocalContext.current
    val game = remember { FarmGame(ctx.applicationContext) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var phase by remember { mutableStateOf(CastPhase.IDLE) }
    var biteAtMs by remember { mutableLongStateOf(0L) }
    var biteUntilMs by remember { mutableLongStateOf(0L) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var feedbackBad by remember { mutableStateOf(false) }
    var lastCatch by remember { mutableStateOf<Fish?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            game.tick()
            val now = System.currentTimeMillis()
            nowMs = now
            when (phase) {
                CastPhase.WAITING -> if (now >= biteAtMs) {
                    phase = CastPhase.BITING
                    biteUntilMs = now + BITE_WINDOW_MS
                }
                CastPhase.BITING -> if (now > biteUntilMs) {
                    phase = CastPhase.IDLE
                    feedback = "Got away!"
                    feedbackBad = true
                }
                CastPhase.IDLE -> Unit
            }
            delay(50)
        }
    }
    DisposableEffect(Unit) { onDispose { game.save() } }

    val state = game.state

    fun cast() {
        if (phase != CastPhase.IDLE) return
        if (!game.spendEnergy(FISHING_ENERGY_COST)) {
            feedback = "Need ⚡$FISHING_ENERGY_COST"
            feedbackBad = true
            return
        }
        val now = System.currentTimeMillis()
        biteAtMs = if (com.shadaeiou.charmingfarmer.data.DebugSettings.skipTimers) {
            now
        } else {
            now + Random.nextLong(MIN_CAST_WAIT_MS, MAX_CAST_WAIT_MS)
        }
        biteUntilMs = 0L
        phase = CastPhase.WAITING
        feedback = "Line cast. Watch the water…"
        feedbackBad = false
    }

    fun reel() {
        if (phase != CastPhase.BITING) return
        val fish = rollFish()
        game.addCoins(fish.coins)
        lastCatch = fish
        feedback = "Caught a ${fish.name}! +🪙${fish.coins}"
        feedbackBad = false
        phase = CastPhase.IDLE
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🎣 Pond", fontWeight = FontWeight.Bold) },
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
            // Reuse the same stat row style as the farm so the shared
            // energy/coins pool is obviously the same one.
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
                        Text("⚡ Energy", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${state.energy.toInt()} / ${state.maxEnergy}",
                            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        LinearProgressIndicator(
                            progress = { (state.energy / state.maxEnergy).coerceIn(0f, 1f) },
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
                        Text("🪙 Coins", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${state.coins}",
                            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            text = lastCatch?.let { "Last: ${it.emoji} +🪙${it.coins}" } ?: "No catch yet",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = feedback ?: "Tap the pond to cast a line (⚡$FISHING_ENERGY_COST).",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                textAlign = TextAlign.Center,
                color = if (feedbackBad) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(Modifier.height(8.dp))

            Pond(
                phase = phase,
                biteUntilMs = biteUntilMs,
                nowMs = nowMs,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                onTap = {
                    when (phase) {
                        CastPhase.IDLE -> cast()
                        CastPhase.BITING -> reel()
                        CastPhase.WAITING -> {
                            feedback = "Too early! Wait for the bite."
                            feedbackBad = true
                        }
                    }
                },
            )

            Spacer(Modifier.height(8.dp))
            FishingHelp()
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun Pond(
    phase: CastPhase,
    biteUntilMs: Long,
    nowMs: Long,
    modifier: Modifier,
    onTap: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SandColor)
            .border(4.dp, SandEdgeColor, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(WaterColor, WaterDeepColor),
                    )
                )
                .border(3.dp, WaterEdgeColor, RoundedCornerShape(14.dp))
                .clickable { onTap() },
            contentAlignment = Alignment.Center,
        ) {
            // Idle ripples — gentle continuous animation when nothing's happening
            val transition = rememberInfiniteTransition(label = "ripple")
            val rippleScale by transition.animateFloat(
                initialValue = 0.85f, targetValue = 1.05f,
                animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
                label = "ripple-scale",
            )

            when (phase) {
                CastPhase.IDLE -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎣", fontSize = 56.sp, modifier = Modifier.scale(rippleScale))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Tap to cast",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                CastPhase.WAITING -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🪝", fontSize = 48.sp, modifier = Modifier.scale(rippleScale))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "…",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                        )
                    }
                }
                CastPhase.BITING -> {
                    val biteScale by transition.animateFloat(
                        initialValue = 1.0f, targetValue = 1.35f,
                        animationSpec = infiniteRepeatable(tween(180), RepeatMode.Reverse),
                        label = "bite-scale",
                    )
                    val remainingMs = (biteUntilMs - nowMs).coerceAtLeast(0)
                    val frac = remainingMs.toFloat() / BITE_WINDOW_MS
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("❗", fontSize = 64.sp, modifier = Modifier.scale(biteScale))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "REEL IT IN!",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(8.dp))
                        val animFrac by animateFloatAsState(frac, tween(80), label = "bite-bar")
                        Box(
                            Modifier
                                .fillMaxWidth(0.6f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x44000000)),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(animFrac)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFFFD24A)),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FishingHelp() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(
                "🐟 Catalogue",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            // Show a compact rarity row — common on the left, legendary on the right.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                FISH_TABLE.forEach { f ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(f.emoji, fontSize = 20.sp)
                        Text(
                            "🪙${shortNum(f.coins)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun shortNum(n: Int): String = when {
    n >= 1_000 -> "${n / 1_000}k"
    else -> n.toString()
}
