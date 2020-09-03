package com.example.oceo.speedread

import android.os.Environment
import android.util.Log
import java.util.*

class SpeedReadUtilities {
    fun listFiles() {
        val sdcard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val files = sdcard.listFiles()
        for (i in files.indices) {
            Log.d("File: ", files[i].name)
        }
    }

    companion object {
        /*
        used to prepend path so that app can find the book in the storaeg
        TODO more robust file openings. sometimes the path is different
         get this to work for multiple file open apps right now this is what works for my current phone, a OP6
         save WPM setting to prefs

     */
        @JvmStatic
        fun modifyFilePath(filePath: String): String {
            var filePath = filePath
            filePath = "/" + filePath.substring(filePath.indexOf(':') + 1)
            filePath = "/storage/emulated/0/$filePath"
            filePath = filePath.replace("//".toRegex(), "/")
            return filePath
        }

        /*
        given path+fileName extract the fileName
     */
        @JvmStatic
        fun bookNameFromPath(fPath: String): String {
            var temp = fPath
            temp = temp.substring(temp.lastIndexOf('/') + 1)
            temp = temp.replace(".epub", "")
            return temp
        }

        /*
        same as bookNameFromPath but modifies all in arrayList
     */
        @JvmStatic
        fun bookNamesFromPath(filePaths: ArrayList<String?>): ArrayList<String> {
            val bookNames = ArrayList<String>()
            for (path in filePaths) {
                val temp = bookNameFromPath(path!!)
                bookNames.add(temp)
            }
            return bookNames
        }

        @JvmStatic
        fun WPMtoMS(WPM: Long): Long {
            return Math.round(1000.0 / (WPM / 60.0))
        }
    }
}