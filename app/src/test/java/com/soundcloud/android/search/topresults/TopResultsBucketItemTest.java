package com.soundcloud.android.search.topresults;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.search.ApiUniversalSearchItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import org.junit.Test;

import java.util.ArrayList;

public class TopResultsBucketItemTest extends AndroidUnitTest {

    private static final Urn QUERY_URN = new Urn("soundcloud:query_urn:123");
    private static final Urn TRACKS_BUCKET_URN = new Urn("soundcloud:search-buckets:tracks");
    public static final ApiTrack TRACK = ModelFixtures.create(ApiTrack.class);
    private static final TrackItem TRACK_ITEM = TrackItem.from(TRACK);
    private static final ApiUniversalSearchItem UNIVERSAL_TRACK_ITEM = new ApiUniversalSearchItem(null, null, TRACK);
    public static final int TOTAL_RESULTS = 5;

    @Test
    public void createBucketItemFromTracks() {
        final ArrayList<ApiUniversalSearchItem> apiUniversalSearchItems = newArrayList(UNIVERSAL_TRACK_ITEM);
        final ApiTopResultsBucket apiTopResultsBucket = ApiTopResultsBucket.create(TRACKS_BUCKET_URN, QUERY_URN, TOTAL_RESULTS, new ModelCollection<>(apiUniversalSearchItems));

        final TopResultsBucketItem topResultsBucketItem = TopResultsBucketItem.create(apiTopResultsBucket);
        assertThat(topResultsBucketItem.kind()).isEqualTo(TopResultsBucketItem.Kind.TRACKS);
        assertThat(topResultsBucketItem.queryUrn()).isEqualTo(QUERY_URN);
        assertThat(topResultsBucketItem.totalResults()).isEqualTo(TOTAL_RESULTS);
        assertThat(topResultsBucketItem.items().size()).isEqualTo(1);
        assertThat(topResultsBucketItem.items().get(0)).isEqualTo(TRACK_ITEM);
    }
}
