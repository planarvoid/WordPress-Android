package com.soundcloud.android.playlists;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.collections.PropertySet;

import java.util.concurrent.TimeUnit;

public class PlaylistWithTracksTests {

    public static PlaylistWithTracks createPlaylistWithTracks(Urn playlist) {
        return new PlaylistWithTracks(
                PropertySet.from(
                        PlaylistProperty.URN.bind(playlist),
                        PlaylistProperty.PLAY_DURATION.bind(TimeUnit.SECONDS.toMillis(60))
                ),
                ModelFixtures.trackItems(2));
    }

}
