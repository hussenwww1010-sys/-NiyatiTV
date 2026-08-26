package com.niyati.tv

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private val red = Color.rgb(233, 21, 66)
    private val background = Color.rgb(8, 11, 18)
    private val card = Color.rgb(18, 23, 33)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)

        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(background)
        root.setPadding(25, 25, 25, 25)

        val title = TextView(this)

        title.text = "NIYATI TV"
        title.textSize = 30f
        title.setTextColor(Color.WHITE)
        title.setTypeface(null, Typeface.BOLD)
        title.gravity = Gravity.CENTER_VERTICAL

        root.addView(
            title,
            LinearLayout.LayoutParams(
                -1,
                80
            )
        )

        val packageTitle = TextView(this)

        packageTitle.text = "الباقات"
        packageTitle.textSize = 22f
        packageTitle.setTextColor(Color.WHITE)
        packageTitle.setTypeface(null, Typeface.BOLD)
        packageTitle.setPadding(10, 15, 10, 15)

        root.addView(
            packageTitle,
            LinearLayout.LayoutParams(
                -1,
                65
            )
        )

        val packageButton = TextView(this)

        packageButton.text = "⚽  الرياضة"
        packageButton.textSize = 21f
        packageButton.setTextColor(Color.WHITE)
        packageButton.gravity = Gravity.CENTER_VERTICAL
        packageButton.setPadding(25, 0, 25, 0)
        packageButton.setBackgroundColor(red)
        packageButton.isFocusable = true
        packageButton.isFocusableInTouchMode = true

        root.addView(
            packageButton,
            LinearLayout.LayoutParams(
                -1,
                70
            )
        )

        val channelTitle = TextView(this)

        channelTitle.text = "القنوات"
        channelTitle.textSize = 22f
        channelTitle.setTextColor(Color.WHITE)
        channelTitle.setTypeface(null, Typeface.BOLD)
        channelTitle.setPadding(10, 25, 10, 15)

        root.addView(
            channelTitle,
            LinearLayout.LayoutParams(
                -1,
                70
            )
        )

        val channel = TextView(this)

        channel.text = "▶  beIN SPORT 1HD"
        channel.textSize = 20f
        channel.setTextColor(Color.WHITE)
        channel.gravity = Gravity.CENTER_VERTICAL
        channel.setPadding(25, 0, 25, 0)
        channel.setBackgroundColor(card)
        channel.isFocusable = true
        channel.isFocusableInTouchMode = true

        root.addView(
            channel,
            LinearLayout.LayoutParams(
                -1,
                75
            )
        )

        val info = TextView(this)

        info.text = "اختر قناة للمتابعة"
        info.textSize = 18f
        info.setTextColor(Color.LTGRAY)
        info.gravity = Gravity.CENTER
        info.setPadding(10, 30, 10, 10)

        root.addView(
            info,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        channel.setOnClickListener {
            info.text = "تم اختيار: beIN SPORT 1HD\n\nالمشغل سيتم إضافته في المرحلة التالية."
        }

        setContentView(root)

        packageButton.requestFocus()
    }
}
