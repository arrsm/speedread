package com.example.oceo.speedread

import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.oceo.speedread.BookListAdapter.MyViewHolder
import com.example.oceo.speedread.BookSelectionFragment.RemoveChosenFile
import com.example.oceo.speedread.BookSelectionFragment.SendChosenFile
import java.util.*

class BookListAdapter(private val mDataset: ArrayList<String>, private val bookList: List<String?>, activity: Activity) : RecyclerView.Adapter<MyViewHolder>() {
    private var selectionCallback: SendChosenFile
    private var bookRemovalCallback: RemoveChosenFile

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class MyViewHolder(v: View) : ViewHolder(v) {
        // each data item is just a string in this case
        var textView: TextView
        var deleteButton: Button

        init {
            textView = v.findViewById(R.id.book_title)
            deleteButton = v.findViewById(R.id.delete_btn)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.book_selection_item, parent, false)
        return MyViewHolder(v)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.textView.text = mDataset[position].replace("asset__", "")
        holder.textView.setOnClickListener {
            val value = mDataset[position]
            Log.d("ADAPTER", "onItemSelected: " + mDataset[position])
            selectionCallback.sendFilePath(bookList[position])
        }
        holder.deleteButton.setOnClickListener {
            val value = mDataset[position]
            Log.d("ADAPTER", "deleting: " + mDataset[position])
            bookRemovalCallback.removeFile(bookList[position])
        }
    }

    override fun getItemCount(): Int {
        return mDataset.size
    }

    init {
        selectionCallback = activity as MainActivity
        bookRemovalCallback = activity
    }
}