package com.speedpubread.oceo.speedread

fun displayTOC() {
    // original ToC function. Would attempt to parse the ToC from the epub and map ths section to it
//    val tocRefs = EPubLibUtil.exploreTOC(book!!)
//    var TOCTitles = ArrayList<String>()
//    TOCTitles = EPubLibUtil.getTOCTitles(tocRefs, 0, TOCTitles)
//    dropdown = rootView!!.findViewById(R.id.spinner1)
//    val adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, TOCTitles)
//    dropdown!!.setAdapter(adapter)
//
//    val spineRefs = book!!.spine.spineReferences
//    var currentSpineID: String
//    if (currentChapter < spineRefs.size && currentChapter > 0) {
//        currentSpineID = spineRefs[currentChapter].resourceId
//    } else {
//        currentChapter = 0
//        currentSpineID = spineRefs[0].resourceId
//    }
//    val currentToCIdx = EPubLibUtil.mapSpineToTOC(currentSpineID, tocResourceIds!!) // find out if current chapter is in TOC
//
//    // if not then dont set the ToC there
//    if (currentToCIdx != -1) {
//        dropdown!!.setSelection(currentToCIdx)
//    }
//
//    dropdown!!.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
//        override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
//            val selectedItem = tocResourceIds!![position]
//            if (++firstTimeFlag > 1) { // do not update on launch
//                currentChapter = EPubLibUtil.mapTOCToSpine(book!!, selectedItem)
//                disposeListener()
//                bookDetails!![CHAPTER_KEY] = currentChapter.toString()
//                PrefsUtil.writeBookDetailsToPrefs(activity!!, chosenFileName!!, bookDetails)
//                val currSection = (currentChapter + 1).toString()
//
//                currentChapterview!!.text = "Section: ${currSection}"
//
//                resetChapterGlobals()
//                setStoryTokens()
//                iterateWords()
//            }
//        }
//        override fun onNothingSelected(parent: AdapterView<*>?) {}
//    })
}