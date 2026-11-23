package com.sion.laundry

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class CheckWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        Notifier.check(applicationContext)
        return Result.success()
    }
}
