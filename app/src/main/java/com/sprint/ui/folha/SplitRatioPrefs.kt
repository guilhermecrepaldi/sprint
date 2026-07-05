package com.sprint.ui.folha

import android.content.Context

/**
 * Persiste scratchRatio (divisor rascunho/resposta) por fieldIndex via SharedPreferences.
 */
object SplitRatioPrefs {

    private const val PREFS = "split_ratio_v1"

    fun get(context: Context, fieldIndex: Int, default: Float): Float =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getFloat("field_$fieldIndex", default)

    fun set(context: Context, fieldIndex: Int, ratio: Float) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putFloat("field_$fieldIndex", ratio)
            .apply()
}
