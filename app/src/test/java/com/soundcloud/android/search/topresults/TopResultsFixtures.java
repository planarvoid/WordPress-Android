package com.soundcloud.android.search.topresults;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.search.ApiUniversalSearchItem;
import edu.emory.mathcs.backport.java.util.Arrays;

import java.util.Collections;

public final class TopResultsFixtures {

    static final Urn QUERY_URN = new Urn("soundcloud:query_urn:123");
    static final Urn TRACKS_BUCKET_URN = new Urn("soundcloud:search-buckets:freetiertracks");
    static final Urn TOP_RESULT_BUCKET_URN = new Urn("soundcloud:search-buckets:top");

    static ApiUniversalSearchItem searchTrackItem(ApiTrack track) {
        return new ApiUniversalSearchItem(null, null, track);
    }

    static ApiUniversalSearchItem searchUserItem(ApiUser user) {
        return new ApiUniversalSearchItem(user, null, null);
    }

    static ApiUniversalSearchItem searchPlaylistItem(ApiPlaylist playlist) {
        return new ApiUniversalSearchItem(null, playlist, null);
    }

    static ApiTopResultsBucket apiTrackResultsBucket(ApiUniversalSearchItem... apiUniversalSearchItems) {
        return ApiTopResultsBucket.create(TRACKS_BUCKET_URN, apiUniversalSearchItems.length, new ModelCollection<>(Arrays.asList(apiUniversalSearchItems), Collections.emptyMap(), QUERY_URN));
    }

    static ApiTopResultsBucket apiTopResultsBucket(ApiUniversalSearchItem... apiUniversalSearchItems) {
        return ApiTopResultsBucket.create(TOP_RESULT_BUCKET_URN, apiUniversalSearchItems.length, new ModelCollection<>(Arrays.asList(apiUniversalSearchItems), Collections.emptyMap(), QUERY_URN));
    }
}
