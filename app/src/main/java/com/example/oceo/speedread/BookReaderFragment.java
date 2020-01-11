package com.example.oceo.speedread;

import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.widget.Spinner;
import android.widget.TextView;

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
    TODO check file permissions
    percentage read of chapter/book
    indication for when reading is happening
    keep track of start and end indexes to have better resume experience
    make spine a dropdown so user can choose section?
    move all epub related stuff to the epubutil class
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
    int firstTimeFlag = 0; // should spinner action be called
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


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.activity = getActivity();
        this.frag = this;
        Bundle bundle = this.getArguments();
        this.chosenFilePath = bundle.getString("file_path");
        this.chosenFileName = SpeedReadUtilities.bookNameFromPath(this.chosenFilePath);
        setDefaultValues();
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
        if (disposableReader != null && !disposableReader.isDisposed()) {
            disposableReader.dispose();
        }
        bookDetails.put(CHAPTER_KEY, String.valueOf(currentChapter));
        bookDetails.put(WORD_KEY, String.valueOf(currentWordIdx));
        bookDetails.put(SENTENCE_START_KEY, String.valueOf(currSentenceStart));
        PrefsUtil.writeBookDetailsToPrefs(activity, chosenFileName, bookDetails);
        super.onPause();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        rootView = inflater.inflate(R.layout.book_reader, container, false);

        this.book = readFile(this.chosenFilePath);
        setupWPMControls();
        setupChapterControls(this.book);


        currentChunkView = rootView.findViewById(R.id.current_chunk);
        currentChunkView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!disposableReader.isDisposed()) {
                    disposableReader.dispose();
                } else {
                    iterateWords();
                }

            }
        });
        currentWordView = rootView.findViewById(R.id.current_word);
        fullStoryView = rootView.findViewById(R.id.file_test);

        if (this.book != null) {
            PrefsUtil.writeBookToPrefs(activity, this.chosenFilePath);
            this.tocResourceIds = getTOCResourceIDs();
            displayTOC();
            readStory();
        }

        return rootView;

    }

    public void scrollSentences(int numSentences) {
        //TODO testing
        // average number of sentences in a page for page turn?
        int startIdx = getNextSentencesEndIdx(this.story, numSentences, currentWordIdx);
        this.currSentenceStart = startIdx + 1;
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
                    if (disposableReader != null && !disposableReader.isDisposed()) {
                        disposableReader.dispose();
                    }
                    bookDetails.put(CHAPTER_KEY, String.valueOf(currentChapter));
                    PrefsUtil.writeBookDetailsToPrefs(activity, chosenFileName, bookDetails);
                    currentChapterview.setText("Chapter: " + String.valueOf(currentChapter + 1));
                    resetChapterGlobals();
                    readStory();
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

    public void readStory() {
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

    public int getNextSentencesEndIdx(ArrayList<String> tokens, int numSentences, int startIdx) {
        int foundSentences = 0;
        while (foundSentences < numSentences) {
            while (startIdx < maxWordIdx &&
                    (!tokens.get(startIdx).contains(".")
                            || tokens.get(startIdx).contains("?")
                            || tokens.get(startIdx).contains("!"))) {
                startIdx++;
            }
            startIdx += 1;
            foundSentences += 1;
        }
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

    public void iterateWords() {
        int tempWordIdx = this.currentWordIdx;
        int sentencesEndIdx = getNextSentencesEndIdx(story, 1, this.currentWordIdx);
        this.displayStrs = buildBoldSentences(this.story, currSentenceStart, sentencesEndIdx);
//        ArrayList<StringBuilder> formattedStrings = buildBoldSentences(this.story, currSentenceStart, sentencesEndIdx);

        Observable rangeObs = Observable.range(tempWordIdx, sentencesEndIdx - currentWordIdx);
        rangeObs = rangeObs.concatMap(i -> Observable.just(i).delay(WPM_MS, TimeUnit.MILLISECONDS));
        rangeObs = rangeObs.delay(this.sentenceDelay, TimeUnit.MILLISECONDS); // delay at the end of the sentence
        rangeObs = rangeObs.observeOn(AndroidSchedulers.mainThread());

        disposableReader = rangeObs.subscribe(wordIdx -> {
//                    Log.d("The OBS", String.valueOf(wordIdx) + " / " + String.valueOf(sentencesEndIdx));
                    if (this.currSentenceIdx < this.displayStrs.size()) {

                        currentChunkView.setText(Html.fromHtml(this.displayStrs.get(this.currSentenceIdx).toString()));
                        this.currentWordView.setText(this.story.get(this.currentWordIdx));
                        this.currSentenceIdx++;
                        this.currentWordIdx++;
                    } else {
                        Log.d("The OBS", "Is Out of Bounds");
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
                    if (disposableReader != null && !disposableReader.isDisposed()) {
                        disposableReader.dispose();
                    }
                    bookDetails.put(CHAPTER_KEY, String.valueOf(currentChapter));
                    PrefsUtil.writeBookDetailsToPrefs(activity, chosenFileName, bookDetails);
                    currentChapterview.setText("Chapter: " + String.valueOf(currentChapter + 1));
                    resetChapterGlobals();
                    readStory();
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
                        currentChapterview.setText("Chapter: " + String.valueOf(currentChapter + 1));
                        if (disposableReader != null && !disposableReader.isDisposed()) {
                            disposableReader.dispose();
                        }
                        resetChapterGlobals();
                        readStory();
                        iterateWords();
                    }
                }
            }
        });

        currentChapterview.setText("Chapter: " + String.valueOf(currentChapter + 1));

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
