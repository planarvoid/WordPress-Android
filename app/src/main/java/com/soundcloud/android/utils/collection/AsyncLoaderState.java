package com.soundcloud.android.utils.collection;

import static com.soundcloud.java.optional.Optional.of;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.AsyncLoadingState;
import com.soundcloud.android.view.ViewError;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class AsyncLoaderState<ItemType> {

    public abstract AsyncLoadingState asyncLoadingState();

    public abstract Optional<ItemType> data();

    public abstract Builder<ItemType> toBuilder();

    AsyncLoaderState<ItemType> updateWithRefreshState(boolean isRefreshing, Optional<Throwable> refreshError) {
        if (isRefreshing) {
            if (asyncLoadingState().isRefreshing()) {
                return this;
            } else {
                return toBuilder().asyncLoadingState(asyncLoadingState().toRefreshStarted()).build();
            }
        } else if (refreshError.isPresent()) {
            return toBuilder().asyncLoadingState(asyncLoadingState().toRefreshError(refreshError.get())).build();
        } else {
            return this;
        }
    }

    public static <ItemType> Builder<ItemType> builder() {
        return new AutoValue_AsyncLoaderState.Builder<ItemType>()
                .asyncLoadingState(AsyncLoadingState.builder().build())
                .data(Optional.absent());
    }

    public static <ItemType> AsyncLoaderState<ItemType> loadingNextPage() {
        final AsyncLoadingState loadingState = AsyncLoadingState.builder()
                                                                .isLoadingNextPage(true)
                                                                .build();
        return AsyncLoaderState.<ItemType>builder().asyncLoadingState(loadingState).build();
    }

    public static <ItemType> AsyncLoaderState<ItemType> firstPageError(Throwable throwable) {
        final AsyncLoadingState loadingState = AsyncLoadingState.builder()
                                                                .isLoadingNextPage(false)
                                                                .nextPageError(Optional.of(ViewError.from(throwable)))
                                                                .build();
        return AsyncLoaderState.<ItemType>builder().asyncLoadingState(loadingState).build();
    }



    AsyncLoaderState<ItemType> toNextPageError(Throwable throwable) {
        final AsyncLoadingState loadingState = asyncLoadingState().toBuilder()
                                                                  .isLoadingNextPage(false)
                                                                  .nextPageError(of(ViewError.from(throwable)))
                                                                  .build();
        return toBuilder()
                .asyncLoadingState(loadingState)
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder<ItemType> {

        public Builder<ItemType> data(ItemType value) {
            return data(of(value));
        }

        public abstract Builder<ItemType> data(Optional<ItemType> value);

        public abstract Builder<ItemType> asyncLoadingState(AsyncLoadingState value);

        public abstract AsyncLoaderState<ItemType> build();
    }
}
