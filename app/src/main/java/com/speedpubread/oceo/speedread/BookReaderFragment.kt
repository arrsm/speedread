package com.speedpubread.oceo.speedread

import android.app.Activity
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.speedpubread.oceo.speedread.EPubLibUtil.Companion.exploreTOC
import com.speedpubread.oceo.speedread.EPubLibUtil.Companion.getBook
import com.speedpubread.oceo.speedread.EPubLibUtil.Companion.getTOCResourceIds
import com.speedpubread.oceo.speedread.SpeedReadUtilities.Companion.bookNameFromPath
import com.speedpubread.oceo.speedread.parser.getChapter
import nl.siegmann.epublib.domain.Book
import java.util.*
import kotlin.collections.HashMap

class BookReaderFragment : Fragment() {
    var TAG = "BookReaderFragment"
    var activity: Activity? = null
    var frag: Fragment? = null

    // logic globals
    var book: Book? = null
    var firstTimeFlag = 0 // should spinner action be called
    protected var fullText: StringBuilder? = null// holds full story in memory
    private var story: ArrayList<String>? = null // fullText converted to arraylist
    private var tocResourceIds: ArrayList<String>? = null

    protected var chosenFilePath: String? = null
    protected var chosenFileName: String? = null
    lateinit var reader: Reader
    lateinit var wpm: WPM
    lateinit var seeker: Seeker

    // views
    private var rootView: View? = null
    private var currentWordView: TextView? = null
    private var currentChunkView: TextView? = null
    private var currentChapterview: TextView? = null
    private var raiseChapterButton: Button? = null
    private var lowerChapterButton: Button? = null
    private var titleView: TextView? = null
    private var chapterSeekBar: SeekBar? = null
    private var chptProgressView: TextView? = null
    private var pauseResumeBtn: Button? = null

    // prefs keys
    val CHAPTER_KEY = "chapter"
    val WORD_KEY = "page"
    val SENTENCE_START_KEY = "sentence_start"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity = getActivity()
        frag = this
        val bundle = this.arguments
        chosenFilePath = bundle!!.getString("file_path")
        chosenFileName = bookNameFromPath(chosenFilePath!!)
        PrefsUtil.writeBookToPrefs(activity!!, chosenFilePath)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.book_reader, container, false)
        titleView = rootView!!.findViewById(R.id.item_title)
        currentChunkView = rootView!!.findViewById(R.id.current_chunk)
        pauseResumeBtn = rootView!!.findViewById(R.id.pause_resume)
        chapterSeekBar = rootView!!.findViewById(R.id.seekBar)
        chptProgressView = rootView!!.findViewById(R.id.chapter_progress_view)
        currentWordView = rootView!!.findViewById(R.id.current_word)

        titleView!!.text = chosenFileName?.replace("asset__", "")
        currentChunkView!!.movementMethod = ScrollingMovementMethod()

        book = getBook(chosenFilePath, context!!)
        val storyConfig = getStoryDetails() // metadata about user pos in book
        reader = Reader(activity = activity!!, bookDetails = storyConfig!!, rootView = rootView!!)
        setReaderPositionFromPrefs(storyConfig)
        val tokens = getStory(storyConfig[CHAPTER_KEY]!!.toInt())

        wpm = WPM(activity!!, rootView!!, reader)
        wpm.setupWPMControls()

        readChapter(storyConfig[CHAPTER_KEY]!!.toInt())
//        if (tokens != null) {
//            story = tokens
//            reader.chapter = tokens
//            reader.maxWordIdx = story!!.size
//            chapterSeekBar!!.max = reader.maxWordIdx
//        }


        setupChapterControls(book)

        seeker = Seeker(rootView!!, reader, tokens!!)
        seeker.setupSeekbar()

        pauseResumeBtn!!.setOnClickListener(View.OnClickListener {
            if (!reader.disposableReader!!.isDisposed) {
                pauseResumeBtn!!.text = ">"
                reader.disposeListener()
            } else {
                pauseResumeBtn!!.text = "||"
                reader.iterateWords()
            }
        })
        tocResourceIds = getTOCResourceIds(exploreTOC(book!!), 0, ArrayList<String>())


        return rootView
    }

    override fun onResume() {
        // updaste this for continue on resume
        if (book != null) {
            setReaderPositionFromPrefs(getStoryDetails()!!)
            if (firstTimeFlag == 0) {
//                reader.iterateWords()
            }
        }
        super.onResume()
    }

    override fun onPause() {
        reader.disposeListener()
        saveBookDetailsToPrefs()
        super.onPause()
    }

    fun saveBookDetailsToPrefs() {
        val bookDetails = getStoryDetails()
        bookDetails!![CHAPTER_KEY] = reader.currentChapter.toString()
        bookDetails[WORD_KEY] = reader.currentWordIdx.toString()
        bookDetails[SENTENCE_START_KEY] = reader.currSentenceStart.toString()
        reader.bookDetails = bookDetails
        PrefsUtil.writeBookDetailsToPrefs(activity!!, chosenFileName!!, bookDetails)
    }


    fun getStory(chapter: Int): ArrayList<String>? {
        val chapterTxt = getChapter(book!!.spine, chapter, book!!, rootView!!)
        fullText = StringBuilder(chapterTxt!!)
        val tokens = getWordTokens(fullText.toString())
        return tokensToArrayList(tokens!!)
    }


    fun setupChapterControls(book: Book?) {

        val maxChapter = (book!!.spine.spineReferences.size)
        raiseChapterButton = rootView!!.findViewById(R.id.raise_chpt_button)
        lowerChapterButton = rootView!!.findViewById(R.id.lower_chpt_btn)
        currentChapterview = rootView!!.findViewById(R.id.current_chapter)
        raiseChapterButton!!.setOnClickListener(View.OnClickListener {
            if (reader.currentChapter < maxChapter - 1) {
                reader.currentChapter += 1
                currentChapterview!!.text = "Section: ${reader.currentChapter + 1}/${maxChapter}"

                val bookDetails = getStoryDetails()
                bookDetails!![CHAPTER_KEY] = reader.currentChapter.toString()
                PrefsUtil.writeBookDetailsToPrefs(activity!!, chosenFileName!!, bookDetails)
                readChapter(reader.currentChapter)
            }
        })
        lowerChapterButton!!.setOnClickListener(View.OnClickListener {
            if (reader.currentChapter > 0) {
                reader.currentChapter -= 1
                currentChapterview!!.text = "Section: ${reader.currentChapter + 1}/${maxChapter}"
                val bookDetails = getStoryDetails()
                bookDetails!![CHAPTER_KEY] = reader.currentChapter.toString()
                PrefsUtil.writeBookDetailsToPrefs(activity!!, chosenFileName!!, bookDetails)
                readChapter(reader.currentChapter)
            }
        })
        currentChapterview!!.text = "Section: ${reader.currentChapter + 1}/${maxChapter}"
    }

    fun readChapter(chapter: Int) {
        // dependency on chapter..
        val tokens = getStory(chapter)
        tokens?.let {
            reader.maxWordIdx = it.size
            chapterSeekBar!!.max = it.size
            reader.loadChapter(it)
        }
    }

    fun setReaderPositionFromPrefs(bookDetails: HashMap<String?, String?>) {
        val tempChpt = bookDetails[CHAPTER_KEY]
        val tempWord = bookDetails[WORD_KEY]
        val tempSentenceStart = bookDetails[SENTENCE_START_KEY]

        reader.currentChapter = if (tempChpt == null) 0 else Integer.valueOf(tempChpt)
        reader.currentWordIdx = if (tempWord == null) 0 else Integer.valueOf(tempWord)
        reader.currSentenceStart = if (tempSentenceStart == null) 0 else Integer.valueOf(tempSentenceStart)
    }

    fun getUserConfigFromPrefs() {
        val WPM = PrefsUtil.readLongFromPrefs(activity!!, "wpm")
        val sentenceDelay = PrefsUtil.readLongFromPrefs(activity!!, "sentence_delay")
    }

    fun getStoryDetails(): HashMap<String?, String?>? {
        // metadata about users book. eg currentchapter, current word etc from profs
        return PrefsUtil.readBookDetailsFromPrefs(activity!!, chosenFileName)?.let { it }
                ?: HashMap()
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