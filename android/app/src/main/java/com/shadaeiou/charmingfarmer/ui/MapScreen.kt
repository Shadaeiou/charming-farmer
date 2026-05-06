package com.shadaeiou.charmingfarmer.ui

import android.widget.Toast
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class Destination(
    val emoji: String,
    val name: String,
    val blurb: String,
    val unlocked: Boolean,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onGoToFarm: () -> Unit,
    onGoToPond: () -> Unit,
    onGoToBirds: () -> Unit,
    onGoToMalthouse: () -> Unit,
    onGoToBrewery: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val comingSoon: () -> Unit = {
        Toast.makeText(context, "Coming soon!", Toast.LENGTH_SHORT).show()
    }

    val destinations = listOf(
        Destination("🌾", "Farm", "Till dirt, plant seeds, raise trees, harvest the works.", true, onGoToFarm),
        Destination("🎣", "Pond", "Cast a line, time the bite, reel in ten kinds of fish.", true, onGoToPond),
        Destination("🦜", "Birdwatching", "Spot rare birds as they fly past and add them to your field notes.", true, onGoToBirds),
        Destination("🏭", "Malthouse", "Steep, germinate, and kiln your grain into pale, crystal, chocolate or black malt.", true, onGoToMalthouse),
        Destination("🍺", "Brewery", "Mash, boil, ferment, bottle. Recipes only succeed with the right malt and hops.", true, onGoToBrewery),
        Destination("🐝", "Apiary", "Tend hives, harvest jars of honey, breed rarer bees over time.", false, comingSoon),
        Destination("🐄", "Livestock", "Feed cows, sheep, and chickens for steady milk, wool, and eggs.", false, comingSoon),
        Destination("⛏️", "Mine", "Swing a pickaxe through stone for ore, gems, and rare relics.", false, comingSoon),
        Destination("🌲", "Forest", "Forage mushrooms, herbs, and lumber by wandering wild tiles.", false, comingSoon),
        Destination("🍳", "Kitchen", "Turn raw harvest into pies, jams, and stews that fetch a premium.", false, comingSoon),
        Destination("🍷", "Cellar", "Age wine, cheese, and preserves — patience triples the price.", false, comingSoon),
        Destination("⚒️", "Workshop", "Craft tools and decor from the materials you've gathered.", false, comingSoon),
        Destination("🌷", "Greenhouse", "Grow exotic crops faster, no matter the weather outside.", false, comingSoon),
        Destination("🛖", "Cottage", "Upgrade your home — bigger beds raise your max energy cap.", false, comingSoon),
        Destination("⛪", "Shrine", "Leave rare offerings for short bursts of luck and yield buffs.", false, comingSoon),
        Destination("☕", "Café", "Sip coffee for a temporary boost to your energy regen rate.", false, comingSoon),
        Destination("🏪", "Market", "Set up a stall and sell goods directly at a markup.", false, comingSoon),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🗺️ Atlas", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Close map")
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5E6C8)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Choose your destination",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF5C3D1A),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                destinations.chunked(2).forEach { rowDestinations ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        rowDestinations.forEach { dest ->
                            LocationCard(
                                dest = dest,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        // Trailing spacer keeps the last card aligned when a row
                        // has only one destination, so card widths stay uniform.
                        if (rowDestinations.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationCard(dest: Destination, modifier: Modifier = Modifier) {
    val bg = if (dest.unlocked) Color(0xFFD4A74A) else Color(0xFFB0956B)
    val borderColor = if (dest.unlocked) Color(0xFF8B6914) else Color(0xFF7A6040)
    val nameColor = Color(0xFF3D2608)
    val blurbColor = Color(0xFF5C4020)
    Box(
        modifier = modifier
            .heightIn(min = 150.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(3.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { dest.onClick() }
            .padding(10.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(dest.emoji, fontSize = 34.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                dest.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = nameColor,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                dest.blurb,
                style = MaterialTheme.typography.labelSmall,
                color = blurbColor,
                textAlign = TextAlign.Center,
            )
            if (!dest.unlocked) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "🔒 Coming Soon",
                    style = MaterialTheme.typography.labelSmall,
                    color = blurbColor,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
