package com.soundcloud.android.model;

import static com.soundcloud.java.optional.Optional.of;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.view.ViewError;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class CollectionLoadingState {

    public abstract boolean isLoadingNextPage();

    public abstract Optional<ViewError> nextPageError();

    public abstract boolean isRefreshing();

    public abstract Optional<ViewError> refreshError();

    public abstract boolean hasMorePages();

    public abstract Builder toBuilder();

    public static Builder builder() {
        return new AutoValue_CollectionLoadingState.Builder()
                .nextPageError(Optional.absent())
                .refreshError(Optional.absent())
                .isLoadingNextPage(false)
                .isRefreshing(false)
                .hasMorePages(true);
    }

    public CollectionLoadingState toNextPageLoaded(boolean hasMorePages) {
        return toBuilder().nextPageError(Optional.absent())
                          .isLoadingNextPage(false)
                          .hasMorePages(hasMorePages)
                          .build();
    }

    public CollectionLoadingState toNextPageLoading() {
        return toBuilder().nextPageError(Optional.absent())
                          .isLoadingNextPage(true)
                          .build();
    }

    public CollectionLoadingState toNextPageError(Throwable throwable) {
        return toBuilder().nextPageError(Optional.of(ViewError.from(throwable)))
                          .isLoadingNextPage(false)
                          .build();
    }

    public CollectionLoadingState toRefreshStarted() {
        return toBuilder().isRefreshing(true)
                          .build();
    }

    public CollectionLoadingState toRefreshError(Throwable throwable) {
        return toBuilder().isRefreshing(false)
                          .refreshError(of(ViewError.from(throwable)))
                          .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder isLoadingNextPage(boolean value);

        public abstract Builder nextPageError(Optional<ViewError> value);

        public abstract Builder isRefreshing(boolean value);

        public abstract Builder refreshError(Optional<ViewError> value);

        public abstract Builder hasMorePages(boolean value);

        public abstract CollectionLoadingState build();
    }
}
