package com.soundcloud.android.sync.likes;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.storage.TableColumns.Sounds;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class LoadLikesPendingAdditionCommandTest extends StorageIntegrationTest {

    private LoadLikesPendingAdditionCommand command;

    @Before
    public void setup() {
        command = new LoadLikesPendingAdditionCommand(propeller());
    }

    @Test
    public void shouldLoadTrackLikesPendingAddition() throws Exception {
        ApiTrack track = testFixtures().insertLikedTrackPendingAddition(new Date(100));
        testFixtures().insertLikedTrack(new Date(200)); // must not be returned

        List<PropertySet> toBeAdded = command.with(Sounds.TYPE_TRACK).call();

        expect(toBeAdded).toContainExactly(expectedLikeFor(track.getUrn(), new Date(0), new Date(100)));
    }

    @Test
    public void shouldLoadPlaylistLikesPendingAddition() throws Exception {
        ApiPlaylist playlist = testFixtures().insertLikedPlaylistPendingAddition(new Date(100));
        testFixtures().insertLikedPlaylist(new Date(200)); // must not be returned

        List<PropertySet> toBeAdded = command.with(Sounds.TYPE_PLAYLIST).call();

        expect(toBeAdded).toContainExactly(expectedLikeFor(playlist.getUrn(), new Date(0), new Date(100)));
    }

    private PropertySet expectedLikeFor(Urn urn, Date createdAt, Date addedAt) {
        return PropertySet.from(
                LikeProperty.TARGET_URN.bind(urn),
                LikeProperty.CREATED_AT.bind(createdAt)).put(LikeProperty.ADDED_AT, addedAt);
    }
}