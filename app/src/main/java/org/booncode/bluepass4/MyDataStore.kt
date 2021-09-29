package org.booncode.bluepass4

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

val Context.myDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "preferences"
)

val PREF_REGEX_SENDER = stringPreferencesKey("regex-sender")
val PREF_REGEX_MESSAGE = stringPreferencesKey("regex-message")
val PREF_BLUETOOTH_ADDRESS = stringPreferencesKey("bt-address")
val PREF_BLUETOOTH_NAME = stringPreferencesKey("bt-name")
val PREF_SHOWCASE = intPreferencesKey("tmp-showcase")

data class MsgFilterParams(
    val pattern_sender: Pattern? = null,
    val pattern_message: Pattern? = null,
)

data class MsgFilterText(
    var sender_regex: String? = null,
    var message_regex: String? = null
)

data class BtDeviceParams(
    val address: String? = null,
    val name: String? = null,
)

class MyDataStore(private val _context: Context) {
    val msgFilterParams: Flow<MsgFilterParams> = _context.myDataStore.data.map {
        MsgFilterParams(
            pattern_sender = Companion.tryCompilePattern(it[PREF_REGEX_SENDER]),
            pattern_message = Companion.tryCompilePattern(it[PREF_REGEX_MESSAGE]),
        )
    }

    val msgFilterText: Flow<MsgFilterText> = _context.myDataStore.data.map {
        MsgFilterText(
            sender_regex = it[PREF_REGEX_SENDER],
            message_regex = it[PREF_REGEX_MESSAGE]
        )
    }

    val senderFilter: Flow<String> = _context.myDataStore.data.map {
        it[PREF_REGEX_SENDER] ?: ""
    }

    val messageFilter: Flow<String> = _context.myDataStore.data.map {
        it[PREF_REGEX_MESSAGE] ?: ""
    }

    val msgFilterParamsBlocking = fetchBlocking(msgFilterParams)

    suspend fun setMsgFilterParams(sender_regex: String, message_regex: String) {
        _context.myDataStore.edit {
            setCompileableString(sender_regex, it, PREF_REGEX_SENDER)
            setCompileableString(message_regex, it, PREF_REGEX_MESSAGE)
        }
    }

    suspend fun setMsgFilterSender(pattern: String) {
        _context.myDataStore.edit {
            setCompileableString(pattern, it, PREF_REGEX_SENDER)
        }
    }

    suspend fun setMsgFilterMessage(pattern: String) {
        _context.myDataStore.edit {
            setCompileableString(pattern, it, PREF_REGEX_MESSAGE)
        }
    }

    fun setCompileableString(
        pattern: String,
        pref: MutablePreferences,
        key: Preferences.Key<String>,
        isValid: ((Pattern) -> Boolean)? = null
    ) {
        val rx = Companion.tryCompilePattern(pattern)
        if (rx != null) {
            if ((isValid == null) || isValid(rx)) {
                pref[key] = pattern
            }
        }
    }

    val btDeviceParams: Flow<BtDeviceParams> = _context.myDataStore.data.map {
        BtDeviceParams(
            address = it[PREF_BLUETOOTH_ADDRESS],
            name = it[PREF_BLUETOOTH_NAME]
        )
    }

    val btDeviceParamsBlocking = fetchBlocking(btDeviceParams)

    suspend fun setBtDeviceParams(address: String, name: String?) {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            Log.w(TAG, "Ignoring invalid bluetooth address: '$address'")
            return
        }

        _context.myDataStore.edit {
            it[PREF_BLUETOOTH_ADDRESS] = address
            it[PREF_BLUETOOTH_NAME] = name ?: ""
        }
    }

    private fun <T> fetchBlocking(f: Flow<T>): T {
        return runBlocking {
            f.first()
        }
    }

    companion object {
        internal val TAG = "MyDataStore"
        //internal val RE_BT_ADDRESS = Pattern.compile("^((0-9a-fA-F){2}:?){6}\$")

        fun tryCompilePattern(pattern: String?): Pattern? {
            return if (pattern == null) {
                null
            } else {
                try {
                    Pattern.compile(pattern)
                } catch (e: PatternSyntaxException) {
                    Log.w(TAG, "Failed to compile pattern '$pattern': $e")
                    null
                }
            }
        }
    }
}

