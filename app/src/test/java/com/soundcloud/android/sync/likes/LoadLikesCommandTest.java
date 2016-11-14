package com.soundcloud.android.sync.likes;

import static com.soundcloud.android.storage.TableColumns.Sounds.TYPE_PLAYLIST;
import static com.soundcloud.android.storage.TableColumns.Sounds.TYPE_TRACK;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.List;

public class LoadLikesCommandTest extends StorageIntegrationTest {

    private LoadLikesCommand command;

    @Before
    public void setup() {
        command = new LoadLikesCommand(propeller());
    }

    @Test
    public void shouldLoadTrackLikes() throws Exception {
        ApiTrack track = testFixtures().insertLikedTrack(new Date(100));
        testFixtures().insertLikedTrackPendingRemoval(new Date(200)); // must not be returned

        List<PropertySet> trackLikes = command.call(TYPE_TRACK);

        assertThat(trackLikes).containsExactly(expectedLikeFor(track.getUrn(), new Date(100)));
    }

    @Test
    public void shouldLoadPlaylistLikes() throws Exception {
        ApiPlaylist playlist = testFixtures().insertLikedPlaylist(new Date(100));
        testFixtures().insertLikedPlaylistPendingRemoval(new Date(200)); // must not be returned

        List<PropertySet> playlistLikes = command.call(TYPE_PLAYLIST);

        assertThat(playlistLikes).containsExactly(expectedLikeFor(playlist.getUrn(), new Date(100)));
    }

    private PropertySet expectedLikeFor(Urn urn, Date createdAt) {
        return PropertySet.from(
                LikeProperty.TARGET_URN.bind(urn),
                LikeProperty.CREATED_AT.bind(createdAt));
    }
}