package com.soundcloud.android.search.history

import com.soundcloud.android.testsupport.AndroidUnitTest
import io.reactivex.schedulers.Schedulers
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Collections

class SearchHistoryStorageTest : AndroidUnitTest() {
    private lateinit var storage: SearchHistoryStorage

    @Before
    @Throws(Exception::class)
    fun `set up`() {
        val databaseOpenHelper = SearchHistoryDatabaseOpenHelper(AndroidUnitTest.context())
        storage = SearchHistoryStorage(SearchHistoryDatabase(databaseOpenHelper, Schedulers.trampoline()))
    }

    @Test
    @Throws(Exception::class)
    fun `gets empty list`() {
        assertEquals(Collections.EMPTY_LIST, storage.getSearchHistory().blockingFirst())
    }

    @Test
    @Throws(Exception::class)
    fun `can add single item`() {
        val searchHistoryItem = SearchHistoryItem("one", 1)
        storage.addSearchHistoryItem(searchHistoryItem)
        assertEquals(listOf(searchHistoryItem), storage.getSearchHistory().blockingFirst())
    }

    @Test
    @Throws(Exception::class)
    fun `can add multiple items`() {
        storage.addSearchHistoryItem(SearchHistoryItem("one", 1))
        storage.addSearchHistoryItem(SearchHistoryItem("two", 2))
        storage.addSearchHistoryItem(SearchHistoryItem("three", 3))
        storage.addSearchHistoryItem(SearchHistoryItem("four", 4))
        storage.addSearchHistoryItem(SearchHistoryItem("five", 5))

        assertEquals(5, storage.getSearchHistory().blockingFirst().size)
    }

    @Test
    @Throws(Exception::class)
    fun `returns items in timestamp desc`() {
        storage.addSearchHistoryItem(SearchHistoryItem("one", 1))
        storage.addSearchHistoryItem(SearchHistoryItem("two", 2))
        storage.addSearchHistoryItem(SearchHistoryItem("three", 3))
        storage.addSearchHistoryItem(SearchHistoryItem("four", 4))
        storage.addSearchHistoryItem(SearchHistoryItem("five", 5))

        val searchHistory = storage.getSearchHistory().blockingFirst()
        assertEquals("five", searchHistory.get(0).searchTerm)
        assertEquals("four", searchHistory.get(1).searchTerm)
        assertEquals("three", searchHistory.get(2).searchTerm)
        assertEquals("two", searchHistory.get(3).searchTerm)
        assertEquals("one", searchHistory.get(4).searchTerm)
    }

    @Test
    @Throws(Exception::class)
    fun `updates row with new timestamp`() {
        storage.addSearchHistoryItem(SearchHistoryItem("one", 100))
        storage.addSearchHistoryItem(SearchHistoryItem("one", 200))

        assertEquals(1, storage.getSearchHistory().blockingFirst().size)
        assertEquals(200, storage.getSearchHistory().blockingFirst().get(0).timestamp)
    }

    @Test
    @Throws(Exception::class)
    fun `uses search term as key`() {
        storage.addSearchHistoryItem(SearchHistoryItem("one", 1))
        storage.addSearchHistoryItem(SearchHistoryItem("two", 1))
        storage.addSearchHistoryItem(SearchHistoryItem("three", 1))

        assertEquals(3, storage.getSearchHistory().blockingFirst().size)
    }

    @Test
    @Throws(Exception::class)
    fun `truncates table when greater than limit`() {
        storage.addSearchHistoryItem(SearchHistoryItem("one", 100))
        storage.addSearchHistoryItem(SearchHistoryItem("five", 500))
        storage.addSearchHistoryItem(SearchHistoryItem("two", 200))
        storage.addSearchHistoryItem(SearchHistoryItem("four", 400))
        storage.addSearchHistoryItem(SearchHistoryItem("three", 300))

        storage.truncate(2)

        val searchHistory = storage.getSearchHistory().blockingFirst()
        assertEquals(2, searchHistory.size)
        assertEquals("five", searchHistory.get(0).searchTerm)
        assertEquals("four", searchHistory.get(1).searchTerm)
    }

    @Test
    @Throws(Exception::class)
    fun `clears table`() {
        storage.addSearchHistoryItem(SearchHistoryItem("one", 100))
        storage.addSearchHistoryItem(SearchHistoryItem("five", 500))
        storage.addSearchHistoryItem(SearchHistoryItem("two", 200))

        assertEquals(3, storage.getSearchHistory().blockingFirst().size)

        storage.clear()

        assertEquals(0, storage.getSearchHistory().blockingFirst().size)
    }

}
