package ru.neurolastic.moibudilnik

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.provider.AlarmClock
import android.text.format.DateFormat
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import java.util.Date

class AlarmWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> refreshAll(context)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { WidgetStyleStore.delete(context, it) }
    }

    companion object {
        private const val REQUEST_OPEN_ALARMS_BASE = 4101

        private val CLOCK_PACKAGES = listOf(
            "com.hihonor.deskclock",
            "com.android.deskclock",
            "com.google.android.deskclock",
            "com.huawei.deskclock",
            "com.huawei.android.deskclock"
        )

        /** Public so the app can refresh all widgets when the user returns from Clock. */
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, AlarmWidgetProvider::class.java)
            )
            ids.forEach { updateWidget(context, manager, it) }
        }

        fun updateWidget(
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
                setAllTimeTexts(views, "— — : — —")
                setAllCaptionTexts(views, "Нет активных будильников")
            } else {
                val alarmDate = Date(nextAlarm.triggerTime)
                val time = DateFormat.getTimeFormat(context).format(alarmDate)
                val date = DateFormat.getDateFormat(context).format(alarmDate)
                setAllTimeTexts(views, time)
                setAllCaptionTexts(views, "Следующее срабатывание · $date")
            }

            applyStyle(views, WidgetStyleStore.load(context, widgetId))

            // Go through MainActivity so the HONOR-aware clock fallback is retained.
            val openIntent = Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_OPEN_ALARMS, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                REQUEST_OPEN_ALARMS_BASE + widgetId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            manager.updateAppWidget(widgetId, views)
        }

        private fun setAllTimeTexts(views: RemoteViews, value: String) {
            views.setTextViewText(R.id.alarm_time_sans, value)
            views.setTextViewText(R.id.alarm_time_serif, value)
            views.setTextViewText(R.id.alarm_time_mono, value)
        }

        private fun setAllCaptionTexts(views: RemoteViews, value: String) {
            views.setTextViewText(R.id.alarm_caption_sans, value)
            views.setTextViewText(R.id.alarm_caption_serif, value)
            views.setTextViewText(R.id.alarm_caption_mono, value)
        }

        private fun applyStyle(views: RemoteViews, style: WidgetStyle) {
            val timeIds = intArrayOf(
                R.id.alarm_time_sans, R.id.alarm_time_serif, R.id.alarm_time_mono
            )
            val captionIds = intArrayOf(
                R.id.alarm_caption_sans, R.id.alarm_caption_serif, R.id.alarm_caption_mono
            )

            timeIds.forEachIndexed { index, id ->
                views.setViewVisibility(id, if (index == style.fontIndex) View.VISIBLE else View.GONE)
                views.setTextViewTextSize(id, android.util.TypedValue.COMPLEX_UNIT_SP, style.fontSizeSp.toFloat())
                views.setTextColor(id, style.textColor)
            }
            captionIds.forEachIndexed { index, id ->
                views.setViewVisibility(id, if (index == style.fontIndex) View.VISIBLE else View.GONE)
                views.setTextColor(id, style.textColor)
                views.setTextViewTextSize(
                    id,
                    android.util.TypedValue.COMPLEX_UNIT_SP,
                    (style.fontSizeSp / 3).coerceIn(9, 14).toFloat()
                )
            }

            val alpha = (style.backgroundAlpha.coerceIn(0, 100) * 255 / 100)
            val background = if (style.backgroundColor == Color.TRANSPARENT) {
                Color.TRANSPARENT
            } else {
                Color.argb(
                    alpha,
                    Color.red(style.backgroundColor),
                    Color.green(style.backgroundColor),
                    Color.blue(style.backgroundColor)
                )
            }
            views.setInt(R.id.widget_root, "setBackgroundColor", background)
            views.setInt(R.id.alarm_icon, "setColorFilter", style.textColor)
        }

        /** Opens the HONOR Clock alarm page, falling back to its app screen. */
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
                } catch (_: Exception) { }

                try {
                    val launchIntent = packageManager.getLaunchIntentForPackage(clockPackage)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        return true
                    }
                } catch (_: Exception) { }
            }

            val genericIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                if (genericIntent.resolveActivity(packageManager) != null) {
                    context.startActivity(genericIntent)
                    return true
                }
            } catch (_: Exception) { }

            Toast.makeText(
                context,
                "Не удалось найти приложение «Часы». Откройте его вручную.",
                Toast.LENGTH_LONG
            ).show()
            return false
        }
    }
}
