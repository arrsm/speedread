package com.speedpubread.oceo.speedread

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView

class WPM(val activity: Activity, val rootView: View, val reader: Reader) {
    val WPM_KEY = "wpm"
    private var autoIncrementWPM = false
    private var autoDecrementWPM = false
    private val REPEAT_DELAY: Long = 50
    private val WPMUpdateHandler = Handler()

    val raiseWPMButton: Button = rootView.findViewById(R.id.raise_wpm_button)
    val lowerWPMButton: Button = rootView.findViewById(R.id.lower_wpm_button)
    val WPM_view: TextView = rootView.findViewById(R.id.current_wpm_view)

    init {
        setupWPMControls()
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setupWPMControls() {

        WPM_view.text = reader.WPM.toString()

        raiseWPMButton.setOnClickListener(View.OnClickListener { //                Log.d(TAG, "onClick");
            incrementWPM()
            PrefsUtil.writeLongToPrefs(activity, WPM_KEY, reader.WPM)
        })

        raiseWPMButton.setOnLongClickListener(View.OnLongClickListener { //                Log.d(TAG, "long click");
            autoIncrementWPM = true
            WPMUpdateHandler.post(RepetitiveUpdater())
            false
        })

        raiseWPMButton.setOnTouchListener(View.OnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP && autoIncrementWPM) {
                // Log.d(TAG, "touch up")
                autoIncrementWPM = false
                PrefsUtil.writeLongToPrefs(activity, WPM_KEY, reader.WPM)
            }
            false
        })

        lowerWPMButton.setOnClickListener(View.OnClickListener { //                Log.d(TAG, "onClick");
            decrementWPM()
        })

        lowerWPMButton.setOnLongClickListener(View.OnLongClickListener { //                Log.d(TAG, "long click");
            autoDecrementWPM = true
            WPMUpdateHandler.post(RepetitiveUpdater())
            false
        })

        lowerWPMButton.setOnTouchListener(View.OnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP && autoDecrementWPM) {
//                    Log.d(TAG, "touch up");
                autoDecrementWPM = false
                PrefsUtil.writeLongToPrefs(activity, WPM_KEY, reader.WPM)
            }
            false
        })
    }

    fun incrementWPM() {
        if (reader.WPM < 1000) {
            reader.WPM++
            WPM_view.text = reader.WPM.toString()
        }
    }

    fun decrementWPM() {
        if (reader.WPM > 0) {
            reader.WPM--
            WPM_view.text = reader.WPM.toString()
        }
    }

    /* used to update WPM values while button held */
    internal inner class RepetitiveUpdater : Runnable {
        override fun run() {
            if (autoIncrementWPM) {
                incrementWPM()
                WPMUpdateHandler.postDelayed(RepetitiveUpdater(), REPEAT_DELAY)
            } else if (autoDecrementWPM) {
                decrementWPM()
                WPMUpdateHandler.postDelayed(RepetitiveUpdater(), REPEAT_DELAY)
            }
        }
    }

}
