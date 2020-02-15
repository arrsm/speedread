package com.example.oceo.speedread;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;

import androidx.fragment.app.Fragment;

import android.text.Html;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.Spine;
import nl.siegmann.epublib.domain.SpineReference;
import nl.siegmann.epublib.domain.TOCReference;

import static com.example.oceo.speedread.EPubLibUtil.exploreTOC;
import static com.example.oceo.speedread.EPubLibUtil.mapSpineToTOC;


public class BookReaderFragment extends Fragment {

    /*
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

    String TAG = "BookReaderFragment";
    Activity activity;
    Fragment frag;
    View rootView;

    Book book;
    private long WPM;
    private long WPM_MS;
    private long sentenceDelay;
    private int currSentenceStart;
    private int currSentenceIdx;
    private int currentWordIdx; // current word being iterated over
    private int maxWordIdx; // last word in chapter
    private int currentChapter;
    private int mTouchX;
    private int mTouchY;
    int firstTimeFlag = 0; // should spinner action be called
    private float chptPercentageComplete;
    protected StringBuilder fullText; // holds full story in memory
    private ArrayList<String> story; // fullText converted to arraylist
    private ArrayList<StringBuilder> displayStrs; // crutch to display bolded words. would like to change
    private ArrayList<String> tocResourceIds;
    HashMap<String, String> bookDetails;
    protected String chosenFilePath;
    protected String chosenFileName;
    Disposable disposableReader;

    private TextView fullStoryView;
    private TextView currentWordView;
    private TextView currentChunkView;
    private Button raiseWPMButton;
    private Button lowerWPMButton;
    private TextView currentChapterview;
    private Button raiseChapterButton;
    private Button lowerChapterButton;
    private TextView WPM_view;
    private Spinner dropdown;
    private SeekBar chapterSeekBar;
    private TextView chptProgressView;
    private Button pauseResumeBtn;

    final String CHAPTER_KEY = "chapter";
    final String WORD_KEY = "page";
    final String SENTENCE_START_KEY = "sentence_start";
    final String WPM_KEY = "wpm";
    final String SENTENCE_DELAY_KEY = "sentence_delay";


    //long held incrementers
    private boolean autoIncrementWPM = false;
    private boolean autoDecrementWPM = false;
    private final long REPEAT_DELAY = 50;
    private Handler WPMUpdateHandler = new Handler();
    cTextSelectionMenu textSelectionMenu;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.activity = getActivity();
        this.frag = this;
        Bundle bundle = this.getArguments();
        this.chosenFilePath = bundle.getString("file_path");
        this.chosenFileName = SpeedReadUtilities.bookNameFromPath(this.chosenFilePath);
        setDefaultValues();


        CharSequence text = activity.getIntent().getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
        if (!(text == null)) {
            Log.d("play with text", text.toString());

            // process the text
        }
    }

    @Override
    public void onResume() {
        Log.d(TAG, "bookreader fragment resumes");
        if (book != null) {
            String tempChpt = this.bookDetails.get(CHAPTER_KEY);
            String tempWord = this.bookDetails.get(WORD_KEY);
            String tempSentenceStart = this.bookDetails.get(SENTENCE_START_KEY);
            this.currentChapter = (tempChpt == null ? 0 : Integer.valueOf(tempChpt));
            this.currentWordIdx = (tempWord == null ? 0 : Integer.valueOf(tempWord));
            this.currSentenceStart = (tempSentenceStart == null ? 0 : Integer.valueOf(tempSentenceStart));
            if (firstTimeFlag == 0) {
                iterateWords();
            }
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onCreateView");
        disposeListener();
        bookDetails.put(CHAPTER_KEY, String.valueOf(currentChapter));
        bookDetails.put(WORD_KEY, String.valueOf(currentWordIdx));
        bookDetails.put(SENTENCE_START_KEY, String.valueOf(currSentenceStart));
        PrefsUtil.writeBookDetailsToPrefs(activity, chosenFileName, bookDetails);
        super.onPause();
    }

    /*
    text selection
    */
    private void handleTextSelection() {
        if (currentChunkView == null) {
            return;
        }

        if (currentChunkView.hasSelection()) {
            textSelectionMenu.setMetadata(this.chosenFileName, this.currentChapter, this.currSentenceStart);
            currentChunkView.setCustomSelectionActionModeCallback(textSelectionMenu);
        }
        Log.d("text selection", "(" + String.valueOf(mTouchX) + ", " + String.valueOf(mTouchY) + ")");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        rootView = inflater.inflate(R.layout.book_reader, container, false);

        this.book = readFile(this.chosenFilePath);
        setupWPMControls();
        setupChapterControls(this.book);


        currentChunkView = rootView.findViewById(R.id.current_chunk);
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
        */

        /*
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


        currentChunkView.setOnTouchListener(new OnSwipeTouchListener(this.activity) {
            public void onSwipeTop() {
//                Toast.makeText(activity, "top", Toast.LENGTH_SHORT).show();
            }

            public void onSwipeRight() {
//                Toast.makeText(activity, "right", Toast.LENGTH_SHORT).show();
                moveToNextSentence();
            }

            public void onSwipeLeft() {
//                Toast.makeText(activity, "left", Toast.LENGTH_SHORT).show();
                moveToPrevSentence();
            }

            public void onSwipeBottom() {
//                Toast.makeText(activity, "bottom", Toast.LENGTH_SHORT).show();
            }

        });


        pauseResumeBtn = rootView.findViewById(R.id.pause_resume);
        pauseResumeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!disposableReader.isDisposed()) {
                    pauseResumeBtn.setText(">");
                    disposableReader.dispose();
                } else {
                    pauseResumeBtn.setText("||");
                    iterateWords();
                }
            }
        });

        textSelectionMenu = new cTextSelectionMenu(currentChunkView);
        currentWordView = rootView.findViewById(R.id.current_word);
        fullStoryView = rootView.findViewById(R.id.file_test);
        chapterSeekBar = rootView.findViewById(R.id.seekBar);
        chptProgressView = rootView.findViewById(R.id.chapter_progress_view);


        if (this.book != null) {
            PrefsUtil.writeBookToPrefs(activity, this.chosenFilePath);
            this.tocResourceIds = getTOCResourceIDs();
            displayTOC();
            setStoryTokens();
        }

        chapterSeekBar.setMax(this.maxWordIdx);
        chapterSeekBar.setMin(0);
        chapterSeekBar.setProgress(this.currentWordIdx);

        chptPercentageComplete = Float.valueOf(currentWordIdx) / Float.valueOf(maxWordIdx) * 100;
        if (String.valueOf(chptPercentageComplete).length() > 3) {
            chptProgressView.setText(String.valueOf(chptPercentageComplete).substring(0, 4) + "%");
        } else {
            chptProgressView.setText(String.valueOf(chptPercentageComplete) + "%");
        }

        chapterSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                Log.d("seeking: ", String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
//                Log.d(TAG, "seekBar start tracking touch");
                disposeListener();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
//                Log.d(TAG, "seekBar stop tracking touch");
                int progress = seekBar.getProgress();
                currSentenceStart = getSentenceStartIdx(progress);
                currentWordIdx = currSentenceStart;
//                currSentenceIdx = getWordPositionInSentence(progress);
                currSentenceIdx = 0;
                if (String.valueOf(chptPercentageComplete).length() > 3) {
                    chptProgressView.setText(String.valueOf(chptPercentageComplete).substring(0, 4) + "%");
                } else {
                    chptProgressView.setText(String.valueOf(chptPercentageComplete) + "%");
                }
                iterateWords();
            }
        });
        return rootView;
    }

    public void moveToPrevSentence() {
        Log.d("moveToPrevSentence before: ", "curr: " + String.valueOf(this.currSentenceStart));
        int prevSentenceStart = getSentenceStartIdx(this.currSentenceStart - 2);
        Log.d("moveToPrevSentence after: ", "curr: " + String.valueOf(prevSentenceStart));
        disposeListener();
        this.currSentenceStart = prevSentenceStart;
        this.currentWordIdx = prevSentenceStart;
        this.currSentenceIdx = 0;
        int sentenceEndIdx = getNextSentencesStartIdx(story, 1, this.currentWordIdx);

        StringBuilder prevSentence = getStringFromTokenIndexes(prevSentenceStart, sentenceEndIdx);
        this.currentChunkView.setText(prevSentence);
//        iterateWords();
    }

    public void disposeListener() {
        Log.d("disposing Listener", "START");
        Log.d("disposing Listener", "currentWordIdx: " + String.valueOf(this.currentWordIdx));
        Log.d("disposing Listener", "currentSentenceStart: " + String.valueOf(this.currSentenceStart));
        Log.d("disposing Listener", "cur: " + String.valueOf(this.currSentenceStart));

        if (disposableReader != null && !disposableReader.isDisposed()) {
            disposableReader.dispose();
        }
    }

    public void moveToNextSentence() {
        int nextSentenceStart = getNextSentencesStartIdx(this.story, 1, currentWordIdx);
        Log.d("moveToPrevSentence before: ", "curr: " + String.valueOf(this.currSentenceStart));
        int prevSentenceStart = getSentenceStartIdx(this.currSentenceStart - 2);
        Log.d("moveToPrevSentence after: ", "curr: " + String.valueOf(prevSentenceStart));
        disposeListener();
        this.currSentenceStart = nextSentenceStart;
        this.currentWordIdx = nextSentenceStart;
        int sentenceEndIdx = getNextSentencesStartIdx(story, 1, nextSentenceStart);
        StringBuilder nextSentence = getStringFromTokenIndexes(nextSentenceStart, sentenceEndIdx);
        this.currentChunkView.setText(nextSentence);
        this.currSentenceIdx = 0;
//        iterateWords();
    }


    public StringBuilder getStringFromTokenIndexes(int startIdx, int endIdx) {
        int temp = startIdx;
        StringBuilder str = new StringBuilder();
        while (temp < endIdx) {
            str.append(this.story.get(temp) + " ");
            temp++;
        }
        return str;
    }

    public void scrollSentences(int numSentences) {
        //TODO testing
        // average number of sentences in a page for page turn?
        int startIdx = getNextSentencesStartIdx(this.story, numSentences, currentWordIdx);
        this.currSentenceStart = startIdx + 1;
    }

    private Bitmap getBookImages(List<Resource> res, String imgHref) {
//        String tempHref = "images/Simm_9780307781888_epub_L03_r1.jpg";
//        tempHref = "OEBPS/images/Simm_9780307781888_epub_L03_r1.jpg";
//        tempHref = "images/OB_ARCH_ebook_004.gif.transcoded1535572045.png" // WORKS sanderson chap/t 4;
        return EPubLibUtil.getBitmapFromResources(res, imgHref, this.book);
    }

    public void displayTOC() {
        List<TOCReference> tocRefs = EPubLibUtil.exploreTOC(this.book);
        ArrayList<String> TOCTitles = new ArrayList<String>();
        TOCTitles = EPubLibUtil.getTOCTitles(tocRefs, 0, TOCTitles);
        dropdown = rootView.findViewById(R.id.spinner1);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, TOCTitles);
        dropdown.setAdapter(adapter);

        // TODO maybe move this bit to the epublib utils
        List<SpineReference> spineRefs = book.getSpine().getSpineReferences();
        String currentSpineID = spineRefs.get(currentChapter).getResourceId();
        int currentToCIdx = mapSpineToTOC(currentSpineID, tocResourceIds); // find out if current chapter is in TOC

        // if not then dont set the ToC there
        if (currentToCIdx != -1) {
            dropdown.setSelection(currentToCIdx);
        }

        dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = tocResourceIds.get(position);
                if (++firstTimeFlag > 1) { // do not update on launch
                    currentChapter = EPubLibUtil.mapTOCToSpine(book, selectedItem);
                    disposeListener();
                    bookDetails.put(CHAPTER_KEY, String.valueOf(currentChapter));
                    PrefsUtil.writeBookDetailsToPrefs(activity, chosenFileName, bookDetails);
                    currentChapterview.setText("Section: " + String.valueOf(currentChapter + 1));
                    resetChapterGlobals();
                    setStoryTokens();
                    iterateWords();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public ArrayList<String> getTOCResourceIDs() {
        ArrayList<String> tocIDs = new ArrayList<String>();
        tocIDs = EPubLibUtil.getTOCResourceIds(exploreTOC(this.book), 0, tocIDs);
        return tocIDs;
    }

    public Book readFile(String fName) {
        Book book = EPubLibUtil.getBook(fName); // think about how to better structure this
        return book;
    }

    public void setStoryTokens() {
        // sets fullText (String containing entire chapter text)
        // and calculates and sets story(ArrayList of each word in chapter)
        String chapter = readSampleChapter(book, currentChapter);
        this.fullText = new StringBuilder(chapter);
        StringTokenizer tokens = getWordTokens(fullText.toString());
        if (tokens != null) {
            this.maxWordIdx = tokens.countTokens();
            this.story = tokensToArrayList(tokens);
        }
    }

    public int getSentenceStartIdx(int idx) {
        while (!this.story.get(idx).contains(".") && idx > 0) {
            idx -= 1;
        }
        return idx + 1;
    }

    public int getWordPositionInSentence(int idx) {
        int earlierTokenCount = 0;
        while (!this.story.get(idx).contains(".") && idx > 0) {
            idx -= 1;
            earlierTokenCount += 1;
        }
        return (earlierTokenCount -= 1);
    }

    public int getNextSentencesStartIdx(ArrayList<String> tokens, int numSentences, int startIdx) {
        int foundSentences = 0;
        int temp = startIdx;
        while (foundSentences < numSentences) {
            while (startIdx < maxWordIdx && (!tokens.get(startIdx).contains(".")
                    || tokens.get(startIdx).contains("?")
                    || tokens.get(startIdx).contains("!"))) {
                startIdx++;
            }
            startIdx += 1;
            foundSentences += 1;
        }

        if (tokens != null && startIdx < tokens.size() && tokens.get(startIdx).contains("”")) {
            startIdx += 1;
        }

//        List<String> words = tokens.subList(temp, startIdx);
//        Log.d("the tokes", words.toString());
        return startIdx;
    }

    public ArrayList<StringBuilder> buildBoldSentences(ArrayList<String> tokenList, int startIdx, int endIdx) {
        // TODO wish there was a better way to do this rather than building and holding o(n^2)
        //  strings in the number of words

        if (endIdx > this.maxWordIdx) {
            endIdx = this.maxWordIdx;
        }

        ArrayList<StringBuilder> displayStrs = new ArrayList<StringBuilder>();

        for (int targetWord = startIdx; targetWord < endIdx; targetWord++) {
            StringBuilder formattedDisplayStr = new StringBuilder();

            for (int i = startIdx; i < endIdx; i++) {
                if (targetWord == i) {
                    formattedDisplayStr.append("<font color=\"gray\">" + tokenList.get(i) + "</font> ");
                } else {
                    formattedDisplayStr.append(tokenList.get(i) + " ");
                }
            }
            displayStrs.add(formattedDisplayStr);
        }

        return displayStrs;
    }

    public void setDisplayStrs(int sentenceEndIdx) {
        this.displayStrs = buildBoldSentences(this.story, currSentenceStart, sentenceEndIdx);
    }

    public void iterateWords() {
        int sentencesEndIdx = getNextSentencesStartIdx(story, 1, this.currentWordIdx);
        this.displayStrs = buildBoldSentences(this.story, currSentenceStart, sentencesEndIdx);

//        int tempWordIdx = this.currentWordIdx;
        int tempWordIdx = this.currSentenceStart;
        Observable rangeObs = Observable.range(tempWordIdx, sentencesEndIdx - currentWordIdx);
        rangeObs = rangeObs.concatMap(i -> Observable.just(i).delay(WPM_MS, TimeUnit.MILLISECONDS));
        rangeObs = rangeObs.delay(this.sentenceDelay, TimeUnit.MILLISECONDS); // delay at the end of the sentence
        rangeObs = rangeObs.observeOn(AndroidSchedulers.mainThread());

        disposableReader = rangeObs.subscribe(wordIdx -> {
//                    Log.d("The OBS", String.valueOf(wordIdx) + " / " + String.valueOf(sentencesEndIdx));
                    if (this.currSentenceIdx < this.displayStrs.size()) {
                        Log.d("The OBS", "Is IN of Bounds");
                        Log.d(TAG, String.valueOf(this.currentWordIdx) + " / " + String.valueOf(this.displayStrs.size()));
                        currentChunkView.setText(Html.fromHtml(this.displayStrs.get(this.currSentenceIdx).toString()));
                        this.currentWordView.setText(this.story.get(this.currentWordIdx));
                        this.currSentenceIdx++;
                        this.currentWordIdx++;
                        this.chptPercentageComplete = Float.valueOf(this.currentWordIdx) / Float.valueOf(this.maxWordIdx) * 100;
                        if (String.valueOf(chptPercentageComplete).length() > 3) {
                            chptProgressView.setText(String.valueOf(chptPercentageComplete).substring(0, 4) + "%");
                        } else {
                            chptProgressView.setText(String.valueOf(chptPercentageComplete) + "%");
                        }
                        chapterSeekBar.setProgress(currentWordIdx);
//                        Log.d("setting progress to ", String.valueOf(currentWordIdx));
//                        Log.d(TAG + "SUB", String.valueOf(wordIdx) + " / " + String.valueOf(this.maxWordIdx));
                    } else {
                        Log.d("The OBS", "Is Out of Bounds");
                        Log.d(TAG, String.valueOf(this.currentWordIdx) + " / " + String.valueOf(this.displayStrs.size()));
                    }

                },
                e -> {

                },
                () -> {
//                        Log.d("obs", "k do the next chunk");
                    if (currentWordIdx < maxWordIdx) {
                        currSentenceIdx = 0;
                        currSentenceStart = currentWordIdx;
                        iterateWords();
                    } else {
                        Log.d("Observable", "No more sentences");
                    }
                });
    }

    public void setupChapterControls(Book book) {

        raiseChapterButton = rootView.findViewById(R.id.raise_chpt_button);
        lowerChapterButton = rootView.findViewById(R.id.lower_chpt_btn);
        currentChapterview = rootView.findViewById(R.id.current_chapter);


        raiseChapterButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                currentChapter += 1;
                if (book != null) {
                    disposeListener();
                    bookDetails.put(CHAPTER_KEY, String.valueOf(currentChapter));
                    PrefsUtil.writeBookDetailsToPrefs(activity, chosenFileName, bookDetails);
                    currentChapterview.setText("Section: " + String.valueOf(currentChapter + 1));
                    resetChapterGlobals();
                    setStoryTokens();
                    iterateWords();
                }
            }
        });


        lowerChapterButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (currentChapter >= 0) {
                    currentChapter -= 1;
                    if (book != null) {

                        bookDetails.put(CHAPTER_KEY, String.valueOf(currentChapter));
                        PrefsUtil.writeBookDetailsToPrefs(activity, chosenFileName, bookDetails);
                        currentChapterview.setText("Section: " + String.valueOf(currentChapter + 1));
                        disposeListener();
                        resetChapterGlobals();
                        setStoryTokens();
                        iterateWords();
                    }
                }
            }
        });

        currentChapterview.setText("Section: " + String.valueOf(currentChapter + 1));

    }

    public void resetChapterGlobals() {
        currSentenceStart = 0;
        currentWordIdx = 0;
        currSentenceIdx = 0;
    }

    public void setDefaultValues() {
        this.WPM = PrefsUtil.readLongFromPrefs(activity, WPM_KEY);
        this.WPM_MS = SpeedReadUtilities.WPMtoMS(WPM);
        this.sentenceDelay = PrefsUtil.readLongFromPrefs(activity, SENTENCE_DELAY_KEY);
        this.bookDetails = PrefsUtil.readBookDetailsFromPrefs(activity, chosenFileName);
        if (this.bookDetails == null) {
            this.bookDetails = new HashMap<String, String>();
        }
        String tempChpt = this.bookDetails.get(CHAPTER_KEY);
        String tempWord = this.bookDetails.get(WORD_KEY);
        String tempSentenceStart = this.bookDetails.get(SENTENCE_START_KEY);
        this.currentChapter = (tempChpt == null ? 0 : Integer.valueOf(tempChpt));
        this.currentWordIdx = (tempWord == null ? 0 : Integer.valueOf(tempWord));
        this.currSentenceStart = (tempSentenceStart == null ? 0 : Integer.valueOf(tempSentenceStart));
        resetChapterGlobals();
    }

    public static StringTokenizer getWordTokens(String words) {
        if (words == null || words.isEmpty()) {
            return null;
        }
        StringTokenizer tokens = new StringTokenizer(words, " \t\n\r\f", false);
        return tokens;
    }

    public static ArrayList<String> tokensToArrayList(StringTokenizer tokens) {
        // given a story tokenized by words dump them into arraylist
        ArrayList<String> story = new ArrayList<String>();
        while (tokens.hasMoreTokens()) {
            story.add(tokens.nextToken());
        }
        return story;

    }

    public String readSampleChapter(Book book, int chapterNumber) {
        // TODO test if invalid chapter passed in
        String chapterContents = null;
        if (book != null) {
            Spine spine = book.getSpine();
            chapterContents = getChapter(spine, chapterNumber);
        } else {
            Log.d("readSampleChpt", "book is null");
        }

        return chapterContents;

    }

    private String getChapter(Spine spine, int spineLocation) {
        if (spineLocation > spine.size()) {
            return null;
        }
        StringBuilder string = new StringBuilder();
        Resource res;
        InputStream is;
        BufferedReader reader;
        String line;
        res = spine.getResource(spineLocation);
        try {
            is = res.getInputStream();
            reader = new BufferedReader(new InputStreamReader(is));
            while ((line = reader.readLine()) != null) {
                Spanned span = lineParser(line);
                if (span != null) {
                    string.append(span);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // TODO check if this is faster than the if statement to add a space after each insert
        return string.toString().replace(".", ". ");
    }

    private Spanned lineParser(String line) {

        if (line.contains("<title>")) {
            return null;
        }
        String formattedLine = formatLine(line);
        Spanned HTMLText = Html.fromHtml(formattedLine);
        return HTMLText;
    }

    private String formatLine(String line) {
        /*
         * belongs to above fn
         */
        if (line.contains("http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd")) {
            line = line.substring(line.indexOf(">") + 1, line.length());
        }

        // REMOVE STYLES AND COMMENTS IN HTML
        if ((line.contains("{") && line.contains("}"))
                || ((line.contains("/*")) && line.contains("*/"))
                || (line.contains("<!--") && line.contains("-->"))) {
            line = line.substring(line.length());
        }

        if (line.contains("<img")) {
            String src = line.substring(line.indexOf("src=\"") + 5);
            src = src.substring(0, src.indexOf("\""));
            Log.d("checking image files", line.toString());
//            Log.d("against", src.toString());
            List<Resource> phList = new ArrayList<Resource>();
            Bitmap bm = getBookImages(phList, src);
            Log.d("more test", bm.toString());
            ImageView im = rootView.findViewById(R.id.image1);
            im.setImageBitmap(bm);
//            Log.d("image set", "image set");
        }
        return line;
    }

    public void setStoryContent(StringBuilder fullText) {
        fullStoryView.setText(fullText);
        fullStoryView.setMovementMethod(new ScrollingMovementMethod());
    }


    @SuppressLint("ClickableViewAccessibility")
    public void setupWPMControls() {
        raiseWPMButton = rootView.findViewById(R.id.raise_wpm_button);
        lowerWPMButton = rootView.findViewById(R.id.lower_wpm_button);
        WPM_view = rootView.findViewById(R.id.current_wpm_view);
        WPM_view.setText(String.valueOf(WPM));


        raiseWPMButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Log.d(TAG, "onClick");
                incrementWPM();
                // TODO necessary to do here AND in motion up?
                PrefsUtil.writeLongToPrefs(activity, WPM_KEY, WPM);
            }
        });

        raiseWPMButton.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
//                Log.d(TAG, "long click");
                autoIncrementWPM = true;
                WPMUpdateHandler.post(new RepetitiveUpdater());
                return false;
            }
        });

        raiseWPMButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_UP && autoIncrementWPM) {
                    Log.d(TAG, "touch up");
                    autoIncrementWPM = false;
                    PrefsUtil.writeLongToPrefs(activity, WPM_KEY, WPM);

                }
                return false;
            }
        });


        lowerWPMButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Log.d(TAG, "onClick");
                decrementWPM();
            }
        });

        lowerWPMButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
//                Log.d(TAG, "long click");
                autoDecrementWPM = true;
                WPMUpdateHandler.post(new RepetitiveUpdater());
                return false;
            }
        });

        lowerWPMButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_UP && autoDecrementWPM) {
//                    Log.d(TAG, "touch up");
                    autoDecrementWPM = false;
                }
                return false;
            }
        });
    }

    public void incrementWPM() {
        if (WPM < 1000) {
            WPM++;
            WPM_view.setText(String.valueOf(WPM));
        }
    }

    public void decrementWPM() {
        if (WPM > 0) {
            WPM--;
            WPM_view.setText(String.valueOf(WPM));
        }
    }

    /* used to update WPM values while button held */
    class RepetitiveUpdater implements Runnable {
        @Override
        public void run() {
            if (autoIncrementWPM) {
                incrementWPM();
                WPMUpdateHandler.postDelayed(new RepetitiveUpdater(), REPEAT_DELAY);
            } else if (autoDecrementWPM) {
                decrementWPM();
                WPMUpdateHandler.postDelayed(new RepetitiveUpdater(), REPEAT_DELAY);
            }
        }
    }
}
