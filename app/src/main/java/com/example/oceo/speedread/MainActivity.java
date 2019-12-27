package com.example.oceo.speedread;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.Spine;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.epub.EpubReader;

//TODO check file permissions
// file selection tool
// readSampleFile spits back the whole text in one variable. I do not think this is the right way
// to go about that
// can i count the number of words in file faster? currently converting StringBuilder to String and tokenizing
// min values for WPM (setting to 0 for example will cause a never ending postdelayed call)
// show values changing WHILE button held https://stackoverflow.com/questions/12071090/triggering-event-continuously-when-button-is-pressed-down-in-android
// also prob cant touch both at the same time
// determine how to chunk/reference epub parsings so we don't need to keep the whole book in memory

public class MainActivity extends AppCompatActivity {

    String TAG = "MainActivity";
    private long WPM;
    private long WPM_MS;
    private int currentWordIdx;
    private int maxWordIdx;
    protected StringBuilder fullText; // holds full story in memory
    private ArrayList<String> story; // fullText converted to arraylist
    private Handler storyWordsIterationHandler = new Handler();
    private TextView fullStoryView;
    private TextView currentWordView;
    private Button raiseWPMButton;
    private Button lowerWPMButton;
    private TextView WPM_view;

    //long held incrementers
    Timer fixedTimer = new Timer();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setDefaultValues();
        setupWPMControls();

        currentWordView = findViewById(R.id.current_word);
        fullStoryView = findViewById(R.id.file_test);
//        fullText = readSampleFile();
//        fullText = readSampleEpub();
        int chapterNumber = 15;
        fullText = new StringBuilder(readSampleChapter(chapterNumber));
        setStoryContent(fullText);

        // TODO store max size in prefs so we dont have to calculate each open
        StringTokenizer tokens = countWordsUsingStringTokenizer(fullText.toString());
        maxWordIdx = tokens.countTokens();
        story = tokensToArrayList(tokens);

        iterateWordChunks();
//        iterateWordsInStory();
    }

    public StringBuilder chunkTextIntoWords(ArrayList<String> tokens, int chunkSize) {
        // TODO must I use the global var here
        // TODO probably fails words the end
        StringBuilder displayStr = new StringBuilder();
        int chunkMax = currentWordIdx + chunkSize;
        while (currentWordIdx < chunkMax) {
            displayStr.append(tokens.get(currentWordIdx) + " ");
            currentWordIdx++;
        }
        return displayStr;
    }

    void initTimer() {
        /*
            timer currently used for: long-pressing wpm inc/dec
         */
        fixedTimer = new Timer();
    }

    @SuppressLint("ClickableViewAccessibility")
    public void setupWPMControls() {
        raiseWPMButton = findViewById(R.id.raise_wpm_button);
        lowerWPMButton = findViewById(R.id.lower_wpm_button);
        WPM_view = findViewById(R.id.current_wpm_view);


        raiseWPMButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                WPM += 1;
                WPM_MS = WPMtoMS(WPM);
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
                    WPM_MS = WPMtoMS(WPM);
                }
                return false;
            }
        });


        lowerWPMButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                WPM -= 1;
                WPM_MS = WPMtoMS(WPM);
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
                    WPM_MS = WPMtoMS(WPM);
                }

                return false;
            }
        });

        WPM_view.setText(String.valueOf(WPM));

    }

    public void setDefaultValues() {
        WPM = 200;
        WPM_MS = WPMtoMS(WPM);
        currentWordIdx = 0;
    }

    private long WPMtoMS(long WPM) {
        return Math.round(1000.0 / (WPM / 60.0));
    }

    public void iterateWordChunks() {
        currentWordView.setText(story.get(currentWordIdx));
        Runnable runnable = new Runnable() {
            public void run() {
                // TODO magic seven
                if (currentWordIdx < (maxWordIdx - 7)) {
                    StringBuilder displayStr = chunkTextIntoWords(story, 6);
                    currentWordView.setText(displayStr.toString());
                    storyWordsIterationHandler.postDelayed(this, WPM_MS * 5);
                } else {
                    storyWordsIterationHandler.removeCallbacks(this);
                }
            }
        };
        runnable.run();
    }

    public void iterateWordsInStory() {
        currentWordView.setText(story.get(currentWordIdx));
        Runnable runnable = new Runnable() {
            public void run() {
                if (currentWordIdx < (maxWordIdx - 1)) {
                    currentWordIdx++;
                    currentWordView.setText(story.get(currentWordIdx));
                    storyWordsIterationHandler.postDelayed(this, WPM_MS);
                } else {
                    storyWordsIterationHandler.removeCallbacks(this);
                }
            }
        };
        runnable.run();
    }

    public void setStoryContent(StringBuilder fullText) {
        fullStoryView.setText(fullText);
        fullStoryView.setMovementMethod(new ScrollingMovementMethod());
    }

    public static StringTokenizer countWordsUsingStringTokenizer(String words) {
        if (words == null || words.isEmpty()) {
            return null;
        }
        StringTokenizer tokens = new StringTokenizer(words);
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

    public StringBuilder readSampleFile() {
        //requires file.txt to be present in downloads directory
        String TAG = "MA readSampleFile: ";
        File sdcard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(sdcard, "file.txt");

        StringBuilder fullText = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
//                Log.d(TAG, line);
                fullText.append(line);
//                fullText.append('\n');
            }
            br.close();

        } catch (IOException e) {
            e.printStackTrace();
            //TODO handle errors here
        }
        return fullText;
    }

    public void listFiles() {
        File sdcard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File[] files = sdcard.listFiles();
//        for (int i = 0; i < files.length; i++) {
//            Log.d("File: ", files[i].getName());
//        }
    }


    public String readSampleChapter(int chapterNumber) {
        // TODO test if invalid chapter passed in
        String chapterContents;
        Book book = getBook();
        Spine spine = book.getSpine();
        chapterContents = getChapter(book, spine, chapterNumber);
        return chapterContents;
    }

    public Book getBook() {
        // TODO allow file system selection
        File sdcard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String fName = "Malazan 10 - The Crippled God - Erikson_ Steven.epub";
        File file = new File(sdcard, fName);
        Book book = null;
        // TODO check permissions here
        try {
            InputStream epubInputStream = new FileInputStream(file.toString());
            book = (new EpubReader()).readEpub(epubInputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return book;
    }

    private String getChapter(Book book, Spine spine, int spineLocation) {
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
        return string.toString();
    }

    public StringBuilder readSampleEpub() {
        //DEPRECATED use chapters instead
        StringBuilder fullText = new StringBuilder();
        File sdcard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String fName = "Malazan 10 - The Crippled God - Erikson_ Steven.epub";
        File file = new File(sdcard, fName);

        try {
            InputStream epubInputStream = new FileInputStream(file.toString());
            // Load Book from inputStream
            Book book = (new EpubReader()).readEpub(epubInputStream);
            // Log the book's authors
            Log.d("epublib", "author(s): " + book.getMetadata().getAuthors());
            // Log the book's title
            Log.d("epublib", "title: " + book.getTitle());
            // Log the book's coverimage property
            Bitmap coverImage = BitmapFactory.decodeStream(book.getCoverImage()
                    .getInputStream());
            Log.d("epublib", "Coverimage is " + coverImage.getWidth() + " by "
                    + coverImage.getHeight() + " pixels");
            // Log the tale of contents
            logTableOfContents(book.getTableOfContents().getTocReferences(), 0);

            ArrayList<String> contents = getSomeText2(book); // TODO obviously need to refactor this
            Log.d("size of stuff: ", String.valueOf(contents.size()));
            // TODO hardcoding the chapter here is not so good want to avoid pulling whole book at a time too
            fullText = new StringBuilder(contents.get(15));

        } catch (IOException e) {
            e.printStackTrace();
        }

        return fullText;
    }

    private ArrayList<String> getSomeText2(Book book) {
        // deprecated with readSampleEpub
        StringBuilder string = new StringBuilder();
        ArrayList<String> listOfPages = new ArrayList<>();
        Resource res;
        InputStream is;
        BufferedReader reader;
        String line;
        Spine spine = book.getSpine();

        for (int i = 0; i < spine.size(); i++) {
            res = spine.getResource(i);
            try {
                is = res.getInputStream();
                reader = new BufferedReader(new InputStreamReader(is));
                while ((line = reader.readLine()) != null) {
//                    System.out.println(line);
                    // FIRST PAGE LINE -> <?xml version="1.0" encoding="utf-8" standalone="no"?>
                    if (line.contains("<html")) {
//                        string.delete(0, string.length()); // is this better?
                        string = new StringBuilder();
                    }

                    // ADD THAT LINE TO THE FINAL STRING REMOVING ALL THE HTML
                    if (!line.contains("<title>")) {
                        Spanned HTMLText = Html.fromHtml(formatLine(line));
                        string.append(HTMLText);
                    }

                    // LAST PAGE LINE -> </html>
                    if (line.contains("</html>")) {
                        listOfPages.add(string.toString());
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return listOfPages;
    }


    private void logTableOfContents(List<TOCReference> tocReferences, int depth) {
        /**
         * belongs to above fn
         * Recursively Log the Table of Contents
         *
         * @param tocReferences
         * @param depth
         */
        if (tocReferences == null) {
            return;
        }
        for (TOCReference tocReference : tocReferences) {
            StringBuilder tocString = new StringBuilder();
            for (int i = 0; i < depth; i++) {
                tocString.append("\t");
            }
            tocString.append(tocReference.getTitle());
            Log.i("epublib", tocString.toString());

//            logTableOfContents(tocReference.getChildren(), depth + 1);
        }
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
