package com.soundcloud.android.utils.collection

import com.soundcloud.android.model.AsyncLoadingState
import com.soundcloud.android.view.ViewError
import com.soundcloud.java.functions.Function
import com.soundcloud.java.optional.Optional
import com.soundcloud.java.optional.Optional.absent
import com.soundcloud.java.optional.Optional.of

data class AsyncLoaderState<ItemType>(val asyncLoadingState: AsyncLoadingState = AsyncLoadingState.builder().build(),
                                                  val data: Optional<ItemType> = absent()) {

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

    fun update(updateFunction: Function<ItemType, ItemType>): AsyncLoaderState<ItemType> = copy(data = data.transform(updateFunction))

    fun toNextPageError(throwable: Throwable): AsyncLoaderState<ItemType> {
        val loadingState = asyncLoadingState.toBuilder()
                .isLoadingNextPage(false)
                .nextPageError(of(ViewError.from(throwable)))
                .build()
        return copy(asyncLoadingState = loadingState)
    }

    companion object {

        fun <ItemType> loadingNextPage(): AsyncLoaderState<ItemType> {
            val loadingState = AsyncLoadingState.builder()
                    .isLoadingNextPage(true)
                    .build()
            return AsyncLoaderState(asyncLoadingState = loadingState)
        }
    }
}

