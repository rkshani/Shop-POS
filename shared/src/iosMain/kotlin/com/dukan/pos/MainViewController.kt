package com.dukan.pos

import androidx.compose.ui.window.ComposeUIViewController
import com.dukan.pos.di.initKoin
import platform.UIKit.UIViewController

private var koinStarted = false

fun MainViewController(): UIViewController {
    if (!koinStarted) {
        initKoin()
        koinStarted = true
    }
    return ComposeUIViewController { DukanApp() }
}
