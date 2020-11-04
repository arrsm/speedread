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
import com.speedpubread.oceo.speedread.EPubLibUtil.Companion.getBook
import com.speedpubread.oceo.speedread.SpeedReadUtilities.Companion.bookNameFromPath
import com.speedpubread.oceo.speedread.parser.parseChapter
import com.speedpubread.oceo.speedread.parser.parseBook
import nl.siegmann.epublib.domain.Book
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class BookReaderFragment(val book: Book) : Fragment() {
    var TAG = "BookReaderFragment"
    var activity: Activity? = null

    // logic globals
    protected var chosenFilePath: String? = null
    protected var chosenFileName: String? = null
    lateinit var reader: Reader
    lateinit var wpm: WPM
    lateinit var seeker: Seeker
    lateinit var chapterControl: ChapterControl

    // views
    lateinit var rootView: View
    private var currentChunkView: TextView? = null
    private var titleView: TextView? = null
    private var chapterSeekBar: SeekBar? = null
    private var pauseResumeBtn: Button? = null

    // prefs keys
    val CHAPTER_KEY = "chapter"
    val WORD_KEY = "page"
    val SENTENCE_START_KEY = "sentence_start"
    val TOTAL_WORDS = "total_words"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity = getActivity()
        val bundle = this.arguments
        chosenFilePath = bundle!!.getString("file_path")
        chosenFileName = bookNameFromPath(chosenFilePath!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.book_reader, container, false)
        titleView = rootView.findViewById(R.id.item_title)
        currentChunkView = rootView.findViewById(R.id.current_chunk)
        pauseResumeBtn = rootView.findViewById(R.id.pause_resume)
        chapterSeekBar = rootView.findViewById(R.id.seekBar)
        titleView!!.text = chosenFileName?.replace("asset__", "")
        currentChunkView!!.movementMethod = ScrollingMovementMethod()

        val storyConfig = getStoryDetails() // metadata about user pos in book
        val offsets = getChapterTokens(book).map { it.size }
        val offset = offsets[storyConfig[CHAPTER_KEY]!!.toInt()]

        reader = Reader(activity = activity!!, rootView = rootView, currentChapter = storyConfig[CHAPTER_KEY]!!.toInt())
        wpm = WPM(activity!!, rootView, reader)
        setReaderPositionFromPrefs(storyConfig)
        Log.d("WILL READ", storyConfig[CHAPTER_KEY]!!)
        readChapter(storyConfig[CHAPTER_KEY]!!.toInt()) // sets some reader attrb reqd for seeker
//        readBook()
        chapterControl = ChapterControl(this, activity!!, rootView, reader, storyConfig, chosenFileName!!, book)
        seeker = Seeker(rootView, reader)

        pauseResumeBtn!!.setOnClickListener(View.OnClickListener {
            if (!reader.disposableReader!!.isDisposed) {
                pauseResumeBtn!!.text = ">"
                reader.disposeListener()
            } else {
                pauseResumeBtn!!.text = "||"
                reader.iterateWords()
            }
        })
        return rootView
    }

    override fun onResume() {
        setReaderPositionFromPrefs(getStoryDetails())
        super.onResume()
    }

    override fun onPause() {
        reader.disposeListener()
        saveBookDetailsToPrefs()
        super.onPause()
    }

    fun saveBookDetailsToPrefs() {
        val bookDetails = getStoryDetails()
        val bookSize: String = if (bookDetails[TOTAL_WORDS] == null) getBookWords().size.toString() else bookDetails[TOTAL_WORDS]!!
        val theBookChapters = getChapterTokens(book)

        bookDetails[TOTAL_WORDS] = bookSize
        bookDetails[WORD_KEY] = reader.currentWordIdx.toString()
        Log.d("the current word is", bookDetails[WORD_KEY].toString())
        bookDetails[SENTENCE_START_KEY] = reader.currSentenceStart.toString()

        val chpt = getChapterWord(bookDetails[WORD_KEY]!!.toInt(), theBookChapters.map { it.size } as ArrayList<Int>)
        bookDetails[CHAPTER_KEY] = chpt.toString()

        PrefsUtil.writeBookDetailsToPrefs(activity!!, chosenFileName!!, bookDetails)
    }

    fun getChapterWord(word: Int, chapterLengths: ArrayList<Int>): Int {
        val cumSum = cumSum(chapterLengths)
//        Log.d("the chapter lengths are", chapterLengths.toString())
//        Log.d("the cum sum is", cumSum.toString())
//        Log.d("Length comparison", "chapters: ${chapterLengths.size} sum:${cumSum.size}")
        var i = 0
        while (word > cumSum[i]) {
            i += 1
        }
        return i + 1
    }

    fun cumSum(nums: ArrayList<Int>): ArrayList<Int> {
        var acc = 0
        return nums.map { acc += it; acc } as ArrayList<Int>
    }


    fun readChapter(chapterId: Int) {
        val chapter = parseChapter(book, chapterId)
        val chapterText = StringBuilder(chapter!!)
        val tokens = getWordTokens(chapterText.toString())?.let { tokensToArrayList(it) }
                ?: ArrayList()

        Log.d("readChapter:", "currentChapter: ${chapterId} maxWordIdx: ${tokens.size}")
        Log.d("readerDetails",
                "readerCurrWord: ${reader.currentWordIdx} " + "sentenceStart: ${reader.currSentenceStart}")


        reader.maxWordIdx = tokens.size
        reader.currentChapter = chapterId
        chapterSeekBar!!.max = tokens.size
        reader.loadChapter(tokens)
    }

    fun readBook() {
        val bookTokens = getBookWords()
        reader.maxWordIdx = bookTokens.size
//        reader.currentChapter = 0
        chapterSeekBar!!.max = bookTokens.size
        reader.loadChapter(bookTokens)
    }

    fun getBookWords(): ArrayList<String> {
        val fullBook = parseBook(book).joinToString(" ")
        val fullBookText = StringBuilder(fullBook)
        val bookTokens = tokensToArrayList(getWordTokens(fullBookText.toString())!!)
        return bookTokens
    }

    fun getChapterTokens(book: Book): List<ArrayList<String>> {
        //  use this in place of above to return tokens by chapter
        val fullBook = parseBook(book)
        val chaptersText = fullBook.map { StringBuilder(it.toString()) }
        val chaptersTokens = chaptersText.map { tokensToArrayList(getWordTokens(it.toString())!!) }
        return chaptersTokens
    }

    fun setReaderPositionFromPrefs(bookDetails: HashMap<String, String>) {
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

    fun getStoryDetails(): HashMap<String, String> {
        // metadata about users book. eg currentchapter, current word etc from profs
        return PrefsUtil.readBookDetailsFromPrefs(activity!!, chosenFileName)
                ?: hashMapOf(CHAPTER_KEY to "0")
    }


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