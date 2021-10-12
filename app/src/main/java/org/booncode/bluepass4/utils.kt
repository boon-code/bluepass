package org.booncode.bluepass4

import android.content.Context
import android.os.PowerManager

fun newPartialWakeLock(context: Context, tag: String): PowerManager.WakeLock {
    val APP_TAG = "bluepass"
    return (context.getSystemService(Context.POWER_SERVICE) as PowerManager).run {
        newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "${APP_TAG}:${tag}")
    }
}