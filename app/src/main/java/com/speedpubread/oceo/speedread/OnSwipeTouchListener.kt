package com.speedpubread.oceo.speedread

import android.content.Context
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener

class OnSwipeTouchListener(ctx: Context?) : OnTouchListener {
    //    https://stackoverflow.com/questions/4139288/android-how-to-handle-right-to-left-swipe-gestures
    private val gestureDetector: GestureDetector
    private val SWIPE_THRESHOLD = 100
    private val SWIPE_VELOCITY_THRESHOLD = 100
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    private inner class GestureListener : SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            var result = false
            try {
                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight()
                        } else {
                            onSwipeLeft()
                        }
                        result = true
                    }
                } else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        onSwipeBottom()
                    } else {
                        onSwipeTop()
                    }
                    result = true
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
            return result
        }


    }

    fun onSwipeRight() {
//       Toast.makeText(activity, "right", Toast.LENGTH_SHORT).show();
//        moveToPrevSentence()
    }

    fun onSwipeLeft() {
//        moveToNextSentence()
    }

    fun onSwipeTop() {}
    fun onSwipeBottom() {}


//    fun moveToPrevSentence() {
//        Log.d("moveToPrevSentence before: ", "curr: " + currSentenceStart.toString())
//        val prevSentenceStart = getSentenceStartIdx(currSentenceStart - 2)
//        Log.d("moveToPrevSentence after: ", "curr: $prevSentenceStart")
//        disposeListener()
//        currSentenceStart = prevSentenceStart
//        currentWordIdx = prevSentenceStart
//        currSentenceIdx = 0
//        val sentenceEndIdx = getNextSentencesStartIdx(story, 1, currentWordIdx)
//        val prevSentence = getStringFromTokenIndexes(prevSentenceStart, sentenceEndIdx)
//        currentChunkView!!.text = prevSentence
//        //        iterateWords();
//    }
//
//
//    fun moveToNextSentence() {
//        val nextSentenceStart = getNextSentencesStartIdx(story, 1, currentWordIdx)
//        Log.d("moveToPrevSentence before: ", "curr: " + currSentenceStart.toString())
//        val prevSentenceStart = getSentenceStartIdx(currSentenceStart - 2)
//        Log.d("moveToPrevSentence after: ", "curr: $prevSentenceStart")
//        disposeListener()
//        currSentenceStart = nextSentenceStart
//        currentWordIdx = nextSentenceStart
//        val sentenceEndIdx = getNextSentencesStartIdx(story, 1, nextSentenceStart)
//        val nextSentence = getStringFromTokenIndexes(nextSentenceStart, sentenceEndIdx)
//        currentChunkView!!.text = nextSentence
//        currSentenceIdx = 0
//        //        iterateWords();
//    }

    init {
        gestureDetector = GestureDetector(ctx, GestureListener())
    }
}