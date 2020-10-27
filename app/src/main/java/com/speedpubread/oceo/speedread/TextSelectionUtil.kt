package com.speedpubread.oceo.speedread

import android.util.Log
import android.widget.TextView

class TouchSelection() {
    private val mTouchX = 0
    private val mTouchY = 0
    private fun handleTextSelection(currentChunkView: TextView?,
                                    textSelectionMenu: cTextSelectionMenu?,
                                    chosenFileName: String?,
                                    currentChapter: Int,
                                    currSentenceStart: Int,
                                    mTouchX: Int,
                                    mTouchY: Int) {
        if (currentChunkView == null) {
            return
        }
        if (currentChunkView.hasSelection()) {
            textSelectionMenu!!.setMetadata(chosenFileName, currentChapter, currSentenceStart)
            currentChunkView.customSelectionActionModeCallback = textSelectionMenu
        }
        Log.d("text selection", "($mTouchX, $mTouchY)")
    }
}