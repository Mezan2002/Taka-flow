package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(EXTRA_NOTIF_ID, 1001)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "TakaFlow Reminder"
        val body = intent.getStringExtra(EXTRA_BODY) ?: "Check your daily finance log."
        val targetRoute = intent.getStringExtra(EXTRA_ROUTE) ?: "home"

        showNotification(context, id, title, body, targetRoute)
    }

    companion object {
        const val CHANNEL_ID = "takaflow_reminders"
        const val EXTRA_NOTIF_ID = "extra_notif_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"
        const val EXTRA_ROUTE = "extra_route"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Reminders & Budget Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Daily check-in reminders, due recurring/loan alerts, and budget warnings."
                }
                manager.createNotificationChannel(channel)
            }
        }

        fun showNotification(
            context: Context,
            id: Int,
            title: String,
            body: String,
            targetRoute: String,
        ) {
            ensureChannel(context)
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_ROUTE, targetRoute)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                id,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            runCatching { manager.notify(id, notification) }
        }
    }
}
