package com.example.oceo.speedread;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
    RemoveChosenFile removalCallback;
    Button fileChooseButton;
    String filePath;

    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.activity = getActivity();
        this.frag = this;

        Bundle bundle = this.getArguments();
        //TODO maybe remove this stuff
//        this.chosenFilePath = bundle.getString("file_path");
//        this.chosenFileName = SpeedReadUtilities.bookNameFromPath(this.chosenFilePath);


        this.bookList = PrefsUtil.readBooksFromPrefs(activity);
        displayList = SpeedReadUtilities.bookNamesFromPath(this.bookList);
        selectionCallback = (MainActivity) activity;
        removalCallback = (MainActivity) activity;


    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.book_selection, container, false);

//        bookListView = rootView.findViewById(R.id.book_list);
//        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, displayList);
//        bookListView.setAdapter(adapter);


//        this.activity.setContentView(R.layout.book_selection);
        recyclerView = (RecyclerView) rootView.findViewById(R.id.book_recycle_view);
        layoutManager = new LinearLayoutManager(this.activity);
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new BookListAdapter(displayList, bookList, activity);
        recyclerView.setAdapter(mAdapter);

//        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, displayList);
//        bookListView.setAdapter(adapter);


        setUpFileChoice(); // TODO come up with a better name

        /*
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

         */


        return rootView;
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
        filePath = SpeedReadUtilities.modifyFilePath(filePath);
        selectionCallback.sendFilePath(filePath);
    }


    /*
        callback interfaces - why are they defined here but implemented in main
        https://stackoverflow.com/questions/18279302/how-do-i-perform-a-java-callback-between-classes
        TODO - test implementing in another class as well as mainactivity and see if both implementors get triggerd
     */
    interface SendChosenFile {
        void sendFilePath(String fPath);
    }

    interface RemoveChosenFile {
        void removeFile(String fPath);
    }
}
