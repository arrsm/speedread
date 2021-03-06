package com.speedpubread.oceo.speedread.parser

import android.graphics.Bitmap
import android.text.Html
import android.text.Spanned
import android.util.Log
import com.speedpubread.oceo.speedread.EPubLibUtil
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.Resource
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.ArrayList

fun parseBook(book: Book): List<String?> {
    // parse entire book
    return (0..book.spine.size() - 1).map { parseChapter(book, it) }
}

fun parseChapter(book: Book, spineLocation: Int): String? {
    if (spineLocation > book.spine.size()) {
        return null
    }
    val string = StringBuilder()
    val inStream: InputStream
    val reader: BufferedReader
    var line: String?
    val res: Resource = book.spine.getResource(spineLocation)
    try {
        inStream = res.inputStream
        reader = BufferedReader(InputStreamReader(inStream))
        while (reader.readLine().also { line = it } != null) {
            val span = lineParser(line!!, book)
            if (span != null) {
                string.append("$span")
            }
        }
    } catch (e: IOException) {
        Log.d("BookParse", "Error here")
        e.printStackTrace()
    }
    // TODO check if this is faster than the if statement to add a space after each insert
    return string.toString().replace(".", ". ")
}

private fun lineParser(line: String, book: Book): Spanned? {
    if (line.contains("<title>")) {
        return null
    }
    val formattedLine = formatLine(line, book)
    return Html.fromHtml(formattedLine)
}

private fun formatLine(line: String, book: Book): String {
    var line = line
    if (line.contains("http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd")) {
        line = line.substring(line.indexOf(">") + 1, line.length)
    }

    // REMOVE STYLES AND COMMENTS IN HTML
    if (line.contains("{") && line.contains("}")
            || line.contains("/*") && line.contains("*/")
            || line.contains("<!--") && line.contains("-->")) {
        line = line.substring(line.length)
    }
    if (line.contains("<img")) {
        var src = line.substring(line.indexOf("src=\"") + 5)
        src = src.substring(0, src.indexOf("\""))
//        Log.d("checking image files", line)
        val phList: List<Resource?> = ArrayList()
//        val bm = getBookImages(phList, src, book)
//        Log.d("more test", bm.toString())
//        val im = rootView!!.findViewById<ImageView>(R.id.image1)
//        im.setImageBitmap(bm)
    }
    return line
}

private fun getBookImages(res: List<Resource?>, imgHref: String, book: Book): Bitmap {
//        String tempHref = "images/Simm_9780307781888_epub_L03_r1.jpg";
//        tempHref = "OEBPS/images/Simm_9780307781888_epub_L03_r1.jpg";
//        tempHref = "images/OB_ARCH_ebook_004.gif.transcoded1535572045.png" // WORKS sanderson chap/t 4;
    return EPubLibUtil.getBitmapFromResources(res, imgHref, book)
}