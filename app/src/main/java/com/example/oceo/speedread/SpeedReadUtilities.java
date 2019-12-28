package com.example.oceo.speedread;

import android.os.Environment;

import java.io.File;

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

}
