package com.soundcloud.android.search.topresults;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.view.ViewError;
import com.soundcloud.java.optional.Optional;

@AutoValue
abstract class ViewModel {
    public abstract boolean isRefreshing();

    public abstract boolean isLoading();

    public abstract Optional<ViewError> error();

    public abstract Optional<TopResultsViewModel> data();

    public static ViewModel empty() {
        return new AutoValue_ViewModel.Builder().isRefreshing(false).isLoading(false).error(Optional.absent()).data(Optional.absent()).build();
    }

    public ViewModel with(SearchResult searchResult) {
        final Builder builder = this.toBuilder();
        switch (searchResult.kind()) {
            case DATA:
                builder.isLoading(false);
                builder.isRefreshing(false);
                builder.error(Optional.absent());
                builder.data(TopResultsMapper.toViewModel(((SearchResult.Data) searchResult).data()));
                break;
            case ERROR:
                builder.isLoading(false);
                builder.isRefreshing(false);
                builder.error(Optional.of(ViewError.from(((SearchResult.Error) searchResult).throwable())));
                break;
            case LOADING:
                builder.isLoading(true);
                builder.isRefreshing(((SearchResult.Loading) searchResult).isRefreshing());
                break;
            default:
                throw new IllegalArgumentException("Unexpect search result type");
        }
        return builder.build();
    }

    abstract Builder toBuilder();

    @AutoValue.Builder
    abstract static class Builder {
        abstract Builder isRefreshing(boolean isRefreshing);

        abstract Builder isLoading(boolean isLoading);

        abstract Builder error(Optional<ViewError> error);

        abstract Builder data(Optional<TopResultsViewModel> data);

        Builder data(TopResultsViewModel data) {
            return data(Optional.of(data));
        }

        abstract ViewModel build();
    }
}
