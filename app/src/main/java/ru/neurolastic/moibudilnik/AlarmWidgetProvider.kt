package ru.neurolastic.moibudilnik

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.AlarmClock
import android.text.format.DateFormat
import android.widget.RemoteViews
import android.widget.Toast
import java.util.Date

class AlarmWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { updateOne(context, appWidgetManager, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED -> updateAll(context)
        }
    }

    companion object {
        private const val REQUEST_OPEN_ALARMS = 4101

        // HONOR Clock first; the remaining packages cover common Android clock apps.
        private val CLOCK_PACKAGES = listOf(
            "com.hihonor.deskclock",
            "com.android.deskclock",
            "com.google.android.deskclock",
            "com.huawei.deskclock",
            "com.huawei.android.deskclock"
        )

        private fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, AlarmWidgetProvider::class.java)
            )
            ids.forEach { updateOne(context, manager, it) }
        }

        private fun updateOne(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.alarm_widget)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val nextAlarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                alarmManager.nextAlarmClock
            } else null

            if (nextAlarm == null) {
                views.setTextViewText(R.id.alarm_time, "— — : — —")
                views.setTextViewText(R.id.alarm_caption, "Нет активных будильников")
            } else {
                val whenAlarm = Date(nextAlarm.triggerTime)
                val time = DateFormat.getTimeFormat(context).format(whenAlarm)
                val date = DateFormat.getDateFormat(context).format(whenAlarm)
                views.setTextViewText(R.id.alarm_time, time)
                views.setTextViewText(R.id.alarm_caption, "Следующее срабатывание · $date")
            }

            // Route widget taps through MainActivity so the same HONOR-aware
            // fallback is used by both the app button and the widget itself.
            val openIntent = Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_OPEN_ALARMS, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                REQUEST_OPEN_ALARMS,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            manager.updateAppWidget(widgetId, views)
        }

        /**
         * Opens the alarm screen through the standard Android action when the
         * clock app supports it. Otherwise launches the installed clock app.
         * Returns false and shows a message if no known clock app is found.
         */
        fun openAlarms(context: Context): Boolean {
            val packageManager = context.packageManager

            for (clockPackage in CLOCK_PACKAGES) {
                val showAlarmsIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                    setPackage(clockPackage)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                try {
                    if (showAlarmsIntent.resolveActivity(packageManager) != null) {
                        context.startActivity(showAlarmsIntent)
                        return true
                    }
                } catch (_: Exception) {
                    // Try launching this clock app's main screen below.
                }

                try {
                    val launchIntent = packageManager.getLaunchIntentForPackage(clockPackage)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        return true
                    }
                } catch (_: Exception) {
                    // Continue to the next known clock package.
                }
            }

            // Last attempt: let Android resolve any app that supports this action.
            val genericIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                if (genericIntent.resolveActivity(packageManager) != null) {
                    context.startActivity(genericIntent)
                    return true
                }
            } catch (_: Exception) {
                // Fall through to the visible error message.
            }

            Toast.makeText(
                context,
                "Не удалось найти приложение «Часы». Откройте его вручную один раз.",
                Toast.LENGTH_LONG
            ).show()
            return false
        }
    }
}
