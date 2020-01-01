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

    /*
    set up singular methods that take different types.
    Json for more cmoplex stuff
    think over what/how much we can store here reasonably
     */

    public static void writeChapterToPrefs(Activity activity, int chapter) {
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("chapter", chapter);
        editor.apply();
        editor.commit();
    }

    public static int readChapterFromPrefs(Activity activity) {
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        int chapter = sharedPref.getInt("chapter", 0);
        Log.d("chapter from prefs", String.valueOf(chapter));
        return chapter;
    }


    public static void writeCurrWordIdxToPrefs(Activity activity, int wordIdx) {
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("currWordIdx", wordIdx);
        editor.apply();
    }

    public static int readCurrWordIdxFromPrefs(Activity activity) {
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        int chapter = sharedPref.getInt("currWordIdx", 0);
        return chapter;
    }

    public static void writeRecentlyUsedBookToPrefs(Activity activity, String bookName) {
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("recent_books", bookName);
        editor.apply();
    }

    public static void readRecentlyUsedBookToPrefs(Activity activity) {
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        sharedPref.getString("recent_books", "");
    }


    public static void writeBookToPrefs(Activity activity, String book) {
        Log.d("writing book to prefs", activity.toString());
        Gson gson = new Gson();
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        ArrayList<String> currentBookList = readBooksFromPrefs(activity);
        if (currentBookList == null) {
            currentBookList = new ArrayList<String>();
        }
        currentBookList.add(0, book);
        String booksListJSON = gson.toJson(currentBookList);
        editor.putString("books", booksListJSON);
        editor.apply();
        editor.commit();
    }

    public static ArrayList<String> readBooksFromPrefs(Activity activity) {
        Gson gson = new Gson();
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        String gsonBooksString = sharedPref.getString("books", null);

        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();
        ArrayList<String> bookList = gson.fromJson(gsonBooksString, type);

        return bookList;
    }

}
