package com.shadaeiou.charmingfarmer.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadaeiou.charmingfarmer.data.FarmGame
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

private const val SPOT_ENERGY_COST = 1
private const val MIN_BIRD_DURATION_MS = 4_000L
private const val MAX_BIRD_DURATION_MS = 7_000L
private const val MIN_SPAWN_INTERVAL_MS = 1_400L
private const val MAX_SPAWN_INTERVAL_MS = 3_000L
private const val MAX_BIRDS_ON_SCREEN = 3

private val SkyTopColor = Color(0xFF8FCFFF)
private val SkyBottomColor = Color(0xFFCFE9FF)
private val SkyEdgeColor = Color(0xFF1F5B85)
private val MeadowColor = Color(0xFF7CC36B)

private data class BirdSpecies(
    val key: String,
    val displayName: String,
    val emoji: String,
    val coins: Int,
    val weight: Int,
)

private val BIRD_TABLE = listOf(
    BirdSpecies("SPARROW",  "Sparrow",  "🐦",   5,    4000),
    BirdSpecies("PIGEON",   "Pigeon",   "🕊️",  8,    2500),
    BirdSpecies("DUCK",     "Duck",     "🦆",   25,   1500),
    BirdSpecies("GOOSE",    "Goose",    "🦢",   50,   800),
    BirdSpecies("OWL",      "Owl",      "🦉",   100,  600),
    BirdSpecies("PARROT",   "Parrot",   "🦜",   200,  400),
    BirdSpecies("EAGLE",    "Eagle",    "🦅",   400,  100),
    BirdSpecies("FLAMINGO", "Flamingo", "🦩",   900,  50),
    BirdSpecies("PEACOCK",  "Peacock",  "🦚",   2_500, 50),
)
private val BIRD_TOTAL_WEIGHT = BIRD_TABLE.sumOf { it.weight }

private fun rollBird(): BirdSpecies {
    var roll = Random.nextInt(BIRD_TOTAL_WEIGHT)
    for (b in BIRD_TABLE) {
        if (roll < b.weight) return b
        roll -= b.weight
    }
    return BIRD_TABLE.first()
}

private data class FlyingBird(
    val id: Long,
    val species: BirdSpecies,
    val startMs: Long,
    val durationMs: Long,
    val yPercent: Float,
    val leftToRight: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirdwatchingScreen(onBack: () -> Unit, onOpenMap: () -> Unit) {
    val ctx = LocalContext.current
    val game = remember { FarmGame(ctx.applicationContext) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val birds = remember { mutableStateListOf<FlyingBird>() }
    var nextSpawnMs by remember { mutableLongStateOf(0L) }
    var nextBirdId by remember { mutableLongStateOf(1L) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var feedbackBad by remember { mutableStateOf(false) }
    var lastSpot by remember { mutableStateOf<BirdSpecies?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            game.tick()
            val now = System.currentTimeMillis()
            nowMs = now
            // Despawn birds whose flight has ended.
            birds.removeAll { now - it.startMs >= it.durationMs }
            // Spawn a new bird if we're due and there's room.
            if (now >= nextSpawnMs && birds.size < MAX_BIRDS_ON_SCREEN) {
                birds.add(
                    FlyingBird(
                        id = nextBirdId++,
                        species = rollBird(),
                        startMs = now,
                        durationMs = Random.nextLong(MIN_BIRD_DURATION_MS, MAX_BIRD_DURATION_MS),
                        yPercent = Random.nextFloat() * 0.6f + 0.10f,
                        leftToRight = Random.nextBoolean(),
                    )
                )
                nextSpawnMs = now + Random.nextLong(MIN_SPAWN_INTERVAL_MS, MAX_SPAWN_INTERVAL_MS)
            }
            delay(50)
        }
    }
    DisposableEffect(Unit) { onDispose { game.save() } }

    val state = game.state

    fun spot(bird: FlyingBird) {
        if (!birds.remove(bird)) return  // already gone
        if (!game.spendEnergy(SPOT_ENERGY_COST)) {
            // Refund the bird so the player sees it fly off naturally.
            birds.add(bird)
            feedback = "Need ⚡$SPOT_ENERGY_COST"
            feedbackBad = true
            return
        }
        game.addCoins(bird.species.coins)
        game.recordBird(bird.species.key)
        lastSpot = bird.species
        feedback = "Spotted ${bird.species.displayName}! +🪙${bird.species.coins}"
        feedbackBad = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🦜 Birdwatching", fontWeight = FontWeight.Bold) },
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
                            text = lastSpot?.let { "Last: ${it.emoji} +🪙${it.coins}" } ?: "No spots yet",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = feedback ?: "Tap birds as they fly past (⚡$SPOT_ENERGY_COST per spot).",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                textAlign = TextAlign.Center,
                color = if (feedbackBad) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(Modifier.height(8.dp))

            Sky(
                birds = birds,
                nowMs = nowMs,
                onBirdTap = ::spot,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            Spacer(Modifier.height(8.dp))

            Collection(birdsSeen = state.birdsSeen)

            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun Sky(
    birds: List<FlyingBird>,
    nowMs: Long,
    onBirdTap: (FlyingBird) -> Unit,
    modifier: Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(SkyTopColor, SkyBottomColor)))
            .border(4.dp, SkyEdgeColor, RoundedCornerShape(20.dp)),
    ) {
        // Drifting cloud emojis for ambient flavor.
        val transition = rememberInfiniteTransition(label = "clouds")
        val cloudDrift by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(40_000), RepeatMode.Restart),
            label = "drift",
        )
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }

        listOf(0.05f to 0.18f, 0.45f to 0.08f, 0.78f to 0.22f).forEach { (yFrac, offsetFrac) ->
            val drift = ((cloudDrift + offsetFrac) % 1f)
            val x = (drift * (widthPx + 200f) - 100f).roundToInt()
            val y = (yFrac * heightPx).roundToInt()
            Text(
                "☁️",
                fontSize = 28.sp,
                modifier = Modifier
                    .offset { IntOffset(x, y) },
            )
        }

        // The actual flock — each bird is its own clickable Text positioned by elapsed time.
        birds.forEach { bird ->
            val elapsed = (nowMs - bird.startMs).coerceAtLeast(0)
            val t = (elapsed.toFloat() / bird.durationMs).coerceIn(0f, 1f)
            // Bird travels off-screen on either side, so map t onto a slightly wider range.
            val xFrac = if (bird.leftToRight) t * 1.2f - 0.1f else (1f - t) * 1.2f - 0.1f
            val xPx = (xFrac * widthPx).roundToInt()
            val yPx = (bird.yPercent * heightPx).roundToInt()
            Text(
                bird.species.emoji,
                fontSize = 36.sp,
                modifier = Modifier
                    .offset { IntOffset(xPx, yPx) }
                    .clickable { onBirdTap(bird) },
            )
        }

        // Tiny meadow strip at the bottom so the sky has somewhere to land.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .align(Alignment.BottomCenter)
                .background(MeadowColor),
        )
    }
}

@Composable
private fun Collection(birdsSeen: Map<String, Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(10.dp)) {
            val total = birdsSeen.values.sum()
            Text(
                "🪶 Field Notes — $total birds spotted",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BIRD_TABLE.forEach { species ->
                    val count = birdsSeen[species.key] ?: 0
                    val seen = count > 0
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (seen) species.emoji else "❓",
                            fontSize = 22.sp,
                        )
                        Text(
                            text = if (seen) "x$count" else "—",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

