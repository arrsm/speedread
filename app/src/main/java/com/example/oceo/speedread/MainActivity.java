package com.example.oceo.speedread;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.util.ArrayList;

import io.reactivex.disposables.Disposable;


public class MainActivity extends AppCompatActivity
        implements BookSelectionFragment.SendChosenFile, BookSelectionFragment.RemoveChosenFile {
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

            //https://stackoverflow.com/questions/38390085/how-to-inflate-an-optionsmenu-vertically-on-a-button-click
        }


      /*
        firebase
     */
      /*

        Button logTokenButton = findViewById(R.id.logTokenButton);
        logTokenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get token
                // [START retrieve_current_token]
                FirebaseInstanceId.getInstance().getInstanceId()
                        .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                            @Override
                            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                                if (!task.isSuccessful()) {
                                    Log.w(TAG, "getInstanceId failed", task.getException());
                                    return;
                                }

                                // Get new Instance ID token
                                String token = task.getResult().getToken();

                                // Log and toast
                                String msg = getString(R.string.msg_token_fmt, token);
                                Log.d(TAG, msg);
                                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                            }
                        });
                // [END retrieve_current_token]
            }
        });


        Button subscribeButton = findViewById(R.id.subscribeButton);
        subscribeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Subscribing to weather topic");
                // [START subscribe_topics]
                FirebaseMessaging.getInstance().subscribeToTopic("weather")
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                String msg = getString(R.string.msg_subscribed);
                                if (!task.isSuccessful()) {
                                    msg = getString(R.string.msg_subscribe_failed);
                                    Log.d("FB", "failed to sub to weatehr");
                                }
                                Log.d(TAG, msg);
                                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
                            }
                        });
                // [END subscribe_topics]
            }
        });

       */

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //https://developer.android.com/guide/topics/ui/menus.html
        Log.d("main", "crating menu");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.color_menu_item:
                // Action goes here
                return true;
            case R.id.wpm_menu_option:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    protected void onStart() {
//        Log.d(TAG, "onStart");
        super.onStart();  // Always call the superclass method first

//        RxSandbox.testMappingObs();

    }

    @Override
    protected void onRestart() {
//        Log.d(TAG, "onSRestart");
        super.onRestart();  // Always call the superclass method first

    }

    @Override
    public void onPause() {
//        Log.d(TAG, "onPause");

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

    @Override
    public void removeFile(String fPath) {
        Bundle bundle = new Bundle();
        bundle.putString("file_path", fPath);
        BookSelectionFragment bs = new BookSelectionFragment();
        bs.setArguments(bundle);
        PrefsUtil.removeBookFromPrefs(this, fPath);

        FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, bs)
                .addToBackStack("tag");
        transaction.commit();
    }
}
