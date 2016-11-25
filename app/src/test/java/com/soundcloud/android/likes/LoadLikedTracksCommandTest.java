package com.soundcloud.android.likes;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Date;

public class LoadLikedTracksCommandTest extends StorageIntegrationTest {

    private LoadLikedTracksCommand command;

    @Before
    public void setUp() throws Exception {
        command = new LoadLikedTracksCommand(propeller());
    }

    @Test
    public void shouldLoadLikedTracks() {
        testFixtures().insertTrack();
        final Date likedDate = new Date();
        final ApiTrack likedTrack = testFixtures().insertLikedTrack(likedDate);

        final Collection<PropertySet> likedTracks = command.call(null);

        assertThat(likedTracks).containsExactly(likedTrackProperties(likedDate, likedTrack));
    }

    @Test
    public void shouldFilterTracksWithoutPolicies() {
        testFixtures().insertLikedTrack(new Date());
        propeller().delete(Tables.TrackPolicies.TABLE);

        final Collection<PropertySet> likedTracks = command.call(null);

        assertThat(likedTracks).isEmpty();
    }

    private PropertySet likedTrackProperties(Date likedDate, ApiTrack likedTrack) {
        return likedTrack.toPropertySet().slice(
                TrackProperty.URN,
                PlayableProperty.TITLE,
                PlayableProperty.CREATOR_NAME,
                TrackProperty.SNIPPET_DURATION,
                TrackProperty.FULL_DURATION,
                TrackProperty.PLAY_COUNT,
                PlayableProperty.LIKES_COUNT,
                PlayableProperty.IS_PRIVATE,
                TrackProperty.SNIPPED,
                TrackProperty.BLOCKED,
                TrackProperty.SUB_MID_TIER,
                TrackProperty.SUB_HIGH_TIER
        ).merge(PropertySet.from(
                LikeProperty.CREATED_AT.bind(likedDate),
                OfflineProperty.OFFLINE_STATE.bind(OfflineState.NOT_OFFLINE)));
    }
}
