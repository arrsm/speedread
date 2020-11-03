package com.speedpubread.oceo.speedread

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
import com.speedpubread.oceo.speedread.FileSelector.Companion.launchFileChooser
import com.speedpubread.oceo.speedread.FileSelector.Companion.requestReadPermission
import com.speedpubread.oceo.speedread.SpeedReadUtilities.Companion.bookNamesFromPath
import com.speedpubread.oceo.speedread.SpeedReadUtilities.Companion.modifyFilePath

class BookSelectionFragment : Fragment() {
    private val TAG = "BookSelectionFragment"
    var activity: Activity? = null
    var frag: Fragment? = null
    var rootView: View? = null
    var bookListView: ListView? = null
    private var bookList: List<String?> = ArrayList()
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
        bookList = PrefsUtil.readBooksFromPrefs(activity!!)
        val deletedBooks = PrefsUtil.readBookDeleted(activity!!) ?: HashMap()
        bookList = (bookList + getDefaultEpubFiles())
        bookList = bookList.distinct().toList()
        // this is a stopgap from prefs to be able to delete the default books. may need a way to
        // return them
        bookList = bookList.filter { deletedBooks[it] == false || deletedBooks[it] == null }

        displayList = bookNamesFromPath(bookList)
        selectionCallback = activity as MainActivity?
        removalCallback = activity as MainActivity?
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.book_selection, container, false)
        recyclerView = rootView!!.findViewById<View>(R.id.book_recycle_view) as RecyclerView
        layoutManager = LinearLayoutManager(activity)
        recyclerView!!.layoutManager = layoutManager
        mAdapter = BookListAdapter(displayList!!, bookList, activity!!)
        recyclerView!!.adapter = mAdapter
        setUpFileChoice() // TODO come up with a better name
        return rootView
    }

    /* open files */
    fun setUpFileChoice() {
        fileChooseButton = rootView!!.findViewById(R.id.choose_file_button)
        fileChooseButton!!.setOnClickListener(View.OnClickListener {
            if (activity!!.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                launchFileChooser(frag)
            } else {
//                Log.d("Open file", "No File Permissions")
                requestReadPermission(activity)
            }
        })
    }

    override fun onResume() {
        super.onResume()
    }


    /*
    result of selecting a file from OP6 file explorer
        would have liked this to be in the FileSelector class but seems the result should be here
        otherwise no access to the filepath var as it can not be returned. Im sure there's a better
        way tod o this
    */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
//        Log.d("result", requestCode.toString())
        when (requestCode) {
            1 -> if (resultCode == -1) {
                val fileUri = data.data
                Log.d("the uri", fileUri.toString())
                filePath = fileUri?.path
            }
        }
        Log.d("what is the file path", filePath)
//       /document/raw:/storage/emulated/0/Download/Dune - Frank Herbert.epub
        filePath = modifyFilePath(filePath!!)
        Log.d("what is the modified file path", filePath)
//        /storage/emulated/0//storage/emulated/0/Download/Dune - Frank Herbert.epub
        // and on op6
//        D/what is the file path: /document/primary:Books/MoonReader/J.R.R. Tolkien - Complete eBook Collection [EN EPUB] [ebook] [p_s]/The Legend of Sigurd and Gudrun/The Legend of Sigurd and Gudrun - J. R. R. Tolkien.epub
//        2020-11-03 10:01:41.140 8853-8853/com.speedpubread.oceo.speedread D/what is the modified file path: /Books/MoonReader/J.R.R. Tolkien - Complete eBook Collection [EN EPUB] [ebook] [p_s]/The Legend of Sigurd and Gudrun/The Legend of Sigurd and Gudrun - J. R. R. Tolkien.epub
//        2020-11-03 10:01:41.142 8853-8853/com.speedpubread.oceo.speedread W/System.err: java.io.FileNotFoundException: /Books/MoonReader/J.R.R. Tolkien - Complete eBook Collection [EN EPUB] [ebook] [p_s]/The Legend of Sigurd and Gudrun/The Legend of Sigurd and Gudrun - J. R. R. Tolkien.epub: open failed: ENOENT (No such file or directory)

        selectionCallback!!.sendFilePath(filePath)
    }


    fun getDefaultEpubFiles(): ArrayList<String?> {
        val assets = context?.assets?.list("")
        return assets?.map {
            "asset__" + it.toString()
        }!!.filter { it.contains(".epub") } as ArrayList<String?>
    }

    interface SendChosenFile {
        fun sendFilePath(fPath: String?)
    }

    interface RemoveChosenFile {
        fun removeFile(fPath: String?)
    }


}