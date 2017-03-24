package com.soundcloud.android.search.topresults;

import static com.soundcloud.android.search.topresults.TopResults.Bucket.Kind.ALBUMS;
import static com.soundcloud.android.search.topresults.TopResults.Bucket.Kind.GO_TRACKS;
import static com.soundcloud.android.search.topresults.TopResults.Bucket.Kind.PLAYLISTS;
import static com.soundcloud.android.search.topresults.TopResults.Bucket.Kind.TOP_RESULT;
import static com.soundcloud.android.search.topresults.TopResults.Bucket.Kind.TRACKS;
import static com.soundcloud.android.search.topresults.TopResults.Bucket.Kind.USERS;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.Sets;

import java.util.List;
import java.util.Set;

class ApiTopResultsMapper {
    private static final String URN_BUCKET_TOP = "soundcloud:search-buckets:topresult";
    private static final String URN_BUCKET_TRACKS = "soundcloud:search-buckets:freetiertracks";
    private static final String URN_BUCKET_GO_TRACKS = "soundcloud:search-buckets:hightiertracks";
    private static final String URN_BUCKET_PEOPLE = "soundcloud:search-buckets:users";
    private static final String URN_BUCKET_PLAYLISTS = "soundcloud:search-buckets:playlists";
    private static final String URN_BUCKET_ALBUMS = "soundcloud:search-buckets:albums";

    private static final Set<String> AVAILABLE_URNS = Sets.newHashSet(URN_BUCKET_TOP, URN_BUCKET_TRACKS, URN_BUCKET_GO_TRACKS, URN_BUCKET_PEOPLE, URN_BUCKET_PLAYLISTS, URN_BUCKET_ALBUMS);

    static TopResults toDomainModel(ApiTopResults apiTopResults) {
        final List<ApiTopResultsBucket> collection = Lists.newArrayList(Iterables.filter(apiTopResults.buckets().getCollection(), bucket -> isAvailableUrn(bucket.urn().toString())));
        final List<TopResults.Bucket> buckets = Lists.transform(collection,
                                                                apiBucket -> TopResults.Bucket.create(urnToKind(apiBucket.urn()), apiBucket.totalResults(), apiBucket.collection().getCollection()));
        return TopResults.create(apiTopResults.totalResults(), apiTopResults.buckets().getQueryUrn(), buckets);
    }

    private static boolean isAvailableUrn(String urn) {
        return AVAILABLE_URNS.contains(urn);
    }

    private static TopResults.Bucket.Kind urnToKind(Urn urn) {
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
}
