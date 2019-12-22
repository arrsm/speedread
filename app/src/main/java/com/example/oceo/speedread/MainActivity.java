package com.example.oceo.speedread;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

//TODO check file permissions
// file selection tool
// readSampleFile spits back the whole text in one variable. I do not think this is the right way
// to go about that
// can i count the number of words in file faster? currently converting StringBuilder to String and tokenizing

public class MainActivity extends AppCompatActivity {

    String TAG = "MainActivity";
    protected StringBuilder fullText;
    private TextView fullStoryView;
    private int currentWordIdx;
    private int maxWordIdx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fullText = readSampleFile();
        maxWordIdx = countWordsUsingStringTokenizer(fullText.toString());
        setStoryContent(fullText);
    }

    public void setStoryContent(StringBuilder fullText) {
        fullStoryView = findViewById(R.id.file_test);
        fullStoryView.setText(fullText);
        fullStoryView.setMovementMethod(new ScrollingMovementMethod());
    }

    public void ChunkText() {
        // should chunk parsed text into some manageable sizes to display to the user
    }

    public static int countWordsUsingStringTokenizer(String words) {
        if (words == null || words.isEmpty()) {
            return 0;
        }
        StringTokenizer tokens = new StringTokenizer(words);
        return tokens.countTokens();
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
