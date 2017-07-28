package com.soundcloud.android.playlists;

import static com.soundcloud.java.optional.Optional.absent;
import static com.soundcloud.java.optional.Optional.of;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.view.ViewError;
import com.soundcloud.java.optional.Optional;

@Deprecated
@AutoValue
/***
 * @deprecated Use {@link com.soundcloud.android.utils.collection.AsyncLoaderState} instead
 */
public abstract class PlaylistAsyncViewModel<ViewModelType> {

    public abstract Optional<ViewModelType> data();

    public abstract boolean isLoadingNextPage();

    public abstract boolean isRefreshing();

    public abstract Optional<ViewError> error();

    // TODO : squash with error ?
    public abstract Optional<ViewError> refreshError();

    public abstract Builder<ViewModelType> toBuilder();

    public static <ViewModelType> Builder<ViewModelType> builder() {
        return new AutoValue_PlaylistAsyncViewModel.Builder<>();
    }

    public static <T> PlaylistAsyncViewModel<T> initial() {
        return PlaylistAsyncViewModel.<T>builder()
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

        public abstract PlaylistAsyncViewModel<ViewModelType> build();
    }
}
