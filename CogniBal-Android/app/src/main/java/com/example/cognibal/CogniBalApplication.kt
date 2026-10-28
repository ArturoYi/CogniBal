package com.example.cognibal

import android.app.Application
import android.os.Handler
import android.os.Looper
import com.example.cognibal.widget.HomeWidgetRefresh
import com.example.cognibal.widget.WidgetAvailability
import dagger.hilt.android.HiltAndroidApp

/**
 * Hilt 入口。账单库在第一次注入 [com.example.cognibal.data.database.AppDatabase] 时才打开，
 * 这里不主动建表，避免启动页还在播时就碰磁盘。
 *
 * 桌面小组件的开关推迟到首帧之后再对齐。只有桌面上已经有小组件时才订阅账本。
 */
@HiltAndroidApp
class CogniBalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 旧版本把小组件接收器关掉过。这个状态会留在系统里，不先打开，桌面就列不出卡片。
        WidgetAvailability.ensureEnabled(this)
        Handler(Looper.getMainLooper()).post {
            HomeWidgetRefresh.sync(this)
        }
    }
}
