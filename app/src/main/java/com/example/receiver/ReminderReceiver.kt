package com.example.receiver

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
        val taskId = intent.getIntExtra("task_id", 0)
        val taskTitle = intent.getStringExtra("task_title") ?: "Task"
        val notificationType = intent.getStringExtra("notification_type") ?: "h-jam" // "h_one_day_before" or "h_due_hour"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "todo_reminder_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Task Reminders"
            val channelDesc = "Critical notifications for upcoming todo task deadlines"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDesc
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Action when tapping notification
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = when (notificationType) {
            "h_one_day_before" -> "⚠️ Deadline H-1 Hari: Standard reminder for task \"$taskTitle\" tomorrow!"
            "h_one_hour_before" -> "⏳ Deadline H-1 Jam: Task \"$taskTitle\" is due in 1 hour!"
            else -> "⏰ Deadline H-JAM: The time for task \"$taskTitle\" is now!"
        }

        val title = when (notificationType) {
            "h_one_day_before" -> "Tomorrow's Deadline Reminder"
            "h_one_hour_before" -> "1 Hour Left Reminder"
            else -> "Task Deadline Reached"
        }

        // Dynamic unique notification id using hash of fields with a multiplier of 3
        val notificationId = taskId * 3 + when (notificationType) {
            "h_one_day_before" -> 1
            "h_one_hour_before" -> 2
            else -> 0
        }

        // Using standard mipmap app icon for guaranteed compatibility in notifications
        val smallIcon = R.mipmap.ic_launcher

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(notificationId, builder.build())
    }
}
