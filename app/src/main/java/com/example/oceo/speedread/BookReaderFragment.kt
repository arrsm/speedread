package com.example.oceo.speedread

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.text.Html
import android.text.Spanned
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnLongClickListener
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.fragment.app.Fragment
import com.example.oceo.speedread.EPubLibUtil.Companion.exploreTOC
import com.example.oceo.speedread.EPubLibUtil.Companion.getBitmapFromResources
import com.example.oceo.speedread.EPubLibUtil.Companion.getBook
import com.example.oceo.speedread.EPubLibUtil.Companion.getTOCResourceIds
import com.example.oceo.speedread.EPubLibUtil.Companion.getTOCTitles
import com.example.oceo.speedread.EPubLibUtil.Companion.mapSpineToTOC
import com.example.oceo.speedread.EPubLibUtil.Companion.mapTOCToSpine
import com.example.oceo.speedread.SpeedReadUtilities.Companion.WPMtoMS
import com.example.oceo.speedread.SpeedReadUtilities.Companion.bookNameFromPath
import com.example.oceo.speedread.parser.getChapter
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Function
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.Resource
import nl.siegmann.epublib.domain.Spine
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*
import java.util.concurrent.TimeUnit

class BookReaderFragment : Fragment() {
    /*
    sometimes sentence scroll doesnt work..
    long sentences can break the ui by overflowing to the point that the menu options become hidden
    is the span method used in cTextSelect callback better than my sentence generations?
    percentage read of chapter/book
    indication for when reading is happening
    keep track of start and end indexes to have better resume experience
    make spine a dropdown so user can choose section?
    move all epub related stuff to the epubutil class
    move seekbar styling and stuff to a new xml file
    programmable night/bright modes
    set scrolling sentences based on finger swipes - test how fast android can build bold sentences
    auto scroll to next section
    show images in epub
    file importer
    bookmarks and notes
    settings menu
        hide progress /slider
    override back to book selection action so new books added appear there
    scroll left or right for next sentences
    currsentenceIdx variable name is causing confusing and issues - update it
    maybe modify main rx function to take display strs as a parameter instead of using the global?
    err message if book is not epub

    https://www.programcreek.com/java-api-examples/?api=nl.siegmann.epublib.domain.SpineReference
    */
    var TAG = "BookReaderFragment"
    var activity: Activity? = null
    var frag: Fragment? = null
    var rootView: View? = null
    var book: Book? = null
    private var WPM: Long = 0
    private var WPM_MS: Long = 0
    private var sentenceDelay: Long = 0
    private var currSentenceStart = 0
    private var currSentenceIdx = 0
    private var currentWordIdx = 0 // current word being iterated over = 0
    private var maxWordIdx = 0 // last word in chapter = 0
    private var currentChapter = 0
    private val mTouchX = 0
    private val mTouchY = 0
    var firstTimeFlag = 0 // should spinner action be called
    private var chptPercentageComplete = 0f
    protected var fullText // holds full story in memory
            : StringBuilder? = null
    private var story // fullText converted to arraylist
            : ArrayList<String>? = null
    private var displayStrs // crutch to display bolded words. would like to change
            : ArrayList<StringBuilder>? = null
    private var tocResourceIds: ArrayList<String>? = null
    var bookDetails: HashMap<String?, String?>? = null
    protected var chosenFilePath: String? = null
    protected var chosenFileName: String? = null
    var disposableReader: Disposable? = null
    private var fullStoryView: TextView? = null
    private var currentWordView: TextView? = null
    private var currentChunkView: TextView? = null
    private var raiseWPMButton: Button? = null
    private var lowerWPMButton: Button? = null
    private var currentChapterview: TextView? = null
    private var raiseChapterButton: Button? = null
    private var lowerChapterButton: Button? = null
    private var WPM_view: TextView? = null
    private var dropdown: Spinner? = null
    private var chapterSeekBar: SeekBar? = null
    private var chptProgressView: TextView? = null
    private var pauseResumeBtn: Button? = null
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
    var textSelectionMenu: cTextSelectionMenu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity = getActivity()
        frag = this
        val bundle = this.arguments
        chosenFilePath = bundle!!.getString("file_path")
        chosenFileName = bookNameFromPath(chosenFilePath!!)
        setDefaultValues()
        val text = activity!!.intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
        if (text != null) {
            Log.d("play with text", text.toString())
        }
    }

    override fun onResume() {
        Log.d(TAG, "bookreader fragment resumes")
        if (book != null) {
            val tempChpt = bookDetails!![CHAPTER_KEY]
            val tempWord = bookDetails!![WORD_KEY]
            val tempSentenceStart = bookDetails!![SENTENCE_START_KEY]
            currentChapter = if (tempChpt == null) 0 else Integer.valueOf(tempChpt)
            currentWordIdx = if (tempWord == null) 0 else Integer.valueOf(tempWord)
            currSentenceStart = if (tempSentenceStart == null) 0 else Integer.valueOf(tempSentenceStart)
            if (firstTimeFlag == 0) {
                iterateWords()
            }
        }
        super.onResume()
    }

    override fun onPause() {
        Log.d(TAG, "onCreateView")
        disposeListener()
        bookDetails!![CHAPTER_KEY] = currentChapter.toString()
        bookDetails!![WORD_KEY] = currentWordIdx.toString()
        bookDetails!![SENTENCE_START_KEY] = currSentenceStart.toString()
        PrefsUtil.writeBookDetailsToPrefs(activity!!, chosenFileName!!, bookDetails)
        super.onPause()
    }


    private fun handleTextSelection() {
        if (currentChunkView == null) {
            return
        }
        if (currentChunkView!!.hasSelection()) {
            textSelectionMenu!!.setMetadata(chosenFileName, currentChapter, currSentenceStart)
            currentChunkView!!.customSelectionActionModeCallback = textSelectionMenu
        }
        Log.d("text selection", "($mTouchX, $mTouchY)")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG, "onCreateView")
        rootView = inflater.inflate(R.layout.book_reader, container, false)
        book = getBook(chosenFilePath)
        setupWPMControls()
        setupChapterControls(book)
        currentChunkView = rootView!!.findViewById(R.id.current_chunk)

        //textSelection()
        handleSwipes()

        /* maybe a good candidate to try to move to a new fragment? */
        pauseResumeBtn = rootView!!.findViewById(R.id.pause_resume)
        pauseResumeBtn!!.setOnClickListener(View.OnClickListener {
            if (!disposableReader!!.isDisposed) {
                pauseResumeBtn!!.setText(">")
                disposableReader!!.dispose()
            } else {
                pauseResumeBtn!!.setText("||")
                iterateWords()
            }
        })

        textSelectionMenu = cTextSelectionMenu(currentChunkView!!)
        currentWordView = rootView!!.findViewById(R.id.current_word)
        fullStoryView = rootView!!.findViewById(R.id.file_test)
        chapterSeekBar = rootView!!.findViewById(R.id.seekBar)
        chptProgressView = rootView!!.findViewById(R.id.chapter_progress_view)

        if (book != null) {
            PrefsUtil.writeBookToPrefs(activity!!, chosenFilePath)
            tocResourceIds = getTOCResourceIds(exploreTOC(book!!), 0, ArrayList<String>())
            displayTOC()
            setStoryTokens()
        }
        chapterSeekBar!!.max = maxWordIdx
        chapterSeekBar!!.min = 0
        chapterSeekBar!!.progress = currentWordIdx
        chptPercentageComplete = java.lang.Float.valueOf(currentWordIdx.toFloat()) / java.lang.Float.valueOf(maxWordIdx.toFloat()) * 100
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
                disposeListener()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
//                Log.d(TAG, "seekBar stop tracking touch");
                val progress = seekBar.progress
                currSentenceStart = getSentenceStartIdx(progress)
                currentWordIdx = currSentenceStart
                //                currSentenceIdx = getWordPositionInSentence(progress);
                currSentenceIdx = 0
                if (chptPercentageComplete.toString().length > 3) {
                    chptProgressView!!.setText(chptPercentageComplete.toString().substring(0, 4) + "%")
                } else {
                    chptProgressView!!.setText("$chptPercentageComplete%")
                }
                iterateWords()
            }
        })
        return rootView
    }

    fun handleSwipes() {
        currentChunkView!!.setOnTouchListener(object : OnSwipeTouchListener(activity) {
            override fun onSwipeTop() {
//                Toast.makeText(activity, "top", Toast.LENGTH_SHORT).show();
            }

            override fun onSwipeRight() {
//                Toast.makeText(activity, "right", Toast.LENGTH_SHORT).show();
                moveToPrevSentence()
            }

            override fun onSwipeLeft() {
//                Toast.makeText(activity, "left", Toast.LENGTH_SHORT).show();
                moveToNextSentence()
            }

            override fun onSwipeBottom() {
//                Toast.makeText(activity, "bottom", Toast.LENGTH_SHORT).show();
            }
        })
    }

    fun textSelection() {
        /*
        currentChunkView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!disposableReader.isDisposed()) {
                    disposableReader.dispose();
                } else {
                    iterateWords();
                }

            }
        });

        // TODO incorporate text selection. this is not functional atm due to swipe replacing the touchlistenr
        currentChunkView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (MotionEvent.ACTION_UP == event.getAction()) {
                    handleTextSelection();
                }
                return false;
            }
        });
        */
    }

    fun moveToPrevSentence() {
        Log.d("moveToPrevSentence before: ", "curr: " + currSentenceStart.toString())
        val prevSentenceStart = getSentenceStartIdx(currSentenceStart - 2)
        Log.d("moveToPrevSentence after: ", "curr: $prevSentenceStart")
        disposeListener()
        currSentenceStart = prevSentenceStart
        currentWordIdx = prevSentenceStart
        currSentenceIdx = 0
        val sentenceEndIdx = getNextSentencesStartIdx(story, 1, currentWordIdx)
        val prevSentence = getStringFromTokenIndexes(prevSentenceStart, sentenceEndIdx)
        currentChunkView!!.text = prevSentence
        //        iterateWords();
    }

    fun disposeListener() {
        Log.d("disposing Listener", "START")
        Log.d("disposing Listener", "currentWordIdx: " + currentWordIdx.toString())
        Log.d("disposing Listener", "currentSentenceStart: " + currSentenceStart.toString())
        Log.d("disposing Listener", "cur: " + currSentenceStart.toString())
        if (disposableReader != null && !disposableReader!!.isDisposed) {
            disposableReader!!.dispose()
        }
    }

    fun moveToNextSentence() {
        val nextSentenceStart = getNextSentencesStartIdx(story, 1, currentWordIdx)
        Log.d("moveToPrevSentence before: ", "curr: " + currSentenceStart.toString())
        val prevSentenceStart = getSentenceStartIdx(currSentenceStart - 2)
        Log.d("moveToPrevSentence after: ", "curr: $prevSentenceStart")
        disposeListener()
        currSentenceStart = nextSentenceStart
        currentWordIdx = nextSentenceStart
        val sentenceEndIdx = getNextSentencesStartIdx(story, 1, nextSentenceStart)
        val nextSentence = getStringFromTokenIndexes(nextSentenceStart, sentenceEndIdx)
        currentChunkView!!.text = nextSentence
        currSentenceIdx = 0
        //        iterateWords();
    }

    fun getStringFromTokenIndexes(startIdx: Int, endIdx: Int): StringBuilder {
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
        // average number of sentences in a page for page turn?
        val startIdx = getNextSentencesStartIdx(story, numSentences, currentWordIdx)
        currSentenceStart = startIdx + 1
    }


    // can i make this logic more modular??
    fun displayTOC() {
        val tocRefs = exploreTOC(book!!)
        var TOCTitles = ArrayList<String>()
        TOCTitles = getTOCTitles(tocRefs, 0, TOCTitles)
        dropdown = rootView!!.findViewById(R.id.spinner1)
        val adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, TOCTitles)
        dropdown!!.setAdapter(adapter)

        // TODO maybe move this bit to the epublib utils
        val spineRefs = book!!.spine.spineReferences
        val currentSpineID = spineRefs[currentChapter].resourceId
        val currentToCIdx = mapSpineToTOC(currentSpineID, tocResourceIds!!) // find out if current chapter is in TOC

        // if not then dont set the ToC there
        if (currentToCIdx != -1) {
            dropdown!!.setSelection(currentToCIdx)
        }

        dropdown!!.setOnItemSelectedListener(object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                val selectedItem = tocResourceIds!![position]
                if (++firstTimeFlag > 1) { // do not update on launch
                    currentChapter = mapTOCToSpine(book!!, selectedItem)
                    disposeListener()
                    bookDetails!![CHAPTER_KEY] = currentChapter.toString()
                    PrefsUtil.writeBookDetailsToPrefs(activity!!, chosenFileName!!, bookDetails)
                    currentChapterview!!.text = "Section: " + (currentChapter + 1).toString()
                    resetChapterGlobals()
                    setStoryTokens()
                    iterateWords()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        })
    }


    fun setStoryTokens() {
        // sets fullText (String containing entire chapter text)
        // and calculates and sets story(ArrayList of each word in chapter)
        val chapter = readSampleChapter(book, currentChapter)
        fullText = StringBuilder(chapter)
        val tokens = getWordTokens(fullText.toString())
        if (tokens != null) {
            maxWordIdx = tokens.countTokens()
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

    fun getWordPositionInSentence(idx: Int): Int {
        var idx = idx
        var earlierTokenCount = 0
        while (!story!![idx].contains(".") && idx > 0) {
            idx -= 1
            earlierTokenCount += 1
        }
        return 1.let { earlierTokenCount -= it; earlierTokenCount }
    }

    fun getNextSentencesStartIdx(tokens: ArrayList<String>?, numSentences: Int, startIdx: Int): Int {
        var startIdx = startIdx
        var foundSentences = 0
        val temp = startIdx
        while (foundSentences < numSentences) {
            while (startIdx < maxWordIdx && (!tokens!![startIdx].contains(".")
                            || tokens[startIdx].contains("?")
                            || tokens[startIdx].contains("!"))) {
                startIdx++
            }
            startIdx += 1
            foundSentences += 1
        }
        if (tokens != null && startIdx < tokens.size && tokens[startIdx].contains("”")) {
            startIdx += 1
        }

//        List<String> words = tokens.subList(temp, startIdx);
//        Log.d("the tokes", words.toString());
        return startIdx
    }

    fun buildBoldSentences(tokenList: ArrayList<String>?, startIdx: Int, endIdx: Int): ArrayList<StringBuilder> {
        // TODO wish there was a better way to do this rather than building and holding o(n^2)
        //  strings in the number of words
        var endIdx = endIdx
        if (endIdx > maxWordIdx) {
            endIdx = maxWordIdx
        }
        val displayStrs = ArrayList<StringBuilder>()
        for (targetWord in startIdx until endIdx) {
            val formattedDisplayStr = StringBuilder()
            for (i in startIdx until endIdx) {
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


    fun iterateWords() {
        val sentencesEndIdx = getNextSentencesStartIdx(story, 1, currentWordIdx)
        displayStrs = buildBoldSentences(story, currSentenceStart, sentencesEndIdx)

//        int tempWordIdx = this.currentWordIdx;
        val tempWordIdx = currSentenceStart
        var rangeObs: Observable<*> = Observable.range(tempWordIdx, sentencesEndIdx - currentWordIdx)
                .concatMap { i: Any -> Observable.just(i).delay(WPM_MS, TimeUnit.MILLISECONDS) }

        rangeObs = rangeObs.delay(sentenceDelay, TimeUnit.MILLISECONDS) // delay at the end of the sentence
        rangeObs = rangeObs.observeOn(AndroidSchedulers.mainThread())
        disposableReader = rangeObs.subscribe({ wordIdx: Any? ->
//                    Log.d("The OBS", String.valueOf(wordIdx) + " / " + String.valueOf(sentencesEndIdx));
            if (currSentenceIdx < displayStrs!!.size) {
//                Log.d("The OBS", "Is IN of Bounds")
//                Log.d(TAG, currentWordIdx.toString() + " / " + displayStrs!!.size.toString())
                currentChunkView!!.text = Html.fromHtml(displayStrs!![currSentenceIdx].toString())
                currentWordView!!.text = story!![currentWordIdx]
                currSentenceIdx++
                currentWordIdx++
                chptPercentageComplete = java.lang.Float.valueOf(currentWordIdx.toFloat()) / java.lang.Float.valueOf(maxWordIdx.toFloat()) * 100
                if (chptPercentageComplete.toString().length > 3) {
                    chptProgressView!!.text = chptPercentageComplete.toString().substring(0, 4) + "%"
                } else {
                    chptProgressView!!.text = "$chptPercentageComplete%"
                }
                chapterSeekBar!!.progress = currentWordIdx
                //                        Log.d("setting progress to ", String.valueOf(currentWordIdx));
//                        Log.d(TAG + "SUB", String.valueOf(wordIdx) + " / " + String.valueOf(this.maxWordIdx));
            } else {
                Log.d("The OBS", "Is Out of Bounds")
                Log.d(TAG, currentWordIdx.toString() + " / " + displayStrs!!.size.toString())
            }
        },
                { e: Any? -> }
        ) {
//                        Log.d("obs", "k do the next chunk");
            if (currentWordIdx < maxWordIdx) {
                currSentenceIdx = 0
                currSentenceStart = currentWordIdx
                iterateWords()
            } else {
                Log.d("Observable", "No more sentences")
            }
        }
    }

    fun setupChapterControls(book: Book?) {
        raiseChapterButton = rootView!!.findViewById(R.id.raise_chpt_button)
        lowerChapterButton = rootView!!.findViewById(R.id.lower_chpt_btn)
        currentChapterview = rootView!!.findViewById(R.id.current_chapter)
        raiseChapterButton!!.setOnClickListener(View.OnClickListener {
            currentChapter += 1
            if (book != null) {
                disposeListener()
                bookDetails!![CHAPTER_KEY] = currentChapter.toString()
                PrefsUtil.writeBookDetailsToPrefs(activity!!, chosenFileName!!, bookDetails)
                currentChapterview!!.setText("Section: " + (currentChapter + 1).toString())
                resetChapterGlobals()
                setStoryTokens()
                iterateWords()
            }
        })
        lowerChapterButton!!.setOnClickListener(View.OnClickListener {
            if (currentChapter >= 0) {
                currentChapter -= 1
                if (book != null) {
                    bookDetails!![CHAPTER_KEY] = currentChapter.toString()
                    PrefsUtil.writeBookDetailsToPrefs(activity!!, chosenFileName!!, bookDetails)
                    currentChapterview!!.setText("Section: " + (currentChapter + 1).toString())
                    disposeListener()
                    resetChapterGlobals()
                    setStoryTokens()
                    iterateWords()
                }
            }
        })
        currentChapterview!!.text = "Section: " + (currentChapter + 1).toString()
    }

    fun resetChapterGlobals() {
        currSentenceStart = 0
        currentWordIdx = 0
        currSentenceIdx = 0
    }

    fun setDefaultValues() {
        val activityCopy = activity!!
        WPM = PrefsUtil.readLongFromPrefs(activityCopy, WPM_KEY)
        WPM_MS = WPMtoMS(WPM)
        sentenceDelay = PrefsUtil.readLongFromPrefs(activityCopy, SENTENCE_DELAY_KEY)
        bookDetails = PrefsUtil.readBookDetailsFromPrefs(activityCopy, chosenFileName) as HashMap<String?, String?>?
        if (bookDetails == null) {
            bookDetails = HashMap()
        }
        val tempChpt = bookDetails!![CHAPTER_KEY]
        val tempWord = bookDetails!![WORD_KEY]
        val tempSentenceStart = bookDetails!![SENTENCE_START_KEY]
        currentChapter = if (tempChpt == null) 0 else Integer.valueOf(tempChpt)
        currentWordIdx = if (tempWord == null) 0 else Integer.valueOf(tempWord)
        currSentenceStart = if (tempSentenceStart == null) 0 else Integer.valueOf(tempSentenceStart)
        resetChapterGlobals()
    }

    fun readSampleChapter(book: Book?, chapterNumber: Int): String? {
        // TODO test if invalid chapter passed in
        var chapterContents: String? = null
        if (book != null) {
            val spine = book.spine
            chapterContents = getChapter(spine, chapterNumber, book, rootView!!)
        } else {
            Log.d("readSampleChpt", "book is null")
        }
        return chapterContents
    }


    fun setStoryContent(fullText: StringBuilder?) {
        fullStoryView!!.text = fullText
        fullStoryView!!.movementMethod = ScrollingMovementMethod()
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setupWPMControls() {
        raiseWPMButton = rootView!!.findViewById(R.id.raise_wpm_button)
        lowerWPMButton = rootView!!.findViewById(R.id.lower_wpm_button)
        WPM_view = rootView!!.findViewById(R.id.current_wpm_view)
        WPM_view!!.setText(WPM.toString())
        raiseWPMButton!!.setOnClickListener(View.OnClickListener { //                Log.d(TAG, "onClick");
            incrementWPM()
            // TODO necessary to do here AND in motion up?
            PrefsUtil.writeLongToPrefs(activity!!, WPM_KEY, WPM)
        })
        raiseWPMButton!!.setOnLongClickListener(OnLongClickListener { //                Log.d(TAG, "long click");
            autoIncrementWPM = true
            WPMUpdateHandler.post(RepetitiveUpdater())
            false
        })
        raiseWPMButton!!.setOnTouchListener(OnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP && autoIncrementWPM) {
                Log.d(TAG, "touch up")
                autoIncrementWPM = false
                PrefsUtil.writeLongToPrefs(activity!!, WPM_KEY, WPM)
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
            }
            false
        })
    }

    fun incrementWPM() {
        if (WPM < 1000) {
            WPM++
            WPM_view!!.text = WPM.toString()
        }
    }

    fun decrementWPM() {
        if (WPM > 0) {
            WPM--
            WPM_view!!.text = WPM.toString()
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