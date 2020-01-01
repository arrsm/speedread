package com.example.oceo.speedread;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.HashMap;

public class PrefsUtil {

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


}
