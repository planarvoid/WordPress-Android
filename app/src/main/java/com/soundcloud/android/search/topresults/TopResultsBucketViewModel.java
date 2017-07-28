package com.soundcloud.android.search.topresults;

import static com.soundcloud.android.search.topresults.TopResultsBucketViewModel.Kind.TOP_RESULT;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.R;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.search.SearchType;
import com.soundcloud.java.optional.Optional;

import java.util.List;

@AutoValue
@SuppressWarnings("PMD.GodClass")
public abstract class TopResultsBucketViewModel {

    public abstract Kind kind();

    public abstract String searchQuery();

    public abstract int titleResourceId();

    public abstract Optional<Integer> viewAllResourceId();

    public abstract int totalResults();

    public abstract List<SearchItem> items();

    public abstract Optional<UiAction.ViewAllClick> viewAllClick();

    public static TopResultsBucketViewModel create(List<SearchItem> topResults, Kind kind, int totalResults, String searchQuery, ClickParams clickParams) {
        final int titleResourceId = kindToTitleResource(kind);
        final Optional<Integer> viewAllResourceId = kindToViewAllResource(kind);
        final boolean shouldShowViewAll = kind != TOP_RESULT && totalResults > TopResultsOperations.RESULT_LIMIT;
        return new AutoValue_TopResultsBucketViewModel.Builder()
                .searchQuery(searchQuery)
                .items(topResults)
                .kind(kind)
                .totalResults(totalResults)
                .titleResourceId(titleResourceId)
                .viewAllResourceId(viewAllResourceId)
                .viewAllClick(shouldShowViewAll ? Optional.of(UiAction.ViewAllClick.create(clickParams, kind)) : Optional.absent())
                .build();
    }

    public Optional<UiAction.ViewAllClick> viewAllAction() {
        return viewAllClick();
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

    public enum Kind {
        TOP_RESULT,
        TRACKS,
        GO_TRACKS,
        USERS,
        PLAYLISTS,
        ALBUMS;

        public SearchType toSearchType() {
            switch (this) {
                case GO_TRACKS:
                case TRACKS:
                    return SearchType.TRACKS;
                case USERS:
                    return SearchType.USERS;
                case PLAYLISTS:
                    return SearchType.PLAYLISTS;
                case ALBUMS:
                    return SearchType.ALBUMS;
                default:
                    throw new IllegalArgumentException("Unexpected kind for search");
            }
        }


        public SearchEvent.ClickSource toClickSource() {
            switch (this) {
                case TOP_RESULT:
                    return SearchEvent.ClickSource.TOP_RESULTS_BUCKET;
                case GO_TRACKS:
                    return SearchEvent.ClickSource.GO_TRACKS_BUCKET;
                case TRACKS:
                    return SearchEvent.ClickSource.TRACKS_BUCKET;
                case USERS:
                    return SearchEvent.ClickSource.PEOPLE_BUCKET;
                case PLAYLISTS:
                    return SearchEvent.ClickSource.PLAYLISTS_BUCKET;
                case ALBUMS:
                    return SearchEvent.ClickSource.ALBUMS_BUCKET;
                default:
                    throw new IllegalArgumentException("Unexpected kind for search");
            }
        }
    }

    @AutoValue.Builder
    abstract static class Builder {
        abstract Builder kind(Kind value);

        abstract Builder searchQuery(String searchQuery);

        abstract Builder titleResourceId(int value);

        abstract Builder viewAllResourceId(Optional<Integer> value);

        abstract Builder totalResults(int value);

        abstract Builder items(List<SearchItem> value);

        abstract Builder viewAllClick(Optional<UiAction.ViewAllClick> viewAllClick);

        abstract TopResultsBucketViewModel build();
    }
}
