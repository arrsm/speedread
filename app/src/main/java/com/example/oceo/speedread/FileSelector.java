package com.example.oceo.speedread;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;


public class FileSelector extends AppCompatActivity {
    private String TAG = "FileSelector";
    private final int EXTERNAL_STORAGE_READ_PERMISSION_REQ = 3;
    Activity activity; // set when launchFile initially called. maybe a better way?
    Fragment frag;


    public static void launchFileChooser(Fragment frag) {
        // TODO https://developer.android.com/reference/android/content/Intent.html
        Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFile.setType("*/*");
        chooseFile = Intent.createChooser(chooseFile, "Choose a file");
        frag.startActivityForResult(chooseFile, 1);
    }

    public static void requestReadPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 3);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // file permission request callback
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d("permissionsReqResult", String.valueOf(requestCode));
        switch (requestCode) {
            case EXTERNAL_STORAGE_READ_PERMISSION_REQ:
                Log.d(TAG, "Read External storage Permission");
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.v(TAG, "Permission: " + permissions[0] + "was " + grantResults[0]);
                    launchFileChooser(frag);
                } else {
                    Log.d("Permission", "File Perm not granted");
                }

                break;
        }
    }


}
