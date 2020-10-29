package com.speedpubread.oceo.speedread

import android.widget.SeekBar
import android.widget.TextView
import io.reactivex.disposables.Disposable

fun setupSeekbar(chapterSeekBar: SeekBar,
                 chptProgressView: TextView,
                 currSentenceStart: Int,
                 currentWordIdx: Int,
                 maxWordIdx: Int,
                 disposableReader: Disposable) {

    chapterSeekBar.max = maxWordIdx
    chapterSeekBar.min = 0
    chapterSeekBar.progress = currentWordIdx

    val chptPercentageComplete = (currentWordIdx.toFloat()) / java.lang.Float.valueOf(maxWordIdx.toFloat()) * 100

    if (chptPercentageComplete.toString().length > 3) {
        chptProgressView.text = chptPercentageComplete.toString().substring(0, 4) + "%"
    } else {
        chptProgressView.text = "$chptPercentageComplete%"
    }

    chapterSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
//                Log.d("seeking: ", String.valueOf(progress));
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
//                Log.d(TAG, "seekBar start tracking touch");
            disposableReader.dispose() // test
//            disposeListener()
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
//                Log.d(TAG, "seekBar stop tracking touch");
            val progress = seekBar.progress
//            currSentenceStart = getSentenceStartIdx(progress)
//            currentWordIdx = currSentenceStart
            //                currSentenceIdx = getWordPositionInSentence(progress);
//            currSentenceIdx = 0

            if (chptPercentageComplete.toString().length > 3) {
                chptProgressView.setText(chptPercentageComplete.toString().substring(0, 4) + "%")
            } else {
                chptProgressView.setText("$chptPercentageComplete%")
            }
//            iterateWords()
        }
    })
}