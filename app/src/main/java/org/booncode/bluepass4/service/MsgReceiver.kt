package org.booncode.bluepass4.service

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.app.job.JobWorkItem
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log
import android.widget.Toast
import androidx.core.content.getSystemService
import org.booncode.bluepass4.MsgFilterParams
import org.booncode.bluepass4.MyDataStore
import java.lang.Exception

class MsgReceiver : BroadcastReceiver() {
    private var _context: Context? = null
    private var _filter = MsgFilterParams()

    override fun onReceive(context: Context, intent: Intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        Log.d(TAG, "SMS was received ...")

        if (RECEIVED_ACTION != intent.action) {
            Log.w(TAG, "Unexpected intent action: ${intent.action}")
            return
        }

        _filter = MyDataStore(context).msgFilterParamsBlocking
        Log.v(TAG, "MsgFilterParams: sender=${_filter.pattern_sender}, message=${_filter.pattern_message}")
        _context = context
        inspectIntent(intent)
    }

    private fun inspectIntent(intent: Intent) {
        val bundle = intent.extras
        if (bundle != null) {
            var pdus: Array<*>? = null
            try {
                pdus = bundle["pdus"] as Array<*>?
            } catch (e: Exception) {
                // Ignore this error...
                Log.e(TAG, "inspectIntent: Couldn't retrieve PDU's", e)
            }
            if (pdus != null) {
                for (pdu in pdus.filterNotNull()) {
                    val data = pdu as ByteArray?
                    val msg = extractMessage(data, bundle)
                    if (msg != null) {
                        processMessage(msg)
                    }
                }
            }
        }
    }

    private fun extractMessage(data: ByteArray?, bundle: Bundle): SmsMessage? {
        if (data == null) {
            return null
        }
        return try {
            val format = bundle.getString("format")
            SmsMessage.createFromPdu(data, format)
        } catch (e: Exception) {
            // do nothing and return null
            Log.w(TAG, "Failed to extract SMS: $e")
            null
        }
    }

    private fun processMessage(msg: SmsMessage) {
        if ((_filter.pattern_sender == null) || (_filter.pattern_message == null)) {
            Log.v(TAG, "Filters are not set up")
            return
        }

        Log.v(TAG, String.format("Got message to check: sender=%s, msg=%s", msg.originatingAddress, msg.messageBody))

        if (!(_filter.pattern_sender?.matcher(msg.originatingAddress ?: "")?.matches() ?: false)) {
            Log.d(TAG, "Sender '${msg.originatingAddress}' doesn't match pattern '${_filter.pattern_sender}")
            return
        }

        val m = _filter.pattern_message?.matcher(msg.messageBody ?: "")

        if (!(m?.matches() ?: false)) {
            Log.d(TAG, "Content doesn't match pattern '${_filter.pattern_message}': ${msg.messageBody}")
            return
        }

        val number = m?.group(1)

        if (number == null) {
            Log.d(TAG, "Couldn't extract a number from the message")
            return
        }

        startBluePassServiceViaJobService(number)
        Toast.makeText(_context, "Extracted number: $number", Toast.LENGTH_LONG).show()
    }

    private fun startBluePassService(code: String) {
        val intent = Intent(_context, BlueService::class.java)
        intent.putExtra(BlueService.INTENT_COMMAND, BlueService.CMD_PUSH_CODE)
        intent.putExtra(BlueService.INTENT_CODE, code)

        _context?.startForegroundService(intent)
    }

    private fun startBluePassServiceViaJobService(code: String) {
        val context = _context ?: return;

        val intent = Intent(context, BlueService::class.java)
        intent.putExtra(BlueService.INTENT_COMMAND, BlueService.CMD_PUSH_CODE)
        intent.putExtra(BlueService.INTENT_CODE, code)

        val component = ComponentName(context, MsgReceivedJobService::class.java)
        val jobInfo = JobInfo.Builder(1, component).build()
        val jobScheduler = context.getSystemService(JobScheduler::class.java)
        jobScheduler.enqueue(jobInfo, JobWorkItem(intent, 0, 0))
    }

    companion object {
        private const val RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED"
        private const val TAG = "bluepass.MsgReceiver"
    }
}