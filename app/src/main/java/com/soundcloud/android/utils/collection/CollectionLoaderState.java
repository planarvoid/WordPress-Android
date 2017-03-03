package com.soundcloud.android.utils.collection;

import static com.soundcloud.java.optional.Optional.of;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.CollectionLoadingState;
import com.soundcloud.android.view.ViewError;
import com.soundcloud.java.optional.Optional;
import rx.functions.Func2;

@AutoValue
public abstract class CollectionLoaderState<ItemType> {

    public <TransformedType> CollectionLoaderState<TransformedType> withNewData(TransformedType data) {
        return CollectionLoaderState.<TransformedType>builder()
                .data(of(data))
                .collectionLoadingState(collectionLoadingState())
                .build();
    }

    public abstract CollectionLoadingState collectionLoadingState();

    public abstract Optional<ItemType> data();

    public abstract Builder<ItemType> toBuilder();

    public static <ItemType> Builder<ItemType> builder() {
        return new AutoValue_CollectionLoaderState.Builder()
                .collectionLoadingState(CollectionLoadingState.builder().build())
                .data(Optional.<ItemType>absent());
    }

    static <ItemType> CollectionLoaderState<ItemType> loadingNextPage() {
        final CollectionLoadingState loadingState = CollectionLoadingState.builder()
                                                                          .isLoadingNextPage(true)
                                                                          .build();
        return CollectionLoaderState.<ItemType>builder().collectionLoadingState(loadingState).build();
    }

    public CollectionLoaderState<ItemType> toFirstPageLoaded(ItemType data) {
        final CollectionLoadingState loadingState = collectionLoadingState().toBuilder()
                                                                            .isLoadingNextPage(false)
                                                                            .nextPageError(Optional.absent())
                                                                            .build();
        return toBuilder()
                .collectionLoadingState(loadingState)
                .data(data)
                .build();
    }

    public CollectionLoaderState<ItemType> toRefreshStarted() {
        final CollectionLoadingState loadingState = collectionLoadingState().toBuilder()
                                                                            .isRefreshing(true)
                                                                            .refreshError(Optional.absent())
                                                                            .build();
        return toBuilder()
                .collectionLoadingState(loadingState)
                .build();
    }

    CollectionLoaderState<ItemType> toNextPageError(Throwable throwable) {
        final CollectionLoadingState loadingState = collectionLoadingState().toBuilder()
                                                                            .isLoadingNextPage(false)
                                                                            .nextPageError(of(ViewError.from(throwable)))
                                                                            .build();
        return toBuilder()
                .collectionLoadingState(loadingState)
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder<ItemType> {

        public Builder<ItemType> data(ItemType value) {
            return data(of(value));
        }

        public abstract Builder<ItemType> data(Optional<ItemType> value);

        public abstract Builder<ItemType> collectionLoadingState(CollectionLoadingState value);

        public abstract CollectionLoaderState<ItemType> build();
    }


    interface PartialState<ItemType> {

        CollectionLoaderState<ItemType> newState(CollectionLoaderState<ItemType> oldState);

        class NextPageLoaded<ItemType> implements PartialState<ItemType> {

            private ItemType data;
            private boolean hasMorePages;
            private final Optional<Func2<ItemType, ItemType, ItemType>> combiner;

            NextPageLoaded(ItemType data, boolean hasMorePages, Optional<Func2<ItemType, ItemType, ItemType>> combiner) {
                this.data = data;
                this.hasMorePages = hasMorePages;
                this.combiner = combiner;
            }

            @Override
            public CollectionLoaderState<ItemType> newState(CollectionLoaderState<ItemType> oldState) {
                return oldState.toBuilder()
                               .data(oldState.data().isPresent() && combiner.isPresent() ? combiner.get().call(oldState.data().get(), data) : data)
                               .collectionLoadingState(oldState.collectionLoadingState().toNextPageLoaded(hasMorePages))
                               .build();
            }
        }

        class NextPageLoading<ItemType> implements PartialState<ItemType> {

            @Override
            public CollectionLoaderState<ItemType> newState(CollectionLoaderState<ItemType> oldState) {
                return oldState.toBuilder()
                               .collectionLoadingState(oldState.collectionLoadingState().toNextPageLoading())
                               .build();
            }
        }

        class NextPageError<ItemType> implements PartialState<ItemType> {

            private final Throwable throwable;

            NextPageError(Throwable throwable) {
                this.throwable = throwable;
            }

            @Override
            public CollectionLoaderState<ItemType> newState(CollectionLoaderState<ItemType> oldState) {
                return oldState.toBuilder().collectionLoadingState(oldState.collectionLoadingState().toNextPageError(throwable))
                               .build();

            }
        }

        class RefreshStarted<ItemType> implements PartialState<ItemType> {

            @Override
            public CollectionLoaderState<ItemType> newState(CollectionLoaderState<ItemType> oldState) {
                return oldState.toBuilder()
                               .collectionLoadingState(oldState.collectionLoadingState().toRefreshStarted())
                               .data(oldState.data())
                               .build();
            }
        }

        class RefreshError<ItemType> implements PartialState<ItemType> {

            private final Throwable throwable;

            RefreshError(Throwable throwable) {
                this.throwable = throwable;
            }

            @Override
            public CollectionLoaderState<ItemType> newState(CollectionLoaderState<ItemType> oldState) {
                return oldState.toBuilder()
                               .collectionLoadingState(oldState.collectionLoadingState().toRefreshError(throwable))
                               .build();

            }
        }

    }

}
