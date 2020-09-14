package com.example.oceo.speedread

import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button

/*
fun setupWPMControls(rootView: View, WPM: Long) {
    val raiseWPMButton: Button = rootView.findViewById(R.id.raise_wpm_button)
    val lowerWPMButton: Button = rootView.findViewById(R.id.lower_wpm_button)
    val WPM_view: Button = rootView.findViewById(R.id.current_wpm_view)
    WPM_view.setText(WPM.toString())
    raiseWPMButton.setOnClickListener(View.OnClickListener { //                Log.d(TAG, "onClick");
        incrementWPM()
        // TODO necessary to do here AND in motion up?
        PrefsUtil.writeLongToPrefs(activity!!, WPM_KEY, WPM)
    })
    raiseWPMButton!!.setOnLongClickListener(View.OnLongClickListener { //                Log.d(TAG, "long click");
        autoIncrementWPM = true
        WPMUpdateHandler.post(RepetitiveUpdater())
        false
    })
    raiseWPMButton!!.setOnTouchListener(View.OnTouchListener { v, event ->
        if (event.action == MotionEvent.ACTION_UP && autoIncrementWPM) {
//            Log.d(TAG, "touch up")
            autoIncrementWPM = false
            PrefsUtil.writeLongToPrefs(activity!!, WPM_KEY, WPM)
        }
        false
    })
    lowerWPMButton!!.setOnClickListener(View.OnClickListener { //                Log.d(TAG, "onClick");
        decrementWPM()
    })
    lowerWPMButton!!.setOnLongClickListener(View.OnLongClickListener { //                Log.d(TAG, "long click");
        autoDecrementWPM = true
        WPMUpdateHandler.post(RepetitiveUpdater())
        false
    })
    lowerWPMButton!!.setOnTouchListener(View.OnTouchListener { v, event ->
        if (event.action == MotionEvent.ACTION_UP && autoDecrementWPM) {
//                    Log.d(TAG, "touch up");
            autoDecrementWPM = false
        }
        false
    })
}

fun incrementWPM() {
    if (WPM < 1000) {
        WPM++
        WPM_view!!.text = WPM.toString()
    }
}

fun decrementWPM() {
    if (WPM > 0) {
        WPM--
        WPM_view!!.text = WPM.toString()
    }
}

/* used to update WPM values while button held */
internal class RepetitiveUpdater : Runnable {
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

 */