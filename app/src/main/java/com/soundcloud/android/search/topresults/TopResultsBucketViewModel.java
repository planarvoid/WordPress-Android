package com.soundcloud.android.search.topresults;

import static com.soundcloud.android.search.topresults.TopResults.Bucket.Kind.TOP_RESULT;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.R;
import com.soundcloud.android.search.topresults.TopResults.Bucket.Kind;

import java.util.List;

@AutoValue
public abstract class TopResultsBucketViewModel {

    public abstract Kind kind();

    public abstract int titleResourceId();

    public abstract int totalResults();

    public abstract List<SearchItem> items();

    public abstract boolean shouldShowViewAll();

    public static TopResultsBucketViewModel create(List<SearchItem> topResults, Kind kind, int totalResults) {
        final int titleResourceId = kindToTitleResource(kind);
        final boolean shouldShowViewAll = kind != TOP_RESULT && totalResults > TopResultsOperations.RESULT_LIMIT;
        return new AutoValue_TopResultsBucketViewModel.Builder()
                .items(topResults)
                .kind(kind)
                .totalResults(totalResults)
                .titleResourceId(titleResourceId)
                .shouldShowViewAll(shouldShowViewAll)
                .build();
    }

    private static int kindToTitleResource(Kind kind) {
        switch (kind) {
            case TOP_RESULT:
                return R.string.top_results_top_item;
            case TRACKS:
                return R.string.top_results_tracks;
            case GO_TRACKS:
                return R.string.top_results_go_tracks;
            case USERS:
                return R.string.top_results_people;
            case PLAYLISTS:
                return R.string.top_results_playlists;
            case ALBUMS:
                return R.string.top_results_albums;
            default:
                throw new IllegalArgumentException("Unexpected urn type for search");
        }
    }

    @AutoValue.Builder
    abstract static class Builder {
        abstract Builder kind(Kind value);

        abstract Builder titleResourceId(int value);

        abstract Builder totalResults(int value);

        abstract Builder items(List<SearchItem> value);

        abstract Builder shouldShowViewAll(boolean shouldShowViewAll);

        abstract TopResultsBucketViewModel build();
    }
}
