package com.example.cognibal.screen.feature.settings.appearance

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.SystemClock
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.ComponentDialog
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import com.example.cognibal.R
import com.example.cognibal.locale.AppLanguage
import com.example.cognibal.ui.background.currentPageBackgroundColor
import com.example.cognibal.ui.theme.appColors
import android.util.Xml
import java.io.FileNotFoundException
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser

/**
 * 语言列表。名字是各语言自己的写法，不随当前界面语言翻译。
 * 当前项用主题色卡片标出，不再使用单选按钮。
 */
@Composable
internal fun LanguageSelectDialog(
    selectedTag: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.language),
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.background)
                    .verticalScroll(rememberScrollState())
                    .selectableGroup()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                LanguageOptionRow(
                    label = stringResource(R.string.language_follow_system),
                    selected = selectedTag == null,
                    onClick = {
                        if (selectedTag == null) onDismiss() else onSelect(null)
                    },
                )
                AppLanguage.entries.forEach { language ->
                    val selected = language.matches(selectedTag)
                    LanguageOptionRow(
                        label = stringResource(language.nameRes),
                        selected = selected,
                        onClick = {
                            if (selected) onDismiss() else onSelect(language.languageTag)
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        containerColor = colors.cardBackground,
        shape = RoundedCornerShape(20.dp),
    )
}

@Composable
private fun LanguageOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val shape = RoundedCornerShape(12.dp)
    val background by animateColorAsState(
        targetValue = if (selected) colors.primary else Color.Transparent,
        animationSpec = tween(durationMillis = 180),
        label = "languageRowBackground",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) colors.onPrimary else colors.textPrimary,
        animationSpec = tween(durationMillis = 180),
        label = "languageRowText",
    )
    Text(
        text = label,
        color = textColor,
        fontSize = 15.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(shape)
            .background(background, shape)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.RadioButton
                this.selected = selected
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
    )
}

/**
 * 切换前的确认。确认之后才进入全屏倒计时。
 */
@Composable
internal fun LanguageSwitchConfirmDialog(
    languageName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.language_switch_title),
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.language_switch_message, languageName),
                color = colors.textSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.language_switch_confirm),
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = colors.textTertiary,
                )
            }
        },
        containerColor = colors.cardBackground,
        shape = RoundedCornerShape(20.dp),
    )
}

/**
 * 全屏盖层。只放一个循环动画，不写说明文字。
 *
 * 盖层淡到不透明之后才关语言弹窗并写入语言。写入已经落到本地、
 * 并且当前界面配置已经换成新语言之后，再多停 2 秒。整段至少 3 秒才淡出。
 */
@Composable
internal fun LanguageSwitchCover(
    targetTag: String?,
    dialogsDismissed: Boolean,
    onCoverShown: () -> Unit,
    onApply: () -> Unit,
    onFinished: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val coverColor = currentPageBackgroundColor()
    val context = LocalContext.current
    val configTag = LocalConfiguration.current.primaryLanguageTag()
    val initialTag = remember { configTag }
    val latestConfigTag by rememberUpdatedState(configTag)
    val latestDialogsDismissed by rememberUpdatedState(dialogsDismissed)
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(targetTag) {
        val startedAt = SystemClock.elapsedRealtime()
        alpha.animateTo(1f, tween(COVER_FADE_MILLIS))
        // 盖层已经不透明，这时拆掉确认框和语言列表，用户看不到它们退出。
        onCoverShown()
        while (!latestDialogsDismissed) {
            delay(FRAME_MILLIS)
        }
        // 再等一帧，让弹窗窗口真正 detach，再改语言。
        withFrameNanos { }
        onApply()
        while (!isLanguagePersisted(context, targetTag)) {
            delay(FRAME_MILLIS)
        }
        while (!isLanguageRefreshComplete(targetTag, latestConfigTag, initialTag)) {
            delay(FRAME_MILLIS)
        }
        // 新语言先画进被盖住的界面。更新完成后再停 2 秒，并且整段不少于 3 秒。
        withFrameNanos { }
        withFrameNanos { }
        val elapsed = SystemClock.elapsedRealtime() - startedAt
        delay(max(MIN_COVER_MILLIS - elapsed, POST_UPDATE_DELAY_MILLIS))
        alpha.animateTo(0f, tween(COVER_FADE_MILLIS))
        onFinished()
    }

    OpaqueFullScreenDialog(lightSystemBars = !colors.isDark) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { this.alpha = alpha.value }
                .background(coverColor),
            contentAlignment = Alignment.Center,
        ) {
            LanguageSwitchIndicator(color = colors.primary)
        }
    }
}

/** 盖层中央的环形动画。没有文字，颜色跟当前主题走。 */
@Composable
private fun LanguageSwitchIndicator(color: Color) {
    val transition = rememberInfiniteTransition(label = "languageSwitch")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
        ),
        label = "languageSwitchRotation",
    )
    val sweep by transition.animateFloat(
        initialValue = 48f,
        targetValue = 270f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "languageSwitchSweep",
    )
    Canvas(Modifier.size(48.dp)) {
        val strokeWidth = 3.dp.toPx()
        val inset = strokeWidth / 2f
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val topLeft = Offset(inset, inset)
        drawArc(
            color = color.copy(alpha = 0.16f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke,
        )
        drawArc(
            color = color,
            startAngle = rotation,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke,
        )
    }
}

/**
 * 透明全屏窗口。底色由内容自己画，才能跟着主题淡入淡出，而不是先闪一块黑。
 * 返回键在这里被吃掉，盖层还在时不能关掉。
 */
@Composable
private fun OpaqueFullScreenDialog(
    lightSystemBars: Boolean,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val composition = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)
    val dialog = remember(context) {
        ComponentDialog(context, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen).apply {
            val window = window ?: return@apply
            window.setWindowAnimations(0)
            window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            window.setDimAmount(0f)
            window.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
            )
            WindowCompat.setDecorFitsSystemWindows(window, false)
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }
    }
    DisposableEffect(dialog) {
        val composeView = ComposeView(dialog.context).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setParentCompositionContext(composition)
            setContent {
                SideEffect {
                    dialog.window?.let { window ->
                        WindowCompat.getInsetsController(window, this@apply).apply {
                            isAppearanceLightStatusBars = lightSystemBars
                            isAppearanceLightNavigationBars = lightSystemBars
                        }
                    }
                }
                currentContent()
            }
        }
        dialog.setContentView(
            composeView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        val backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        }
        dialog.onBackPressedDispatcher.addCallback(backCallback)
        dialog.show()
        onDispose {
            backCallback.remove()
            dialog.dismiss()
            composeView.disposeComposition()
        }
    }
}

/**
 * 请求的语言是否已经写入，并且能再读回来。
 *
 * API 33 起由系统 LocaleManager 保存，[AppCompatDelegate.getApplicationLocales] 读的就是这份。
 * 更低版本由 AppCompat 在后台线程写进应用私有文件，内存里的列表会先变，文件后变。
 */
private suspend fun isLanguagePersisted(context: Context, targetTag: String?): Boolean {
    if (!applicationLocalesMatch(targetTag)) return false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return true
    val storedTags = withContext(Dispatchers.IO) {
        readPersistedLanguageTags(context)
    }
    return storedLanguageMatches(storedTags, targetTag)
}

/** 系统或 AppCompat 内存里的语言是否已经是这次要切换的值。 */
private fun applicationLocalesMatch(targetTag: String?): Boolean {
    return localesMatch(AppCompatDelegate.getApplicationLocales(), targetTag)
}

/** 本地文件里的语言是否已经是这次要切换的值。空文件表示跟随系统。 */
private fun storedLanguageMatches(storedTags: String, targetTag: String?): Boolean {
    return localesMatch(LocaleListCompat.forLanguageTags(storedTags), targetTag)
}

private fun localesMatch(locales: LocaleListCompat, targetTag: String?): Boolean {
    if (targetTag == null) return locales.isEmpty
    val target = AppLanguage.fromTag(targetTag) ?: return false
    val appliedTag = locales[0]?.toLanguageTag() ?: return false
    return target.matches(appliedTag)
}

/**
 * 读 AppCompat 自动保存的语言文件。文件还不存在表示当前是跟随系统。
 * 写入过程中文件可能不完整，读失败就当还没保存，等下一轮再读。
 */
private fun readPersistedLanguageTags(context: Context): String {
    return try {
        context.applicationContext.openFileInput(LOCALE_RECORD_FILE).use { input ->
            val parser = Xml.newPullParser()
            parser.setInput(input, "UTF-8")
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "locales") {
                    return parser.getAttributeValue(null, "application_locales").orEmpty()
                }
            }
            ""
        }
    } catch (_: FileNotFoundException) {
        ""
    } catch (_: Exception) {
        ""
    }
}

/**
 * 请求的语言是否已经反映到当前界面配置。
 *
 * 应用内语言列表可能先于资源配置更新。两者一致，或配置已经相对盖层出现时发生过变化，才算刷新成功。
 */
private fun isLanguageRefreshComplete(
    targetTag: String?,
    configTag: String,
    initialTag: String,
): Boolean {
    val configurationChanged = !configTag.equals(initialTag, ignoreCase = true)
    val appLocales = AppCompatDelegate.getApplicationLocales()
    if (targetTag == null) {
        if (!appLocales.isEmpty) return false
        val systemTag = Resources.getSystem().configuration.primaryLanguageTag()
        return configTag.equals(systemTag, ignoreCase = true) || configurationChanged
    }
    val target = AppLanguage.fromTag(targetTag) ?: return false
    val appTag = appLocales[0]?.toLanguageTag() ?: return false
    if (!target.matches(appTag)) return false
    return target.matches(configTag) || configurationChanged
}

private fun Configuration.primaryLanguageTag(): String {
    val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        locales[0]
    } else {
        @Suppress("DEPRECATION")
        locale
    }
    return locale?.toLanguageTag().orEmpty()
}

private const val COVER_FADE_MILLIS = 280
private const val FRAME_MILLIS = 50L
private const val MIN_COVER_MILLIS = 3_000L
private const val POST_UPDATE_DELAY_MILLIS = 2_000L

/** 与 AppCompat `AppLocalesStorageHelper` 使用的文件名一致。 */
private const val LOCALE_RECORD_FILE =
    "androidx.appcompat.app.AppCompatDelegate.application_locales_record_file"

/**
 * 当前应用内语言。空表示跟随系统。
 *
 * 预览没有应用进程，读 [AppCompatDelegate] 会落到空列表。
 * 读取 [LocalConfiguration]，语言配置变更后即使 Activity 不重建也会重组。
 */
@Composable
internal fun currentApplicationLanguageTag(): String? {
    if (LocalInspectionMode.current) return null
    LocalConfiguration.current
    val locales = AppCompatDelegate.getApplicationLocales()
    if (locales.isEmpty) return null
    return locales[0]?.toLanguageTag()
}
