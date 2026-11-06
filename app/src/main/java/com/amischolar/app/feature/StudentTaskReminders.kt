package com.amischolar.app.feature

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.annotation.SuppressLint
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.amischolar.app.MainActivity
import com.amischolar.app.data.ScholarDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val CHANNEL_ID = "student-task-reminders"
private const val EXTRA_TASK_ID = "student-task-id"
private const val EXTRA_TASK_TITLE = "student-task-title"

object StudentTaskReminders {
    fun schedule(context: Context, taskId: Long, title: String, atMillis: Long) {
        if (atMillis <= System.currentTimeMillis()) return
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending(context, taskId, title))
    }

    fun cancel(context: Context, taskId: Long) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.cancel(pending(context, taskId, ""))
    }

    fun canNotify(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    private fun pending(context: Context, taskId: Long, title: String): PendingIntent {
        val intent = Intent(context, StudentTaskReminderReceiver::class.java)
            .putExtra(EXTRA_TASK_ID, taskId)
            .putExtra(EXTRA_TASK_TITLE, title)
        val requestCode = (taskId xor (taskId ushr 32)).toInt()
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    @SuppressLint("MissingPermission")
    internal fun show(context: Context, taskId: Long, title: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Task reminders", NotificationManager.IMPORTANCE_DEFAULT))
        }
        if (!canNotify(context)) return
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Study task reminder")
            .setContentText(title.ifBlank { "A task is due." })
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(requestCode(taskId), notification) }
    }

    private fun requestCode(taskId: Long) = (taskId xor (taskId ushr 32)).toInt()
}

class StudentTaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TASK_TITLE).orEmpty()
        StudentTaskReminders.show(context, intent.getLongExtra(EXTRA_TASK_ID, 0), title)
    }
}

class StudentTaskReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ScholarDatabase.getInstance(context).studentWorkspaceDao().getTasks()
                    .filter { it.status != "done" && it.reminderAtEpochMillis != null }
                    .forEach { task -> StudentTaskReminders.schedule(context, task.id, task.title, task.reminderAtEpochMillis!!) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
