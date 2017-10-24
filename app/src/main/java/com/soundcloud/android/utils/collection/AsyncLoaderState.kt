package com.soundcloud.android.utils.collection

import com.soundcloud.android.model.AsyncLoadingState
import com.soundcloud.android.view.ViewError
import com.soundcloud.java.optional.Optional

data class AsyncLoaderState<ItemType>(val asyncLoadingState: AsyncLoadingState = AsyncLoadingState(),
                                      val data: ItemType? = null) {

    internal fun updateWithRefreshState(isRefreshing: Boolean, refreshError: Optional<Throwable>): AsyncLoaderState<ItemType> {
        return if (isRefreshing) {
            if (asyncLoadingState.isRefreshing) {
                this
            } else {
                copy(asyncLoadingState = asyncLoadingState.toRefreshStarted())
            }
        } else if (refreshError.isPresent) {
            copy(asyncLoadingState = asyncLoadingState.toRefreshError(refreshError.get()))
        } else {
            this
        }
    }

    fun update(updateFunction: (ItemType) -> ItemType): AsyncLoaderState<ItemType> = copy(data = data?.let { updateFunction(it) })

    fun toNextPageError(throwable: Throwable): AsyncLoaderState<ItemType> {
        val loadingState = asyncLoadingState.copy(isLoadingNextPage = false, nextPageError = ViewError.from(throwable))
        return copy(asyncLoadingState = loadingState)
    }

    companion object {

        fun <ItemType> loadingNextPage(): AsyncLoaderState<ItemType> {
            return AsyncLoaderState(asyncLoadingState = AsyncLoadingState(isLoadingNextPage = true))
        }

        fun <ItemType> firstPageError(throwable: Throwable): AsyncLoaderState<ItemType> {
            val errorState = AsyncLoadingState(isLoadingNextPage = false, nextPageError = ViewError.from(throwable))
            return AsyncLoaderState(asyncLoadingState = errorState)
        }
    }
}

