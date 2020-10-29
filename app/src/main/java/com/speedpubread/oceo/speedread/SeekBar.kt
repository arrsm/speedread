package com.speedpubread.oceo.speedread

import android.widget.SeekBar
import android.widget.TextView

class Seeker(val chptProgressView: TextView, var chapterSeekBar: SeekBar, val reader: Reader) {

    fun setupSeekbar() {
        chapterSeekBar.min = 0
        chapterSeekBar.max = reader.maxWordIdx
        chapterSeekBar.progress = reader.currentWordIdx


        val chapterPercentageComplete = getChapterPercentageComplete()
        if (chapterPercentageComplete.toString().length > 3) {
            chptProgressView.text = chapterPercentageComplete.toString().substring(0, 4) + "%"
        } else {
            chptProgressView.text = "$chapterPercentageComplete%"
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
                if (chapterPercentageComplete.toString().length > 3) {
                    chptProgressView.text = chapterPercentageComplete.toString().substring(0, 4) + "%"
                } else {
                    chptProgressView.text = "$chapterPercentageComplete%"
                }

//                iterateWords()
//            reader.iterateWords(story!!, currentChunkView!!, currentWordView!!, chptProgressView!!, chapterSeekBar)
            }
        })
    }

    fun getChapterPercentageComplete(): Float {
        return reader.currentWordIdx.toFloat() / reader.maxWordIdx.toFloat() * 100
    }
}
