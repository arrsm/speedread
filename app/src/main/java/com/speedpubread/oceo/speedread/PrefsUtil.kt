package com.speedpubread.oceo.speedread

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*
import kotlin.collections.HashMap

object PrefsUtil {
    private const val TAG = "PrefsUtil"

    /*
    think over what/how much we can store here reasonably
    methods to remove items from prefs

     */
    fun writeBookDetailsToPrefs(activity: Activity, bookName: String, bookDetails: HashMap<String, String>?) {
//        Log.d(TAG, "I want to write the following book details to prefs key: $bookName")
//        Log.d(TAG, bookDetails.toString())
        val sharedPref = activity.getPreferences(Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        if (bookDetails != null) {
            val gson = Gson()
            val bookDetailsJson = gson.toJson(bookDetails)
            editor.putString(bookName, bookDetailsJson)
            editor.apply()
            editor.commit()
        } else {
//            Log.d(TAG, "null details, not written to prefs")
        }
    }

    fun readBookDetailsFromPrefs(activity: Activity, bookName: String?): HashMap<String, String>? {
        val sharedPref = activity.getPreferences(Context.MODE_PRIVATE)
        val bookDetailsJson = sharedPref.getString(bookName, null)
        val gson = Gson()
        val type = object : TypeToken<HashMap<String?, String?>?>() {}.type
        val bookDetails = gson.fromJson<HashMap<String, String>>(bookDetailsJson, type)
//        bookDetails.let {
//            Log.d("chapter from prefs", it?.toString())
//        }
        return bookDetails
    }

    fun writeLongToPrefs(activity: Activity, key: String?, `val`: Long) {
        val sharedPref = activity.getPreferences(Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putLong(key, `val`)
        editor.apply()
        editor.commit()
    }

    fun readLongFromPrefs(activity: Activity, key: String?): Long {
        val sharedPref = activity.getPreferences(Context.MODE_PRIVATE)
        val `val`: Long
        `val` = when (key) {
            "wpm" -> sharedPref.getLong(key, 200)
            "sentence_delay" -> sharedPref.getLong(key, 250)
            else -> sharedPref.getLong(key, 0)
        }
        return `val`
    }

    fun writeBookToPrefs(activity: Activity, book: String?) {
        /*
         * note full paths are kept so a click can just send what path to open
         */
//        Log.d("writing book to prefs", activity.toString())
        val sharedPref = activity.getPreferences(Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        val currentBookList = readBooksFromPrefs(activity) ?: ArrayList()
        if (!currentBookList.contains(book)) {
            val gson = Gson()
            currentBookList.add(0, book)
            val booksListJSON = gson.toJson(currentBookList)
            editor.putString("books", booksListJSON)
            editor.apply()
            editor.commit()
        }
    }

    fun removeBookFromPrefs(activity: Activity, book: String?) {
//        Log.d("removing book from prefs", activity.toString())
        val sharedPref = activity.getPreferences(Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        var currentBookList = readBooksFromPrefs(activity)
        if (currentBookList == null) {
            currentBookList = ArrayList()
        }
        if (currentBookList.contains(book)) {
            currentBookList.remove(book)
            val gson = Gson()
            val booksListJSON = gson.toJson(currentBookList)
            editor.putString("books", booksListJSON)
            editor.apply()
            editor.commit()
        }
    }

    fun readBooksFromPrefs(activity: Activity): ArrayList<String?> {
        val sharedPref = activity.getPreferences(Context.MODE_PRIVATE)
        val gsonBooksString = sharedPref.getString("books", null)
        val gson = Gson()
        val type = object : TypeToken<ArrayList<String?>?>() {}.type
        var bookList = gson.fromJson<ArrayList<String?>>(gsonBooksString, type)
        if (bookList == null) {
            bookList = ArrayList()
        }
        return bookList
    }

    fun writeBookDeleted(activity: Activity, book: String) {
        val sharedPref = activity.getPreferences(Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        val deletedList = readBookDeleted(activity) ?: HashMap()
        Log.d("currentMetadata", deletedList.toString())
        deletedList[book] = true
        val gson = Gson()
        val insertVal = gson.toJson(deletedList)
        editor.putString("deleted", insertVal)
        editor.apply()
        editor.commit()
    }

    fun readBookDeleted(activity: Activity): HashMap<String, Boolean>? {
        val sharedPref = activity.getPreferences(Context.MODE_PRIVATE)
        val gsonBooksString = sharedPref.getString("deleted", null)
        val gson = Gson()
        val type = object : TypeToken<HashMap<String, Boolean>>() {}.type
        val bookList = gson.fromJson<HashMap<String, Boolean>>(gsonBooksString, type)
        return bookList
    }

    fun writeBookMetadata(activity: Activity, book: String) {
        //  stop gap to store deleted status of books
        // currently unused
        val sharedPref = activity.getPreferences(Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        val allMetadata = readBookMetadata(activity)
        val bookMetadata = allMetadata[book]
        Log.d("currentMetadata", bookMetadata.toString())

    }

    fun readBookMetadata(activity: Activity): HashMap<String, HashMap<String, String>> {
        //  stop gap to store deleted status of books
        // currently unused
        val sharedPref = activity.getPreferences(Context.MODE_PRIVATE)
        val gsonBooksString = sharedPref.getString("bookmetadata", null)
        val gson = Gson()
        val type = object : TypeToken<HashMap<String, HashMap<String, String>>?>() {}.type
        val metadata = gson.fromJson<HashMap<String, HashMap<String, String>>?>(gsonBooksString, type)
        return metadata ?: HashMap()
    }


}