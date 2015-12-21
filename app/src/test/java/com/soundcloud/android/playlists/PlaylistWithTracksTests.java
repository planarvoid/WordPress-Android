package com.soundcloud.android.playlists;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.collections.PropertySet;

import java.util.concurrent.TimeUnit;

public class PlaylistWithTracksTests {
    public static PlaylistWithTracks createPlaylistWithTracks(Urn urn) {

        final PropertySet metadata = PropertySet.from(
                PlaylistProperty.URN.bind(urn),
                PlaylistProperty.PLAY_DURATION.bind(TimeUnit.SECONDS.toMillis(60))
        );

        return new PlaylistWithTracks(metadata, ModelFixtures.trackItems(2));
    }

}
