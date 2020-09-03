package com.example.oceo.speedread

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ListView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.oceo.speedread.FileSelector.Companion.launchFileChooser
import com.example.oceo.speedread.FileSelector.Companion.requestReadPermission
import com.example.oceo.speedread.SpeedReadUtilities.Companion.bookNamesFromPath
import com.example.oceo.speedread.SpeedReadUtilities.Companion.modifyFilePath
import java.util.*
import kotlin.collections.ArrayList

class BookSelectionFragment : Fragment() {
    private val TAG = "BookSelectionFragment"
    var activity: Activity? = null
    var frag: Fragment? = null
    var rootView: View? = null
    var bookListView: ListView? = null
    private var bookList: ArrayList<String?> = ArrayList()
    private var displayList: ArrayList<String>? = null
    var selectionCallback: SendChosenFile? = null
    var removalCallback: RemoveChosenFile? = null
    var fileChooseButton: Button? = null
    var filePath: String? = null
    private var recyclerView: RecyclerView? = null
    private var mAdapter: RecyclerView.Adapter<*>? = null
    private var layoutManager: RecyclerView.LayoutManager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity = getActivity()
        frag = this
        val bundle = this.arguments
        //TODO maybe remove this stuff
//        this.chosenFilePath = bundle.getString("file_path");
//        this.chosenFileName = SpeedReadUtilities.bookNameFromPath(this.chosenFilePath);
        bookList = PrefsUtil.readBooksFromPrefs(activity!!)
        displayList = bookNamesFromPath(bookList)
        selectionCallback = activity as MainActivity?
        removalCallback = activity as MainActivity?
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.book_selection, container, false)

//        bookListView = rootView.findViewById(R.id.book_list);
//        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, displayList);
//        bookListView.setAdapter(adapter);


//        this.activity.setContentView(R.layout.book_selection);
        recyclerView = rootView!!.findViewById<View>(R.id.book_recycle_view) as RecyclerView
        layoutManager = LinearLayoutManager(activity)
        recyclerView!!.layoutManager = layoutManager
        mAdapter = BookListAdapter(displayList!!, bookList, activity!!)
        recyclerView!!.adapter = mAdapter

//        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, displayList);
//        bookListView.setAdapter(adapter);
        setUpFileChoice() // TODO come up with a better name

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

         */return rootView
    }

    /*
        open files
     */
    fun setUpFileChoice() {
        fileChooseButton = rootView!!.findViewById(R.id.choose_file_button)
        fileChooseButton!!.setOnClickListener(View.OnClickListener {
            if (activity!!.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                launchFileChooser(frag)
            } else {
                Log.d("Open file", "No File Permissions")
                requestReadPermission(activity)
            }
        })
    }

    /*
    result of selecting a file from OP6 file explorer
        would have liked this to be in the FileSelector class but seems the result should be here
        otherwise no access to the filepath var as it can not be returned. Im sure there's a better
        way tod o this
    */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        Log.d("result", requestCode.toString())
        when (requestCode) {
            1 -> if (resultCode == -1) {
                val fileUri = data.data
                filePath = fileUri.path
            }
        }
        filePath = modifyFilePath(filePath!!)
        selectionCallback!!.sendFilePath(filePath)
    }

    /*
        callback interfaces - why are they defined here but implemented in main
        https://stackoverflow.com/questions/18279302/how-do-i-perform-a-java-callback-between-classes
        TODO - test implementing in another class as well as mainactivity and see if both implementors get triggerd
     */
    interface SendChosenFile {
        fun sendFilePath(fPath: String?)
    }

    interface RemoveChosenFile {
        fun removeFile(fPath: String?)
    }
}