package com.soundcloud.android.search.history

import com.soundcloud.android.search.history.SearchHistoryModel.InsertRow
import com.soundcloud.android.search.history.SearchHistoryModel.TruncateTable
import com.squareup.sqldelight.RowMapper
import io.reactivex.Observable

import javax.inject.Inject

class SearchHistoryStorage
@Inject
internal constructor(private val searchHistoryDatabase: SearchHistoryDatabase) {

    fun clear() {
        searchHistoryDatabase.clear(SearchHistoryDbModel.TABLE_NAME)
    }

    fun truncate(limit: Int) {
        val truncateTable = TruncateTable(searchHistoryDatabase.writableDatabase())
        truncateTable.bind(limit.toLong())
        searchHistoryDatabase.updateOrDelete(SearchHistoryDbModel.TABLE_NAME, truncateTable.program)
    }

    fun addSearchHistoryItem(searchHistoryItem: SearchHistoryItem) {
        val insertRow = InsertRow(searchHistoryDatabase.writableDatabase())
        insertRow.bind(searchHistoryItem.searchTerm, searchHistoryItem.timestamp)
        searchHistoryDatabase.insert(SearchHistoryDbModel.TABLE_NAME, insertRow.program)
    }

    fun getSearchHistory(): Observable<List<SearchHistoryItem>> {
        return searchHistoryDatabase.executeObservableQuery<SearchHistoryItem>(
                RowMapper { SearchHistoryItem(it.getString(0), it.getLong(1)) },
                SearchHistoryDbModel.TABLE_NAME,
                SearchHistoryDbModel.FACTORY.selectAll().statement)
    }

}
