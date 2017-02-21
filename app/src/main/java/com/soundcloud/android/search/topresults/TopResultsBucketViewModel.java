package com.soundcloud.android.search.topresults;

import static com.soundcloud.android.search.topresults.TopResultsBucketViewModel.Kind.*;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;

import java.util.List;

@AutoValue
public abstract class TopResultsBucketViewModel {
    private static final String URN_BUCKET_TOP = "soundcloud:search-buckets:top";
    private static final String URN_BUCKET_TRACKS = "soundcloud:search-buckets:freetiertracks";
    private static final String URN_BUCKET_GO_TRACKS = "soundcloud:search-buckets:hightiertracks";
    private static final String URN_BUCKET_PEOPLE = "soundcloud:search-buckets:users";
    private static final String URN_BUCKET_PLAYLISTS = "soundcloud:search-buckets:playlists";
    private static final String URN_BUCKET_ALBUMS = "soundcloud:search-buckets:albums";
    private static final int MIN_VIEW_ALL_RESULTS = 0;

    public enum Kind {
        TOP_RESULT,
        TRACKS,
        GO_TRACKS,
        USERS,
        PLAYLISTS,
        ALBUMS
    }

    public abstract Kind kind();

    public abstract int titleResourceId();

    public abstract int totalResults();

    public abstract Urn queryUrn();

    public abstract List<SearchItem> items();

    public abstract boolean shouldShowViewAll();

    public static TopResultsBucketViewModel create(List<SearchItem> topResults, Urn urn, int totalResults, Urn queryUrn) {
        final Kind kind = urnToKind(urn);
        final int titleResourceId = kindToTitleResource(kind);
        final boolean shouldShowViewAll = kind != TOP_RESULT && totalResults > MIN_VIEW_ALL_RESULTS;
        return new AutoValue_TopResultsBucketViewModel.Builder()
                .items(topResults)
                .kind(kind)
                .totalResults(totalResults)
                .queryUrn(queryUrn)
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
                return R.string.top_results_albums;
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

        abstract Builder queryUrn(Urn value);

        abstract Builder items(List<SearchItem> value);

        abstract Builder shouldShowViewAll(boolean shouldShowViewAll);

        abstract TopResultsBucketViewModel build();
    }
}
