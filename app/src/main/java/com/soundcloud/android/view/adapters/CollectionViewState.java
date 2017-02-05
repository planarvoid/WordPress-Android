package com.soundcloud.android.view.adapters;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.view.ViewError;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.optional.Optional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@AutoValue
abstract class CollectionViewState<ItemType> {

    public <TransformedType> CollectionViewState<TransformedType> withNewType(Function<? super ItemType, ? extends TransformedType> func){
        return CollectionViewState.<TransformedType>builder()
                .items(Lists.transform(items(), func))
                .isLoadingFirstPage(isLoadingFirstPage())
                .firstPageError(firstPageError())
                .isLoadingNextPage(isLoadingNextPage())
                .nextPageError(nextPageError())
                .isRefreshing(isRefreshing())
                .refreshError(refreshError())
                .hasMorePages(hasMorePages())
                .build();
    }

    public abstract List<ItemType> items();

    public abstract boolean isLoadingFirstPage();

    public abstract Optional<ViewError> firstPageError();

    public abstract boolean isLoadingNextPage();

    public abstract Optional<ViewError> nextPageError();

    public abstract boolean isRefreshing();

    public abstract Optional<ViewError> refreshError();

    public abstract boolean hasMorePages();

    public abstract Builder<ItemType> toBuilder();

    public static <ItemType> Builder<ItemType> builder() {
        return new AutoValue_CollectionViewState.Builder()
                .firstPageError(Optional.absent())
                .nextPageError(Optional.absent())
                .refreshError(Optional.absent())
                .isLoadingFirstPage(false)
                .isLoadingNextPage(false)
                .isRefreshing(false)
                .hasMorePages(true)
                .items(Collections.emptyList());
    }

    public static <ItemType> CollectionViewState<ItemType> loadingFirstPage() {
        return CollectionViewState.<ItemType>builder().isLoadingFirstPage(true).build();
    }

    public static CollectionViewState pageLoaded() {
        return pageLoaded(true);
    }

    public static CollectionViewState pageLoaded(boolean hasMorePages) {
        return builder().hasMorePages(hasMorePages).build();
    }

    public static <ItemType> CollectionViewState<ItemType> firstPageError(Throwable throwable) {
        return CollectionViewState.<ItemType>builder()
                .firstPageError(Optional.of(ViewError.from(throwable)))
                .build();
    }

    public static CollectionViewState loadingNextPage() {
        return builder().isLoadingNextPage(true).build();
    }

    public static CollectionViewState nextPageError(Throwable throwable) {
        return builder()
                .nextPageError(Optional.of(ViewError.from(throwable)))
                .build();
    }

    @AutoValue.Builder
    abstract static class Builder<ItemType> {

        abstract Builder<ItemType> items(List<ItemType> value);

        abstract Builder<ItemType> isLoadingFirstPage(boolean value);

        abstract Builder<ItemType> firstPageError(Optional<ViewError> value);

        abstract Builder<ItemType> isLoadingNextPage(boolean value);

        abstract Builder<ItemType> nextPageError(Optional<ViewError> value);

        abstract Builder<ItemType> isRefreshing(boolean value);

        abstract Builder<ItemType> refreshError(Optional<ViewError> value);

        abstract Builder<ItemType> hasMorePages(boolean value);

        abstract CollectionViewState<ItemType> build();
    }


    public interface PartialState<ItemType> {

        CollectionViewState<ItemType> newState(CollectionViewState<ItemType> oldState);

        class FirstPageLoaded<ItemType> implements PartialState<ItemType> {

            private List<ItemType> items;

            FirstPageLoaded(List<ItemType> items) {
                this.items = items;
            }

            @Override
            public CollectionViewState<ItemType> newState(CollectionViewState<ItemType> oldState) {
                Builder<ItemType> builder = oldState.toBuilder();
                builder.items(items);
                builder.nextPageError(Optional.absent());
                builder.firstPageError(Optional.absent());
                builder.isLoadingFirstPage(false);
                builder.isLoadingNextPage(false);
                return builder.build();
            }
        }

        class FirstPageError<ItemType> implements PartialState<ItemType> {

            private final Throwable throwable;

            FirstPageError(Throwable throwable) {
                this.throwable = throwable;
            }

            @Override
            public CollectionViewState<ItemType> newState(CollectionViewState<ItemType> oldState) {
                return CollectionViewState.firstPageError(throwable);

            }
        }

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
                               .nextPageError(Optional.of(ViewError.from(throwable)))
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
                               .refreshError(Optional.of(ViewError.from(throwable)))
                               .build();

            }
        }

    }

}
