package com.soundcloud.android.view.adapters;

import static com.soundcloud.java.optional.Optional.of;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.view.ViewError;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.optional.Optional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@AutoValue
public abstract class CollectionViewState<ItemType> {

    public <TransformedType> CollectionViewState<TransformedType> withNewType(Function<? super ItemType, ? extends TransformedType> func){
        return CollectionViewState.<TransformedType>builder()
                .items(Lists.transform(items(), func))
                .isLoadingNextPage(isLoadingNextPage())
                .nextPageError(nextPageError())
                .isRefreshing(isRefreshing())
                .refreshError(refreshError())
                .hasMorePages(hasMorePages())
                .build();
    }

    public abstract List<ItemType> items();

    public abstract boolean isLoadingNextPage();

    public abstract Optional<ViewError> nextPageError();

    public abstract boolean isRefreshing();

    public abstract Optional<ViewError> refreshError();

    public abstract boolean hasMorePages();

    public abstract Builder<ItemType> toBuilder();

    public static <ItemType> Builder<ItemType> builder() {
        return new AutoValue_CollectionViewState.Builder()
                .nextPageError(Optional.absent())
                .refreshError(Optional.absent())
                .isLoadingNextPage(false)
                .isRefreshing(false)
                .hasMorePages(true)
                .items(Collections.emptyList());
    }

    public static <ItemType> CollectionViewState<ItemType> loadingNextPage() {
        return CollectionViewState.<ItemType>builder().isLoadingNextPage(true).build();
    }

    public CollectionViewState<ItemType> toFirstPageLoaded(List<ItemType> items) {
        return toBuilder()
                .isLoadingNextPage(false)
                .nextPageError(Optional.absent())
                .items(items)
                .build();
    }

    public CollectionViewState<ItemType> toRefreshStarted() {
        return toBuilder()
                .isRefreshing(true)
                .refreshError(Optional.absent())
                .build();
    }

    public CollectionViewState<ItemType> toNextPageError(Throwable throwable) {
        return toBuilder()
                .isLoadingNextPage(false)
                .nextPageError(of(ViewError.from(throwable)))
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder<ItemType> {

        public abstract Builder<ItemType> items(List<ItemType> value);

        public abstract Builder<ItemType> isLoadingNextPage(boolean value);

        public abstract Builder<ItemType> nextPageError(Optional<ViewError> value);

        public abstract Builder<ItemType> isRefreshing(boolean value);

        public abstract Builder<ItemType> refreshError(Optional<ViewError> value);

        public abstract Builder<ItemType> hasMorePages(boolean value);

        public abstract CollectionViewState<ItemType> build();
    }


    public interface PartialState<ItemType> {

        CollectionViewState<ItemType> newState(CollectionViewState<ItemType> oldState);

        class NextPageLoaded<ItemType> implements PartialState<ItemType> {

            private List<ItemType> items;
            private boolean hasMorePages;

            NextPageLoaded(List<ItemType> items, boolean hasMorePages) {
                this.items = items;
                this.hasMorePages = hasMorePages;
            }

            @Override
            public CollectionViewState<ItemType> newState(CollectionViewState<ItemType> oldState) {
                List<ItemType> newItems = new ArrayList<>();
                newItems.addAll(oldState.items());
                newItems.addAll(items);

                return oldState.toBuilder()
                               .items(newItems)
                               .nextPageError(Optional.absent())
                               .isLoadingNextPage(false)
                               .hasMorePages(hasMorePages)
                               .build();
            }
        }

        class NextPageLoading<ItemType> implements PartialState<ItemType> {

            @Override
            public CollectionViewState<ItemType> newState(CollectionViewState<ItemType> oldState) {
                return oldState.toBuilder()
                               .nextPageError(Optional.absent())
                               .isLoadingNextPage(true)
                               .build();
            }
        }

        class NextPageError<ItemType> implements PartialState<ItemType> {

            private final Throwable throwable;

            NextPageError(Throwable throwable) {
                this.throwable = throwable;
            }

            @Override
            public CollectionViewState<ItemType> newState(CollectionViewState<ItemType> oldState) {
                return oldState.toBuilder()
                               .nextPageError(of(ViewError.from(throwable)))
                               .isLoadingNextPage(false)
                               .build();

            }
        }

        class RefreshStarted<ItemType> implements PartialState<ItemType> {

            @Override
            public CollectionViewState<ItemType> newState(CollectionViewState<ItemType> oldState) {
                return oldState.toBuilder()
                               .items(oldState.items())
                               .isRefreshing(true)
                               .build();
            }
        }

        class RefreshError<ItemType> implements PartialState<ItemType> {

            private final Throwable throwable;

            RefreshError(Throwable throwable) {
                this.throwable = throwable;
            }

            @Override
            public CollectionViewState<ItemType> newState(CollectionViewState<ItemType> oldState) {
                return oldState.toBuilder()
                               .isRefreshing(false)
                               .refreshError(of(ViewError.from(throwable)))
                               .build();

            }
        }

    }

}
