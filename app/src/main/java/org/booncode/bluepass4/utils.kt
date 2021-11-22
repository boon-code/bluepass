package org.booncode.bluepass4

import android.content.Context
import android.os.PowerManager
import java.util.regex.Pattern

fun newPartialWakeLock(context: Context, tag: String): PowerManager.WakeLock {
    val APP_TAG = "bluepass"
    return (context.getSystemService(Context.POWER_SERVICE) as PowerManager).run {
        newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "${APP_TAG}:${tag}")
    }
}

data class ParseCodeResult(val code: String?, val error: String)

fun parseCode(pattern: Pattern?, text: CharSequence): ParseCodeResult {
    return if (pattern != null) {
        val m = pattern.matcher(text)
        if (!m.matches()) {
            ParseCodeResult(null, "regular expression doesn't match")
        } else if (m.groupCount() >= 1) {
            val number = try {
                m.group(1)
            } catch (e: Exception) {
                null
            }
            if (number != null) {
                ParseCodeResult(number, "")
            } else {
                ParseCodeResult("", "")
            }
        } else {
            ParseCodeResult(null, "invalid number of groups: ${m.groupCount()}")
        }
    } else {
        ParseCodeResult(null, "invalid regular expression")
    }
}