package com.soundcloud.android.sync.likes;

import static com.soundcloud.android.storage.TableColumns.Sounds;
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

public class LoadLikesPendingRemovalCommandTest extends StorageIntegrationTest {

    private LoadLikesPendingRemovalCommand command;

    @Before
    public void setup() {
        command = new LoadLikesPendingRemovalCommand(propeller());
    }

    @Test
    public void shouldLoadTrackLikesPendingRemoval() throws Exception {
        ApiTrack track = testFixtures().insertLikedTrackPendingRemoval(new Date(100));
        testFixtures().insertLikedTrack(new Date(200)); // must not be returned

        List<PropertySet> toBeRemoved = command.with(Sounds.TYPE_TRACK).call();

        assertThat(toBeRemoved).containsExactly(expectedLikeFor(track.getUrn(), new Date(0), new Date(100)));
    }

    @Test
    public void shouldLoadPlaylistLikesPendingRemoval() throws Exception {
        ApiPlaylist playlist = testFixtures().insertLikedPlaylistPendingRemoval(new Date(100));
        testFixtures().insertLikedPlaylist(new Date(200)); // must not be returned

        List<PropertySet> toBeRemoved = command.with(Sounds.TYPE_PLAYLIST).call();

        assertThat(toBeRemoved).containsExactly(expectedLikeFor(playlist.getUrn(), new Date(0), new Date(100)));
    }

    private PropertySet expectedLikeFor(Urn urn, Date createdAt, Date removedAt) {
        return PropertySet.from(
                LikeProperty.TARGET_URN.bind(urn),
                LikeProperty.CREATED_AT.bind(createdAt)).put(LikeProperty.REMOVED_AT, removedAt);
    }
}
