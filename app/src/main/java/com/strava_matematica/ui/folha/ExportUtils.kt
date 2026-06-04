package com.strava_matematica.ui.folha

import android.content.Context
import android.content.Intent

fun shareSimuladoRules(context: Context, rulesJson: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, rulesJson)
    }
    val chooser = Intent.createChooser(intent, "Compartilhar Simulado")
    context.startActivity(chooser)
}
