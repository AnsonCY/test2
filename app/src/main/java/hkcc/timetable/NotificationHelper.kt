package hkcc.timetable

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import hkcc.timetable.data.Subject
import java.util.Calendar

class NotificationHelper {
    companion object {
        const val CHANNEL_ID = "class_reminders"
        
        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "Class Reminders"
                val descriptionText = "Notifications sent before classes start"
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }
                val notificationManager: NotificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }

        fun scheduleClassReminders(context: Context, subjects: List<Subject>, reminderMinutes: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            subjects.forEach { subject ->
                val calendar = Calendar.getInstance()
                val dayOfWeek = when (subject.dayOfWeek.trim().uppercase().take(3)) {
                    "MON" -> Calendar.MONDAY
                    "TUE" -> Calendar.TUESDAY
                    "WED" -> Calendar.WEDNESDAY
                    "THU" -> Calendar.THURSDAY
                    "FRI" -> Calendar.FRIDAY
                    "SAT" -> Calendar.SATURDAY
                    "SUN" -> Calendar.SUNDAY
                    else -> -1
                }
                
                if (dayOfWeek != -1) {
                    val timeParts = subject.startTime.split(":")
                    if (timeParts.size == 2) {
                        calendar.set(Calendar.DAY_OF_WEEK, dayOfWeek)
                        calendar.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                        calendar.set(Calendar.MINUTE, timeParts[1].toInt())
                        calendar.set(Calendar.SECOND, 0)
                        
                        // Use the configured reminder interval
                        calendar.add(Calendar.MINUTE, -reminderMinutes)
                        
                        if (calendar.timeInMillis < System.currentTimeMillis()) {
                            calendar.add(Calendar.WEEK_OF_YEAR, 1)
                        }

                        val intent = Intent(context, NotificationReceiver::class.java).apply {
                            putExtra("subjectCode", subject.code)
                            putExtra("venue", subject.venue)
                            putExtra("time", subject.startTime)
                        }
                        
                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            subject.id.hashCode(),
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        alarmManager.setRepeating(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            AlarmManager.INTERVAL_DAY * 7,
                            pendingIntent
                        )
                    }
                }
            }
        }

        fun scheduleDeadlineReminder(context: Context, deadline: Deadline) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val calendar = Calendar.getInstance().apply {
                timeInMillis = deadline.dueDate
                set(Calendar.HOUR_OF_DAY, 9) // Remind at 9 AM on the day
                set(Calendar.MINUTE, 0)
            }

            if (calendar.timeInMillis > System.currentTimeMillis()) {
                val intent = Intent(context, NotificationReceiver::class.java).apply {
                    putExtra("isDeadline", true)
                    putExtra("title", deadline.title)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    deadline.id.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
        }
    }
}

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val isDeadline = intent.getBooleanExtra("isDeadline", false)
        
        if (isDeadline) {
            val title = intent.getStringExtra("title") ?: "Deadline"
            showNotification(context, title.hashCode(), "Deadline Today", "Don't forget: $title")
        } else {
            val code = intent.getStringExtra("subjectCode") ?: "Class"
            val venue = intent.getStringExtra("venue") ?: "Unknown"
            val time = intent.getStringExtra("time") ?: ""
            showNotification(context, code.hashCode(), "Upcoming Class: $code", "Starts at $time @ $venue")
        }
    }

    private fun showNotification(context: Context, id: Int, title: String, message: String) {
        val builder = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(id, builder.build())
    }
}
