package com.example.oceo.speedread;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.recyclerview.widget.RecyclerView;

public class BookListAdapter extends RecyclerView.Adapter<BookListAdapter.MyViewHolder> {
    private ArrayList<String> mDataset;
    private ArrayList<String> bookList;
    BookSelectionFragment.SendChosenFile selectionCallback;


    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView textView;

        public MyViewHolder(View v) {
            super(v);
            textView = v.findViewById(R.id.book_title);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public BookListAdapter(ArrayList<String> myDataset, ArrayList<String> bookList, Activity activity) {
        mDataset = myDataset;
        this.bookList = bookList;
        selectionCallback = (MainActivity) activity;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public BookListAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                           int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.book_selection_item, parent, false);
        MyViewHolder vh = new MyViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.textView.setText(mDataset.get(position));
        holder.textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = (String) mDataset.get(position);
                Log.d("ADAPTER", "onItemSelected: " + mDataset.get(position));
                selectionCallback.sendFilePath(bookList.get(position));
                // assuming string and if you want to get the value on click of list item
                // do what you intend to do on click of listview row
            }
        });

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}

