package org.booncode.bluepass4.service

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log

class MsgReceivedJobService: JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        Log.d(TAG, "onStartJob ...")
        while (true) {
            val work = params.dequeueWork() ?: break
            Log.d(TAG, "Dequeued start request")

            startForegroundService(work.intent)
            params.completeWork(work)
        }
        Log.d(TAG, "onStartJob -> done")
        return false
    }

    override fun onStopJob(params: JobParameters): Boolean {
        Log.d(TAG, "onStopJob (rescheduling...)")
        return true  // reschedule if job execution has been interrupted?
    }

    companion object {
        private const val TAG = "bluepass.MsgReceivedJobService"
    }
}