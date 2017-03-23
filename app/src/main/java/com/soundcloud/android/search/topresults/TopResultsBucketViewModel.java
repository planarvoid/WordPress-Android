package com.soundcloud.android.search.topresults;

import static com.soundcloud.android.search.topresults.TopResultsBucketViewModel.Kind.ALBUMS;
import static com.soundcloud.android.search.topresults.TopResultsBucketViewModel.Kind.GO_TRACKS;
import static com.soundcloud.android.search.topresults.TopResultsBucketViewModel.Kind.PLAYLISTS;
import static com.soundcloud.android.search.topresults.TopResultsBucketViewModel.Kind.TOP_RESULT;
import static com.soundcloud.android.search.topresults.TopResultsBucketViewModel.Kind.TRACKS;
import static com.soundcloud.android.search.topresults.TopResultsBucketViewModel.Kind.USERS;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.R;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.search.SearchType;
import com.soundcloud.java.collections.Lists;

import java.util.List;

@AutoValue
public abstract class TopResultsBucketViewModel {
    private static final String URN_BUCKET_TOP = "soundcloud:search-buckets:topresult";
    private static final String URN_BUCKET_TRACKS = "soundcloud:search-buckets:freetiertracks";
    private static final String URN_BUCKET_GO_TRACKS = "soundcloud:search-buckets:hightiertracks";
    private static final String URN_BUCKET_PEOPLE = "soundcloud:search-buckets:users";
    private static final String URN_BUCKET_PLAYLISTS = "soundcloud:search-buckets:playlists";
    private static final String URN_BUCKET_ALBUMS = "soundcloud:search-buckets:albums";

    private static final List<String> AVAILABLE_URNS = Lists.newArrayList(URN_BUCKET_TOP, URN_BUCKET_TRACKS, URN_BUCKET_GO_TRACKS, URN_BUCKET_PEOPLE, URN_BUCKET_PLAYLISTS, URN_BUCKET_ALBUMS);

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

    public static boolean isValidBucketUrn(String urn) {
        return AVAILABLE_URNS.contains(urn);
    }

    public abstract Kind kind();

    public abstract int titleResourceId();

    public abstract int totalResults();

    public abstract List<SearchItem> items();

    public abstract boolean shouldShowViewAll();

    public static TopResultsBucketViewModel create(List<SearchItem> topResults, Urn urn, int totalResults) {
        final Kind kind = urnToKind(urn);
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

    private static TopResultsBucketViewModel.Kind urnToKind(Urn urn) {
        switch (urn.toString()) {
            case URN_BUCKET_TOP:
                return TOP_RESULT;
            case URN_BUCKET_TRACKS:
                return TRACKS;
            case URN_BUCKET_GO_TRACKS:
                return GO_TRACKS;
            case URN_BUCKET_PEOPLE:
                return USERS;
            case URN_BUCKET_PLAYLISTS:
                return PLAYLISTS;
            case URN_BUCKET_ALBUMS:
                return ALBUMS;
            default:
                throw new IllegalArgumentException("Unexpected urn type for search: " + urn);
        }
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
