package com.speedpubread.oceo.speedread

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.TextView

class Seeker(rootView: View,
             val max: Int = 0,
             val min: Int = 0) {

    val chapterSeekBar: SeekBar = rootView.findViewById(R.id.seekBar)
    val chptProgressView: TextView = rootView.findViewById(R.id.chapter_progress_view)

    init {
        chapterSeekBar.max = max
        chapterSeekBar.min = min
        setupSeekbar()
    }

    @SuppressLint("SetTextI18n")
    fun setupSeekbar() {
        Log.d("init seekbar", "max: " + max.toString())

        chapterSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
//                Log.d("seeking: ", "$progress / $max");
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
                // -------------------
                Log.d("SeekBar", "---------------- SEEKBAR TOUCH---------------")
                // progress === currWord / total words
                // chapter from getChapterWord in bookReader fragment
                // calculate word in chapter: (word - offset)
                // calculate sentence start, and then use those to iterate from that point



                Log.d("SeekBar", "----------------STOP TOUCH SEEKBAR ---------------")
            }
        })
    }

    fun setSeekBarData(wordOffset: Int, currentWordIdx: Int) {
//        Log.d("setting seek bar data", "offset ${wordOffset} curr ${currentWordIdx} max ${chapterSeekBar.max}")
        val chptPercentageComplete = getChapterPercentageComplete(currentWordIdx, wordOffset)
        if (chptPercentageComplete.toString().length > 3) {
            chptProgressView.setText(chptPercentageComplete.toString().substring(0, 4) + "%")
        } else {
            chptProgressView.setText("$chptPercentageComplete%")
        }
        chapterSeekBar.progress = currentWordIdx + wordOffset
    }

    fun setSeekerMax(maxVal: Int) {
        chapterSeekBar.max = maxVal
    }

    fun setSeekerMin(minVal: Int) {
        chapterSeekBar.min = minVal

    }

    fun getChapterPercentageComplete(currentWordIdx: Int, wordOffset: Int): Float {
//        Log.d("the percentage calculation: ", "${reader.currentWordIdx} / $max = ${reader.currentWordIdx.toFloat() / max}")
        return (currentWordIdx + wordOffset).toFloat() / max * 100
    }

}


