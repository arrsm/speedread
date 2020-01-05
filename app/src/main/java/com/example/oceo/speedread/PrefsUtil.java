package com.example.oceo.speedread;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class PrefsUtil {
    private static final String TAG = "PrefsUtil";

    /*
    think over what/how much we can store here reasonably
    methods to remove items from prefs

     */

    public static void writeBookDetailsToPrefs(Activity activity, String bookName, HashMap<String, String> bookDetails) {
        Log.d(TAG, "I want to write the following book details to prefs key: " + bookName);
        Log.d(TAG, String.valueOf(bookDetails));
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        if (!(bookDetails == null)) {
            Gson gson = new Gson();
            String bookDetailsJson = gson.toJson(bookDetails);
            editor.putString(bookName, bookDetailsJson);
            editor.apply();
            editor.commit();
        } else {
            Log.d(TAG, "null details, not written to prefs");
        }

    }

    public static HashMap<String, String> readBookDetailsFromPrefs(Activity activity, String bookName) {
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        String bookDetailsJson = sharedPref.getString(bookName, null);

        Gson gson = new Gson();
        Type type = new TypeToken<HashMap<String, String>>() {
        }.getType();
        HashMap<String, String> bookDetails = gson.fromJson(bookDetailsJson, type);
        Log.d("chapter from prefs", String.valueOf(bookDetails));
        return bookDetails;
    }


    public static void writeLongToPrefs(Activity activity, String key, long val) {
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong(key, val);
        editor.apply();
        editor.commit();
    }

    public static long readLongFromPrefs(Activity activity, String key) {
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        long val;
        switch (key) {
            case "wpm":
                val = sharedPref.getLong(key, 200);
                break;
            case "sentence_delay":
                val = sharedPref.getLong(key, 250);
                break;
            default:
                val = sharedPref.getLong(key, 0);
        }
        return val;
    }


    public static void writeBookToPrefs(Activity activity, String book) {
        /*
         * note full paths are kept so a click can just send what path to open
         */
        Log.d("writing book to prefs", activity.toString());
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        ArrayList<String> currentBookList = readBooksFromPrefs(activity);

        if (currentBookList == null) {
            currentBookList = new ArrayList<String>();
        }

        if (!currentBookList.contains(book)) {
            Gson gson = new Gson();
            currentBookList.add(0, book);
            String booksListJSON = gson.toJson(currentBookList);
            editor.putString("books", booksListJSON);
            editor.apply();
            editor.commit();
        }
    }

    public static ArrayList<String> readBooksFromPrefs(Activity activity) {
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        String gsonBooksString = sharedPref.getString("books", null);

        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();
        ArrayList<String> bookList = gson.fromJson(gsonBooksString, type);
        if (bookList == null) {
            bookList = new ArrayList<String>();
        }

        return bookList;
    }

}
