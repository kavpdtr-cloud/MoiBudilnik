package ru.neurolastic.moibudilnik

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
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
            text = "Приложение добавляет на главный экран виджет ближайшего активного будильника."
            textSize = 16f
            setPadding(0, 20, 0, 24)
        })
        root.addView(Button(this).apply {
            text = "Открыть будильники"
            setOnClickListener { AlarmWidgetProvider.openAlarms(this@MainActivity) }
        })
        setContentView(root)
    }
}
