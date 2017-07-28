package com.soundcloud.android.model;

import static com.soundcloud.java.optional.Optional.of;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.view.ViewError;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class AsyncLoadingState {

    public abstract boolean isLoadingNextPage();

    public abstract Optional<ViewError> nextPageError();

    public abstract boolean isRefreshing();

    public abstract Optional<ViewError> refreshError();

    public abstract boolean requestMoreOnScroll();

    public abstract Builder toBuilder();

    public static Builder builder() {
        return new AutoValue_AsyncLoadingState.Builder()
                .nextPageError(Optional.absent())
                .refreshError(Optional.absent())
                .isLoadingNextPage(false)
                .isRefreshing(false)
                .requestMoreOnScroll(false);
    }

    public AsyncLoadingState toRefreshStarted() {
        return toBuilder().isRefreshing(true)
                          .build();
    }

    public AsyncLoadingState toRefreshError(Throwable throwable) {
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

        public abstract Builder requestMoreOnScroll(boolean value);

        public abstract AsyncLoadingState build();
    }
}
