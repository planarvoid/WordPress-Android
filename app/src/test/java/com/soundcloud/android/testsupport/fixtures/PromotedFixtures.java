package com.soundcloud.android.testsupport.fixtures;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.stream.ApiPromotedPlaylist;
import com.soundcloud.android.api.model.stream.ApiPromotedTrack;

import java.util.Arrays;

public class PromotedFixtures {

    public static ApiPromotedTrack promotedTrackItemWithoutPromoter() {
        return promotedTrackItemWithPromoter(null);
    }

    public static ApiPromotedTrack promotedTrackItemWithPromoter(ApiUser promoter) {
        return new ApiPromotedTrack(
                ModelFixtures.create(ApiTrack.class),
                promoter,
                "dfp:ads:123-4567",
                Arrays.asList("http://tracking_track_clicked_url_1", "http://tracking_track_clicked_url_2"),
                Arrays.asList("http://tracking_profile_clicked_url_1", "tracking_profile_clicked_url_2"),
                Arrays.asList("http://tracking_promoter_clicked_url_1", "tracking_promoter_clicked_url_2"),
                Arrays.asList("http://tracking_track_played_url_1", "http://tracking_track_played_url_2"),
                Arrays.asList("http://tracking_track_impression_url_1", "http://tracking_track_impression_url_2")
        );
    }

    public static ApiPromotedPlaylist promotedPlaylistItemWithoutPromoter() {
        return promotedPlaylistItemWithPromoter(null);
    }

    public static ApiPromotedPlaylist promotedPlaylistItemWithPromoter(ApiUser promoter) {
        return new ApiPromotedPlaylist(
                ModelFixtures.create(ApiPlaylist.class),
                promoter,
                "dfp:ads:678-7890",
                Arrays.asList("http://tracking_playlist_clicked_url_1", "http://tracking_playlist_clicked_url_2"),
                Arrays.asList("http://tracking_playlist_impression_url_1", "http://tracking_playlist_impression_url_2"),
                Arrays.asList("http://tracking_profile_clicked_url_1", "tracking_profile_clicked_url_2"),
                Arrays.asList("http://tracking_promoter_clicked_url_1", "tracking_promoter_clicked_url_2")
        );
    }
}
