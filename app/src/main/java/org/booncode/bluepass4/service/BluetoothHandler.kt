package org.booncode.bluepass4.service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.os.Message
import android.util.Log
import java.io.IOException
import java.lang.Exception
import java.util.*

class BluetoothHandler(
    private val _adapter: BluetoothAdapter,
    private val _handler: Handler
) {
    private var socketHandler: SocketHandler? = null
    private var socketThread: Thread? = null

    @Synchronized
    fun stop() {
        Log.d(TAG, "Requested stopping the thread")
        if (socketThread != null) {
            socketHandler?.cancel()
            socketThread?.interrupt()
            socketThread = null
        }
    }

    @Synchronized
    fun sendCode(address: String, code: String, retries: Int) {
        stop()
        val dev = _adapter.getRemoteDevice(address)
        socketHandler = SocketHandler(dev, code, _handler, retries)
        socketThread = Thread(socketHandler).apply {
            name = "BtThread"
            start()
        }
    }

    @Synchronized
    fun probePairing(address: String) {
        Thread(object : Runnable {
            override fun run() {
                if (probeConnectionStatic(_adapter, address)) {
                    Log.i(TAG, "Socket connection to $address suceeded")
                } else {
                    Log.i(TAG, "Socket connection to $address failed")
                }
            }
        }).apply {
            name = "BtPairThread"
            start()
        }
    }

    inner class SocketHandler(
        private val btDevice: BluetoothDevice,
        private val code: String,
        private val handler: Handler,
        private val retries: Int
    ) : Runnable {

        private var socket: BluetoothSocket? = null
        private var _running: Boolean = true

        override fun run() {
            val retCode = try {
                val succeeded = runForResult()
                if (succeeded) {
                    BlueService.SENDING_CODE_OK
                } else {
                    BlueService.SENDING_CODE_FAILED
                }
            } catch (e: AbortException) {
                Log.i(TAG, "Aborting bluetooth sending thread")
                BlueService.SEND_THREAD_ABORTED
            } catch (e: InterruptedException) {
                Log.i(TAG, "Interrupted bluetooth sending thread")
                BlueService.SEND_THREAD_ABORTED
            }

            val tid = Thread.currentThread().id
            Log.d(TAG, "Posting status $retCode for sending code '$code' (tid=$tid)")

            val msg = Message()
            msg.what = retCode
            msg.data.putString(BlueService.MSG_CODE, code)
            msg.data.putInt(BlueService.MSG_RETRIES, retries)
            handler.sendMessage(msg)
        }

        private fun createSocket(): BluetoothSocket {
            try {
                return btDevice.createRfcommSocketToServiceRecord(MY_UUID)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to create rfcomm socket: $e")
                throw e
            }
        }

        @Synchronized
        private fun tryConnect(): Boolean {
            throwIfNotRunning()
            socket = connectBtSocket(btDevice)
            throwIfNotRunning()
            return (socket != null)
        }

        private fun retryConnect(): Boolean {
            for (i in 0..MAX_CONNECT_RETRIES) {
                if (tryConnect()) {
                    Log.d(TAG, "Socket is connected (attempt=$i)")
                    return true
                }
                Thread.sleep(CONNECT_TIMEOUT_MS)
            }
            Log.e(TAG, "Failed to connect after $MAX_CONNECT_RETRIES attempts")
            return false
        }

        @Synchronized
        private fun getSocketStream() = socket?.outputStream

        private fun tryWriteCode(): Boolean {
            val ret = try {
                val data = "${code}\r\n".toByteArray()
                val out = getSocketStream()
                out?.write(data)
                out?.flush()
                Log.d(TAG, "Code '${code} has been written")
                true
            } catch (e: IOException) {
                Log.e(TAG, "Failed to send code to remote device: $e")
                false
            }
            throwIfNotRunning()
            return ret
        }

        private fun runForResult(): Boolean {
            Log.d(TAG, "Starting bluetooth handler thread")

            if (!retryConnect()) {
                return false
            }

            val writeSucceeded = tryWriteCode()

            cancel()
            Log.d(TAG, "Stopping thread (ret=$writeSucceeded)")
            return writeSucceeded
        }

        @Synchronized
        private fun throwIfNotRunning() {
            if (!_running) {
                Log.w(TAG, "Thread was requested to be aborted")
                throw AbortException()
            }
        }

        @Synchronized
        fun cancel() {
            try {
                socket?.close()  // Break socket
                socket = null
                _running = false
            } catch (e: IOException) {
                Log.w(TAG, "Failed to close bluetooth socket: $e")
            }
        }
    }

    inner class AbortException: Exception() {
    }

    companion object {
        private const val _UUID_STR = "e4d56fb3-b86d-4572-9b0d-44d483eb1eee"
        val MY_UUID: UUID = UUID.fromString(_UUID_STR)
        const val TAG = "BluetoothHandler"
        const val MAX_CONNECT_RETRIES = 5
        const val CONNECT_TIMEOUT_MS: Long = 1000

        private fun createBtSocket(device: BluetoothDevice): BluetoothSocket? {
            try {
                return device.createRfcommSocketToServiceRecord(MY_UUID)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to create rfcomm socket: $e")
                return null
            }
        }

        private fun connectBtSocket(device: BluetoothDevice): BluetoothSocket? {
            val socket = createBtSocket(device)
            return try {
                socket?.connect()
                socket
            } catch (e: IOException) {
                Log.d(TAG, "tryConnect: Failed to connect: $e")
                null
            }
        }

        fun probeConnectionStatic(adapter: BluetoothAdapter, address: String): Boolean {
            Log.d(TAG, "Start probing $address")
            val dev = adapter.getRemoteDevice(address)
            val socket = connectBtSocket(dev)
            return if (socket != null) {
                socket.close()
                Log.d(TAG, "Probing succeeded")
                true
            } else {
                Log.d(TAG, "Probing failed")
                false
            }
        }
    }
}