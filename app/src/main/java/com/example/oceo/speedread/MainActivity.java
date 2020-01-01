package com.example.oceo.speedread;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
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
import java.util.List;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.Spine;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.epub.EpubReader;


import io.reactivex.Observable;

import com.example.oceo.speedread.PrefsUtil;
import com.example.oceo.speedread.EPubLibUtil;

import static com.example.oceo.speedread.EPubLibUtil.exploreTOC;
import static com.example.oceo.speedread.EPubLibUtil.getTOCResourceIds;


public class MainActivity extends FragmentActivity implements BookSelectionFragment.SendChosenFile {
    String TAG = "MainActivity";
    Activity activity;
    private int currentChapter;
    private int currentWordIdx;
    Disposable disposableReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
//            addBookReaderFragment();
            addBookSelectionFragment();
        }
    }


    @Override
    protected void onStart() {
//        Log.d(TAG, "onStart");
        super.onStart();  // Always call the superclass method first

    }

    @Override
    protected void onRestart() {
//        Log.d(TAG, "onSRestart");
        super.onRestart();  // Always call the superclass method first

    }

    @Override
    public void onPause() {
//        Log.d(TAG, "onPause");
        if (disposableReader != null && !disposableReader.isDisposed()) {
            disposableReader.dispose();
        }

//        PrefsUtil.writeChapterToPrefs(activity, currentChapter);
//        PrefsUtil.writeCurrWordIdxToPrefs(activity, currentWordIdx);
        super.onPause();  // Always call the superclass method first
    }

    @Override
    protected void onStop() {
//        Log.d(TAG, "onStop");
        super.onStop();  // Always call the superclass method first
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();  // Always call the superclass method first
//        if (book != null) {
//            currentWordIdx = PrefsUtil.readCurrWordIdxFromPrefs(activity);
//            iterateWordChunksRX();
//        }
    }


    private void addBookReaderFragment() {
        BookReaderFragment fragment = new BookReaderFragment();
        FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, fragment)
                .addToBackStack("tag");
        transaction.commit();
    }

    private void addBookSelectionFragment() {
        BookSelectionFragment fragment = new BookSelectionFragment();
        FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, fragment)
                .addToBackStack("tag");
        transaction.commit();
    }

    @Override
    public void sendFilePath(String fPath) {
        Bundle bundle = new Bundle();
        bundle.putString("file_path", fPath);
        BookReaderFragment br = new BookReaderFragment();
        br.setArguments(bundle);

        FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, br)
                .addToBackStack("tag");
        transaction.commit();

    }


}
