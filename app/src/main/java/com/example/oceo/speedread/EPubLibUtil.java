package com.example.oceo.speedread;

import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Guide;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.Resources;
import nl.siegmann.epublib.domain.Spine;
import nl.siegmann.epublib.domain.SpineReference;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.domain.TableOfContents;
import nl.siegmann.epublib.epub.EpubReader;

public class EPubLibUtil {
    private String testBook1Path = "storage/emulated/0/Books/MoonReader/Brandon Sanderson - Oathbringer_ Book Three of the Stormlight Archive-Tor Books (2017).epub";


    public static Book getBook(String fName) {
        File file = new File(fName);
        Book book = null;

        try {
            InputStream epubInputStream = new FileInputStream(file.toString());
            Log.d("what is it", file.toString());
            if (file.toString().contains(".epub")) {
                book = (new EpubReader()).readEpub(epubInputStream);
            } else {
                Log.d("GetBook", "Not an Epub");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return book;
    }


    public static void getContents(Book book) {
        List<Resource> contents = book.getContents();
    }

    public static void exploreResources(Book book) {
        //TODO
        Log.d("explorerResources", "start");
        Resources resource = book.getResources();
        Log.d("explorerResources", "done");

    }


    public static List<TOCReference> exploreTOC(Book book) {
        Log.d("explorerTOC", "start");
        TableOfContents toc = book.getTableOfContents();
//        List<Resource> uniqueRes = toc.getAllUniqueResources();
        List<TOCReference> tocReferences = toc.getTocReferences();
        Log.d("explorerTOC", "done");
        return tocReferences;
    }


    public static void exploreGuide(Book book) {
        Log.d("exploreGuide", "start");
        Guide guide = book.getGuide();
        Log.d("exploreGuide", "done");
    }


    public static void exploreSpine(Book book) {
        Log.d("exploreSpine", "start");
        Spine spine = book.getSpine();
        Log.d("exploreSpine", "done");
    }

    public static void unZipEPUB() {
        //TODO
//        FileUtils.unzipFile(mFilePath, Constant.PATH_EPUB + "/" + mFileName);
    }


    public static int mapTOCToSpine(Book book, String tocID) {
        // gets the index in the spine to be read
        Log.d("mapTOCToSpine", "start");
        List<SpineReference> spineRefs = book.getSpine().getSpineReferences();
        int i;
        for (i = 0; i < spineRefs.size(); i++) {
            if (spineRefs.get(i).getResource().getId().equals(tocID)) {
                break;
            }
        }
        Log.d("mapTOCToSpine", "done");
        return i;
    }


    /*

     */
    public static ArrayList<String> getTOCResourceIds(List<TOCReference> tocReferences, int depth, ArrayList<String> toc) {
        // TODO examine
        if (tocReferences == null) {
            return toc;
        }
        for (TOCReference tocReference : tocReferences) {
            StringBuilder tocString = new StringBuilder();
            for (int i = 0; i < depth; i++) {
                tocString.append("\t");
            }
            tocString.append(tocReference.getTitle());
            Log.i("epublib", tocString.toString());
//            toc.add(tocString.toString());
            toc.add(tocReference.getResource().getId()); // resource Id
            getTOCResourceIds(tocReference.getChildren(), depth + 1, toc);
        }
        return toc;
    }

    public static ArrayList<String> getTOCTitles(List<TOCReference> tocReferences, int depth, ArrayList<String> toc) {
        // TODO examine
        if (tocReferences == null) {
            return toc;
        }
        for (TOCReference tocReference : tocReferences) {
            StringBuilder tocString = new StringBuilder();
            for (int i = 0; i < depth; i++) {
                tocString.append("\t");
            }
            tocString.append(tocReference.getTitle());
            toc.add(tocString.toString());
            getTOCTitles(tocReference.getChildren(), depth + 1, toc);
        }
        return toc;
    }

    private String getChapterTitleFromToc(int chapter, Book mBook) {
        // Is there no easier way to connect a TOCReference
        // to an absolute spine index?
        String title = "";

        List<Resource> targetResources = mBook.getTableOfContents().getAllUniqueResources();
        Resource targetResource = targetResources.get(chapter);

        ArrayList<TOCReference> references = (ArrayList<TOCReference>) mBook.getTableOfContents().getTocReferences();

        for (TOCReference ref : references) {
            if (ref.getResource().equals(targetResource)) {
                return ref.getTitle();
            }
            for (TOCReference childRef : ref.getChildren()) {
                if (childRef.getResource().equals(targetResource)) {
                    return childRef.getTitle();
                }
            }
        }
        return title;
    }

}
