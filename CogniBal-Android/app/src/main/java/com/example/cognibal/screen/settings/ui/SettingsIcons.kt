package com.example.cognibal.screen.settings.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 设置页专用轻量矢量图标集合，无需额外第三方扩展库即可独立呈现
 */
object SettingsIcons {
    val Settings: ImageVector = Icons.Rounded.Settings
    val Check: ImageVector = Icons.Rounded.Check
    val Info: ImageVector = Icons.Rounded.Info

    /**
     * 调色板图标 (Palette)
     */
    val Palette: ImageVector by lazy {
        ImageVector.Builder(
            name = "Palette",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
                curveTo(12.83f, 22f, 13.5f, 21.33f, 13.5f, 20.5f)
                curveTo(13.5f, 20.11f, 13.35f, 19.76f, 13.11f, 19.49f)
                curveTo(12.88f, 19.23f, 12.73f, 18.88f, 12.73f, 18.5f)
                curveTo(12.73f, 17.67f, 13.4f, 17f, 14.23f, 17f)
                horizontalLineTo(16f)
                curveTo(19.31f, 17f, 22f, 14.31f, 22f, 11f)
                curveTo(22f, 6.03f, 17.52f, 2f, 12f, 2f)
                close()
                // 四个颜色小点
                moveTo(6.5f, 11.5f)
                curveTo(5.67f, 11.5f, 5f, 10.83f, 5f, 10f)
                curveTo(5f, 9.17f, 5.67f, 8.5f, 6.5f, 8.5f)
                curveTo(7.33f, 8.5f, 8f, 9.17f, 8f, 10f)
                curveTo(8f, 10.83f, 7.33f, 11.5f, 6.5f, 11.5f)
                close()

                moveTo(9.5f, 7.5f)
                curveTo(8.67f, 7.5f, 8f, 6.83f, 8f, 6f)
                curveTo(8f, 5.17f, 8.67f, 4.5f, 9.5f, 4.5f)
                curveTo(10.33f, 4.5f, 11f, 5.17f, 11f, 6f)
                curveTo(11f, 6.83f, 10.33f, 7.5f, 9.5f, 7.5f)
                close()

                moveTo(14.5f, 7.5f)
                curveTo(13.67f, 7.5f, 13f, 6.83f, 13f, 6f)
                curveTo(13f, 5.17f, 13.67f, 4.5f, 14.5f, 4.5f)
                curveTo(15.33f, 4.5f, 16f, 5.17f, 16f, 6f)
                curveTo(16f, 6.83f, 15.33f, 7.5f, 14.5f, 7.5f)
                close()

                moveTo(17.5f, 11.5f)
                curveTo(16.67f, 11.5f, 16f, 10.83f, 16f, 10f)
                curveTo(16f, 9.17f, 16.67f, 8.5f, 17.5f, 8.5f)
                curveTo(18.33f, 8.5f, 19f, 9.17f, 19f, 10f)
                curveTo(19f, 10.83f, 18.33f, 11.5f, 17.5f, 11.5f)
                close()
            }
        }.build()
    }

    /**
     * 浅色模式/太阳图标 (LightMode)
     */
    val LightMode: ImageVector by lazy {
        ImageVector.Builder(
            name = "LightMode",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 7f)
                curveTo(9.24f, 7f, 7f, 9.24f, 7f, 12f)
                curveTo(7f, 14.76f, 9.24f, 17f, 12f, 17f)
                curveTo(14.76f, 17f, 17f, 14.76f, 17f, 12f)
                curveTo(17f, 9.24f, 14.76f, 7f, 12f, 7f)
                close()

                moveTo(12f, 2f)
                curveTo(11.45f, 2f, 11f, 2.45f, 11f, 3f)
                verticalLineTo(5f)
                curveTo(11f, 5.55f, 11.45f, 6f, 12f, 6f)
                curveTo(12.55f, 6f, 13f, 5.55f, 13f, 5f)
                verticalLineTo(3f)
                curveTo(13f, 2.45f, 12.55f, 2f, 12f, 2f)
                close()

                moveTo(12f, 18f)
                curveTo(11.45f, 18f, 11f, 18.45f, 11f, 19f)
                verticalLineTo(21f)
                curveTo(11f, 21.55f, 11.45f, 22f, 12f, 22f)
                curveTo(12.55f, 22f, 13f, 21.55f, 13f, 21f)
                verticalLineTo(19f)
                curveTo(13f, 18.45f, 12.55f, 18f, 12f, 18f)
                close()

                moveTo(5f, 11f)
                horizontalLineTo(3f)
                curveTo(2.45f, 11f, 2f, 11.45f, 2f, 12f)
                curveTo(2f, 12.55f, 2.45f, 13f, 3f, 13f)
                horizontalLineTo(5f)
                curveTo(5.55f, 13f, 6f, 12.55f, 6f, 12f)
                curveTo(6f, 11.45f, 5.55f, 11f, 5f, 11f)
                close()

                moveTo(21f, 11f)
                horizontalLineTo(19f)
                curveTo(18.45f, 11f, 18f, 11.45f, 18f, 12f)
                curveTo(18f, 12.55f, 18.45f, 13f, 19f, 13f)
                horizontalLineTo(21f)
                curveTo(21.55f, 13f, 22f, 12.55f, 22f, 12f)
                curveTo(22f, 11.45f, 21.55f, 11f, 21f, 11f)
                close()

                moveTo(5.64f, 4.22f)
                curveTo(5.25f, 3.83f, 4.62f, 3.83f, 4.22f, 4.22f)
                curveTo(3.83f, 4.62f, 3.83f, 5.25f, 4.22f, 5.64f)
                lineTo(5.64f, 7.05f)
                curveTo(6.03f, 7.44f, 6.66f, 7.44f, 7.05f, 7.05f)
                curveTo(7.44f, 6.66f, 7.44f, 6.03f, 7.05f, 5.64f)
                lineTo(5.64f, 4.22f)
                close()

                moveTo(18.36f, 16.95f)
                curveTo(17.97f, 16.56f, 17.34f, 16.56f, 16.95f, 16.95f)
                curveTo(16.56f, 17.34f, 16.56f, 17.97f, 16.95f, 18.36f)
                lineTo(18.36f, 19.78f)
                curveTo(18.75f, 20.17f, 19.38f, 20.17f, 19.78f, 19.78f)
                curveTo(20.17f, 19.38f, 20.17f, 18.75f, 19.78f, 18.36f)
                lineTo(18.36f, 16.95f)
                close()

                moveTo(7.05f, 18.36f)
                curveTo(6.66f, 17.97f, 6.03f, 17.97f, 5.64f, 18.36f)
                lineTo(4.22f, 19.78f)
                curveTo(3.83f, 20.17f, 3.83f, 20.79f, 4.22f, 21.19f)
                curveTo(4.62f, 21.58f, 5.25f, 21.58f, 5.64f, 21.19f)
                lineTo(7.05f, 19.78f)
                curveTo(7.44f, 19.38f, 7.44f, 18.75f, 7.05f, 18.36f)
                close()

                moveTo(19.78f, 4.22f)
                curveTo(19.38f, 3.83f, 18.75f, 3.83f, 18.36f, 4.22f)
                lineTo(16.95f, 5.64f)
                curveTo(16.56f, 6.03f, 16.56f, 6.66f, 16.95f, 7.05f)
                curveTo(17.34f, 7.44f, 17.97f, 7.44f, 18.36f, 7.05f)
                lineTo(19.78f, 5.64f)
                curveTo(20.17f, 5.25f, 20.17f, 4.62f, 19.78f, 4.22f)
                close()
            }
        }.build()
    }

    /**
     * 暗色模式/月亮图标 (DarkMode)
     */
    val DarkMode: ImageVector by lazy {
        ImageVector.Builder(
            name = "DarkMode",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.3f, 2f)
                curveTo(6.9f, 2.15f, 2.5f, 6.6f, 2.5f, 12f)
                curveTo(2.5f, 17.5f, 7f, 22f, 12.5f, 22f)
                curveTo(17.9f, 22f, 22.35f, 17.6f, 22.5f, 12.2f)
                curveTo(22.55f, 11.6f, 22.05f, 11.1f, 21.45f, 11.25f)
                curveTo(17.75f, 12.15f, 13.85f, 9.8f, 12.95f, 6.1f)
                curveTo(12.75f, 5.25f, 12.75f, 4.45f, 12.9f, 3.7f)
                curveTo(13.05f, 3.1f, 12.6f, 2.6f, 12f, 2.6f)
                curveTo(12.1f, 2.4f, 12.2f, 2.2f, 12.3f, 2f)
                close()
            }
        }.build()
    }

    /**
     * 跟随系统图标 (AutoMode/Phone)
     */
    val AutoMode: ImageVector by lazy {
        ImageVector.Builder(
            name = "AutoMode",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 22f)
                curveTo(6.48f, 22f, 2f, 17.52f, 2f, 12f)
                curveTo(2f, 6.48f, 6.48f, 2f, 12f, 2f)
                curveTo(17.52f, 2f, 22f, 6.48f, 22f, 12f)
                curveTo(22f, 17.52f, 17.52f, 22f, 12f, 22f)
                close()
                // 左半边镂空实心，右半边为空，表示深浅随系统半边
                moveTo(12f, 4f)
                verticalLineTo(20f)
                curveTo(16.42f, 20f, 20f, 16.42f, 20f, 12f)
                curveTo(20f, 7.58f, 16.42f, 4f, 12f, 4f)
                close()
            }
        }.build()
    }

    /**
     * 弥散光斑/动效装饰图标 (Sparkles)
     */
    val Sparkles: ImageVector by lazy {
        ImageVector.Builder(
            name = "Sparkles",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2f)
                lineTo(13.5f, 7.5f)
                lineTo(19f, 9f)
                lineTo(13.5f, 10.5f)
                lineTo(12f, 16f)
                lineTo(10.5f, 10.5f)
                lineTo(5f, 9f)
                lineTo(10.5f, 7.5f)
                close()

                moveTo(19f, 15f)
                lineTo(20f, 18.5f)
                lineTo(23.5f, 19.5f)
                lineTo(20f, 20.5f)
                lineTo(19f, 24f)
                lineTo(18f, 20.5f)
                lineTo(14.5f, 19.5f)
                lineTo(18f, 18.5f)
                close()

                moveTo(5f, 16f)
                lineTo(6f, 19.5f)
                lineTo(9.5f, 20.5f)
                lineTo(6f, 21.5f)
                lineTo(5f, 25f)
                lineTo(4f, 21.5f)
                lineTo(0.5f, 20.5f)
                lineTo(4f, 19.5f)
                close()
            }
        }.build()
    }

    /**
     * 偏好与触控反馈图标 (Vibration)
     */
    val Vibration: ImageVector by lazy {
        ImageVector.Builder(
            name = "Vibration",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(0f, 15f)
                horizontalLineTo(2f)
                verticalLineTo(9f)
                horizontalLineTo(0f)
                verticalLineTo(15f)
                close()

                moveTo(3f, 17f)
                horizontalLineTo(5f)
                verticalLineTo(7f)
                horizontalLineTo(3f)
                verticalLineTo(17f)
                close()

                moveTo(22f, 9f)
                verticalLineTo(15f)
                horizontalLineTo(24f)
                verticalLineTo(9f)
                horizontalLineTo(22f)
                close()

                moveTo(19f, 17f)
                horizontalLineTo(21f)
                verticalLineTo(7f)
                horizontalLineTo(19f)
                verticalLineTo(17f)
                close()

                moveTo(16.5f, 3f)
                horizontalLineTo(7.5f)
                curveTo(6.67f, 3f, 6f, 3.67f, 6f, 4.5f)
                verticalLineTo(19.5f)
                curveTo(6f, 20.33f, 6.67f, 21f, 7.5f, 21f)
                horizontalLineTo(16.5f)
                curveTo(17.33f, 21f, 18f, 20.33f, 18f, 19.5f)
                verticalLineTo(4.5f)
                curveTo(18f, 3.67f, 17.33f, 3f, 16.5f, 3f)
                close()
                moveTo(16f, 19f)
                horizontalLineTo(8f)
                verticalLineTo(5f)
                horizontalLineTo(16f)
                verticalLineTo(19f)
                close()
            }
        }.build()
    }

    /**
     * 存储与缓存图标 (CleaningServices / Storage)
     */
    val Storage: ImageVector by lazy {
        ImageVector.Builder(
            name = "Storage",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(2f, 20f)
                horizontalLineTo(22f)
                verticalLineTo(16f)
                horizontalLineTo(2f)
                verticalLineTo(20f)
                close()
                moveTo(4f, 17f)
                horizontalLineTo(6f)
                verticalLineTo(19f)
                horizontalLineTo(4f)
                verticalLineTo(17f)
                close()

                moveTo(2f, 4f)
                verticalLineTo(8f)
                horizontalLineTo(22f)
                verticalLineTo(4f)
                horizontalLineTo(2f)
                close()
                moveTo(6f, 7f)
                horizontalLineTo(4f)
                verticalLineTo(5f)
                horizontalLineTo(6f)
                verticalLineTo(7f)
                close()

                moveTo(2f, 14f)
                horizontalLineTo(22f)
                verticalLineTo(10f)
                horizontalLineTo(2f)
                verticalLineTo(14f)
                close()
                moveTo(4f, 11f)
                horizontalLineTo(6f)
                verticalLineTo(13f)
                horizontalLineTo(4f)
                verticalLineTo(11f)
                close()
            }
        }.build()
    }

    /**
     * 向右指示箭头 (ChevronRight)
     */
    val ChevronRight: ImageVector by lazy {
        ImageVector.Builder(
            name = "ChevronRight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(9f, 18f)
                lineTo(15f, 12f)
                lineTo(9f, 6f)
            }
        }.build()
    }

    /**
     * 货币图标 (AttachMoney)
     */
    val Currency: ImageVector by lazy {
        ImageVector.Builder(
            name = "Currency",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
                curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
                curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
                close()

                moveTo(13.41f, 18.09f)
                verticalLineTo(20f)
                horizontalLineTo(11.08f)
                verticalLineTo(18.06f)
                curveTo(9.09f, 17.65f, 7.55f, 16.44f, 7.42f, 14.3f)
                horizontalLineTo(9.51f)
                curveTo(9.67f, 15.42f, 10.45f, 16.29f, 12.18f, 16.29f)
                curveTo(13.91f, 16.29f, 14.49f, 15.34f, 14.49f, 14.54f)
                curveTo(14.49f, 13.38f, 13.56f, 12.83f, 11.45f, 12.27f)
                curveTo(8.75f, 11.55f, 7.64f, 10.28f, 7.64f, 8.44f)
                curveTo(7.64f, 6.78f, 9.07f, 5.51f, 11.08f, 5.12f)
                verticalLineTo(3.24f)
                horizontalLineTo(13.41f)
                verticalLineTo(5.15f)
                curveTo(15.28f, 5.59f, 16.48f, 6.84f, 16.6f, 8.52f)
                horizontalLineTo(14.54f)
                curveTo(14.38f, 7.65f, 13.72f, 6.94f, 12.18f, 6.94f)
                curveTo(10.74f, 6.94f, 9.98f, 7.74f, 9.98f, 8.44f)
                curveTo(9.98f, 9.42f, 10.82f, 9.91f, 12.79f, 10.43f)
                curveTo(15.32f, 11.1f, 16.84f, 12.3f, 16.84f, 14.38f)
                curveTo(16.84f, 16.14f, 15.38f, 17.56f, 13.41f, 18.09f)
                close()
            }
        }.build()
    }
}
