package ru.neurolastic.moibudilnik

import android.content.Context
import android.graphics.Color

/** Stores independent appearance preferences for every home-screen widget instance. */
data class WidgetStyle(
    val fontIndex: Int = 0,
    val fontSizeSp: Int = 30,
    val textColor: Int = Color.WHITE,
    val backgroundColor: Int = Color.TRANSPARENT,
    val backgroundAlpha: Int = 0
)

object WidgetStyleStore {
    private const val PREFS = "widget_style_preferences"

    fun load(context: Context, widgetId: Int): WidgetStyle {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val suffix = widgetId.toString()
        return WidgetStyle(
            fontIndex = p.getInt("font_$suffix", 0).coerceIn(0, 2),
            fontSizeSp = p.getInt("size_$suffix", 30).coerceIn(18, 42),
            textColor = p.getInt("text_$suffix", Color.WHITE),
            backgroundColor = p.getInt("background_$suffix", Color.TRANSPARENT),
            backgroundAlpha = p.getInt("alpha_$suffix", 0).coerceIn(0, 100)
        )
    }

    fun save(context: Context, widgetId: Int, style: WidgetStyle) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        val suffix = widgetId.toString()
        p.putInt("font_$suffix", style.fontIndex.coerceIn(0, 2))
        p.putInt("size_$suffix", style.fontSizeSp.coerceIn(18, 42))
        p.putInt("text_$suffix", style.textColor)
        p.putInt("background_$suffix", style.backgroundColor)
        p.putInt("alpha_$suffix", style.backgroundAlpha.coerceIn(0, 100))
        p.apply()
    }

    fun delete(context: Context, widgetId: Int) {
        val suffix = widgetId.toString()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove("font_$suffix")
            .remove("size_$suffix")
            .remove("text_$suffix")
            .remove("background_$suffix")
            .remove("alpha_$suffix")
            .apply()
    }
}
