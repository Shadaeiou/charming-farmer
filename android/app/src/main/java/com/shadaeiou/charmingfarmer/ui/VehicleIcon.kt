package com.shadaeiou.charmingfarmer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.shadaeiou.charmingfarmer.data.VehicleType

/**
 * Pixel-art icon per vehicle type, with a player-chosen [tint] used as
 * the body / paint color. Drawn on a 16×16 virtual grid so it stays
 * crisp at every render size and stays cheap to redraw on color
 * changes. Wheel rims, glass, and trim use fixed neutral colors so the
 * vehicle reads recognizably regardless of paint choice.
 */
@Composable
fun VehicleIcon(
    vehicle: VehicleType,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val cols = 16f
        val rows = 16f
        val px = size.width / cols
        val py = size.height / rows
        fun r(cx: Float, cy: Float, cw: Float, ch: Float, color: Color) {
            drawRect(color = color, topLeft = Offset(cx * px, cy * py),
                size = Size(cw * px, ch * py))
        }

        val tintShade = darken(tint, 0.65f)
        val tintHi = lighten(tint, 0.2f)
        val wheel = Color(0xFF22222A)
        val rim = Color(0xFFB0B0B0)
        val glass = Color(0xFF7AB7DD)
        val handle = Color(0xFF6E5036)
        val ground = Color(0xFF3F3F3F)

        when (vehicle) {
            VehicleType.WHEELBARROW -> {
                // Tray + single front wheel + handles back-right.
                r(2f, 7f, 9f, 4f, tint)
                r(2f, 7f, 9f, 0.6f, tintHi)
                r(2f, 10.4f, 9f, 0.6f, tintShade)
                // Front leg
                r(2f, 11f, 1f, 2f, handle)
                // Wheel
                r(8.5f, 10f, 3f, 3f, wheel)
                r(9.3f, 10.8f, 1.4f, 1.4f, rim)
                // Handles
                r(11f, 8f, 4f, 0.8f, handle)
                r(11f, 9.5f, 4f, 0.8f, handle)
            }
            VehicleType.HANDCART -> {
                // Two-wheel cart with vertical handle.
                r(3f, 6f, 9f, 4.5f, tint)
                r(3f, 6f, 9f, 0.6f, tintHi)
                r(3f, 10f, 9f, 0.6f, tintShade)
                // Handle bar arching back
                r(11f, 4.5f, 0.8f, 4f, handle)
                r(11.5f, 4.5f, 2.5f, 0.8f, handle)
                // Two wheels on the bottom
                r(3.5f, 10.5f, 2.5f, 2.5f, wheel)
                r(8.5f, 10.5f, 2.5f, 2.5f, wheel)
                r(4.2f, 11.2f, 1.1f, 1.1f, rim)
                r(9.2f, 11.2f, 1.1f, 1.1f, rim)
            }
            VehicleType.HORSE_CART -> {
                // Horse silhouette + wagon. Horse stays brown so paint
                // applies to the wagon only.
                val horse = Color(0xFF6E4423)
                val mane = Color(0xFF3F2616)
                // Horse body
                r(1f, 7f, 5f, 3f, horse)
                r(1f, 6.5f, 1.5f, 0.7f, horse) // head
                r(2.5f, 5.5f, 1f, 1.5f, horse) // neck
                r(2.5f, 6f, 1f, 0.4f, mane)
                r(1f, 10f, 0.8f, 2f, horse)
                r(5f, 10f, 0.8f, 2f, horse)
                // Wagon
                r(7f, 6.5f, 8f, 4f, tint)
                r(7f, 6.5f, 8f, 0.5f, tintHi)
                r(7f, 10.1f, 8f, 0.5f, tintShade)
                // Wheels
                r(7.5f, 10.5f, 2.5f, 2.5f, wheel)
                r(12f, 10.5f, 2.5f, 2.5f, wheel)
                r(8.2f, 11.2f, 1.1f, 1.1f, rim)
                r(12.7f, 11.2f, 1.1f, 1.1f, rim)
            }
            VehicleType.CARGO_BIKE -> {
                // Bike silhouette: front basket (paint), frame, two wheels.
                // Front cargo box
                r(1f, 7f, 5f, 4f, tint)
                r(1f, 7f, 5f, 0.6f, tintHi)
                r(1f, 10.4f, 5f, 0.6f, tintShade)
                // Frame triangle
                r(6f, 7.5f, 5f, 0.6f, handle)
                r(6f, 7.5f, 0.6f, 4f, handle)
                r(10.4f, 7.5f, 0.6f, 4f, handle)
                r(8f, 6.5f, 0.6f, 1.5f, handle) // seat post
                r(7.5f, 6f, 1.5f, 0.6f, handle) // seat
                // Wheels
                r(4f, 10.5f, 2.5f, 2.5f, wheel)
                r(10f, 10.5f, 2.5f, 2.5f, wheel)
                r(4.6f, 11.1f, 1.3f, 1.3f, rim)
                r(10.6f, 11.1f, 1.3f, 1.3f, rim)
            }
            VehicleType.TRUCK -> {
                // Tractor: big rear wheels, small front, exhaust stack.
                // Body
                r(3f, 7f, 8f, 3f, tint)
                r(3f, 7f, 8f, 0.5f, tintHi)
                r(3f, 9.5f, 8f, 0.5f, tintShade)
                // Cabin with glass
                r(7f, 4.5f, 4f, 3f, tint)
                r(7.5f, 5f, 3f, 2f, glass)
                // Exhaust stack
                r(5f, 4f, 0.8f, 3f, Color(0xFF333333))
                r(4.7f, 4f, 1.4f, 0.5f, Color(0xFF555555))
                // Big rear wheel
                r(8.5f, 9f, 4.5f, 4.5f, wheel)
                r(9.3f, 9.8f, 2.9f, 2.9f, rim)
                // Small front wheel
                r(2.5f, 10.5f, 2.5f, 2.5f, wheel)
                r(3.2f, 11.2f, 1.1f, 1.1f, rim)
            }
            VehicleType.PICKUP_TRUCK -> {
                // Pickup with cabin and bed.
                // Cabin
                r(2f, 5f, 6f, 4f, tint)
                r(2f, 5f, 6f, 0.6f, tintHi)
                r(3f, 6f, 4f, 2.5f, glass)
                // Bed
                r(8f, 6f, 6f, 4f, tint)
                r(8f, 6f, 6f, 0.6f, tintHi)
                r(8f, 9.4f, 6f, 0.6f, tintShade)
                // Front bumper
                r(1f, 8.5f, 1.5f, 1f, tintShade)
                // Wheels
                r(2.5f, 9.5f, 3f, 3f, wheel)
                r(10f, 9.5f, 3f, 3f, wheel)
                r(3.3f, 10.3f, 1.4f, 1.4f, rim)
                r(10.8f, 10.3f, 1.4f, 1.4f, rim)
            }
            VehicleType.DELIVERY_VAN -> {
                // Boxy van with sliding side door.
                r(1f, 4f, 13f, 6f, tint)
                r(1f, 4f, 13f, 0.6f, tintHi)
                r(1f, 9.4f, 13f, 0.6f, tintShade)
                // Windshield
                r(1f, 5f, 3f, 2f, glass)
                // Side door panel
                r(7f, 5f, 4f, 4f, tintShade)
                r(7f, 5f, 4f, 0.4f, tintHi)
                r(8.7f, 6f, 0.6f, 2.5f, handle)
                // Wheels
                r(2.5f, 9.5f, 2.5f, 2.5f, wheel)
                r(10.5f, 9.5f, 2.5f, 2.5f, wheel)
                r(3.1f, 10.1f, 1.3f, 1.3f, rim)
                r(11.1f, 10.1f, 1.3f, 1.3f, rim)
            }
            VehicleType.TRAIN -> {
                // Locomotive with smokestack and one rear wagon.
                // Smokestack
                r(2f, 3.5f, 1.2f, 2.5f, Color(0xFF333333))
                r(1.7f, 3.5f, 1.8f, 0.5f, Color(0xFF555555))
                // Boiler
                r(1.5f, 6f, 5f, 4f, tint)
                r(1.5f, 6f, 5f, 0.5f, tintHi)
                // Cabin
                r(6.5f, 5f, 3f, 5f, tint)
                r(6.5f, 5f, 3f, 0.5f, tintHi)
                r(7f, 6f, 2f, 2f, glass)
                // Wagon
                r(10f, 7f, 5f, 3f, tint)
                r(10f, 7f, 5f, 0.5f, tintHi)
                r(10f, 9.5f, 5f, 0.5f, tintShade)
                // Wheels (small + small)
                r(2f, 10f, 2f, 2f, wheel)
                r(5f, 10f, 2f, 2f, wheel)
                r(7f, 10f, 2f, 2f, wheel)
                r(11f, 10f, 2f, 2f, wheel)
                r(13.5f, 10f, 1.5f, 2f, wheel)
                // Rails
                r(0f, 12f, 16f, 0.4f, ground)
            }
            VehicleType.BOX_TRUCK -> {
                // Big boxy truck — small cabin in front, large box behind.
                // Cabin
                r(1f, 6f, 4f, 4f, tint)
                r(1f, 6f, 4f, 0.5f, tintHi)
                r(2f, 7f, 2.5f, 2f, glass)
                // Box
                r(5f, 3.5f, 10f, 6.5f, tint)
                r(5f, 3.5f, 10f, 0.6f, tintHi)
                r(5f, 9.4f, 10f, 0.6f, tintShade)
                // Roll-up door lines on the box back
                for (i in 0 until 5) {
                    r(5.5f + i * 1.9f, 4.5f, 0.4f, 4.5f, tintShade)
                }
                // Front bumper
                r(0.5f, 8.5f, 0.7f, 1.2f, tintShade)
                // Wheels (dual back, single front)
                r(1.5f, 10f, 2.5f, 2.5f, wheel)
                r(7f, 10f, 2.5f, 2.5f, wheel)
                r(10f, 10f, 2.5f, 2.5f, wheel)
                r(2f, 10.5f, 1.5f, 1.5f, rim)
                r(7.5f, 10.5f, 1.5f, 1.5f, rim)
                r(10.5f, 10.5f, 1.5f, 1.5f, rim)
            }
        }
    }
}

private fun darken(c: Color, factor: Float): Color = Color(
    red = (c.red * factor).coerceIn(0f, 1f),
    green = (c.green * factor).coerceIn(0f, 1f),
    blue = (c.blue * factor).coerceIn(0f, 1f),
    alpha = c.alpha,
)

private fun lighten(c: Color, amount: Float): Color = Color(
    red = (c.red + (1f - c.red) * amount).coerceIn(0f, 1f),
    green = (c.green + (1f - c.green) * amount).coerceIn(0f, 1f),
    blue = (c.blue + (1f - c.blue) * amount).coerceIn(0f, 1f),
    alpha = c.alpha,
)
