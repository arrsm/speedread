package com.speedpubread.oceo.speedread

import android.app.Activity
import android.view.View
import android.widget.Button
import nl.siegmann.epublib.domain.Book

class ChapterControls(val activity: Activity,
                      rootView: View,
                      val reader: Reader,
                      var bookDetails: HashMap<String?, String?>,
                      val chosenFileName: String) {

    val CHAPTER_KEY = "chapter"
    val raiseChapterButton: Button = rootView.findViewById(R.id.raise_chpt_button)
    val currentChapterview: Button = rootView.findViewById(R.id.current_chapter)
    val lowerChapterButton: Button = rootView.findViewById(R.id.lower_chpt_btn)

    fun setupChapterControls(book: Book?) {
        val maxChapter = (book!!.spine.spineReferences.size)
        raiseChapterButton.setOnClickListener(View.OnClickListener {
            if (reader.currentChapter < maxChapter - 1) {
                reader.currentChapter += 1
                currentChapterview.text = "Section: ${reader.currentChapter + 1}/${maxChapter}"
                reader.disposeListener()
                bookDetails[CHAPTER_KEY] = reader.currentChapter.toString()
                PrefsUtil.writeBookDetailsToPrefs(activity, chosenFileName, bookDetails)
//                reader.nextChapter()
//                setStoryTokens()
//                reader.iterateWords(story!!, currentChunkView!!, currentWordView!!, chptProgressView!!, chapterSeekBar!!)
            }
        })
        lowerChapterButton.setOnClickListener(View.OnClickListener {
            if (reader.currentChapter > 0) {
                reader.currentChapter -= 1
                currentChapterview.text = "Section: ${reader.currentChapter + 1}/${maxChapter}"
                bookDetails[CHAPTER_KEY] = reader.currentChapter.toString()
                PrefsUtil.writeBookDetailsToPrefs(activity, chosenFileName, bookDetails)
                reader.disposeListener()
//                reader.resetChapter()
//                setStoryTokens()
//                reader.iterateWords(story!!, currentChunkView!!, currentWordView!!, chptProgressView!!, chapterSeekBar!!)
            }
        })
        currentChapterview.text = "Section: ${reader.currentChapter + 1}/${maxChapter}"
    }

}