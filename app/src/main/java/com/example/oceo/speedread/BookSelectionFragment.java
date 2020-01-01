package com.example.oceo.speedread;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class BookSelectionFragment extends Fragment {
    private String TAG = "BookSelectionFragment";
    Activity activity;
    Fragment frag;
    View rootView;
    ListView bookListView;
    private ArrayList<String> bookList;
    private ArrayList<String> displayList;
    SendChosenFile selectionCallback;
    Button fileChooseButton;
    String filePath;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.activity = getActivity();
        this.frag = this;
        this.bookList = PrefsUtil.readBooksFromPrefs(activity);
        Log.d("bookList", this.bookList.toString());
        displayList = cleanBookNames();
        selectionCallback = (MainActivity) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.book_selection, container, false);
        bookListView = rootView.findViewById(R.id.book_list);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, displayList);
        bookListView.setAdapter(adapter);

        setUpFileChoice(); // TODO come up with a better name

        bookListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int position, long arg3) {
                String value = (String) adapter.getItemAtPosition(position);
                Log.d(TAG, "onItemSelected: " + bookList.get(position));
                selectionCallback.sendFilePath(bookList.get(position));
                // assuming string and if you want to get the value on click of list item
                // do what you intend to do on click of listview row
            }
        });


        return rootView;
    }


    public ArrayList<String> cleanBookNames() {
        ArrayList<String> bookNames = new ArrayList<String>();
        for (String book : this.bookList) {
            String temp = book;
            temp = book.substring(book.lastIndexOf('/') + 1);
            temp = temp.replace(".epub", "");
            bookNames.add(temp);
        }
        return bookNames;

    }

    /*
        open files
     */
    public void setUpFileChoice() {
        fileChooseButton = rootView.findViewById(R.id.choose_file_button);
        fileChooseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    FileSelector.launchFileChooser(frag);
                } else {
                    Log.d("Open file", "No File Permissions");
                    FileSelector.requestReadPermission(activity);
                }
            }
        });
    }

    /*
    result of selecting a file from OP6 file explorer
        would have liked this to be in the FileSelector class but seems the result should be here
        otherwise no access to the filepath var as it can not be returned. Im sure there's a better
        way tod o this
    */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("result", String.valueOf(requestCode));
        switch (requestCode) {
            case 1:
                if (resultCode == -1) {
                    Uri fileUri = data.getData();
                    filePath = fileUri.getPath();
                }
                break;
        }
        filePath = modifyFilePath(filePath);
        selectionCallback.sendFilePath(filePath);
    }

    public String modifyFilePath(String filePath) {
//         TODO more robust file openings. sometimes the path is different
        // TODO can go into utils
        // get this to work for multiple file open apps
        // right now this is what works for my current phone, a OP6
        filePath = "/" + filePath.substring(filePath.indexOf(':') + 1, filePath.length());
        filePath = "/storage/emulated/0/" + filePath;
        filePath = filePath.replaceAll("//", "/");
        return filePath;
    }

    /*
        callback method
     */
    interface SendChosenFile {
        void sendFilePath(String fPath);
    }
}
