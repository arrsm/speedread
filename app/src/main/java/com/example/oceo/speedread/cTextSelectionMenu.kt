package com.example.oceo.speedread

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.style.CharacterStyle
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView

class cTextSelectionMenu(var selectedView: TextView) : ActionMode.Callback {
    private val TAG = "text selection callback"
    private var bookName: String? = null
    private var currentSection = 0
    private var sentenceStart = 0
    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
//        Log.d(TAG, "onCreateActionMode")
        val inflater = mode.menuInflater
        inflater.inflate(R.menu.text_selection_menu, menu)
        menu.removeItem(android.R.id.selectAll)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        return false
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
//        Log.d(TAG, String.format("onActionItemClicked item=%s/%d", item.toString(), item.itemId))
        val cs: CharacterStyle
        val start = selectedView.selectionStart
        val end = selectedView.selectionEnd
        val ssb = SpannableStringBuilder(selectedView.text)
        when (item.itemId) {
            R.id.bold -> {
//                Log.d(TAG, "hanlding the bold case")
                cs = StyleSpan(Typeface.BOLD)
                ssb.setSpan(cs, start, end, 1)
                selectedView.text = ssb
                return true
            }
            R.id.italic -> {
                cs = StyleSpan(Typeface.ITALIC)
                ssb.setSpan(cs, start, end, 1)
                selectedView.text = ssb
                return true
            }
            R.id.underline -> {
                cs = UnderlineSpan()
                ssb.setSpan(cs, start, end, 1)
                selectedView.text = ssb
                return true
            }
            R.id.save -> {
                // TODO need a way to get the book, currentSection. currentSentenceStart,
//                Log.d(TAG, "saving selection")
                val startIdx = selectedView.selectionStart
                val endIdx = selectedView.selectionEnd
                return true
            }
        }
        return false
    }

    override fun onDestroyActionMode(mode: ActionMode) {}
    fun setMetadata(bookName: String?, currentSection: Int, sentenceStart: Int) {
        this.bookName = bookName
        this.currentSection = currentSection
        this.sentenceStart = sentenceStart
    }

}