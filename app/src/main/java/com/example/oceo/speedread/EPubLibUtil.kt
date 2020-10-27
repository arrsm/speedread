package com.example.oceo.speedread

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.Resource
import nl.siegmann.epublib.domain.TOCReference
import nl.siegmann.epublib.epub.EpubReader
import nl.siegmann.epublib.service.MediatypeService
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.*

class EPubLibUtil {
    private val TAG = "EPubLibUtil"
    private val testBook1Path = "storage/emulated/0/Books/MoonReader/Brandon Sanderson - Oathbringer_ Book Three of the Stormlight Archive-Tor Books (2017).epub"

    private fun getChapterTitleFromToc(chapter: Int, mBook: Book): String {
        // Is there no easier way to connect a TOCReference
        // to an absolute spine index?
        val title = ""
        val targetResources = mBook.tableOfContents.allUniqueResources
        val targetResource = targetResources[chapter]
        val references = mBook.tableOfContents.tocReferences as ArrayList<TOCReference>
        for (ref in references) {
            if (ref.resource == targetResource) {
                return ref.title
            }
            for (childRef in ref.children) {
                if (childRef.resource == targetResource) {
                    return childRef.title
                }
            }
        }
        return title
    }

    companion object {
        private val TAG = "EPubLibUtil"
        var bitmapTypes = arrayOf(MediatypeService.PNG, MediatypeService.GIF, MediatypeService.JPG)

        @JvmStatic
        fun getBook(fName: String?, context: Context): Book? {
            var file: File? = null

            if (!fName!!.contains("asset__")) {
                file = File(fName)
            }

            var book: Book? = null
            try {
                val epubInputStream: InputStream
                if (fName.contains("asset__")) {
                    Log.d(TAG, "making in stream from asset")
                    val fileDescriptor = context.assets.openFd(fName.replace("asset__", ""))
                    epubInputStream = fileDescriptor.createInputStream()
                } else {
                    epubInputStream = FileInputStream(file.toString())
                }

                if (fName.contains(".epub")) {
                    book = EpubReader().readEpub(epubInputStream)
                } else {
                    Log.d("GetBook", "Not an Epub");
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return book
        }

        @JvmStatic
        fun getBitmapFromResources(resourcez: List<Resource?>?, imgHref: String, book: Book): Bitmap {
            val resources = book.resources.getResourcesByMediaTypes(bitmapTypes)
            var data = "holder".toByteArray()
            for (ii in resources.indices) {
                val z = resources[ii].href
                // it isnt clear which one will be larger
                // oathbringer chpt 4 is an example of the first condition
                // hyperian prologue is an example of seocnd
                if (imgHref.contains(z) || z.contains(imgHref)) {
//                Log.i("livi", z);
                    try {
                        data = resources[ii].data
                    } catch (e: IOException) {
                        // TODO Auto-generated catch block
                        e.printStackTrace()
                    }
                    break
                }
            }
            //
            return BitmapFactory.decodeByteArray(data, 0, data.size)
        }

        fun getContents(book: Book) {
            val contents = book.contents
        }

        fun exploreResources(book: Book) {
            //TODO
//        Log.d("explorerResources", "start");
            val resource = book.resources
            //        Log.d("explorerResources", "done");
        }

        @JvmStatic
        fun exploreTOC(book: Book): List<TOCReference> {
//        Log.d("explorerTOC", "start");
            val toc = book.tableOfContents
            //        List<Resource> uniqueRes = toc.getAllUniqueResources();
            //        Log.d("explorerTOC", "done");
            return toc.tocReferences
        }

        fun exploreGuide(book: Book) {
//        Log.d("exploreGuide", "start");
            val guide = book.guide
            //        Log.d("exploreGuide", "done");
        }

        fun exploreSpine(book: Book) {
//        Log.d("exploreSpine", "start");
            val spine = book.spine
            //        Log.d("exploreSpine", "done");
        }

        fun unZipEPUB() {
            //TODO
//        FileUtils.unzipFile(mFilePath, Constant.PATH_EPUB + "/" + mFileName);
        }

        @JvmStatic
        fun mapSpineToTOC(spineID: String, tocResList: ArrayList<String>): Int {
            var i = 0
            while (i < tocResList.size) {
                if (spineID == tocResList[i]) {
                    return i
                }
                i++
            }
            return -1
        }

        @JvmStatic
        fun mapTOCToSpine(book: Book, tocID: String): Int {
            val spineRefs = book.spine.spineReferences
            var i = 0
            while (i < spineRefs.size) {
                val spineID = spineRefs[i].resource.id
                if (spineID == tocID) {
                    return i
                }
                i++
            }
            return -1
        }

        @JvmStatic
        fun getTOCResourceIds(tocReferences: List<TOCReference>?, depth: Int, toc: ArrayList<String>): ArrayList<String> {
            if (tocReferences == null) {
                return toc
            }
            for (tocReference in tocReferences) {
                val tocString = StringBuilder()
                for (i in 0 until depth) {
                    tocString.append("\t")
                }
                tocString.append(tocReference.title)
                toc.add(tocReference.resource.id) // resource Id
                getTOCResourceIds(tocReference.children, depth + 1, toc)
            }
            return toc
        }

        @JvmStatic
        fun getTOCTitles(tocReferences: List<TOCReference>?, depth: Int, toc: ArrayList<String>): ArrayList<String> {
            if (tocReferences == null) {
                return toc
            }
            for (tocReference in tocReferences) {
                val tocString = StringBuilder()
                for (i in 0 until depth) {
                    tocString.append("\t")
                }
                tocString.append(tocReference.title)
                toc.add(tocString.toString())
                getTOCTitles(tocReference.children, depth + 1, toc)
            }
            return toc
        }
    }
}