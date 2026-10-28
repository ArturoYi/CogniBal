package com.example.cognibal.ui.picker

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cognibal.R
import com.example.cognibal.ui.preview.FormFactorPreviews
import com.example.cognibal.ui.theme.CogniBalTheme
import com.example.cognibal.ui.theme.appColors
import java.util.Locale

/**
 * 日期时间滚轮。
 *
 * 每一列都是一个 [WheelPicker]。列与列的联动留在这里：某一列停稳后，用
 * [WheelDateTimeRules.clamp] 收束其余列。例如小时变了，分钟能留在原值就留，超出该小时的合法范围就改到最近端点。
 * 基础滚轮不知道年、月、日的换算。
 *
 * 小时、分钟、秒的列表保持完整，合法区间通过滚轮的 min/max 拦截。日列表只包含当月存在的日期。
 * `onValueChange` 在某一列停稳且时间有变化时调用，滚动过程中不调用。
 * 传入的 [value] 超出 [minTime] / [maxTime] 时，静止后会回调一次修正值。父组件需要把回调写回 [value]。
 *
 * ```
 * var dateTime by remember { mutableStateOf(WheelDateTime.now()) }
 * CogniBalWheelDateTimePicker(
 *     value = dateTime,
 *     onValueChange = { dateTime = it },
 *     mode = WheelDateTimeMode.DateTime,
 *     minTime = WheelDateTime.of(2020, 1, 1, 0, 0),
 *     maxTime = WheelDateTime.of(2030, 12, 31, 23, 59),
 * )
 * ```
 */
@Composable
fun CogniBalWheelDateTimePicker(
    value: WheelDateTime,
    onValueChange: (WheelDateTime) -> Unit,
    modifier: Modifier = Modifier,
    mode: WheelDateTimeMode = WheelDateTimeMode.DateTime,
    minTime: WheelDateTime? = null,
    maxTime: WheelDateTime? = null,
    config: WheelPickerConfig = WheelPickerConfig.Default,
) {
    val safeValue = remember(value, minTime, maxTime) {
        WheelDateTimeRules.clamp(value, minTime, maxTime)
    }
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    LaunchedEffect(safeValue, value) {
        if (safeValue != value) currentOnValueChange(safeValue)
    }

    val years = remember(minTime, maxTime) {
        WheelDateTimeRules.yearRange(minTime, maxTime).toList()
    }
    val months = remember { (1..12).toList() }
    val dayCount = WheelDateTimeRules.dayRange(safeValue.year, safeValue.month, null, null).last
    val days = remember(dayCount) { (1..dayCount).toList() }
    val hours = remember { (0..23).toList() }
    val minutes = remember { (0..59).toList() }
    val seconds = remember { (0..59).toList() }
    val fields = mode.fields()
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val itemHeight = config.wheelItemHeight()
    val wheelHeight = config.wheelViewportHeight()
    val decoration = config.selectionStyle.toDecoration()
    val textAppearance = WheelPickerDefaults.textAppearance()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        ) {
            fields.forEach { field ->
                Text(
                    text = stringResource(field.labelRes),
                    modifier = Modifier
                        .weight(1f)
                        .clearAndSetSemantics {},
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.appColors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(wheelHeight)
                .clipToBounds()
                .wheelPickerChrome(
                    itemHeight = itemHeight,
                    decoration = decoration,
                    fadeEnabled = config.fadeEnabled,
                    fadeHeight = config.fadeHeight,
                ),
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                fields.forEach { field ->
                    val column = field.column(
                        value = safeValue,
                        minTime = minTime,
                        maxTime = maxTime,
                        years = years,
                        months = months,
                        days = days,
                        hours = hours,
                        minutes = minutes,
                        seconds = seconds,
                    )
                    val description = stringResource(
                        R.string.wheel_column_value,
                        stringResource(field.labelRes),
                        field.format(safeValue.component(field), locale),
                    )
                    key(field) {
                        WheelPicker(
                            items = column.items,
                            selectedIndex = column.selectedIndex,
                            onSelectedIndexChange = { index ->
                                val number = column.items.getOrNull(index) ?: return@WheelPicker
                                val next = WheelDateTimeRules.clamp(
                                    safeValue.withComponent(field, number),
                                    minTime,
                                    maxTime,
                                )
                                if (next != safeValue) currentOnValueChange(next)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            itemText = { number -> field.format(number, locale) },
                            itemKey = { _, number -> number },
                            minIndex = column.minIndex,
                            maxIndex = column.maxIndex,
                            config = config,
                            decoration = WheelSelectionDecoration.None,
                            textAppearance = textAppearance,
                            contentDescription = description,
                            drawChrome = false,
                        )
                    }
                }
            }
        }
    }
}

private data class IntColumn(
    val items: List<Int>,
    val selectedIndex: Int,
    val minIndex: Int,
    val maxIndex: Int,
)

private fun WheelField.column(
    value: WheelDateTime,
    minTime: WheelDateTime?,
    maxTime: WheelDateTime?,
    years: List<Int>,
    months: List<Int>,
    days: List<Int>,
    hours: List<Int>,
    minutes: List<Int>,
    seconds: List<Int>,
): IntColumn {
    val items: List<Int>
    val selected: Int
    val range: IntRange
    when (this) {
        WheelField.Year -> {
            items = years
            selected = value.year
            range = if (years.isEmpty()) 0..0 else years.first()..years.last()
        }
        WheelField.Month -> {
            items = months
            selected = value.month
            range = WheelDateTimeRules.monthRange(value.year, minTime, maxTime)
        }
        WheelField.Day -> {
            items = days
            selected = value.day
            range = WheelDateTimeRules.dayRange(value.year, value.month, minTime, maxTime)
        }
        WheelField.Hour -> {
            items = hours
            selected = value.hour
            range = WheelDateTimeRules.hourRange(
                value.year,
                value.month,
                value.day,
                minTime,
                maxTime,
            )
        }
        WheelField.Minute -> {
            items = minutes
            selected = value.minute
            range = WheelDateTimeRules.minuteRange(
                value.year,
                value.month,
                value.day,
                value.hour,
                minTime,
                maxTime,
            )
        }
        WheelField.Second -> {
            items = seconds
            selected = value.second
            range = WheelDateTimeRules.secondRange(
                value.year,
                value.month,
                value.day,
                value.hour,
                value.minute,
                minTime,
                maxTime,
            )
        }
    }
    return items.toColumn(selected, range)
}

private fun List<Int>.toColumn(selected: Int, range: IntRange): IntColumn {
    if (isEmpty()) return IntColumn(this, 0, 0, 0)
    val min = indexOf(range.first).let { index -> if (index >= 0) index else 0 }
    val maxFound = indexOf(range.last)
    val max = if (maxFound >= 0) maxFound else lastIndex
    val lower = minOf(min, max)
    val upper = maxOf(min, max)
    val found = indexOf(selected)
    val selectedIndex = when {
        found in lower..upper -> found
        found > upper -> upper
        else -> lower
    }
    return IntColumn(this, selectedIndex, lower, upper)
}

private enum class WheelField(@get:StringRes @param:StringRes val labelRes: Int) {
    Year(R.string.wheel_year),
    Month(R.string.wheel_month),
    Day(R.string.wheel_day),
    Hour(R.string.wheel_hour),
    Minute(R.string.wheel_minute),
    Second(R.string.wheel_second),
}

private fun WheelDateTimeMode.fields(): List<WheelField> = when (this) {
    WheelDateTimeMode.Date -> listOf(WheelField.Year, WheelField.Month, WheelField.Day)
    WheelDateTimeMode.Time -> listOf(WheelField.Hour, WheelField.Minute)
    WheelDateTimeMode.DateTime -> listOf(
        WheelField.Year,
        WheelField.Month,
        WheelField.Day,
        WheelField.Hour,
        WheelField.Minute,
    )
    WheelDateTimeMode.YearMonth -> listOf(WheelField.Year, WheelField.Month)
}

private fun WheelDateTime.component(field: WheelField): Int = when (field) {
    WheelField.Year -> year
    WheelField.Month -> month
    WheelField.Day -> day
    WheelField.Hour -> hour
    WheelField.Minute -> minute
    WheelField.Second -> second
}

private fun WheelDateTime.withComponent(field: WheelField, number: Int): WheelDateTime = when (field) {
    WheelField.Year -> copy(year = number)
    WheelField.Month -> copy(month = number)
    WheelField.Day -> copy(day = number)
    WheelField.Hour -> copy(hour = number)
    WheelField.Minute -> copy(minute = number)
    WheelField.Second -> copy(second = number)
}

private fun WheelField.format(number: Int, locale: Locale): String = when (this) {
    WheelField.Year -> String.format(locale, "%d", number)
    else -> String.format(locale, "%02d", number)
}

@FormFactorPreviews
@Composable
private fun CogniBalWheelDateTimePickerPreview() {
    CogniBalTheme {
        Surface {
            var time by remember { mutableStateOf(WheelDateTime.of(2026, 10, 19, 21, 0)) }
            CogniBalWheelDateTimePicker(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                value = time,
                onValueChange = { time = it },
                minTime = WheelDateTime.of(2020, 1, 1, 0, 0),
                maxTime = WheelDateTime.of(2030, 12, 31, 23, 59),
            )
        }
    }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun CogniBalWheelDateTimePickerDarkPreview() {
    CogniBalTheme(darkTheme = true) {
        Surface {
            CogniBalWheelDateTimePicker(
                modifier = Modifier.padding(16.dp),
                value = WheelDateTime.of(2026, 2, 28, 9, 30),
                onValueChange = {},
                mode = WheelDateTimeMode.Date,
                config = WheelPickerConfig(selectionStyle = WheelSelectionStyle.HighlightBackground),
            )
        }
    }
}
