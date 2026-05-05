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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(onGoToFarm: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
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
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "Choose your destination",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF5C3D1A),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    LocationCard(
                        emoji = "🌾",
                        name = "Farm",
                        unlocked = true,
                        modifier = Modifier.weight(1f),
                        onClick = onGoToFarm,
                    )
                    LocationCard(
                        emoji = "⛏️",
                        name = "Mine",
                        unlocked = false,
                        modifier = Modifier.weight(1f),
                        onClick = { Toast.makeText(context, "Coming soon!", Toast.LENGTH_SHORT).show() },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    LocationCard(
                        emoji = "🍳",
                        name = "Kitchen",
                        unlocked = false,
                        modifier = Modifier.weight(1f),
                        onClick = { Toast.makeText(context, "Coming soon!", Toast.LENGTH_SHORT).show() },
                    )
                    LocationCard(
                        emoji = "🏪",
                        name = "Market",
                        unlocked = false,
                        modifier = Modifier.weight(1f),
                        onClick = { Toast.makeText(context, "Coming soon!", Toast.LENGTH_SHORT).show() },
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationCard(
    emoji: String,
    name: String,
    unlocked: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bg = if (unlocked) Color(0xFFD4A74A) else Color(0xFFB0956B)
    val borderColor = if (unlocked) Color(0xFF8B6914) else Color(0xFF7A6040)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(3.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 40.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3D2608),
            )
            if (!unlocked) {
                Text(
                    "🔒 Coming Soon",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF5C4020),
                )
            }
        }
    }
}
