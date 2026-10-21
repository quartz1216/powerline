package com.quartz.powerline.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

// Tiles inside the overlay panel: concentric with the panel's extraLarge (32dp) corner at PanelItemInset
val PanelItemInset = 12.dp
val PanelItemShape = RoundedCornerShape(20.dp)
