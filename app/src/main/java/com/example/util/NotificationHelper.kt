package com.example.util

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
import com.example.MainActivity
import com.example.R
import com.example.data.model.AlertSeverity
import com.example.data.model.SevereWeatherAlert

object NotificationHelper {

    const val CHANNEL_ID = "weather_severe_alerts_channel"
    private const val CHANNEL_NAME = "تنبيهات الطقس والإنذارات المبكرة"
    private const val CHANNEL_DESC = "إشعارات عاجلة عند رصد عواصف رعدية، أمطار غزيرة، موجات حر أو تقلبات جوية حادة"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        val runtimePermissionGranted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        return runtimePermissionGranted &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /**
     * Returns true only when the notification API accepted the request without a
     * permission failure and the app's notification channel is enabled.
     */
    fun postSevereWeatherNotification(context: Context, alert: SevereWeatherAlert): Boolean {
        createNotificationChannel(context)

        if (!hasNotificationPermission(context)) {
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = manager.getNotificationChannel(CHANNEL_ID) ?: return false
            if (channel.importance == NotificationManager.IMPORTANCE_NONE) return false
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            alert.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val priority = when (alert.severity) {
            AlertSeverity.EMERGENCY, AlertSeverity.WARNING -> NotificationCompat.PRIORITY_MAX
            AlertSeverity.WATCH -> NotificationCompat.PRIORITY_HIGH
            AlertSeverity.ADVISORY -> NotificationCompat.PRIORITY_DEFAULT
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⚠️ " + alert.titleAr)
            .setContentText(alert.descriptionAr)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${alert.descriptionAr}\n\nتعليمات السلامة: ${alert.safetyInstructionsAr}\nصالح حتى: ${alert.validUntil}")
            )
            .setPriority(priority)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(alert.id.hashCode(), notification)
            true
        } catch (e: SecurityException) {
            // Permission may be revoked between the permission check and notify().
            false
        }
    }
}
