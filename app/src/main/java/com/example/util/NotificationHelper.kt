package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.receiver.ReminderReceiver

object NotificationHelper {

    private const val TAG = "NotificationHelper"

    fun scheduleTaskReminders(context: Context, taskId: Int, taskTitle: String, dueDate: Long?) {
        if (dueDate == null) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val currentTime = System.currentTimeMillis()

        // 1. Schedule H-1 Day Reminder (24 hours before)
        val hOneDayTime = dueDate - (24 * 60 * 60 * 1000)
        if (hOneDayTime > currentTime) {
            val hOneIntent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("task_id", taskId)
                putExtra("task_title", taskTitle)
                putExtra("notification_type", "h_one_day_before")
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId * 3 + 1,
                hOneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            trySetAlarm(alarmManager, hOneDayTime, pendingIntent)
        }

        // 2. Schedule H-1 Hour Reminder (1 hour before)
        val hOneHourTime = dueDate - (60 * 60 * 1000)
        if (hOneHourTime > currentTime) {
            val hHourBeforeIntent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("task_id", taskId)
                putExtra("task_title", taskTitle)
                putExtra("notification_type", "h_one_hour_before")
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId * 3 + 2,
                hHourBeforeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            trySetAlarm(alarmManager, hOneHourTime, pendingIntent)
        }

        // 3. Schedule Jam Target Deadline Reminder (At the exact time)
        if (dueDate > currentTime) {
            val hHourIntent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("task_id", taskId)
                putExtra("task_title", taskTitle)
                putExtra("notification_type", "h_due_hour")
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId * 3,
                hHourIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            trySetAlarm(alarmManager, dueDate, pendingIntent)
        }
    }

    fun cancelTaskReminders(context: Context, taskId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Cancel H-1 Day
        val hOneIntent = Intent(context, ReminderReceiver::class.java)
        val p1 = PendingIntent.getBroadcast(
            context,
            taskId * 3 + 1,
            hOneIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (p1 != null) {
            alarmManager.cancel(p1)
            p1.cancel()
        }

        // Cancel H-1 Hour
        val hOneHourIntent = Intent(context, ReminderReceiver::class.java)
        val p2 = PendingIntent.getBroadcast(
            context,
            taskId * 3 + 2,
            hOneHourIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (p2 != null) {
            alarmManager.cancel(p2)
            p2.cancel()
        }

        // Cancel Deadline Exact hour
        val hHourIntent = Intent(context, ReminderReceiver::class.java)
        val p0 = PendingIntent.getBroadcast(
            context,
            taskId * 3,
            hHourIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (p0 != null) {
            alarmManager.cancel(p0)
            p0.cancel()
        }
    }

    private fun trySetAlarm(alarmManager: AlarmManager, triggerTime: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling exact alarm. Falling back to inexact alarm.", e)
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm.", e)
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }
}
