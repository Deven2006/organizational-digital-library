package com.example.digitallibrary.ui.common

/**
 * Localized string provider supporting English and Marathi (मराठी)
 * with devotional cultural nuance.
 */
class LibraryStrings(private val lang: AppLanguage) {

    val appTitle: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "श्री ग्रंथालय"
            AppLanguage.ENGLISH -> "Organizational Digital Library"
        }

    val appSubtitle: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "पवित्र ग्रंथ व अधिकृत प्रकाशने"
            AppLanguage.ENGLISH -> "Sacred Texts & Publications"
        }

    val welcomeGreeting: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "।। श्री गुरुदेव दत्त ।। ज्ञानगंगेत आपले स्वागत आहे"
            AppLanguage.ENGLISH -> "Welcome to the Devotional Digital Library"
        }

    val continueReading: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "वाचन पुढे चालू ठेवा"
            AppLanguage.ENGLISH -> "Continue Reading"
        }

    val lastReadPage: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "शेवटचे वाचलेले पान"
            AppLanguage.ENGLISH -> "Last Read Page"
        }

    val resumeReading: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "पुन्हा वाचा"
            AppLanguage.ENGLISH -> "Resume"
        }

    val allBooks: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "सर्व ग्रंथ"
            AppLanguage.ENGLISH -> "All Books"
        }

    val tabAllBooks: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "सर्व ग्रंथ"
            AppLanguage.ENGLISH -> "All Books"
        }

    val seriesCollections: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "ग्रंथ मालिका"
            AppLanguage.ENGLISH -> "Series Collections"
        }

    val tabSeries: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "मालिका"
            AppLanguage.ENGLISH -> "Series"
        }

    val offlineDownloads: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "ऑफलाइन ग्रंथ"
            AppLanguage.ENGLISH -> "Offline Downloads"
        }

    val tabDownloads: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "डाउनलोड्स"
            AppLanguage.ENGLISH -> "Downloads"
        }

    val searchPlaceholder: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "ग्रंथाचे नाव किंवा लेखकानुसार शोधा..."
            AppLanguage.ENGLISH -> "Search by book title or author..."
        }

    val clearSearch: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "शोध साफ करा"
            AppLanguage.ENGLISH -> "Clear Search"
        }

    val noBooksFound: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "कोणतेही ग्रंथ सापडले नाहीत"
            AppLanguage.ENGLISH -> "No Books Found"
        }

    val noBooksFoundDesc: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "आपल्या शोधाशी जुळणारे कोणतेही ग्रंथ आढळले नाहीत."
            AppLanguage.ENGLISH -> "No published documents match your search criteria."
        }

    val emptyLibraryDesc: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "प्रशासकांद्वारे अद्याप कोणतेही ग्रंथ प्रकाशित केलेले नाहीत."
            AppLanguage.ENGLISH -> "No documents have been published by the administrator yet."
        }

    val loadingBooks: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "ग्रंथ सूची लोड होत आहे..."
            AppLanguage.ENGLISH -> "Loading library books..."
        }

    val unableToReach: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "ग्रंथालयाशी संपर्क होऊ शकला नाही"
            AppLanguage.ENGLISH -> "Unable to Reach Library"
        }

    val retry: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "पुन्हा प्रयत्न करा"
            AppLanguage.ENGLISH -> "Retry"
        }

    val goToDownloads: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "ऑफलाइन ग्रंथ पहा"
            AppLanguage.ENGLISH -> "Go to Downloads"
        }

    val readDocument: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "ग्रंथ वाचा"
            AppLanguage.ENGLISH -> "Read Document"
        }

    val readOffline: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "ऑफलाइन वाचा"
            AppLanguage.ENGLISH -> "Read Offline"
        }

    val downloadForOffline: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "ऑफलाइन वाचनासाठी डाउनलोड करा"
            AppLanguage.ENGLISH -> "Download for Offline Reading"
        }

    val downloadedOfflineReady: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "ऑफलाइन वाचनासाठी उपलब्ध"
            AppLanguage.ENGLISH -> "Downloaded for Offline Reading"
        }

    val downloading: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "डाउनलोड होत आहे..."
            AppLanguage.ENGLISH -> "Downloading..."
        }

    val documentDetails: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "ग्रंथाचा तपशील"
            AppLanguage.ENGLISH -> "Document Details"
        }

    val descriptionAndSummary: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "वर्णन आणि सारांश"
            AppLanguage.ENGLISH -> "Description & Summary"
        }

    val noDescription: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "या ग्रंथासाठी कोणतेही वर्णन दिलेले नाही."
            AppLanguage.ENGLISH -> "No summary or description provided for this publication."
        }

    val unknownAuthor: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "लेखक अज्ञात"
            AppLanguage.ENGLISH -> "Unknown Author"
        }

    val seriesVol: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "मालिका भाग"
            AppLanguage.ENGLISH -> "Series Vol."
        }

    val seriesDetails: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "मालिकेचा तपशील"
            AppLanguage.ENGLISH -> "Series Details"
        }

    val booksInSeries: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "मालिकेतील ग्रंथ"
            AppLanguage.ENGLISH -> "Books in Series"
        }

    val downloadEntireSeries: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "संपूर्ण मालिका डाउनलोड करा"
            AppLanguage.ENGLISH -> "Download Entire Series"
        }

    val downloadSelected: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "निवडलेले डाउनलोड करा"
            AppLanguage.ENGLISH -> "Download Selected"
        }

    val select: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "निवडा"
            AppLanguage.ENGLISH -> "Select"
        }

    val cancel: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "रद्द करा"
            AppLanguage.ENGLISH -> "Cancel"
        }

    val orderedSequence: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "अनुक्रम"
            AppLanguage.ENGLISH -> "Ordered Sequence"
        }

    val noSeriesPublished: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "कोणतीही ग्रंथ मालिका प्रकाशित नाही"
            AppLanguage.ENGLISH -> "No Series Published"
        }

    val noSeriesDesc: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "अद्याप कोणत्याही ग्रंथ मालिका प्रकाशित करण्यात आलेल्या नाहीत."
            AppLanguage.ENGLISH -> "No serialized book collections have been published yet."
        }

    val noDownloads: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "कोणतेही ऑफलाइन ग्रंथ नाहीत"
            AppLanguage.ENGLISH -> "No Downloaded Books"
        }

    val noDownloadsDesc: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "ऑनलाइन असताना ग्रंथ डाउनलोड करा, जेणेकरून इंटरनेटशिवाय कधीही वाचता येईल."
            AppLanguage.ENGLISH -> "Download books while online to read them anywhere, even without internet."
        }

    val booksAvailableOffline: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "ग्रंथ ऑफलाइन उपलब्ध आहेत"
            AppLanguage.ENGLISH -> "books available offline"
        }

    val removeDownloadTitle: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "ऑफलाइन ग्रंथ काढायचा का?"
            AppLanguage.ENGLISH -> "Remove Download?"
        }

    val removeDownloadMsg: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "हे पुस्तक ऑफलाइन साठ्यातून काढले जाईल. आपण ऑनलाइन असताना ते पुन्हा डाउनलोड करू शकता."
            AppLanguage.ENGLISH -> "This will remove the book from your offline storage. You can download it again whenever you are online."
        }

    val remove: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "काढून टाका"
            AppLanguage.ENGLISH -> "Remove"
        }

    val newVersionAvailable: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "सर्व्हरवर नवीन आवृत्ती उपलब्ध आहे"
            AppLanguage.ENGLISH -> "New version available on server"
        }

    val readingFromOffline: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "ऑफलाइन साठ्यातून वाचन चालू आहे"
            AppLanguage.ENGLISH -> "Reading from Offline Storage"
        }

    val profileAndSettings: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "माझे खाते व प्राधान्ये"
            AppLanguage.ENGLISH -> "Profile & Settings"
        }

    val accountDetails: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "खात्याचा तपशील"
            AppLanguage.ENGLISH -> "Account Details"
        }

    val displayLanguage: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "भाषा निवडा (Language)"
            AppLanguage.ENGLISH -> "Display Language"
        }

    val emailAddress: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "ईमेल पत्ता"
            AppLanguage.ENGLISH -> "Email Address"
        }

    val password: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "पासवर्ड"
            AppLanguage.ENGLISH -> "Password"
        }

    val accessRole: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "प्रवेश अधिकार"
            AppLanguage.ENGLISH -> "Access Authorization"
        }

    val roleAdmin: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "प्रशासक (पूर्ण अधिकार)"
            AppLanguage.ENGLISH -> "Administrator (Full Access)"
        }

    val roleMember: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "सदस्य (वाचक अधिकार)"
            AppLanguage.ENGLISH -> "Member (Reader Access)"
        }

    val logOut: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "बाहेर पडा (Log Out)"
            AppLanguage.ENGLISH -> "Log Out"
        }

    val signInPrompt: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "सदस्य व साधक प्रवेशद्वार"
            AppLanguage.ENGLISH -> "Member & Staff Access Portal"
        }

    val signInButton: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "ग्रंथालयात प्रवेश करा"
            AppLanguage.ENGLISH -> "Sign In to Library"
        }

    val signingIn: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "प्रवेश करत आहे..."
            AppLanguage.ENGLISH -> "Signing In..."
        }

    val securityNotice: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "संरक्षित संस्थात्मक डिजिटल ग्रंथालय. सर्व क्रिया सर्व्हर सुरक्षा धोरणांद्वारे प्रमाणित केल्या जातात."
            AppLanguage.ENGLISH -> "Protected organizational resource. All actions are authorized and logged via server security policies."
        }

    val pageCounter: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "पान"
            AppLanguage.ENGLISH -> "Page"
        }

    val ofTotal: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "पैकी"
            AppLanguage.ENGLISH -> "of"
        }

    val completed: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "पूर्ण झाले"
            AppLanguage.ENGLISH -> "Completed"
        }

    val readingProgress: String
        get() = when (lang) {
            AppLanguage.MARATHI -> "वाचन प्रगती"
            AppLanguage.ENGLISH -> "Reading Progress"
        }
}
