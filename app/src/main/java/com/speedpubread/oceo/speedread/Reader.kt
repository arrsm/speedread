package com.speedpubread.oceo.speedread

import android.app.Activity
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.TextView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit

class Reader(var WPM: Long = 0,
             var sentenceDelay: Long = 0,
             var currSentenceStart: Int = 0,
             var currSentenceIdx: Int = 0,
             var currentWordIdx: Int = 0,
             var maxWordIdx: Int = 0,
             var currentChapter: Int = 0,
             var wordOffset: Int = 0,
             var seeker: Seeker,
             activity: Activity,
             rootView: View,
) {

    private val TAG = "Reader"
    val currentChunkView: TextView = rootView.findViewById(R.id.current_chunk)
    val currentWordView: TextView = rootView.findViewById(R.id.current_word)

    var disposableReader: Disposable? = null
    val WPM_KEY = "wpm"
    val SENTENCE_DELAY_KEY = "sentence_delay"
    var chapter: ArrayList<String>? = null

    init {
        WPM = PrefsUtil.readLongFromPrefs(activity, WPM_KEY)
        sentenceDelay = PrefsUtil.readLongFromPrefs(activity, SENTENCE_DELAY_KEY)
    }


    fun validateSection(section: Int, minVal: Int, maxVal: Int): Boolean {
        // TODO test cases
//        Log.d("VALIDIATIOn", section.toString())
        return section in (minVal + 1) until maxVal
    }

    fun disposeListener() {
//        Log.d("disposing Listener", "START")
//        Log.d("disposing Listener", "reader.currentWordIdx: " + currentWordIdx.toString())
//        Log.d("disposing Listener", "currentSentenceStart: " + reader.currSentenceStart.toString())
//        Log.d("disposing Listener", "cur: " + reader.currSentenceStart.toString())
        if (disposableReader != null && !disposableReader!!.isDisposed) {
            disposableReader!!.dispose()
        }
    }

    fun iterateWords() {
        seeker.setSeekBarData(wordOffset, currentWordIdx)
        val sentencesEndIdx = getNextSentencesStartIdx(chapter, 1, currentWordIdx)
//        val sentenceStartAt = getSentenceStartIdx(currentWordIdx)
//        val displayStrs = buildBoldSentences(chapter, sentenceStartAt, sentencesEndIdx)

        // TODO
        val displayStrs = buildBoldSentences(chapter, currSentenceStart, sentencesEndIdx)
        val tempWordIdx = currSentenceStart

        Log.d("OBSERVABLE", "--------------------OBS setup---------------------")
        Log.d("Chapter: ", chapter.toString())
        Log.d("maxWordIdx (chapter)", maxWordIdx.toString())
        Log.d("maxWordIdx (total)", maxWordIdx.toString())
        Log.d("tempWordIdx", tempWordIdx.toString())
        Log.d("sentencesEndIdx", sentencesEndIdx.toString())
        Log.d("WPM_MS", SpeedReadUtilities.WPMtoMS(WPM).toString())
        Log.d("WPM", WPM.toString())
        Log.d("sentenceDelay", sentenceDelay.toString())
        Log.d("OBSERVABLE", "--------------------OBS-setup-END---------------------\n\n")

        var rangeObs: Observable<*> = Observable.range(tempWordIdx, sentencesEndIdx - currentWordIdx)
                .concatMap { i: Any ->
                    Observable.just(i)
                            .delay(SpeedReadUtilities.WPMtoMS(WPM), TimeUnit.MILLISECONDS)
                }

        rangeObs = rangeObs.delay(sentenceDelay, TimeUnit.MILLISECONDS) // delay at the end of the sentence
        rangeObs = rangeObs.observeOn(AndroidSchedulers.mainThread())

        disposableReader = rangeObs.subscribe({ wordIdx: Any? ->
//            Log.d("wordIdx / sentencesEndIdx", wordIdx.toString() + " / " + sentencesEndIdx.toString());
            if (currSentenceIdx < displayStrs.size) {
//                Log.d("IN BOUNDS word / sentence.size", currSentenceIdx.toString() + " / " + displayStrs.size.toString())
                currentChunkView.text = Html.fromHtml(displayStrs[currSentenceIdx].toString())
                currentWordView.text = chapter!![currentWordIdx]
                currSentenceIdx++
                currentWordIdx++
                seeker.setSeekBarData(wordOffset, currentWordIdx)
            } else {
//                Log.d("OUT OF BOUNDS: currentWordIdx / displayStrs.size", currentWordIdx.toString() + " / " + displayStrs.size.toString())
            }
        }, { e: Any? -> }) {
            // FINALLY
            // move to next sentence, if currentwordIdx is less than the last word in the section
            if (currentWordIdx < maxWordIdx) {
                currSentenceIdx = 0
                currSentenceStart = currentWordIdx
                iterateWords()
            } else {
//                Log.d("FINISHED", "SCROLLING FOR CHAPTER HAS ENDED")
            }
        }
    }


    fun getNextSentencesStartIdx(tokens: java.util.ArrayList<String>?, numSentences: Int, startIdx: Int): Int {
        var start = startIdx
        var foundSentences = 0
        while (foundSentences < numSentences) {
            while (start < maxWordIdx && (!tokens!![start].contains(".")
                            || tokens[start].contains("?")
                            || tokens[start].contains("!"))) {
                start++
            }
            start += 1
            foundSentences += 1
        }
        if (tokens != null && startIdx < tokens.size && tokens[startIdx].contains("”")) {
            start += 1
        }
        return start
    }

    fun buildBoldSentences(tokenList: java.util.ArrayList<String>?, startIdx: Int, endIdx: Int): java.util.ArrayList<StringBuilder> {
        var end = endIdx
        if (end > maxWordIdx) {
            end = maxWordIdx
        }
        val displayStrs = java.util.ArrayList<StringBuilder>()
        for (targetWord in startIdx until end) {
            val formattedDisplayStr = StringBuilder()
            for (i in startIdx until end) {
                if (targetWord == i) {
                    formattedDisplayStr.append("<font color=\"gray\">" + tokenList!![i] + "</font> ")
                } else {
                    formattedDisplayStr.append(tokenList!![i] + " ")
                }
            }
            displayStrs.add(formattedDisplayStr)
        }
        return displayStrs
    }

    fun getSentenceStartIdx(idx: Int): Int {
        var temp = idx
        if (chapter == null) return 0
        if (temp >= chapter!!.size) temp = chapter!!.size - 1
        while (temp > 0 && !chapter!![temp].contains(".")) {
            temp -= 1
        }
        return temp + 1
    }

    fun resumeBook() {
        disposeListener()
        iterateWords()
    }

    fun resumeReading(chapter: ArrayList<String>) {
        disposeListener()
        this.chapter = chapter
        currSentenceIdx = 0
        iterateWords()
    }

    fun loadChapter(chapter: ArrayList<String>, wordIndex: Int) {
        disposeListener()
        this.chapter = chapter
        currSentenceStart = getSentenceStartIdx(wordIndex)
        currentWordIdx = wordIndex
        currSentenceIdx = 0
        iterateWords()
    }

}