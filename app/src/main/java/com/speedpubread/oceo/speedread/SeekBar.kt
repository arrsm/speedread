package com.speedpubread.oceo.speedread

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.TextView

class Seeker(rootView: View, val reader: Reader, val max: Int = 0, val min: Int = 0) {

    val chapterSeekBar: SeekBar = rootView.findViewById(R.id.seekBar)
    val chptProgressView: TextView = rootView.findViewById(R.id.chapter_progress_view)

    init {
        chapterSeekBar.max = max
        chapterSeekBar.min = min
        setupSeekbar()
    }

    @SuppressLint("SetTextI18n")
    fun setupSeekbar() {
        chapterSeekBar.progress = reader.currentWordIdx
        val chptPercentageComplete = getChapterPercentageComplete()
        if (chptPercentageComplete.toString().length > 3) {
            chptProgressView.setText(chptPercentageComplete.toString().substring(0, 4) + "%")
        } else {
            chptProgressView.setText("$chptPercentageComplete%")
        }

        chapterSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                Log.d("seeking: ", "$progress / $max");
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
//                Log.d(TAG, "seekBar start tracking touch");
//                reader.disposeListener()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
//                Log.d(TAG, "seekBar stop tracking touch");
//                val progress = seekBar.progress
//                reader.currSentenceStart = reader.getSentenceStartIdx(progress - 1)
//                reader.currentWordIdx = reader.currSentenceStart
//                //                reader.currSentenceIdx = getWordPositionInSentence(progress/;
//                reader.currSentenceIdx = 0
//
//                val chapterPercentage = getChapterPercentageComplete()
//                if (chptPercentageComplete.toString().length > 3) {
//                    chptProgressView.setText(chapterPercentage.toString().substring(0, 4) + "%")
//                } else {
//                    Log.d("updating seekbar text", progress.toString())
//                    chptProgressView.setText("$chapterPercentage%")
//                }


//                reader.iterateWords(story!!, currentChunkView!!, currentWordView!!, chptProgressView!!, chapterSeekBar!!)
            }
        })
    }

    fun setSeekerMax(maxVal: Int) {
        chapterSeekBar.max = maxVal
    }

    fun setSeekerMin(minVal: Int) {
        chapterSeekBar.min = minVal

    }

    fun getChapterPercentageComplete(): Float {
        Log.d("the percentage calculation: ", "${reader.currentWordIdx} / $max = ${reader.currentWordIdx.toFloat() / max}")
        return reader.currentWordIdx.toFloat() / max
    }

}


