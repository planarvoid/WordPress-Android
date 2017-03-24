package com.soundcloud.android.search.topresults;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.search.ApiUniversalSearchItem;
import edu.emory.mathcs.backport.java.util.Arrays;

public final class TopResultsFixtures {

    static final Urn QUERY_URN = new Urn("soundcloud:query_urn:123");

    static ApiUniversalSearchItem searchTrackItem(ApiTrack track) {
        return new ApiUniversalSearchItem(null, null, track);
    }

    static ApiUniversalSearchItem searchUserItem(ApiUser user) {
        return new ApiUniversalSearchItem(user, null, null);
    }

    static ApiUniversalSearchItem searchPlaylistItem(ApiPlaylist playlist) {
        return new ApiUniversalSearchItem(null, playlist, null);
    }

    static TopResults.Bucket trackResultsBucket(ApiUniversalSearchItem... apiUniversalSearchItems) {
        return TopResults.Bucket.create(TopResults.Bucket.Kind.TRACKS, apiUniversalSearchItems.length, Arrays.asList(apiUniversalSearchItems));
    }

    static TopResults.Bucket playlistResultsBucket(ApiUniversalSearchItem... apiUniversalSearchItems) {
        return TopResults.Bucket.create(TopResults.Bucket.Kind.PLAYLISTS, apiUniversalSearchItems.length, Arrays.asList(apiUniversalSearchItems));
    }

    static TopResults.Bucket topResultsBucket(ApiUniversalSearchItem... apiUniversalSearchItems) {
        return TopResults.Bucket.create(TopResults.Bucket.Kind.TOP_RESULT, apiUniversalSearchItems.length, Arrays.asList(apiUniversalSearchItems));
    }
}
