package com.example.oceo.speedread;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import nl.siegmann.epublib.domain.MediaType;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.Resources;
import nl.siegmann.epublib.domain.Spine;
import nl.siegmann.epublib.domain.SpineReference;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.domain.TableOfContents;
import nl.siegmann.epublib.epub.EpubReader;
import nl.siegmann.epublib.service.MediatypeService;

public class EPubLibUtil {
    private String testBook1Path = "storage/emulated/0/Books/MoonReader/Brandon Sanderson - Oathbringer_ Book Three of the Stormlight Archive-Tor Books (2017).epub";
    static MediaType[] bitmapTypes = {MediatypeService.PNG, MediatypeService.GIF, MediatypeService.JPG};


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

    /*
       trying to display images
    */
    public static Bitmap getBitmapFromResources(List<Resource> resourcez, String imgHref, Book book) {
        List<Resource> resources = book.getResources().getResourcesByMediaTypes(bitmapTypes);
        byte[] data = "holder".getBytes();
        for (int ii = 0; ii < resources.size(); ii++) {
            String z = resources.get(ii).getHref();
            Log.d("given: ", imgHref);
            Log.d("the href: ", z);
            Log.d("contains", String.valueOf(imgHref.contains(z)));
            Log.d("contains", String.valueOf(z.contains(imgHref)));
            // it isnt clear which one will be larger
            // oathbringer chpt 4 is an example of the first condition
            // hyperian prologue is an example of seocnd
            if (imgHref.contains(z) || z.contains(imgHref)) {
//                Log.i("livi", z);
                try {
                    data = resources.get(ii).getData();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                break;
            }
        }
        //
        Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);
        return bm;
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

    public static int mapSpineToTOC(String spineID, ArrayList<String> tocResList) {
        int i = 0;
        while (i < tocResList.size()) {
            if (spineID.equals(tocResList.get(i))) {
                return i;
            }
            i++;
        }
        return -1;
    }


    public static int mapTOCToSpine(Book book, String tocID) {
        List<SpineReference> spineRefs = book.getSpine().getSpineReferences();
        int i = 0;
        while (i < spineRefs.size()) {
            String spineID = spineRefs.get(i).getResource().getId();
            if (spineID.equals(tocID)) {
                return i;
            }
            i++;
        }
        return -1;
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
//            Log.i("epublib", tocString.toString());
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
