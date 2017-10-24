package com.soundcloud.android.model

import com.soundcloud.android.view.ViewError

data class AsyncLoadingState(val isLoadingNextPage: Boolean = false,
                             val isRefreshing: Boolean = false,
                             val nextPageError: ViewError? = null,
                             val refreshError: ViewError? = null,
                             val requestMoreOnScroll: Boolean = false) {

    fun toRefreshStarted() = copy(isRefreshing = true)

    fun toRefreshError(throwable: Throwable) = copy(isRefreshing = false, refreshError = ViewError.from(throwable))
}
