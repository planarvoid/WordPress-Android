package com.soundcloud.android.testsupport.fixtures;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.stream.ApiPromotedTrack;

import java.util.Arrays;

public class PromotedFixtures {

    public static ApiPromotedTrack promotedStreamItemWithPromoter(ApiUser apiUser) {
        return getApiPromotedTrack(apiUser);
    }

    public static ApiPromotedTrack promotedStreamItemWithoutPromoter() {
        return getApiPromotedTrack(null);
    }

    private static ApiPromotedTrack getApiPromotedTrack(ApiUser promoter) {
        return new ApiPromotedTrack(
                ModelFixtures.create(ApiTrack.class),
                promoter,
                "adswizz:ads:123",
                Arrays.asList("http://tracking_track_clicked_url_1", "http://tracking_track_clicked_url_2"),
                Arrays.asList("http://tracking_profile_clicked_url_1", "tracking_profile_clicked_url_2"),
                Arrays.asList("http://tracking_promoter_clicked_url_1", "tracking_promoter_clicked_url_2"),
                Arrays.asList("http://tracking_track_played_url_1", "http://tracking_track_played_url_2"),
                Arrays.asList("http://tracking_track_impression_url_1", "http://tracking_track_impression_url_2")
        );
    }
}
