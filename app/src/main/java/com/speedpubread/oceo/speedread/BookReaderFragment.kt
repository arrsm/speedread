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
import com.speedpubread.oceo.speedread.SpeedReadUtilities.Companion.bookNameFromPath
import com.speedpubread.oceo.speedread.parser.parseChapter
import com.speedpubread.oceo.speedread.parser.parseBook
import nl.siegmann.epublib.domain.Book
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
        titleView!!.text = chosenFileName?.replace("asset__", "")
        currentChunkView!!.movementMethod = ScrollingMovementMethod()


        val storyConfig = getStoryDetails() // metadata about user pos in book
        val chapter = storyConfig[CHAPTER_KEY]!!.toInt()

        getChapterTitle(chapter)
        // TODO  make own function based on prefs
//        val offsets = getChapterTokens(book).map { it.size }
//        val offset = offsets[storyConfig[CHAPTER_KEY]!!.toInt()]

        // TODO provide seeker with info on current word as well rather than relying on the reader to do it
        seeker = Seeker(rootView, this, max = getBookTotalWords().toInt())
        reader = Reader(activity = activity!!, rootView = rootView, currentChapter = chapter, seeker = seeker)
        wpm = WPM(activity!!, rootView, reader)
        // TODO obtain the values for this stuff and provide to reader, rather than creating the reader first
        // and and then pulling the info to set
        setReaderPositionFromPrefs(storyConfig)

        readChapter(storyConfig[CHAPTER_KEY]!!.toInt()) // sets some reader attrb reqd for seeker

        chapterControl = ChapterControl(this, activity!!, rootView, reader, storyConfig, chosenFileName!!, book)

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
//        setReaderPositionFromPrefs(getStoryDetails())
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
        val chapterOffsets = getChapterTokens(book).map { it.size } as ArrayList
        chapterOffsets.add(0, 0) // add 0 index

        bookDetails[TOTAL_WORDS] = bookSize
        bookDetails[WORD_KEY] = reader.currentWordIdx.toString()
        bookDetails[SENTENCE_START_KEY] = reader.getSentenceStartIdx(reader.currentWordIdx).toString()
//        Log.d("Saving to Prefs",
//                "WORD_KEY: ${bookDetails[WORD_KEY]} " +
//                        "SENTENCE_START_KEY: " + "${bookDetails[SENTENCE_START_KEY]}")

        PrefsUtil.writeBookDetailsToPrefs(activity!!, chosenFileName!!, bookDetails)
        PrefsUtil.writeChapterSizes(activity!!, chosenFileName!!, chapterOffsets)
    }

    fun getChapterTitle(chapter: Int) {
//        Log.d(TAG, "--------------ToC checking----------------")
        val theBook = book
        val ToC = book.tableOfContents
        val uniqueResources = ToC.allUniqueResources
        val references = ToC.tocReferences

        val spine = book.spine
        val spineRefs = spine.spineReferences
//        Log.d(TAG, "--------------END ToC checking----------------")

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

//        Log.d("readChapter:", "currentChapter: ${chapterId} maxWordIdx: ${tokens.size}")
//        Log.d("readerDetails",
//                "readerCurrWord: ${reader.currentWordIdx} " + "sentenceStart: ${reader.currSentenceStart}")


        reader.currentChapter = chapterId
        reader.maxWordIdx = tokens.size
        reader.resumeReading(tokens)
    }

    fun flipChapter(chapterId: Int) {
        val chapter = parseChapter(book, chapterId)
        val chapterText = StringBuilder(chapter!!)
        val tokens = getWordTokens(chapterText.toString())?.let { tokensToArrayList(it) }
                ?: ArrayList()
        val offsets = getChapterOffsets()

//        Log.d("flipChapter:", "currentChapter: ${chapterId} maxWordIdx: ${tokens.size}")
//        Log.d("readerDetails",
//                "readerCurrWord: ${reader.currentWordIdx} " + "sentenceStart: ${reader.currSentenceStart}")


        reader.wordOffset = if (offsets != null) offsets[chapterId] else 0
        reader.currentChapter = chapterId
        reader.maxWordIdx = tokens.size
        reader.loadChapter(tokens, 0)
    }

    fun loadChapterAtWord(chapterId: Int, word: Int) {

        reader.disposeListener()
        val chapter = parseChapter(book, chapterId)
        val chapterText = StringBuilder(chapter!!)
        val tokens = getWordTokens(chapterText.toString())?.let { tokensToArrayList(it) }
                ?: ArrayList()
        val offsets = getChapterOffsets()

        reader.wordOffset = if (offsets != null) offsets[chapterId] else 0
        reader.currentChapter = chapterId
        reader.maxWordIdx = tokens.size
        chapterControl.setChapterText(chapterId + 1, book.spine.spineReferences.size)
        reader.loadChapter(tokens, word)
    }

    fun getBookTotalWords(): String {
        val bookDetails = getStoryDetails()
        val bookSize: String = if (bookDetails[TOTAL_WORDS] == null) getBookWords().size.toString() else bookDetails[TOTAL_WORDS]!!
        return bookSize
    }

    fun readBook() {
        val bookTokens = getBookWords()
        reader.maxWordIdx = bookTokens.size
        reader.loadChapter(bookTokens, 0)
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
        val chaptersTokens = chaptersText.map {
            tokensToArrayList(getWordTokens(it.toString())!!)
        }
        return chaptersTokens
    }

    fun setReaderPositionFromPrefs(bookDetails: HashMap<String, String>) {
        val bookDetails = getStoryDetails()
        val tempWord = if (bookDetails[WORD_KEY] == null) 0 else bookDetails[WORD_KEY]!!.toInt()
        val tempChpt = if (bookDetails[CHAPTER_KEY] == null) 0 else bookDetails[CHAPTER_KEY]!!.toInt()
        val tempSentenceStart = bookDetails[SENTENCE_START_KEY]

        val chapterOffsets = getChapterOffsets()
        val offset = if (chapterOffsets != null) chapterOffsets[tempChpt] else 0


        reader.currentChapter = tempChpt
        reader.currentWordIdx = tempWord
        reader.wordOffset = offset
        reader.currSentenceStart = if (tempSentenceStart == null) 0 else Integer.valueOf(tempSentenceStart)
    }

    fun getChapterOffsets(): ArrayList<Int>? {
        // check if available from prefs if not calculate offsets for each section
        var chapterBookSizes = PrefsUtil.readBookChapterSizes(activity!!, chosenFileName!!)
        if (chapterBookSizes == null || !chapterBookSizes.containsKey(chosenFileName!!)) saveBookDetailsToPrefs()
        chapterBookSizes = PrefsUtil.readBookChapterSizes(activity!!, chosenFileName!!)!!
        return cumSum(chapterBookSizes[chosenFileName!!]
                ?: getChapterTokens(book).map { it.size } as ArrayList<Int>)
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
        return if (words == null) {
            StringTokenizer(" . ")
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

    fun getChapterWord(word: Int, chapterLengths: ArrayList<Int>): Int {
        // given a word index calclulate the chapter the word belongs to
        // TODO test cases
        val chapterOffsets = getChapterOffsets()!!
//        val cumSum = cumSum(chapterLengths)
//        Log.d("the chapter lengths are", chapterLengths.toString())
//        Log.d("the cum sum is", cumSum.toString())
//        Log.d("Length comparison", "chapters: ${chapterLengths.size} sum:${cumSum.size}")
        var i = 0
        while (word > chapterOffsets[i]) {
            i += 1
        }
        return i
    }

    fun getWordInChapter(word: Int, chapter: Int): Int {
        // TODO test cases
        if (chapter == 0) return word
        val chapterOffsets = getChapterOffsets()!!
        return word - chapterOffsets[chapter - 1]
    }

}