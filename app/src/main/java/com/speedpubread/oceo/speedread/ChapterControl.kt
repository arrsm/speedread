package com.speedpubread.oceo.speedread

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import android.widget.Button
import android.widget.TextView
import nl.siegmann.epublib.domain.Book

class ChapterControl(val context: BookReaderFragment,
                     val activity: Activity,
                     rootView: View,
                     val reader: Reader,
                     var storyConfig: HashMap<String?, String?>,
                     val chosenFileName: String,
                     book: Book) {

    val CHAPTER_KEY = "chapter"
    val raiseChapterButton: Button = rootView.findViewById(R.id.raise_chpt_button)
    val currentChapterview: TextView = rootView.findViewById(R.id.current_chapter)
    val lowerChapterButton: Button = rootView.findViewById(R.id.lower_chpt_btn)

    init {
        setupChapterControls(book)
    }

    @SuppressLint("SetTextI18n")
    fun setupChapterControls(book: Book?) {
        val maxChapter = (book!!.spine.spineReferences.size)
        raiseChapterButton.setOnClickListener(View.OnClickListener {
            if (reader.currentChapter < maxChapter - 1) {
                val nextChapter = reader.currentChapter + 1
                currentChapterview.text = "Section: ${nextChapter + 1}/${maxChapter}"
                storyConfig[CHAPTER_KEY] = nextChapter.toString()
                PrefsUtil.writeBookDetailsToPrefs(activity, chosenFileName, storyConfig)
                context.readChapter(nextChapter)
            }
        })
        lowerChapterButton.setOnClickListener(View.OnClickListener {
            if (reader.currentChapter > 0) {
                val prevChapter = reader.currentChapter - 1
                currentChapterview.text = "Section: ${prevChapter + 1}/${maxChapter}"
                storyConfig[CHAPTER_KEY] = prevChapter.toString()
                PrefsUtil.writeBookDetailsToPrefs(activity, chosenFileName, storyConfig)
                context.readChapter(prevChapter)
            }
        })
        currentChapterview.text = "Section: ${reader.currentChapter + 1}/${maxChapter}"
    }

}