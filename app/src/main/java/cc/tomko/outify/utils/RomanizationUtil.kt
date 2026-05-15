package cc.tomko.outify.utils

import android.icu.text.Transliterator

object RomanizationUtil {
    private val transliterator by lazy {
        Transliterator.getInstance("Any-Latin")
    }

    fun romanize(text: String): String {
        return transliterator.transliterate(text)
    }
}
