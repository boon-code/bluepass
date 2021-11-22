package org.booncode.bluepass4.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import org.booncode.bluepass4.MainActivity
import org.booncode.bluepass4.MyDataStore
import org.booncode.bluepass4.R
import org.booncode.bluepass4.newPartialWakeLock

class BlueService : Service() {

    private var _last_code: String = ""
    private var _handler: BluetoothHandler? = null
    private var _notify_builder: NotificationCompat.Builder? = null
    // Message callback:
    private val _lock = object {}
    private var _cb_handler: Handler? = null
    private var _wake_lock: PowerManager.WakeLock? = null

    private val thisThreadId get() = Thread.currentThread().id

    override fun onBind(intent: Intent): IBinder? {
        return null /* not needed */
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate() tid=$thisThreadId")
        super.onCreate()
        createWakeLock()
        createMessageHandler()
        createBluetoothHandler()
        this.startInForeground()
    }

    private fun createWakeLock() {
        synchronized(_lock) {
            _wake_lock = newPartialWakeLock(this, TAG)
        }
    }

    private fun createMessageHandler() {
        val context = this as Context
        _cb_handler = object : Handler(this.mainLooper) {
            override fun handleMessage(msg: Message) {
                Log.d(TAG, "Running handler from tid=$thisThreadId")
                Log.d(TAG, "Releasing wake lock")
                val code = msg.data.getString(MSG_CODE)
                val retries = msg.data.getInt(MSG_RETRIES)
                val address = MyDataStore(context).btDeviceParamsBlocking.address
                when (msg.what) {
                    SENDING_CODE_OK -> {
                        Log.d(TAG, "Sending code '$code'")
                        updateNotify(R.string.notify_title_with_code_ok, code ?: "")
                    }
                    SENDING_CODE_FAILED -> {
                        Log.w(TAG, "Failed to send code '$code' to bluetooth device")
                        if ((retries > 0) && (address != null) && (code != null)) {
                            updateNotify(
                                R.string.notify_title_with_code_failed_retry,
                                code,
                                (MAX_RETRIES - retries + 1).toString(),
                                MAX_RETRIES.toString()
                            )
                            sendCode(address, code, retries - 1)
                        } else {
                            updateNotify(R.string.notify_title_with_code_failed, code ?: "")
                        }
                    }
                    SEND_THREAD_ABORTED -> Log.w(TAG, "Thread has been aborted")
                    else -> Log.e(TAG, "Illegal msg.what ${msg.what}")
                }
                synchronized(_lock) {
                    _wake_lock?.release()
                }
            }
        }
    }

    private fun createBluetoothHandler() {
        val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = manager.adapter
        if (adapter != null) {
            _handler = BluetoothHandler(adapter, _cb_handler!!)
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

        val retry_intent: PendingIntent =
            Intent(this, BlueService::class.java).let {
                it.putExtra(INTENT_COMMAND, CMD_RETRY_SEND)
                PendingIntent.getService(this, CMD_RETRY_SEND, it, 0)
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
            .addAction(android.R.drawable.ic_media_previous, getString(R.string.notify_retry), retry_intent)
            .setSmallIcon(R.drawable.ic_bluepass_key_notify)

        val notification = _notify_builder!!.build()

        // Notification ID cannot be 0.
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        _wake_lock?.acquire(WAKE_TIMEOUT_MS)
        val cmd = intent?.extras?.getInt(INTENT_COMMAND)

        Log.d(TAG, "onStartCommand(${cmd ?: "<null>"})")
        super.onStartCommand(intent, flags, startId)

        when (cmd) {
            CMD_PUSH_CODE -> startPushCodeFromIntent(intent)
            CMD_STOP -> stopThisService()
            CMD_COPY_LAST_CODE -> copyLastCode()
            CMD_RETRY_SEND -> retryPushCode()
            CMD_PAIR_BACKGROUND -> startPairInBackground(intent)
            else -> {
                Log.d(TAG, "No command set -> ignore")
            }
        }
        _wake_lock?.release()

        return START_NOT_STICKY
    }

    private fun startPushCodeFromIntent(intent: Intent?) {
        val code = intent?.extras?.getString("code")
        if (code == null) {
            Log.w(TAG, "Service started without code")
            return
        }
        startPushCode(code)
    }

    private fun retryPushCode() {
        if (_last_code.isEmpty()) {
            Log.w(TAG, "No last code has been received before")
            return
        }
        startPushCode(_last_code)
    }

    private fun startPushCode(code: String) {
        Log.v(TAG, "startPushCode()")
        _last_code = code
        updateNotify(R.string.notify_title_with_code_pending, code)

        val params = MyDataStore(this).btDeviceParamsBlocking
        if (params.address == null) {
            Log.d(TAG, "Bluetooth address not configured -> skip sending")
            return
        }

        sendCode(params.address, code)
    }

    private fun sendCode(address: String, code: String, retries: Int = MAX_RETRIES) {
        if (_handler != null) {
            Log.d(TAG, "Defer work to background thread")
            _wake_lock?.acquire(WAKE_TIMEOUT_MS)
            _handler?.sendCode(address, code, retries)
            Log.d(TAG, "Services started with code='$code', address=${address}")
        } else {
            Log.e(TAG, "Handler not set")
        }
    }

    private fun updateNotify(template: Int, vararg args: String) {
        val title = getString(template).format(*args)
        _notify_builder?.let {
            val man = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            Log.d(TAG, "Update notification: $title")
            val notification = it.setContentTitle(title).build()
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
        _handler?.stop()
        stopForeground(true)
        stopSelf()
    }

    private fun startPairInBackground(intent: Intent?) {
        Log.v(TAG, "startPairInBackground()")
        val address = intent?.extras?.getString(INTENT_ADDRESS)
        if (address != null) {
            Log.d(TAG, "Probe a connection to $address to start pairing process")
            updateNotify(R.string.notify_title)
            _handler?.probePairing(address)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        _handler?.stop()
        super.onDestroy()
    }

    companion object {
        const val INTENT_COMMAND = "command"
        const val INTENT_CODE = "code"
        const val INTENT_ADDRESS = "address"

        // command codes
        const val CMD_PUSH_CODE = 1
        const val CMD_STOP = 2
        const val CMD_COPY_LAST_CODE = 3
        const val CMD_RETRY_SEND = 4
        const val CMD_PAIR_BACKGROUND = 5

        // result codes
        const val SENDING_CODE_OK = 0
        const val SENDING_CODE_FAILED = 1
        const val SEND_THREAD_ABORTED = 2
        const val MSG_CODE = "code"
        const val MSG_RETRIES = "retries"
        private const val TAG = "BlueService"
        private const val WAKE_TIMEOUT_MS = 2 * 60 * 1000L  // 2 minute
        private const val CHANNEL_ID = "BlueService.notify"
        private const val CHANNEL_NAME = "BlueService channel"
        private const val NOTIFICATION_ID = 1
        private const val MAX_RETRIES = 10
    }
}