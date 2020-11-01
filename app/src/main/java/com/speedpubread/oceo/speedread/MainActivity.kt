package com.speedpubread.oceo.speedread

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.speedpubread.oceo.speedread.BookSelectionFragment.RemoveChosenFile
import com.speedpubread.oceo.speedread.BookSelectionFragment.SendChosenFile
import com.speedpubread.oceo.speedread.EPubLibUtil.Companion.getBook
import io.reactivex.disposables.Disposable
import nl.siegmann.epublib.domain.Book
import java.lang.Exception

class MainActivity : AppCompatActivity(), SendChosenFile, RemoveChosenFile {
    var TAG = "MainActivity"
    var activity: Activity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity = this
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            addBookSelectionFragment()
        }
    }

    fun setupFirebase() {
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        //https://developer.android.com/guide/topics/ui/menus.html
//        Log.d("main", "crating menu")
        val inflater = menuInflater
        inflater.inflate(R.menu.settings_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.color_menu_item ->                 // Action goes here
                true
            R.id.wpm_menu_option -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
//        Log.d(TAG, "onStart");
        super.onStart()
//        RxSandbox.testMappingObs();
    }

    override fun onRestart() {
//        Log.d(TAG, "onSRestart");
        super.onRestart() // Always call the superclass method first
    }

    public override fun onPause() {
//        Log.d(TAG, "onPause");
        super.onPause()
    }

    override fun onStop() {
//        Log.d(TAG, "onStop");
        super.onStop() // Always call the superclass method first
    }

    public override fun onResume() {
        Log.d(TAG, "onResume")
        super.onResume()
    }


    private fun addBookSelectionFragment() {
        val fragment = BookSelectionFragment()
        val transaction = supportFragmentManager
                .beginTransaction()
                .replace(R.id.container, fragment)
        transaction.commit()
    }


    override fun sendFilePath(fPath: String?) {
        val bundle = Bundle()
        bundle.putString("file_path", fPath)

        val book: Book?
        try {
            book = getBook(fPath, this)!!
            PrefsUtil.writeBookToPrefs(activity!!, fPath)
            val br = BookReaderFragment(book)
            br.arguments = bundle
            val transaction = supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.container, br)
                    .addToBackStack("tag")
            transaction.commit()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to open", Toast.LENGTH_LONG).show()
        }
    }

    override fun removeFile(fPath: String?) {
        val bundle = Bundle()
        bundle.putString("file_path", fPath)
        val bs = BookSelectionFragment()
        bs.arguments = bundle
        PrefsUtil.removeBookFromPrefs(this, fPath)
        val transaction = supportFragmentManager
                .beginTransaction()
                .replace(R.id.container, bs)
//                .addToBackStack("tag")
        transaction.commit()
    }


}