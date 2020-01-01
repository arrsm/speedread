package com.example.oceo.speedread;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.Spine;
import nl.siegmann.epublib.domain.TOCReference;

import static com.example.oceo.speedread.EPubLibUtil.exploreTOC;


public class BookReaderFragment extends Fragment {

    /*
    TODO check file permissions
    file selection tool
    write to prefs when app closed or minimized
    can i count the number of words in file faster? currently converting StringBuilder to String and tokenizing
    min values for WPM (setting to 0 for example will cause a never ending postdelayed call)
    show values changing WHILE button held https://stackoverflow.com/questions/12071090/triggering-event-continuously-when-button-is-pressed-down-in-android
    also prob cant touch both at the same time
    indication for when reading is happening
    scroll up or down to get to previous lines. eg if iw ant to reread a paragraph
    keep track of start and end indexes to have better resume experience
    delay on sentence end
    take some time to think about when exactly globals need to be reset and if they need to be global at all
    play with delimeter settings
    will have to make this a fragment and add an additional fragment representing a book library
    prefs needs to associate chapter / page  progress with books
    prefs to store list of recently used books
    reset chapter if keeps failing
    seek to next sentence
    seek to next paragraph
 */

    String TAG = "BookReaderFragment";
    Activity activity;
    Fragment frag;
    View rootView;

    private long WPM;
    private long WPM_MS;
    private int currSentenceStart;
    private int currSentenceEnd;
    private int currentWordIdx; // current word being iterated over
    private int maxWordIdx; // last word in chapter
    private int chunkIdx; // word being iterated over in chunk
    private int currentChapter;
    private int chunkSize; // number of words displayed as focus
    protected StringBuilder fullText; // holds full story in memory
    private ArrayList<String> story; // fullText converted to arraylist
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
    private ArrayList<StringBuilder> displayStrs; // crutch to display bolded words. would like to change
    private ArrayList<String> tocResourceIds;
    HashMap<String, String> bookDetails;
    protected String chosenFilePath;
    protected String chosenFileName;

    final String CHAPTER_KEY = "chapter";
    final String WORD_KEY = "page";


    Book book;

    Disposable disposableReader;

    //long held incrementers
    Timer fixedTimer = new Timer();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.activity = getActivity();
        this.frag = this;
        Bundle bundle = this.getArguments();
        this.chosenFilePath = bundle.getString("file_path");
        this.chosenFileName = SpeedReadUtilities.bookNameFromPath(this.chosenFilePath);
        this.bookDetails = PrefsUtil.readBookDetailsFromPrefs(activity, chosenFileName);
        Log.d(TAG, "i want to read from prefs");
        Log.d(TAG, String.valueOf(this.bookDetails));
        if (this.bookDetails == null) {
            this.bookDetails = new HashMap<String, String>();
        }

        String tempChpt = this.bookDetails.get(CHAPTER_KEY);
        String tempWord = this.bookDetails.get(WORD_KEY);

        this.currentChapter = (tempChpt == null ? 0 : Integer.valueOf(tempChpt));
        this.currentWordIdx = (tempWord == null ? 0 : Integer.valueOf(tempWord));
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.book_reader, container, false);

        readFile(this.chosenFilePath);

        setDefaultValues();
        setupWPMControls();
        setupChapterControls();


        currentChunkView = rootView.findViewById(R.id.current_chunk);
        currentChunkView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!disposableReader.isDisposed()) {
                    disposableReader.dispose();
                } else {
                    iterateWordChunksRX();
                }

            }
        });
        currentWordView = rootView.findViewById(R.id.current_word);
        fullStoryView = rootView.findViewById(R.id.file_test);


//        this.book = EPubLibUtil.getBook("storage/emulated/0/Books/MoonReader/Brandon Sanderson - Oathbringer_ Book Three of the Stormlight Archive-Tor Books (2017).epub");
//        this.tocResourceIds = getTOCResourceIDs();
//        displayTOC();
        return rootView;

    }

    public void displayTOC() {
        List<TOCReference> tocRefs = EPubLibUtil.exploreTOC(this.book);
        ArrayList<String> TOCTitles = new ArrayList<String>();
        TOCTitles = EPubLibUtil.getTOCTitles(tocRefs, 0, TOCTitles);
        dropdown = rootView.findViewById(R.id.spinner1);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, TOCTitles);
        dropdown.setAdapter(adapter);
        dropdown.setSelection(currentChapter);

        dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // TODO something weird with indexign is going on here
                // I thikn it has to do with the fact that its possible for spine items to not be linked in the ToC
                // choosing chpt 4. Oaths for example takes to 14 but on reopen goes to 15
                String selectedItem = tocResourceIds.get(position);
                currentChapter = EPubLibUtil.mapTOCToSpine(book, selectedItem);
                if (disposableReader != null && !disposableReader.isDisposed()) {
                    disposableReader.dispose();
                }
                bookDetails.put(CHAPTER_KEY, String.valueOf(currentChapter));
                PrefsUtil.writeBookDetailsToPrefs(activity, chosenFileName, bookDetails);
                currentChapterview.setText("Chapter: " + String.valueOf(currentChapter + 1));
                resetStoryGlobals();
                readStory();
                iterateWordChunksRX();
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


    public void readFile(String fName) {
        Log.d("file open", fName);

        this.book = EPubLibUtil.getBook(fName); // think about how to better structure this
        Log.d("got the book", String.valueOf(this.book));
        if (this.book != null) {
            PrefsUtil.writeBookToPrefs(activity, fName);
            this.tocResourceIds = getTOCResourceIDs();
            displayTOC();
            readStory();
            iterateWordChunksRX();
        }
    }


    public void readStory() {
        resetStoryGlobals();
        String chapter = readSampleChapter(book, currentChapter);
        fullText = new StringBuilder(chapter);
//        setStoryContent(fullText);

        // TODO store max size in prefs so we dont have to calculate each open
        StringTokenizer tokens = countWordsUsingStringTokenizer(fullText.toString());
        if (tokens != null) {
            maxWordIdx = tokens.countTokens();
            story = tokensToArrayList(tokens);
        }
    }

    public void resetStoryGlobals() {
        currSentenceStart = 0;
        currSentenceEnd = 0;
        currentWordIdx = 0;
        chunkIdx = 0;
    }

    public int getNextSentences(ArrayList<String> tokens, int numSentences) {
        // TODO also keep track of where the sentences end for formatting
        int tempChunkIdx = currentWordIdx;
        int foundSentences = 0;


        while (foundSentences < numSentences) {
            while (tempChunkIdx < maxWordIdx &&
                    (!tokens.get(tempChunkIdx).contains(".")
                            || tokens.get(tempChunkIdx).contains("?")
                            || tokens.get(tempChunkIdx).contains("!"))) {
                tempChunkIdx++;
            }
            tempChunkIdx += 1;
            foundSentences += 1;
        }
        return tempChunkIdx;
    }

    public ArrayList<StringBuilder> buildBoldSentences(int startIdx, int endIdx) {
        // TODO wish there was a better way to do this rather than building and holding o(n^2)
        //  strings in the number of words

        if (maxWordIdx < endIdx) {
            endIdx = maxWordIdx;
        }

        ArrayList<StringBuilder> displayStrs = new ArrayList<StringBuilder>();

        for (int targetWord = startIdx; targetWord < endIdx; targetWord++) {
            StringBuilder formattedDisplayStr = new StringBuilder();

            for (int i = startIdx; i < endIdx; i++) {
                if (targetWord == i) {
                    formattedDisplayStr.append("<b>" + story.get(i) + "</b> ");
                } else {
                    formattedDisplayStr.append(story.get(i) + " ");
                }
            }
            displayStrs.add(formattedDisplayStr);
        }

        return displayStrs;
    }

    public void setDefaultValues() {
        WPM = 175;
        WPM_MS = SpeedReadUtilities.WPMtoMS(WPM);
        chunkSize = 30;
        resetStoryGlobals();
    }

    public void iterateWordChunksRX() {
        int chunkStartIdx = currSentenceStart;
        int chunkMaxIdx = getNextSentences(story, 1);
        displayStrs = buildBoldSentences(chunkStartIdx, chunkMaxIdx);

        Observable rangeObs = Observable.range(currentWordIdx, chunkMaxIdx - currentWordIdx)
                .concatMap(i -> Observable.just(i).delay(WPM_MS, TimeUnit.MILLISECONDS))
                .observeOn(AndroidSchedulers.mainThread());

        disposableReader = rangeObs.subscribe(wordIdx -> {
//                    Log.d("The OBS", String.valueOf(wordIdx) + " / " + String.valueOf(chunkMaxIdx));
                    if (chunkIdx < displayStrs.size()) { // check we dont go out of bounds
                        currentChunkView.setText(Html.fromHtml(displayStrs.get(chunkIdx).toString()));
                        currentWordView.setText(story.get(currentWordIdx));
                        chunkIdx++;
                        currentWordIdx++;
                    } else {
                        // can reach here if we pause then resume
                        Log.d("The OBS", "Is Out of Bounds");
                    }

                },
                e -> {

                },
                () -> {
//                        Log.d("obs", "k do the next chunk");
                    if (currentWordIdx < maxWordIdx) {
                        chunkIdx = 0;
                        currSentenceStart = currentWordIdx;
                        // reset position and scroll through next chunk
                        iterateWordChunksRX();
                    } else {
                        Log.d("Observable", "No more chunks");
                    }
                });
    }

    void initTimer() {
        /*
            timer currently used for: long-pressing wpm inc/dec
         */
        fixedTimer = new Timer();
    }

    public void setupChapterControls() {

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
                    resetStoryGlobals();
                    readStory();
                    iterateWordChunksRX();
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
                        resetStoryGlobals();
                        readStory();
                        iterateWordChunksRX();
                    }
                }
            }
        });

        currentChapterview.setText("Chapter: " + String.valueOf(currentChapter + 1));

    }

    @SuppressLint("ClickableViewAccessibility")
    public void setupWPMControls() {
        raiseWPMButton = rootView.findViewById(R.id.raise_wpm_button);
        lowerWPMButton = rootView.findViewById(R.id.lower_wpm_button);
        WPM_view = rootView.findViewById(R.id.current_wpm_view);


        raiseWPMButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                WPM += 1;
                WPM_MS = SpeedReadUtilities.WPMtoMS(WPM);
                WPM_view.setText(String.valueOf(WPM));
            }
        });


        raiseWPMButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    fixedTimer.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            WPM += 10;
                        }
                    }, 1000, 100);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    fixedTimer.cancel();
                    initTimer();
                    WPM_MS = SpeedReadUtilities.WPMtoMS(WPM);
                }
                return false;
            }
        });


        lowerWPMButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                WPM -= 1;
                WPM_MS = SpeedReadUtilities.WPMtoMS(WPM);
                WPM_view.setText(String.valueOf(WPM));
            }
        });

        lowerWPMButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    fixedTimer.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            WPM -= 10;
                        }
                    }, 1000, 100);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    fixedTimer.cancel();
                    initTimer();
                    WPM_MS = SpeedReadUtilities.WPMtoMS(WPM);
                }

                return false;
            }
        });

        WPM_view.setText(String.valueOf(WPM));

    }

    public void setStoryContent(StringBuilder fullText) {
        fullStoryView.setText(fullText);
        fullStoryView.setMovementMethod(new ScrollingMovementMethod());
    }


    public static StringTokenizer countWordsUsingStringTokenizer(String words) {
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


}
