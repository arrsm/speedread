package com.example.oceo.speedread;

import android.os.Environment;

import java.io.File;
import java.util.ArrayList;

public class SpeedReadUtilities {
    public static long WPMtoMS(long WPM) {
        return Math.round(1000.0 / (WPM / 60.0));
    }

    public void listFiles() {
        File sdcard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File[] files = sdcard.listFiles();
//        for (int i = 0; i < files.length; i++) {
//            Log.d("File: ", files[i].getName());
//        }
    }


    /*
        used to prepend path so that app can find the book in the storaeg
        TODO more robust file openings. sometimes the path is different
         get this to work for multiple file open apps
         right now this is what works for my current phone, a OP6
     */
    static String modifyFilePath(String filePath) {

        filePath = "/" + filePath.substring(filePath.indexOf(':') + 1);
        filePath = "/storage/emulated/0/" + filePath;
        filePath = filePath.replaceAll("//", "/");
        return filePath;
    }

    /*
        given path+fileName extract the fileName
     */
    static String bookNameFromPath(String fPath) {
        String temp = fPath;
        temp = temp.substring(temp.lastIndexOf('/') + 1);
        temp = temp.replace(".epub", "");
        return temp;


    }

    /*
        same as bookNameFromPath but modifies all in arrayList
     */
    static ArrayList<String> bookNamesFromPath(ArrayList<String> filePaths) {
        ArrayList<String> bookNames = new ArrayList<String>();
        for (String path : filePaths) {
            String temp = bookNameFromPath(path);
            bookNames.add(temp);
        }
        return bookNames;
    }

}
