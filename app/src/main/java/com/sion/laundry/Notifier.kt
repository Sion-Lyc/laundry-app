package com.sion.laundry

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.time.LocalDate

object Notifier {
    private const val CHANNEL = "laundry_remind"

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "换洗提醒", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    fun check(ctx: Context) {
        Store.load(ctx)
        val due = Store.items.value.filter { it.due }
        if (due.isEmpty()) return
        val prefs = ctx.getSharedPreferences("laundry_store", Context.MODE_PRIVATE)
        val today = LocalDate.now().toEpochDay()
        if (prefs.getLong("last_notify_day", -1) == today) return   // 一天只提醒一次
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val text = due.joinToString("、") { "${it.name}(${it.daysWorn}天)" }
        val pi = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("该换洗啦！")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text + " 已达到换洗阈值，洗完记得在 App 里点「换洗」。"))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(1001, n)
        prefs.edit().putLong("last_notify_day", today).apply()
    }
}
