package com.example.oceo.speedread;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.epub.EpubReader;

public class FileSelector extends AppCompatActivity {
    private String TAG = "FileSelector";


    public static void launchFileChooser(Activity context) {
        Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFile.setType("*/*");
        chooseFile = Intent.createChooser(chooseFile, "Choose a file");
        context.startActivityForResult(chooseFile, 1);
    }

    //    @Override
    public void onRequestPermissionsResult(Activity activity, int requestCode, String[] permissions, int[] grantResults) {
        // file permission request callback
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d("permissionsReqResult", String.valueOf(requestCode));
        // 3 for external storage read
        switch (requestCode) {
            case 3:
                Log.d(TAG, "Read External storage Permission");
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.v(TAG, "Permission: " + permissions[0] + "was " + grantResults[0]);
                    launchFileChooser(activity);
                } else {
                    Log.d("Permission", "File Perm not granted");
                }

                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // file selection callback
        Log.d(TAG, "fileSelection: " + String.valueOf(requestCode));
        String filePath = null;
        Uri fileUri;
        switch (requestCode) {
            case 1:
                if (resultCode == -1) {
                    fileUri = data.getData();
                    filePath = fileUri.getPath();
                }
                break;
        }
        filePath = modifyFilePath(filePath);
        chooseFile(filePath);
    }

    public String modifyFilePath(String filePath) {
        // get this to work for multiple file open apps
        // right now this is what works for my current phone, a OP6
        filePath = "/" + filePath.substring(filePath.indexOf(':') + 1, filePath.length());
        return filePath;
    }

    public Book chooseFile(String fName) {
        Log.d("file open", fName);
//        Log.d("i used before", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString());
//        File file = new File("/storage/emulated/0/Download/Malazan 10 - The Crippled God - Erikson_ Steven.epub");
        fName = "/storage/emulated/0/" + fName;
        fName = fName.replaceAll("//", "/");
        // TODO more robust file openings. sometimes the path is different
        File file = new File(fName);
        Book book = null;

        try {
            InputStream epubInputStream = new FileInputStream(file.toString());
            Log.d("what is it", file.toString());
            if (file.toString().contains(".epub")) {
                book = (new EpubReader()).readEpub(epubInputStream);
            } else {
                Toast.makeText(this, "Not epub", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return book;

//        this.book = book; // think about how to better structure this
//
//        if (this.book != null) {
//            readStory(book);
//            iterateWordChunksRX();
//        }
    }
}
