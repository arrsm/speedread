package com.speedpubread.oceo.speedread

import android.annotation.SuppressLint
import android.view.View
import android.widget.SeekBar
import android.widget.TextView

class Seeker(rootView: View, val reader: Reader) {

    val chapterSeekBar: SeekBar = rootView.findViewById(R.id.seekBar)
    val chptProgressView: TextView = rootView.findViewById(R.id.chapter_progress_view)

    init {
        setupSeekbar()
    }

    @SuppressLint("SetTextI18n")
    fun setupSeekbar() {
        chapterSeekBar.max = reader.maxWordIdx
        chapterSeekBar.min = 0
        chapterSeekBar.progress = reader.currentWordIdx
        val chptPercentageComplete = getChapterPercentageComplete()
        if (chptPercentageComplete.toString().length > 3) {
            chptProgressView.setText(chptPercentageComplete.toString().substring(0, 4) + "%")
        } else {
            chptProgressView.setText("$chptPercentageComplete%")
        }

        chapterSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
//                Log.d("seeking: ", String.valueOf(progress));
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
//                Log.d(TAG, "seekBar start tracking touch");
                reader.disposeListener()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
//                Log.d(TAG, "seekBar stop tracking touch");
                val progress = seekBar.progress
                reader.currSentenceStart = reader.getSentenceStartIdx(progress)
                reader.currentWordIdx = reader.currSentenceStart
                //                reader.currSentenceIdx = getWordPositionInSentence(progress/;
                reader.currSentenceIdx = 0
                if (chptPercentageComplete.toString().length > 3) {
                    chptProgressView.setText(chptPercentageComplete.toString().substring(0, 4) + "%")
                } else {
                    chptProgressView.setText("$chptPercentageComplete%")
                }
//                reader.iterateWords(story!!, currentChunkView!!, currentWordView!!, chptProgressView!!, chapterSeekBar!!)
            }
        })
    }

    fun getChapterPercentageComplete(): Float {
        return reader.currentWordIdx.toFloat() / reader.maxWordIdx.toFloat() * 100
    }

}


