package com.niyati.tv

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)

        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.rgb(8, 11, 18))
        root.setPadding(30, 30, 30, 30)

        val title = TextView(this)

        title.text = "NIYATI TV"
        title.textSize = 32f
        title.setTextColor(Color.WHITE)
        title.setTypeface(null, Typeface.BOLD)
        title.gravity = Gravity.CENTER

        root.addView(
            title,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                100
            )
        )

        val welcome = TextView(this)

        welcome.text = "مرحباً بك في Niyati TV"
        welcome.textSize = 24f
        welcome.setTextColor(Color.WHITE)
        welcome.gravity = Gravity.CENTER

        root.addView(
            welcome,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val status = TextView(this)

        status.text = "التطبيق يعمل ✅"
        status.textSize = 20f
        status.setTextColor(Color.rgb(80, 220, 130))
        status.gravity = Gravity.CENTER

        root.addView(
            status,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                80
            )
        )

        setContentView(root)
    }
}
