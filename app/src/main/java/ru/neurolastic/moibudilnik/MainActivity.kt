package ru.neurolastic.moibudilnik

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    companion object {
        const val EXTRA_OPEN_ALARMS = "ru.neurolastic.moibudilnik.OPEN_ALARMS"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHomeScreen()
        if (intent.getBooleanExtra(EXTRA_OPEN_ALARMS, false)) {
            intent.removeExtra(EXTRA_OPEN_ALARMS)
            AlarmWidgetProvider.openAlarms(this)
        }
    }

    override fun onResume() {
        super.onResume()
        AlarmWidgetProvider.refreshAll(this)
    }

    private fun showHomeScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(24))
        }
        root.addView(TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 25f
            setTextColor(0xFF202124.toInt())
        })
        root.addView(TextView(this).apply {
            text = "Виджет показывает ближайший активный системный будильник. Настройки внешнего вида можно менять отдельно для каждого виджета."
            textSize = 15f
            setTextColor(0xFF555555.toInt())
            setPadding(0, dp(12), 0, dp(18))
        })
        root.addView(Button(this).apply {
            text = "Открыть будильники"
            setOnClickListener { AlarmWidgetProvider.openAlarms(this@MainActivity) }
        })
        root.addView(Button(this).apply {
            text = "Обновить виджеты сейчас"
            setOnClickListener { AlarmWidgetProvider.refreshAll(this@MainActivity) }
        })
        root.addView(TextView(this).apply {
            text = "Настройка виджетов"
            textSize = 19f
            setTextColor(0xFF202124.toInt())
            setPadding(0, dp(22), 0, dp(8))
        })

        val ids = AppWidgetManager.getInstance(this).getAppWidgetIds(
            ComponentName(this, AlarmWidgetProvider::class.java)
        )
        if (ids.isEmpty()) {
            root.addView(TextView(this).apply {
                text = "На главном экране пока нет виджета. Добавьте его через меню «Виджеты», затем вернитесь сюда для настройки."
                textSize = 14f
                setTextColor(0xFF666666.toInt())
            })
        } else {
            ids.forEachIndexed { index, widgetId ->
                root.addView(Button(this).apply {
                    text = "Настроить виджет ${index + 1}"
                    setOnClickListener {
                        startActivity(Intent(this@MainActivity, WidgetConfigActivity::class.java).apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                        })
                    }
                })
            }
        }
        root.gravity = Gravity.TOP
        setContentView(root)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
