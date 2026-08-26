package com.niyati.tv

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = TextView(this)

        text.text = "NIYATI TV\n\nالتطبيق يعمل ✅"
        text.textSize = 28f
        text.setTextColor(Color.WHITE)
        text.setBackgroundColor(Color.rgb(8, 11, 18))
        text.gravity = Gravity.CENTER

        setContentView(text)
    }
}
