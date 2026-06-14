package com.taskmanager.app

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

object NotificationHelper {
    const val CHANNEL_NEW_TASK   = "new_task"
    const val CHANNEL_NEW_REPLY  = "new_reply"
    const val CHANNEL_FOREGROUND = "polling_fg"

    fun createChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(NotificationChannel(
            CHANNEL_NEW_TASK, "طلبات جديدة",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "إشعار عند وصول طلب جديد"
            enableVibration(true)
            enableLights(true)
        })

        nm.createNotificationChannel(NotificationChannel(
            CHANNEL_NEW_REPLY, "ردود جديدة",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "إشعار عند وصول رد على طلب"
        })

        nm.createNotificationChannel(NotificationChannel(
            CHANNEL_FOREGROUND, "متابعة الطلبات",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "خدمة خلفية لمراقبة الطلبات"
        })
    }

    fun notifyNewTask(ctx: Context, task: Task) {
        if (!hasPermission(ctx)) return

        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_task_id", task.id)
        }
        val pi = PendingIntent.getActivity(ctx, task.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val priority = when (task.priority) {
            "high" -> "🔴 عالية"
            "low"  -> "🟢 منخفضة"
            else   -> "🟡 متوسطة"
        }

        val notif = NotificationCompat.Builder(ctx, CHANNEL_NEW_TASK)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("📋 طلب جديد")
            .setContentText(task.title)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("${task.title}\nالأولوية: $priority"))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // sound + vibration
            .build()

        NotificationManagerCompat.from(ctx).notify(task.id.toInt(), notif)
    }

    fun notifyNewReply(ctx: Context, task: Task, senderName: String) {
        if (!hasPermission(ctx)) return

        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_task_id", task.id)
        }
        val pi = PendingIntent.getActivity(ctx, (task.id + 100000).toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notif = NotificationCompat.Builder(ctx, CHANNEL_NEW_REPLY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("💬 رد من $senderName")
            .setContentText(task.title)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .build()

        NotificationManagerCompat.from(ctx).notify((task.id + 100000).toInt(), notif)
    }

    fun buildForegroundNotification(ctx: Context) =
        NotificationCompat.Builder(ctx, CHANNEL_FOREGROUND)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("نظام توزيع المهام")
            .setContentText("جاري متابعة الطلبات...")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()

    private fun hasPermission(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(ctx,
            Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
}
