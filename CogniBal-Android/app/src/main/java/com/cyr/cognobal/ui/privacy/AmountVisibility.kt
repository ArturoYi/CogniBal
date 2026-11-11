package com.cyr.cognobal.ui.privacy

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cyr.cognobal.R
import com.cyr.cognobal.ui.interaction.interactiveClick

@Stable
class AmountVisibilityState(
    private val hiddenState: MutableState<Boolean>,
) {
    val hidden: Boolean
        get() = hiddenState.value

    fun toggle() {
        hiddenState.value = !hiddenState.value
    }
}

@Composable
fun rememberAmountVisibilityState(): AmountVisibilityState {
    val hidden = rememberSaveable { mutableStateOf(false) }
    return androidx.compose.runtime.remember(hidden) { AmountVisibilityState(hidden) }
}

val LocalAmountVisibility = staticCompositionLocalOf {
    AmountVisibilityState(mutableStateOf(false))
}

/** 隐藏金额时统一替换为简洁星号掩码。 */
fun maskIfHidden(formatted: String, hidden: Boolean, symbol: String = ""): String {
    if (!hidden) return formatted
    return if (symbol.isNotEmpty()) "$symbol ••••" else "••••"
}

@Composable
fun AmountEyeToggle(
    hidden: Boolean,
    onToggle: () -> Unit,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(if (hidden) R.string.show_amount else R.string.hide_amount)
    Box(
        modifier = modifier
            .size(36.dp)
            .semantics { contentDescription = label }
            .interactiveClick(role = Role.Button, onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (hidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
    }
}
