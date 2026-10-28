package com.example.cognibal.ui.process

import android.content.Context
import android.content.Intent
import kotlin.system.exitProcess

/** 清掉当前任务并重新进入应用，用来丢掉内存里的旧账本。 */
internal fun Context.reopenApp() {
    val launch = packageManager.getLaunchIntentForPackage(packageName) ?: return
    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    startActivity(launch)
    exitProcess(0)
}
