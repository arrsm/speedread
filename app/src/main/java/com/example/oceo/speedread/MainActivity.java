package com.example.oceo.speedread;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.Spine;
import nl.siegmann.epublib.epub.EpubReader;


import io.reactivex.Observable;

import com.example.oceo.speedread.PrefsUtil;

//TODO check file permissions
// file selection tool
// write to prefs when app closed or minimized
// can i count the number of words in file faster? currently converting StringBuilder to String and tokenizing
// min values for WPM (setting to 0 for example will cause a never ending postdelayed call)
// show values changing WHILE button held https://stackoverflow.com/questions/12071090/triggering-event-continuously-when-button-is-pressed-down-in-android
// also prob cant touch both at the same time
// indication for when reading is happening
// scroll up or down to get to previous lines. eg if iw ant to reread a paragraph
// keep track of start and end indexes to have better resume experience
// delay on sentence end
// take some time to think about when exactly globals need to be reset and if they need to be global at all
// play with delimeter settings
// will have to make this a fragment and add an additional fragment representing a book library
// prefs needs to associate chapter / page  progress with books
// prefs to store list of recently used books

public class MainActivity extends AppCompatActivity {

    String TAG = "MainActivity";
    Activity activity;

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
    private Button pauseButton;
    private ArrayList<StringBuilder> displayStrs; // crutch to display bolded words. would like to change

    private Button fileChooseButton;
    private Uri fileUri;
    private String filePath;
    Book book;

    Disposable disposableReader;

    //long held incrementers
    Timer fixedTimer = new Timer();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        Book book = null;
        setContentView(R.layout.activity_main);


        currentChapter = PrefsUtil.readChapterFromPrefs(this);

        setDefaultValues();
        setupWPMControls();
        setupChapterControls();
        pauseButton = findViewById(R.id.pause_button);
        fileChooseButton = findViewById(R.id.choose_file_button);
        fileChooseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                chooseFile.setType("*/*");
                chooseFile = Intent.createChooser(chooseFile, "Choose a file");
                startActivityForResult(chooseFile, 1);
            }
        });


        pauseButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!disposableReader.isDisposed()) {
                    Log.d("pause test", "PAUSE IT");
                    disposableReader.dispose();
                } else {
                    resumeStory();
                }

            }
        });


        currentChunkView = findViewById(R.id.current_chunk);

        currentChunkView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!disposableReader.isDisposed()) {
                    Log.d("pause test", "PAUSE IT");
                    disposableReader.dispose();
                } else {
                    Log.d("resume test", "RESUME IT");
                    resumeStory();
                }

            }
        });

        currentWordView = findViewById(R.id.current_word);
        fullStoryView = findViewById(R.id.file_test);

        if (book != null) {
            readStory(book);
            iterateWordChunksRX();
        }
    }

    @Override

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 1:
                if (resultCode == -1) {
                    fileUri = data.getData();
                    filePath = fileUri.getPath();
                }
                break;
        }
        filePath = "/" + filePath.substring(filePath.indexOf(':') + 1, filePath.length());
        chooseFile(filePath);

    }

    public void chooseFile(String fName) {
        Log.d("file open", fName);
//        Log.d("i used before", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString());
//        File file = new File("/storage/emulated/0/Download/Malazan 10 - The Crippled God - Erikson_ Steven.epub");
        fName = "/storage/emulated/0/" + fName;
        fName = fName.replaceAll("//", "/");
        // TODO more robust file openings. sometimes the path is different
        File file = new File(fName);
        Book book = null;

        try {
            InputStream epubInputStream = new FileInputStream(file.toString());
            Log.d("what is it", file.toString());
            if (file.toString().contains(".epub")) {
                book = (new EpubReader()).readEpub(epubInputStream);
            } else {
                Toast.makeText(this, "Not epub", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.book = book; // think about how to better structure this

        if (this.book != null) {
            readStory(book);
            iterateWordChunksRX();
        }
    }


    public void resumeStory() {
        iterateWordChunksRX();
    }

    public void readStory(Book unused) {
        resetStoryGlobals();
        fullText = new StringBuilder(readSampleChapter(book, currentChapter));
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
//            Log.d("sentenceCount: ", String.valueOf(foundSentences));
        }
//        Log.d("what NADA", tokens.get(tempChunkIdx));
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
        // TODO better way to do this is probably https://stackoverflow.com/questions/33291245/rxjava-delay-for-each-item-of-list-emitted
        // check repeat as well


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

        raiseChapterButton = findViewById(R.id.raise_chpt_button);
        lowerChapterButton = findViewById(R.id.lower_chpt_btn);
        currentChapterview = findViewById(R.id.current_chapter);


        raiseChapterButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
//                Log.d("the book:", String.valueOf(book));
                currentChapter += 1;
                if (book != null) {
                    if (disposableReader != null && !disposableReader.isDisposed()) {
                        disposableReader.dispose();
                    }
                    PrefsUtil.writeChapterToPrefs(activity, currentChapter);
                    currentChapterview.setText("Chapter: " + String.valueOf(currentChapter + 1));
                    resetStoryGlobals();
                    readStory(book);
                    iterateWordChunksRX();
                }
            }
        });


        lowerChapterButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (currentChapter >= 0) {
                    currentChapter -= 1;
                    if (book != null) {
                        PrefsUtil.writeChapterToPrefs(activity, currentChapter);
                        currentChapterview.setText("Chapter: " + String.valueOf(currentChapter + 1));
                        if (disposableReader != null && !disposableReader.isDisposed()) {
                            disposableReader.dispose();
                        }
                        resetStoryGlobals();
                        readStory(book);
                        iterateWordChunksRX();
                    }
                }
            }
        });

        currentChapterview.setText("Chapter: " + String.valueOf(currentChapter + 1));

    }

    @SuppressLint("ClickableViewAccessibility")
    public void setupWPMControls() {
        raiseWPMButton = findViewById(R.id.raise_wpm_button);
        lowerWPMButton = findViewById(R.id.lower_wpm_button);
        WPM_view = findViewById(R.id.current_wpm_view);


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

    public void getTotalChapters() {

    }

    public void displayTOC() {

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
                if (!line.contains("<title>")) {
                    Spanned HTMLText = Html.fromHtml(formatLine(line));
                    string.append(HTMLText);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // TODO check if this is faster than the if statement to add a space after each insert
        return string.toString().replace(".", ". ");
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
