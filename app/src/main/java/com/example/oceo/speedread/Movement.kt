package com.example.oceo.speedread

// TODO consider encapsulating movement logic here

fun getWordPositionInSentence(idx: Int, story: ArrayList<String>?): Int {
    var tempIdx = idx
    var earlierTokenCount = 0
    while (!story!![tempIdx].contains(".") && tempIdx > 0) {
        tempIdx -= 1
        earlierTokenCount += 1
    }
    return 1.let { earlierTokenCount -= it; earlierTokenCount }
}

fun getStringFromTokenIndexes(startIdx: Int, endIdx: Int, story: ArrayList<String>?): StringBuilder {
    var temp = startIdx
    val str = StringBuilder()
    while (temp < endIdx) {
        str.append(story!![temp] + " ")
        temp++
    }
    return str
}

fun scrollSentences(numSentences: Int) {
    //TODO testing
    // the idea is to 'skip' some amount of content so a user could scroll easier
    // average number of sentences in a page for page turn?
//    val startIdx = getNextSentencesStartIdx(story, numSentences, currentWordIdx)
//    currSentenceStart = startIdx + 1
}