package com.example.cognibal.ui.interaction

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

private val FocusOutlineWidth = 2.dp

/**
 * 自定义按钮式点击目标的指针与键盘规范。
 *
 * 悬停时使用手型光标，并由 Material 状态层给出 hover / pressed。键盘焦点再画一圈轮廓。
 * 点击区域至少 48.dp。[minimumInteractiveComponentSize] 放在 [clickable] 内侧，
 * 这样放大的是点击节点测到的尺寸，多出来的区域也能点到。
 *
 * Material 的 Button、Surface(onClick)、NavigationSuiteItem 已经带这套反馈，不要再套一层。
 * 复选、开关继续用对应组件。
 */
@Composable
fun Modifier.interactiveClick(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: () -> Unit,
): Modifier {
    val focused by interactionSource.collectIsFocusedAsState()
    val outlined = border(
        width = if (focused && enabled) FocusOutlineWidth else 0.dp,
        color = MaterialTheme.colorScheme.secondary,
        shape = MaterialTheme.shapes.small,
    )
    val withPointer = if (enabled) outlined.pointerHoverIcon(PointerIcon.Hand) else outlined
    return withPointer
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(),
            enabled = enabled,
            role = Role.Button,
            onClick = onClick,
        )
        .minimumInteractiveComponentSize()
}
