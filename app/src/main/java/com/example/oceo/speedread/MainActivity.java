package com.example.oceo.speedread;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.StringTokenizer;

//TODO check file permissions
// file selection tool
// readSampleFile spits back the whole text in one variable. I do not think this is the right way
// to go about that
// can i count the number of words in file faster? currently converting StringBuilder to String and tokenizing
// min values for WPM (setting to 0 for example will cause a never ending postdelayed call)

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setDefaultValues();
        setupWPMControls();

        currentWordView = findViewById(R.id.current_word);
        fullStoryView = findViewById(R.id.file_test);

        fullText = readSampleFile();
        setStoryContent(fullText);

        // TODO store max size in prefs so we dont have to calculate each open
        StringTokenizer tokens = countWordsUsingStringTokenizer(fullText.toString());
//        maxWordIdx = tokens.countTokens();
        story = tokensToArrayList(tokens);
        maxWordIdx = story.size();
        Log.d(TAG, String.valueOf(maxWordIdx));

        iterateWordsInStory();
    }


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


        lowerWPMButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                WPM -= 1;
                WPM_MS = WPMtoMS(WPM);
                WPM_view.setText(String.valueOf(WPM));
            }
        });

        Log.d(TAG, "setting WPM text to: " + String.valueOf(WPM));
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
                Log.d(TAG, line);
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
}
