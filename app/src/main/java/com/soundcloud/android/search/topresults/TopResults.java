package com.soundcloud.android.search.topresults;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.search.ApiUniversalSearchItem;
import com.soundcloud.android.search.SearchType;
import com.soundcloud.java.optional.Optional;

import java.util.List;

@AutoValue
public abstract class TopResults {

    abstract int totalResults();

    abstract Optional<Urn> queryUrn();

    abstract List<Bucket> buckets();

    static TopResults create(int totalResults, Optional<Urn> queryUrn, List<Bucket> buckets) {
        return new AutoValue_TopResults(totalResults, queryUrn, buckets);
    }

    @AutoValue
    public static abstract class Bucket {

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

        abstract Kind kind();

        abstract int totalResults();

        abstract List<ApiUniversalSearchItem> items();

        static Bucket create(Kind kind, int totalResults, List<ApiUniversalSearchItem> items) {
            return new AutoValue_TopResults_Bucket(kind, totalResults, items);
        }
    }
}
