package com.example.cognibal

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Hilt 入口。账单库在第一次注入 [com.example.cognibal.data.database.AppDatabase] 时才打开，
 * 这里不主动建表，避免启动页还在播时就碰磁盘。
 */
@HiltAndroidApp
class CogniBalApplication : Application()
