package com.speedpubread.oceo.speedread

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnLongClickListener
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.fragment.app.Fragment
import com.speedpubread.oceo.speedread.EPubLibUtil.Companion.exploreTOC
import com.speedpubread.oceo.speedread.EPubLibUtil.Companion.getBook
import com.speedpubread.oceo.speedread.EPubLibUtil.Companion.getTOCResourceIds
import com.speedpubread.oceo.speedread.SpeedReadUtilities.Companion.WPMtoMS
import com.speedpubread.oceo.speedread.SpeedReadUtilities.Companion.bookNameFromPath
import com.speedpubread.oceo.speedread.parser.getChapter
import nl.siegmann.epublib.domain.Book
import java.util.*

class BookReaderFragment : Fragment() {
    var TAG = "BookReaderFragment"
    var activity: Activity? = null
    var frag: Fragment? = null

    // logic globals
    var book: Book? = null
    private var WPM_MS: Long = 0
    var firstTimeFlag = 0 // should spinner action be called
    private var chptPercentageComplete = 0f
    protected var fullText: StringBuilder? = null// holds full story in memory
    private var story: ArrayList<String>? = null // fullText converted to arraylist
    private var displayStrs: ArrayList<StringBuilder>? = null // crutch to display bolded words. would like to change
    private var tocResourceIds: ArrayList<String>? = null
    var bookDetails: HashMap<String?, String?>? = null
    protected var chosenFilePath: String? = null
    protected var chosenFileName: String? = null
    val reader = Reader()

    // views
    private var rootView: View? = null
    private var fullStoryView: TextView? = null
    private var currentWordView: TextView? = null
    private var currentChunkView: TextView? = null
    private var raiseWPMButton: Button? = null
    private var lowerWPMButton: Button? = null
    private var currentChapterview: TextView? = null
    private var raiseChapterButton: Button? = null
    private var lowerChapterButton: Button? = null
    private var titleView: TextView? = null
    private var WPM_view: TextView? = null
    private var dropdown: Spinner? = null
    private var chapterSeekBar: SeekBar? = null
    private var chptProgressView: TextView? = null
    private var pauseResumeBtn: Button? = null

    // prefs keys
    val CHAPTER_KEY = "chapter"
    val WORD_KEY = "page"
    val SENTENCE_START_KEY = "sentence_start"
    val WPM_KEY = "wpm"
    val SENTENCE_DELAY_KEY = "sentence_delay"

    //long held incrementers
    private var autoIncrementWPM = false
    private var autoDecrementWPM = false
    private val REPEAT_DELAY: Long = 50
    private val WPMUpdateHandler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity = getActivity()
        frag = this
        val bundle = this.arguments
        chosenFilePath = bundle!!.getString("file_path")
        chosenFileName = bookNameFromPath(chosenFilePath!!)
        setDefaultValues()
    }

    override fun onResume() {
//        Log.d(TAG, "bookreader fragment resumes")
        if (book != null) {
            val tempChpt = bookDetails!![CHAPTER_KEY]
            val tempWord = bookDetails!![WORD_KEY]
            val tempSentenceStart = bookDetails!![SENTENCE_START_KEY]
            // TODO make fn to load this within the reader class
            reader.currentChapter = if (tempChpt == null) 0 else Integer.valueOf(tempChpt)
            reader.currentWordIdx = if (tempWord == null) 0 else Integer.valueOf(tempWord)
            reader.currSentenceStart = if (tempSentenceStart == null) 0 else Integer.valueOf(tempSentenceStart)
            if (firstTimeFlag == 0) {
                reader.iterateWords(story!!, currentChunkView!!, currentWordView!!, chptProgressView!!, chapterSeekBar!!)
            }
        }
        super.onResume()
    }

    override fun onPause() {
        reader.disposeListener()
        bookDetails!![CHAPTER_KEY] = reader.currentChapter.toString()
        bookDetails!![WORD_KEY] = reader.currentWordIdx.toString()
        bookDetails!![SENTENCE_START_KEY] = reader.currSentenceStart.toString()
        PrefsUtil.writeBookDetailsToPrefs(activity!!, chosenFileName!!, bookDetails)
        super.onPause()
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.book_reader, container, false)
        titleView = rootView!!.findViewById(R.id.item_title)
        titleView!!.text = chosenFileName?.replace("asset__", "")

        book = getBook(chosenFilePath, context!!)
        if (!(validateSection(reader.currentChapter, 0, book!!.spine.spineReferences.size - 1))) {
            reader.currentChapter = 0
        }
        setupWPMControls()
        setupChapterControls(book)
        currentChunkView = rootView!!.findViewById(R.id.current_chunk)
        currentChunkView!!.movementMethod = ScrollingMovementMethod()

        pauseResumeBtn = rootView!!.findViewById(R.id.pause_resume)
        pauseResumeBtn!!.setOnClickListener(View.OnClickListener {
            if (!reader.disposableReader!!.isDisposed) {
                pauseResumeBtn!!.setText(">")
                reader.disposeListener()
            } else {
                pauseResumeBtn!!.setText("||")
                reader.iterateWords(story!!, currentChunkView!!, currentWordView!!, chptProgressView!!, chapterSeekBar!!)
            }
        })

        currentWordView = rootView!!.findViewById(R.id.current_word)
        chapterSeekBar = rootView!!.findViewById(R.id.seekBar)
        chptProgressView = rootView!!.findViewById(R.id.chapter_progress_view)

        if (book != null) {
            PrefsUtil.writeBookToPrefs(activity!!, chosenFilePath)
            tocResourceIds = getTOCResourceIds(exploreTOC(book!!), 0, ArrayList<String>())
            setStoryTokens()
        }

        setupSeekbar()
        return rootView
    }

    // TODO move to new class that will handle the seekbar
    fun setupSeekbar() {
        chapterSeekBar!!.max = reader.maxWordIdx
        chapterSeekBar!!.min = 0
        chapterSeekBar!!.progress = reader.currentWordIdx
        chptPercentageComplete = java.lang.Float.valueOf(reader.currentWordIdx.toFloat()) / java.lang.Float.valueOf(reader.maxWordIdx.toFloat()) * 100
        if (chptPercentageComplete.toString().length > 3) {
            chptProgressView!!.setText(chptPercentageComplete.toString().substring(0, 4) + "%")
        } else {
            chptProgressView!!.setText("$chptPercentageComplete%")
        }

        chapterSeekBar!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
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
                reader.currSentenceStart = getSentenceStartIdx(progress)
                reader.currentWordIdx = reader.currSentenceStart
                //                reader.currSentenceIdx = getWordPositionInSentence(progress/;
                reader.currSentenceIdx = 0
                if (chptPercentageComplete.toString().length > 3) {
                    chptProgressView!!.setText(chptPercentageComplete.toString().substring(0, 4) + "%")
                } else {
                    chptProgressView!!.setText("$chptPercentageComplete%")
                }
//                iterateWords()
                reader.iterateWords(story!!, currentChunkView!!, currentWordView!!, chptProgressView!!, chapterSeekBar!!)
            }
        })
    }



    fun setStoryTokens() {
        // sets fullText (String containing entire chapter text)
        // and calculates and sets story(ArrayList of each word in chapter)
        val chapter = readSampleChapter(book, reader.currentChapter)
        fullText = StringBuilder(chapter!!)
        val tokens = getWordTokens(fullText.toString())
        if (tokens != null) {
            reader.maxWordIdx = tokens.countTokens()
            chapterSeekBar!!.max = reader.maxWordIdx
            story = tokensToArrayList(tokens)
        }
    }

    fun getSentenceStartIdx(idx: Int): Int {
        var idx = idx
        while (!story!![idx].contains(".") && idx > 0) {
            idx -= 1
        }
        return idx + 1
    }


    fun validateSection(section: Int, minVal: Int, maxVal: Int): Boolean {
//        Log.d("VALIDIATIOn", section.toString())
        return section in (minVal + 1) until maxVal
    }


    fun setupChapterControls(book: Book?) {

        val maxChapter = (book!!.spine.spineReferences.size)
        raiseChapterButton = rootView!!.findViewById(R.id.raise_chpt_button)
        lowerChapterButton = rootView!!.findViewById(R.id.lower_chpt_btn)
        currentChapterview = rootView!!.findViewById(R.id.current_chapter)
        raiseChapterButton!!.setOnClickListener(View.OnClickListener {
            if (reader.currentChapter < maxChapter - 1) {
                reader.currentChapter += 1
                currentChapterview!!.setText("Section: ${reader.currentChapter + 1}/${maxChapter}")
//                disposeListener()
                reader.disposeListener()
                bookDetails!![CHAPTER_KEY] = reader.currentChapter.toString()
                PrefsUtil.writeBookDetailsToPrefs(activity!!, chosenFileName!!, bookDetails)
                resetChapterGlobals()
                setStoryTokens()
                reader.iterateWords(story!!, currentChunkView!!, currentWordView!!, chptProgressView!!, chapterSeekBar!!)
//                iterateWords()
            }
        })
        lowerChapterButton!!.setOnClickListener(View.OnClickListener {
            if (reader.currentChapter > 0) {
                reader.currentChapter -= 1
                currentChapterview!!.setText("Section: ${reader.currentChapter + 1}/${maxChapter}")
                bookDetails!![CHAPTER_KEY] = reader.currentChapter.toString()
                PrefsUtil.writeBookDetailsToPrefs(activity!!, chosenFileName!!, bookDetails)
                reader.disposeListener()
                resetChapterGlobals()
                setStoryTokens()
                reader.iterateWords(story!!, currentChunkView!!, currentWordView!!, chptProgressView!!, chapterSeekBar!!)
            }
        })
        currentChapterview!!.setText("Section: ${reader.currentChapter + 1}/${maxChapter}")
    }

    fun resetChapterGlobals() {
        reader.currSentenceStart = 0
        reader.currentWordIdx = 0
        reader.currSentenceIdx = 0
    }

    fun setDefaultValues() {
        // gets data stored in prefs and sets defaults if values are unreasonable
        val activityCopy = activity!!
        reader.WPM = PrefsUtil.readLongFromPrefs(activityCopy, WPM_KEY)
        WPM_MS = WPMtoMS(reader.WPM)
        reader.sentenceDelay = PrefsUtil.readLongFromPrefs(activityCopy, SENTENCE_DELAY_KEY)
        bookDetails = PrefsUtil.readBookDetailsFromPrefs(activityCopy, chosenFileName) as HashMap<String?, String?>?
        if (bookDetails == null) {
            bookDetails = HashMap()
        }
        val tempChpt = bookDetails!![CHAPTER_KEY]
        val tempWord = bookDetails!![WORD_KEY]
        val tempSentenceStart = bookDetails!![SENTENCE_START_KEY]

        reader.currentChapter = if (tempChpt == null) 0 else Integer.valueOf(tempChpt)
        reader.currentWordIdx = if (tempWord == null) 0 else Integer.valueOf(tempWord)
        reader.currSentenceStart = if (tempSentenceStart == null) 0 else Integer.valueOf(tempSentenceStart)
        resetChapterGlobals()
    }

    fun readSampleChapter(book: Book?, chapterNumber: Int): String? {
        var chapterContents: String? = null
        val spine = book!!.spine
        chapterContents = getChapter(spine, chapterNumber, book, rootView!!)
        return chapterContents
    }


    @SuppressLint("ClickableViewAccessibility")
    fun setupWPMControls() {
        raiseWPMButton = rootView!!.findViewById(R.id.raise_wpm_button)
        lowerWPMButton = rootView!!.findViewById(R.id.lower_wpm_button)
        WPM_view = rootView!!.findViewById(R.id.current_wpm_view)
        WPM_view!!.setText(reader.WPM.toString())
        raiseWPMButton!!.setOnClickListener(View.OnClickListener { //                Log.d(TAG, "onClick");
            incrementWPM()
            // TODO necessary to do here AND in motion up?
            PrefsUtil.writeLongToPrefs(activity!!, WPM_KEY, reader.WPM)
        })
        raiseWPMButton!!.setOnLongClickListener(OnLongClickListener { //                Log.d(TAG, "long click");
            autoIncrementWPM = true
            WPMUpdateHandler.post(RepetitiveUpdater())
            false
        })
        raiseWPMButton!!.setOnTouchListener(OnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP && autoIncrementWPM) {
//                Log.d(TAG, "touch up")
                autoIncrementWPM = false
                WPM_MS = WPMtoMS(reader.WPM)
                PrefsUtil.writeLongToPrefs(activity!!, WPM_KEY, reader.WPM)
            }
            false
        })
        lowerWPMButton!!.setOnClickListener(View.OnClickListener { //                Log.d(TAG, "onClick");
            decrementWPM()
        })
        lowerWPMButton!!.setOnLongClickListener(OnLongClickListener { //                Log.d(TAG, "long click");
            autoDecrementWPM = true
            WPMUpdateHandler.post(RepetitiveUpdater())
            false
        })
        lowerWPMButton!!.setOnTouchListener(OnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP && autoDecrementWPM) {
//                    Log.d(TAG, "touch up");
                autoDecrementWPM = false
                WPM_MS = WPMtoMS(reader.WPM)
                PrefsUtil.writeLongToPrefs(activity!!, WPM_KEY, reader.WPM)
            }
            false
        })
    }

    fun incrementWPM() {
        if (reader.WPM < 1000) {
            reader.WPM++
            WPM_view!!.text = reader.WPM.toString()
        }
    }

    fun decrementWPM() {
        if (reader.WPM > 0) {
            reader.WPM--
            WPM_view!!.text = reader.WPM.toString()
        }
    }

    /* used to update WPM values while button held */
    internal inner class RepetitiveUpdater : Runnable {
        override fun run() {
            if (autoIncrementWPM) {
                incrementWPM()
                WPMUpdateHandler.postDelayed(RepetitiveUpdater(), REPEAT_DELAY)
            } else if (autoDecrementWPM) {
                decrementWPM()
                WPMUpdateHandler.postDelayed(RepetitiveUpdater(), REPEAT_DELAY)
            }
        }
    }

    companion object {
        fun getWordTokens(words: String?): StringTokenizer? {
            return if (words == null || words.isEmpty()) {
                null
            } else StringTokenizer(words, " \t\n\r\u000C", false)
        }

        fun tokensToArrayList(tokens: StringTokenizer): ArrayList<String> {
            // given a story tokenized by words dump them into arraylist
            val story = ArrayList<String>()
            while (tokens.hasMoreTokens()) {
                story.add(tokens.nextToken())
            }
            return story
        }
    }
}