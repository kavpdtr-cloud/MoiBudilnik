package ru.neurolastic.moibudilnik

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView

class WidgetConfigActivity : Activity() {

    private var widgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var preview: TextView
    private lateinit var previewBox: LinearLayout
    private lateinit var fontSpinner: Spinner
    private lateinit var textColorSpinner: Spinner
    private lateinit var backgroundSpinner: Spinner
    private lateinit var sizeSeekBar: SeekBar
    private lateinit var alphaSeekBar: SeekBar
    private lateinit var sizeValue: TextView
    private lateinit var alphaValue: TextView

    private val textColorNames = arrayOf("Белый", "Чёрный", "Жёлтый", "Голубой", "Красный", "Зелёный")
    private val textColors = intArrayOf(
        Color.WHITE, Color.BLACK, Color.YELLOW, Color.CYAN,
        Color.RED, Color.rgb(80, 220, 120)
    )
    private val backgroundNames = arrayOf("Прозрачный", "Чёрный", "Белый", "Тёмно-синий", "Серый", "Бордовый")
    private val backgroundColors = intArrayOf(
        Color.TRANSPARENT, Color.BLACK, Color.WHITE,
        Color.rgb(18, 37, 68), Color.rgb(95, 99, 104), Color.rgb(92, 20, 35)
    )
    private val fontNames = arrayOf("Обычный (Sans)", "С засечками (Serif)", "Моноширинный (Mono)")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        buildScreen(WidgetStyleStore.load(this, widgetId))
    }

    private fun buildScreen(style: WidgetStyle) {
        val scroll = ScrollView(this)
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(24))
        }
        scroll.addView(page)

        page.addView(TextView(this).apply {
            text = "Настройки виджета"
            textSize = 24f
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
        })
        page.addView(TextView(this).apply {
            text = "Изменения относятся только к выбранному экземпляру виджета."
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, dp(6), 0, dp(12))
        })

        previewBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            minimumHeight = dp(80)
        }
        preview = TextView(this).apply {
            text = "07:30"
            setTextColor(style.textColor)
            textSize = style.fontSizeSp.toFloat()
            gravity = Gravity.START
        }
        previewBox.addView(preview)
        previewBox.addView(TextView(this).apply {
            text = "Следующее срабатывание · пример"
            textSize = 11f
            setTextColor(style.textColor)
        })
        page.addView(previewBox, matchWidth())

        fontSpinner = addSpinner(page, "Шрифт", fontNames, style.fontIndex)
        sizeValue = addValueLabel(page, "Размер основного времени: ${style.fontSizeSp} sp")
        sizeSeekBar = SeekBar(this).apply {
            max = 24
            progress = style.fontSizeSp - 18
        }
        page.addView(sizeSeekBar, matchWidth())

        textColorSpinner = addSpinner(
            page, "Цвет текста", textColorNames,
            textColors.indexOf(style.textColor).let { if (it >= 0) it else 0 }
        )
        backgroundSpinner = addSpinner(
            page, "Цвет фона", backgroundNames,
            backgroundColors.indexOf(style.backgroundColor).let { if (it >= 0) it else 0 }
        )
        alphaValue = addValueLabel(page, "Непрозрачность фона: ${style.backgroundAlpha}%")
        alphaSeekBar = SeekBar(this).apply {
            max = 100
            progress = style.backgroundAlpha
        }
        page.addView(alphaSeekBar, matchWidth())
        page.addView(TextView(this).apply {
            text = "Для прозрачного фона выберите «Прозрачный». Для цветного фона задайте желаемую непрозрачность."
            textSize = 12f
            setTextColor(Color.DKGRAY)
            setPadding(0, dp(4), 0, dp(14))
        })

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        buttons.addView(Button(this).apply {
            text = "Отмена"
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        buttons.addView(Button(this).apply {
            text = "Сохранить"
            setOnClickListener { saveAndFinish() }
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        page.addView(buttons, matchWidth())

        val updatePreview = { refreshPreview() }
        fontSpinner.onItemSelectedListener = SimpleItemSelectedListener { updatePreview() }
        textColorSpinner.onItemSelectedListener = SimpleItemSelectedListener { updatePreview() }
        backgroundSpinner.onItemSelectedListener = SimpleItemSelectedListener { updatePreview() }
        sizeSeekBar.setOnSeekBarChangeListener(simpleSeekListener {
            sizeValue.text = "Размер основного времени: ${sizeSeekBar.progress + 18} sp"
            updatePreview()
        })
        alphaSeekBar.setOnSeekBarChangeListener(simpleSeekListener {
            alphaValue.text = "Непрозрачность фона: ${alphaSeekBar.progress}%"
            updatePreview()
        })
        refreshPreview()
        setContentView(scroll)
    }

    private fun refreshPreview() {
        val font = when (fontSpinner.selectedItemPosition) {
            1 -> Typeface.SERIF
            2 -> Typeface.MONOSPACE
            else -> Typeface.DEFAULT
        }
        val textColor = textColors[textColorSpinner.selectedItemPosition.coerceIn(textColors.indices)]
        val bgColor = backgroundColors[backgroundSpinner.selectedItemPosition.coerceIn(backgroundColors.indices)]
        val alpha = alphaSeekBar.progress * 255 / 100
        val resolvedBg = if (bgColor == Color.TRANSPARENT) Color.TRANSPARENT else Color.argb(
            alpha, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor)
        )
        preview.typeface = font
        preview.textSize = (sizeSeekBar.progress + 18).toFloat()
        preview.setTextColor(textColor)
        previewBox.setBackgroundColor(resolvedBg)
        (previewBox.getChildAt(1) as TextView).setTextColor(textColor)
    }

    private fun saveAndFinish() {
        val bgColor = backgroundColors[backgroundSpinner.selectedItemPosition.coerceIn(backgroundColors.indices)]
        val style = WidgetStyle(
            fontIndex = fontSpinner.selectedItemPosition.coerceIn(0, 2),
            fontSizeSp = sizeSeekBar.progress + 18,
            textColor = textColors[textColorSpinner.selectedItemPosition.coerceIn(textColors.indices)],
            backgroundColor = bgColor,
            backgroundAlpha = alphaSeekBar.progress
        )
        WidgetStyleStore.save(this, widgetId, style)
        val manager = AppWidgetManager.getInstance(this)
        AlarmWidgetProvider.updateWidget(this, manager, widgetId)

        val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        setResult(RESULT_OK, result)
        finish()
    }

    private fun addSpinner(parent: LinearLayout, title: String, items: Array<String>, selection: Int): Spinner {
        parent.addView(TextView(this).apply {
            text = title
            textSize = 15f
            setTextColor(Color.BLACK)
            setPadding(0, dp(12), 0, dp(4))
        })
        val spinner = Spinner(this)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinner.adapter = adapter
        spinner.setSelection(selection.coerceIn(items.indices))
        parent.addView(spinner, matchWidth())
        return spinner
    }

    private fun addValueLabel(parent: LinearLayout, value: String): TextView {
        return TextView(this).apply {
            text = value
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, dp(10), 0, 0)
            parent.addView(this)
        }
    }

    private fun simpleSeekListener(onChange: () -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) = onChange()
        override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
        override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
    }

    private fun matchWidth() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    /** Small adapter so we do not need an AndroidX dependency. */
    private class SimpleItemSelectedListener(
        private val action: () -> Unit
    ) : android.widget.AdapterView.OnItemSelectedListener {
        override fun onItemSelected(
            parent: android.widget.AdapterView<*>?,
            view: View?,
            position: Int,
            id: Long
        ) = action()

        override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
    }
}
