package com.example.cognibal.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 小组件接收器必须保持启用，桌面长按图标才能列出服务卡片。
 *
 * 以前的版本会把它关掉。启动时如果发现还是关的，重新打开。
 */
internal object WidgetAvailability {
    fun ensureEnabled(context: Context) {
        val app = context.applicationContext
        val component = ComponentName(app, LedgerWidgetReceiver::class.java)
        val state = app.packageManager.getComponentEnabledSetting(component)
        if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) return
        app.packageManager.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
    }
}

internal enum class WidgetPinResult {
    Requested,
    Unsupported,
    Rejected,
}

/**
 * 请系统把已声明的微件固定到主屏幕。确认界面由启动器弹出，这里不自己做菜单项。
 *
 * 不需要知道新微件的 id，所以成功回调传 null。见微件可检测性文档。
 */
internal object WidgetPinner {
    fun request(context: Context): WidgetPinResult {
        val activity = context.findActivity() ?: return WidgetPinResult.Rejected
        WidgetAvailability.ensureEnabled(activity)
        val manager = AppWidgetManager.getInstance(activity)
        if (!manager.isRequestPinAppWidgetSupported) return WidgetPinResult.Unsupported
        val provider = ComponentName(activity, LedgerWidgetReceiver::class.java)
        return try {
            val accepted = manager.requestPinAppWidget(provider, null, null)
            if (accepted) WidgetPinResult.Requested else WidgetPinResult.Rejected
        } catch (_: RuntimeException) {
            WidgetPinResult.Rejected
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/**
 * 账本或开关变化时刷新已经放在桌面上的小组件。
 *
 * 进程起来时只对齐接收器的启用状态。桌面上确实有小组件，才订阅账本，避免空开着数据库。
 */
internal object HomeWidgetRefresh {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val gate = Mutex()
    private var watch: Job? = null

    fun sync(context: Context) {
        val app = context.applicationContext
        scope.launch {
            WidgetAvailability.ensureEnabled(app)
            if (hasPlacedWidgets(app)) startWatching(app)
        }
    }

    fun ensureStarted(context: Context) {
        val app = context.applicationContext
        scope.launch { startWatching(app) }
    }

    fun stop() {
        scope.launch { stopWatching() }
    }

    /** 开关关掉后立刻把已经放在桌面上的卡片收成“未开启”，不等下一次账本变化。 */
    fun refreshNow(context: Context) {
        val app = context.applicationContext
        scope.launch {
            try {
                LedgerAppWidget().updateAll(app)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // 桌面上还没有卡片时，刷新失败不用提示。
            }
        }
    }

    private suspend fun startWatching(app: Context) {
        gate.withLock {
            if (watch?.isActive == true) return
            watch = scope.launch { watch(app) }
        }
    }

    private suspend fun stopWatching() {
        gate.withLock {
            watch?.cancel()
            watch = null
        }
    }

    @OptIn(FlowPreview::class)
    private suspend fun watch(app: Context) {
        try {
            LedgerWidgetLoader.observeContent(app)
                .debounce(400.milliseconds)
                .collect { content ->
                LedgerAppWidget().updateAll(app)
                }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            // 订阅断了之后，系统仍会按小组件的更新周期再拉一次。
        }
    }
}

private suspend fun hasPlacedWidgets(context: Context): Boolean {
    return try {
        GlanceAppWidgetManager(context).getGlanceIds(LedgerAppWidget::class.java).isNotEmpty()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        false
    }
}
