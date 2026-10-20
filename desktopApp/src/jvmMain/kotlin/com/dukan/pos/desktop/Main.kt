package com.dukan.pos.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.dukan.pos.DukanApp
import com.dukan.pos.di.initKoin

fun main() {
    initKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Dukan POS",
            state = rememberWindowState(size = DpSize(1100.dp, 780.dp)),
        ) {
            DukanApp()
        }
    }
}
