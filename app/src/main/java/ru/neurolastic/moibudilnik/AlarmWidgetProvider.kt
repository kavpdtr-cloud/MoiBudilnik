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
                val time = DateFormat.getTimeFormat(context).format(Date(nextAlarm.triggerTime))
                val date = DateFormat.getDateFormat(context).format(Date(nextAlarm.triggerTime))
                views.setTextViewText(R.id.alarm_time, time)
                views.setTextViewText(R.id.alarm_caption, "Следующее срабатывание · $date")
            }

            val openIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
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

        fun openAlarms(context: Context) {
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                val fallback = context.packageManager.getLaunchIntentForPackage("com.android.deskclock")
                    ?: context.packageManager.getLaunchIntentForPackage("com.google.android.deskclock")
                if (fallback != null) context.startActivity(fallback)
            }
        }
    }
}
