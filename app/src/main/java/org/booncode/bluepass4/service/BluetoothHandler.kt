package org.booncode.bluepass4.service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import androidx.compose.runtime.Composable
import java.io.IOException
import java.io.OutputStream
import java.util.*

class BluetoothHandler(private val _adapter: BluetoothAdapter) {
    private var socketHandler: SocketHandler? = null
    private var socketThread: Thread? = null

    @Synchronized
    fun stop() {
        if (socketThread != null) {
            socketHandler?.cancel()
            socketThread = null
        }
    }

    @Synchronized
    fun sendCode(address: String, code: String) {
        stop()
        val dev = _adapter.getRemoteDevice(address)
        var socket: BluetoothSocket? = null
        try {
            socket = dev.createRfcommSocketToServiceRecord(MY_UUID)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to create rfcomm socket: $e")
            return
        }

        socketHandler = SocketHandler(socket!!, code)
        socketThread = Thread(socketHandler).apply {
            name = "BtThread"
            start()
        }
    }

    inner class SocketHandler(
        private val socket: BluetoothSocket,
        private val code: String
    ) : Runnable {

        override fun run() {
            Log.d(TAG, "Starting bluetooth handler thread")
            try {
                socket.connect()
                Log.d(TAG, "Socket is connected")
            } catch (e: IOException) {
                Log.e(TAG, "Failed to connect: $e")
                return
            }

            try {
                val data = "${code}\r\n".toByteArray()
                socket.outputStream.write(data)
                socket.outputStream.flush()
                Log.d(TAG, "Code '${code} has been writen")
            } catch (e: IOException) {
                Log.e(TAG, "Failed to send code to remote device: $e")
            }

            cancel()
            Log.d(TAG, "Stopping thread")
        }

        @Synchronized
        fun cancel() {
            try {
                socket.close()
            } catch (e: IOException) {
                Log.w(TAG, "Failed to close bluetooth socket: $e")
            }
        }
    }

    companion object {
        //val MY_UUID = UUID.fromString("e4d56fb3-b86d-4572-9b0d-44d483eb1eee")
        val MY_UUID = UUID.fromString("e8e10f95-1a70-4b27-9ccf-02010264e9c8")
        val TAG = "BluetoothHandler"
    }
}