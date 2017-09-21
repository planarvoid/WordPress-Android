package com.soundcloud.android.search.history

data class SearchHistoryItem(val searchTerm: String, val timestamp: Long = System.currentTimeMillis())
