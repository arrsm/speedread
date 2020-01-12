package com.example.oceo.speedread;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.style.CharacterStyle;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;


class cTextSelectionMenu implements ActionMode.Callback {
    TextView selectedView;
    private String TAG = "text selection callback";
    private String bookName;
    private int currentSection;
    private int sentenceStart;

    public cTextSelectionMenu(TextView selectedView) {
        this.selectedView = selectedView;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        Log.d(TAG, "onCreateActionMode");
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.text_selection_menu, menu);
        menu.removeItem(android.R.id.selectAll);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        Log.d(TAG, String.format("onActionItemClicked item=%s/%d", item.toString(), item.getItemId()));
        CharacterStyle cs;
        int start = selectedView.getSelectionStart();
        int end = selectedView.getSelectionEnd();
        SpannableStringBuilder ssb = new SpannableStringBuilder(selectedView.getText());

        switch (item.getItemId()) {

            case R.id.bold:
                Log.d(TAG, "hanlding the bold case");
                cs = new StyleSpan(Typeface.BOLD);
                ssb.setSpan(cs, start, end, 1);
                selectedView.setText(ssb);
                return true;

            case R.id.italic:
                cs = new StyleSpan(Typeface.ITALIC);
                ssb.setSpan(cs, start, end, 1);
                selectedView.setText(ssb);
                return true;

            case R.id.underline:
                cs = new UnderlineSpan();
                ssb.setSpan(cs, start, end, 1);
                selectedView.setText(ssb);
                return true;

            case R.id.save:
                // TODO need a way to get the book, currentSection. currentSentenceStart,
                Log.d(TAG, "saving selection");
                int startIdx = selectedView.getSelectionStart();
                int endIdx = selectedView.getSelectionEnd();
                return true;


        }
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
    }

    public void setMetadata(String bookName, int currentSection, int sentenceStart) {
        this.bookName = bookName;
        this.currentSection = currentSection;
        this.sentenceStart = sentenceStart;
    }
}

