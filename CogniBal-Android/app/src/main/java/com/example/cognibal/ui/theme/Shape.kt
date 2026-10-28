package com.example.cognibal.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** 卡片用 extraLarge。控件和标签用更小的圆角，不另做一套描边。 */
val CogniBalShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/** 白卡片浮在冷灰底上的落影。再高就会变成厚重的投影。 */
val CogniBalCardElevation = 3.dp
