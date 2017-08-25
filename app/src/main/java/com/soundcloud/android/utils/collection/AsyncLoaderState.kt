package com.soundcloud.android.utils.collection

import com.soundcloud.android.model.AsyncLoadingState
import com.soundcloud.android.view.ViewError
import com.soundcloud.java.functions.Function
import com.soundcloud.java.optional.Optional
import com.soundcloud.java.optional.Optional.absent
import com.soundcloud.java.optional.Optional.of

data class AsyncLoaderState<ItemType, ActionType>(val asyncLoadingState: AsyncLoadingState = AsyncLoadingState.builder().build(),
                                                  val data: Optional<ItemType> = absent(),
                                                  val action: Optional<ActionType> = absent()) {

    internal fun updateWithRefreshState(isRefreshing: Boolean, refreshError: Optional<Throwable>): AsyncLoaderState<ItemType, ActionType> {
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

    fun stripAction() = copy(action = absent())

    fun update(updateFunction: Function<ItemType, ItemType>): AsyncLoaderState<ItemType, ActionType> = copy(data = data.transform(updateFunction))

    fun toNextPageError(throwable: Throwable): AsyncLoaderState<ItemType, ActionType> {
        val loadingState = asyncLoadingState.toBuilder()
                .isLoadingNextPage(false)
                .nextPageError(of(ViewError.from(throwable)))
                .build()
        return copy(asyncLoadingState = loadingState)
    }

    companion object {

        fun <ItemType, ActionType> loadingNextPage(): AsyncLoaderState<ItemType, ActionType> {
            val loadingState = AsyncLoadingState.builder()
                    .isLoadingNextPage(true)
                    .build()
            return AsyncLoaderState(asyncLoadingState = loadingState)
        }

        fun <ItemType, ActionType> firstPageError(throwable: Throwable): AsyncLoaderState<ItemType, ActionType> {
            val errorState = AsyncLoadingState.builder()
                    .isLoadingNextPage(false)
                    .nextPageError(Optional.of(ViewError.from(throwable)))
                    .build()
            return AsyncLoaderState(asyncLoadingState = errorState)
        }
    }
}

