package com.speedpubread.oceo.speedread

class Reader(var WPM: Long = 0,
             var sentenceDelay: Long = 0,
             var currSentenceStart: Int = 0,
             var currSentenceIdx: Int = 0,
             var currentWordIdx: Int = 0,
             var maxWordIdx: Int = 0,
             var currentChapter: Int = 0) {
    init {
    }


    private var WPM_MS: Long = 0


}