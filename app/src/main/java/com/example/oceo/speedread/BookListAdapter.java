package com.example.oceo.speedread;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.recyclerview.widget.RecyclerView;

public class BookListAdapter extends RecyclerView.Adapter<BookListAdapter.MyViewHolder> {
    private ArrayList<String> mDataset;
    private ArrayList<String> bookList;
    BookSelectionFragment.SendChosenFile selectionCallback;
    BookSelectionFragment.RemoveChosenFile bookRemovalCallback;


    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView textView;
        public Button deleteButton;

        public MyViewHolder(View v) {
            super(v);
            textView = v.findViewById(R.id.book_title);
            deleteButton = v.findViewById(R.id.delete_btn);
        }
    }

    public BookListAdapter(ArrayList<String> myDataset, ArrayList<String> bookList, Activity activity) {
        mDataset = myDataset;
        this.bookList = bookList;
        selectionCallback = (MainActivity) activity;
        bookRemovalCallback = (MainActivity) activity;
    }

    @Override
    public BookListAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.book_selection_item, parent, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.textView.setText(mDataset.get(position));
        holder.textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = (String) mDataset.get(position);
                Log.d("ADAPTER", "onItemSelected: " + mDataset.get(position));
                selectionCallback.sendFilePath(bookList.get(position));
            }
        });

        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = (String) mDataset.get(position);
                Log.d("ADAPTER", "deleting: " + mDataset.get(position));
                bookRemovalCallback.removeFile(bookList.get(position));
            }
        });

    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}

