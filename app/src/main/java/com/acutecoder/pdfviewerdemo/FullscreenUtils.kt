package com.acutecoder.pdfviewerdemo.utils

import android.app.Activity
import android.view.View
import androidx.appcompat.app.AppCompatActivity

fun Activity.setFullscreen(fullscreen: Boolean) {
    if (fullscreen) {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        if (this is AppCompatActivity) {
            supportActionBar?.hide()
        }
    } else {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        if (this is AppCompatActivity) {
            supportActionBar?.show()
        }
    }
}
