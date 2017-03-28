package com.soundcloud.android.search.topresults;

import static com.soundcloud.android.search.topresults.TopResults.Bucket.Kind.TOP_RESULT;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.R;
import com.soundcloud.android.search.topresults.TopResults.Bucket.Kind;
import com.soundcloud.java.optional.Optional;

import java.util.List;

@AutoValue
public abstract class TopResultsBucketViewModel {

    public abstract Kind kind();

    public abstract int titleResourceId();

    public abstract Optional<Integer> viewAllResourceId();

    public abstract int totalResults();

    public abstract List<SearchItem> items();

    public abstract boolean shouldShowViewAll();

    public static TopResultsBucketViewModel create(List<SearchItem> topResults, Kind kind, int totalResults) {
        final int titleResourceId = kindToTitleResource(kind);
        final Optional<Integer> viewAllResourceId = kindToViewAllResource(kind);
        final boolean shouldShowViewAll = kind != TOP_RESULT && totalResults > TopResultsOperations.RESULT_LIMIT;
        return new AutoValue_TopResultsBucketViewModel.Builder()
                .items(topResults)
                .kind(kind)
                .totalResults(totalResults)
                .titleResourceId(titleResourceId)
                .viewAllResourceId(viewAllResourceId)
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
                return R.string.top_results_soundcloud_go_tracks;
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

    private static Optional<Integer> kindToViewAllResource(Kind kind) {
        switch (kind) {
            case TRACKS:
                return Optional.of(R.string.top_results_see_all_tracks_results);
            case GO_TRACKS:
                return Optional.of(R.string.top_results_see_all_go_tracks_results);
            case USERS:
                return Optional.of(R.string.top_results_see_all_people_results);
            case PLAYLISTS:
                return Optional.of(R.string.top_results_see_all_playlists_results);
            case ALBUMS:
                return Optional.of(R.string.top_results_see_all_albums_results);
            default:
                return Optional.absent();
        }
    }

    @AutoValue.Builder
    abstract static class Builder {
        abstract Builder kind(Kind value);

        abstract Builder titleResourceId(int value);

        abstract Builder viewAllResourceId(Optional<Integer> value);

        abstract Builder totalResults(int value);

        abstract Builder items(List<SearchItem> value);

        abstract Builder shouldShowViewAll(boolean shouldShowViewAll);

        abstract TopResultsBucketViewModel build();
    }
}
