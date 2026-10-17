package com.example.digitallibrary.ui

import com.example.digitallibrary.data.model.Book
import com.example.digitallibrary.ui.common.AppLanguage
import com.example.digitallibrary.ui.common.LibraryStrings
import com.example.digitallibrary.ui.theme.DeepMaroon
import com.example.digitallibrary.ui.theme.MutedGold
import com.example.digitallibrary.ui.theme.ParchmentBackground
import com.example.digitallibrary.ui.theme.TerracottaPrimary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignSystemAndLanguageTest {

    @Test
    fun testBilingualStringsPresence() {
        val enStrings = LibraryStrings(AppLanguage.ENGLISH)
        val mrStrings = LibraryStrings(AppLanguage.MARATHI)

        // English assertions
        assertEquals("Organizational Digital Library", enStrings.appTitle)
        assertEquals("Continue Reading", enStrings.continueReading)
        assertEquals("All Books", enStrings.allBooks)
        assertEquals("Offline Downloads", enStrings.offlineDownloads)
        assertEquals("Read Document", enStrings.readDocument)
        assertEquals("Sign In to Library", enStrings.signInButton)

        // Marathi assertions
        assertEquals("श्री ग्रंथालय", mrStrings.appTitle)
        assertEquals("वाचन पुढे चालू ठेवा", mrStrings.continueReading)
        assertEquals("सर्व ग्रंथ", mrStrings.allBooks)
        assertEquals("ऑफलाइन ग्रंथ", mrStrings.offlineDownloads)
        assertEquals("ग्रंथ वाचा", mrStrings.readDocument)
        assertEquals("ग्रंथालयात प्रवेश करा", mrStrings.signInButton)

        // Verify Devanagari unicode range in Marathi strings
        assertTrue(mrStrings.appTitle.any { it in '\u0900'..'\u097F' })
        assertTrue(mrStrings.continueReading.any { it in '\u0900'..'\u097F' })
        assertTrue(mrStrings.readDocument.any { it in '\u0900'..'\u097F' })
    }

    @Test
    fun testColorTokensNonEquality() {
        // Ensure core devotional accents are distinct and defined
        assertNotEquals(TerracottaPrimary, DeepMaroon)
        assertNotEquals(TerracottaPrimary, MutedGold)
        assertNotEquals(ParchmentBackground, DeepMaroon)
    }

    @Test
    fun testBookTypeClassification() {
        val pdfBook = Book(id = "1", title = "Pothi", fileType = "pdf", storagePath = "p.pdf")
        val docxBook = Book(id = "2", title = "Charitra", fileType = "DOCX", storagePath = "c.docx")
        val txtBook = Book(id = "3", title = "Stotra", fileType = "Txt", storagePath = "s.txt")

        assertTrue(pdfBook.isPdf)
        assertFalse(pdfBook.isDocx)
        assertFalse(pdfBook.isTxt)

        assertTrue(docxBook.isDocx)
        assertFalse(docxBook.isPdf)

        assertTrue(txtBook.isTxt)
        assertFalse(txtBook.isDocx)
    }

    @Test
    fun testSearchFilteringLogic() {
        val books = listOf(
            Book(id = "1", title = "Navnath Bhaktisar", author = "Malojirao", fileType = "pdf", storagePath = "1.pdf"),
            Book(id = "2", title = "Guru Charitra", author = "Saraswati Gangadhar", fileType = "docx", storagePath = "2.docx"),
            Book(id = "3", title = "Datta Prabodh", author = "Ramchandra", fileType = "txt", storagePath = "3.txt")
        )

        val queryTitle = "navnath"
        val filteredByTitle = books.filter {
            it.title.lowercase().contains(queryTitle) || it.author?.lowercase()?.contains(queryTitle) == true
        }
        assertEquals(1, filteredByTitle.size)
        assertEquals("Navnath Bhaktisar", filteredByTitle.first().title)

        val queryAuthor = "gangadhar"
        val filteredByAuthor = books.filter {
            it.title.lowercase().contains(queryAuthor) || it.author?.lowercase()?.contains(queryAuthor) == true
        }
        assertEquals(1, filteredByAuthor.size)
        assertEquals("Guru Charitra", filteredByAuthor.first().title)
    }
}
