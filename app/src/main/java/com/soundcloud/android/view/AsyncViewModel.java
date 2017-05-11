package com.soundcloud.android.view;

import static com.soundcloud.java.optional.Optional.absent;
import static com.soundcloud.java.optional.Optional.of;

import com.google.auto.value.AutoValue;
import com.soundcloud.java.optional.Optional;

@Deprecated // use CollectionLoaderState
@AutoValue
public abstract class AsyncViewModel<ViewModelType> {

    public abstract Optional<ViewModelType> data();

    public abstract boolean isLoadingNextPage();

    public abstract boolean isRefreshing();

    public abstract Optional<ViewError> error();

    // TODO : squash with error ?
    public abstract Optional<ViewError> refreshError();

    public abstract Builder<ViewModelType> toBuilder();

    public static <ViewModelType> Builder<ViewModelType> builder() {
        return new AutoValue_AsyncViewModel.Builder<>();
    }

    public static <T> AsyncViewModel<T> initial() {
        return AsyncViewModel.<T>builder()
                .data(absent())
                .isLoadingNextPage(true)
                .isRefreshing(false)
                .error(absent())
                .refreshError(absent())
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder<ViewModelType> {
        public Builder<ViewModelType> data(ViewModelType data) {
            return data(of(data));
        }

        public abstract Builder<ViewModelType> data(Optional<ViewModelType> data);

        public abstract Builder<ViewModelType> isLoadingNextPage(boolean isLoadingNextPage);

        public abstract Builder<ViewModelType> isRefreshing(boolean isRefreshing);

        public abstract Builder<ViewModelType> error(Optional<ViewError> error);

        public abstract Builder<ViewModelType> refreshError(Optional<ViewError> error);

        public abstract AsyncViewModel<ViewModelType> build();
    }
}
