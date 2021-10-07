package org.booncode.bluepass4.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import org.booncode.bluepass4.MainActivity
import org.booncode.bluepass4.MyDataStore
import org.booncode.bluepass4.R

class BlueService : Service() {

    private var _last_code: String = ""
    private var _handler: BluetoothHandler? = null
    private var _notify_builder: NotificationCompat.Builder? = null

    override fun onBind(intent: Intent): IBinder? {
        return null /* not needed */
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate()")
        super.onCreate()
        createHandler()
        this.startInForeground()
    }

    fun createHandler() {
        val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = manager.adapter
        if (adapter != null) {
            _handler = BluetoothHandler(adapter)
        } else {
            _handler = null
            Log.w(TAG, "No bluetooth adapter available")
        }
    }

    private fun startInForeground() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }

        val stop_intent: PendingIntent =
            Intent(this, BlueService::class.java).let {
                it.putExtra(INTENT_COMMAND, CMD_STOP)
                PendingIntent.getService(this, CMD_STOP, it, 0)
            }

        val copy_intent: PendingIntent =
            Intent(this, BlueService::class.java).let {
                it.putExtra(INTENT_COMMAND, CMD_COPY_LAST_CODE)
                PendingIntent.getService(this, CMD_COPY_LAST_CODE, it, 0)
            }

        _notify_builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getText(R.string.notify_title))
            .setContentText(getText(R.string.notify_text))
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getText(R.string.notify_stop),
                stop_intent
            )
            .addAction(android.R.drawable.ic_input_get, getText(R.string.notify_copy), copy_intent)
            .setSmallIcon(R.drawable.ic_bluepass_key)

        val notification = _notify_builder!!.build()

        // Notification ID cannot be 0.
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val cmd = intent?.extras?.getInt(INTENT_COMMAND)

        Log.d(TAG, "onStartCommand(${cmd ?: "<null>"})")
        super.onStartCommand(intent, flags, startId)

        when (cmd) {
            CMD_PUSH_CODE -> startPushCode(intent)
            CMD_STOP -> stopThisService()
            CMD_COPY_LAST_CODE -> copyLastCode()
            else -> {
                Log.d(TAG, "No command set -> ignore")
            }
        }

        return START_NOT_STICKY
    }

    private fun startPushCode(intent: Intent?) {
        Log.v(TAG, "startPushCode()")
        val code = intent?.extras?.getString("code")
        if (code == null) {
            Log.w(TAG, "Service started without code")
            return
        }
        _last_code = code
        updateNotify(code)

        val params = MyDataStore(this).btDeviceParamsBlocking
        if (params.address == null) {
            Log.d(TAG, "Bluetooth address not configured -> skip sending")
            return
        }

        Log.d(TAG, "Defer work to background thread")
        _handler?.sendCode(params.address, code)
        Log.d(TAG, "Services started with code='$code', address=${params.address}")
    }

    private fun updateNotify(code: String) {
        _notify_builder?.let {
            val man = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification =
                it.setContentTitle(getString(R.string.notify_title_with_code).format(code)).build()
            man.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun copyLastCode() {
        Log.v(TAG, "copyLastCode()")
        if (_last_code.isEmpty()) {
            Log.v(TAG, "Skip copy to clipboard -> no code received so far")
            return
        }

        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip: ClipData = ClipData.newPlainText("code", _last_code)
        clipboard.setPrimaryClip(clip)
        Log.d(TAG, "Copyied code $_last_code")
    }

    private fun stopThisService() {
        Log.v(TAG, "stopThisService()")
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        super.onDestroy()
    }

    companion object {
        const val PREF_TAG = "BlueService"
        const val INTENT_COMMAND = "command"
        const val INTENT_CODE = "code"
        const val CMD_PUSH_CODE = 1
        const val CMD_STOP = 2
        const val CMD_COPY_LAST_CODE = 3
        private const val TAG = "BlueService"
        private const val CHANNEL_ID = "BlueService.notify"
        private const val CHANNEL_NAME = "BlueService channel"
        private const val NOTIFICATION_ID = 1
    }
}