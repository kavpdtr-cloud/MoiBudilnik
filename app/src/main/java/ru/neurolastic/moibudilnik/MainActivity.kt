package ru.neurolastic.moibudilnik

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    companion object {
        const val EXTRA_OPEN_ALARMS = "ru.neurolastic.moibudilnik.OPEN_ALARMS"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 40, 32, 32)
        }
        root.addView(TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 24f
        })
        root.addView(TextView(this).apply {
            text = "Виджет показывает ближайший активный системный будильник."
            textSize = 16f
            setPadding(0, 20, 0, 24)
        })
        root.addView(Button(this).apply {
            text = "Открыть будильники"
            setOnClickListener { AlarmWidgetProvider.openAlarms(this@MainActivity) }
        })
        setContentView(root)

        // If this activity was opened by tapping the home-screen widget,
        // immediately use the same clock-launch routine as the button.
        if (intent.getBooleanExtra(EXTRA_OPEN_ALARMS, false)) {
            intent.removeExtra(EXTRA_OPEN_ALARMS)
            AlarmWidgetProvider.openAlarms(this)
        }
    }
}
