package com.soundcloud.android.search.topresults;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

import java.util.List;

@AutoValue
public abstract class TopResultsBucketViewModel {
    public enum Kind {
        TOP_RESULT, TRACKS, GO_TRACKS, USERS, PLAYLISTS, ALBUMS
    }

    public abstract Kind kind();

    public abstract int totalResults();

    public abstract Urn queryUrn();

    public abstract List<SearchItem> items();

    public static TopResultsBucketViewModel create(List<SearchItem> topResults, Kind kind, int totalResults, Urn queryUrn) {
        return new AutoValue_TopResultsBucketViewModel.Builder()
                .items(topResults)
                .kind(kind)
                .totalResults(totalResults)
                .queryUrn(queryUrn)
                .build();
    }

    @AutoValue.Builder
    abstract static class Builder {

        abstract Builder kind(Kind value);

        abstract Builder totalResults(int value);

        abstract Builder queryUrn(Urn value);

        abstract Builder items(List<SearchItem> value);

        abstract TopResultsBucketViewModel build();

    }

}
