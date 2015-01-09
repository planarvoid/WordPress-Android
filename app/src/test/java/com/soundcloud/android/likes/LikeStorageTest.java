package com.soundcloud.android.likes;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observer;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class LikeStorageTest extends StorageIntegrationTest {

    private LikeStorage storage;

    @Mock private Observer<PropertySet> observer;

    @Before
    public void setUp() {
        storage = new LikeStorage(testScheduler());
    }

    @Test
    public void shouldLoadTrackLikesFromObservable() {
        ApiTrack track1 = testFixtures().insertLikedTrack(new Date(100));
        ApiTrack track2 = testFixtures().insertLikedTrack(new Date(200));

        storage.trackLikes().subscribe(observer);

        verify(observer).onNext(eq(expectedLikeFor(track1.getUrn(), new Date(100))));
        verify(observer).onNext(eq(expectedLikeFor(track2.getUrn(), new Date(200))));

        verify(observer).onCompleted();
    }

    @Test
    public void shouldLoadTrackLikes() {
        ApiTrack track1 = testFixtures().insertLikedTrack(new Date(100));
        testFixtures().insertLikedTrackPendingRemoval(new Date(200)); // must not be returned

        List<PropertySet> trackLikes = storage.loadTrackLikes();

        expect(trackLikes).toEqual(Arrays.asList(expectedLikeFor(track1.getUrn(), new Date(100))));
    }

    @Test
    public void shouldLoadPlaylistLikes() {
        ApiPlaylist playlist1 = testFixtures().insertLikedPlaylist(new Date(100));
        testFixtures().insertLikedPlaylistPendingRemoval(new Date(200)); // must not be returned

        List<PropertySet> playlistLikes = storage.loadPlaylistLikes();

        expect(playlistLikes).toEqual(Arrays.asList(expectedLikeFor(playlist1.getUrn(), new Date(100))));
    }

    @Test
    public void shouldLoadTrackLikesPendingRemoval() {
        ApiTrack track = testFixtures().insertLikedTrackPendingRemoval(new Date(100));
        testFixtures().insertLikedTrack(new Date(200)); // must not be returned

        List<PropertySet> toBeRemoved = storage.loadTrackLikesPendingRemoval();

        expect(toBeRemoved).toEqual(Arrays.asList(expectedLikeFor(track.getUrn(), new Date(0), new Date(100))));
    }

    @Test
    public void shouldLoadPlaylistLikesPendingRemoval() {
        ApiPlaylist playlist = testFixtures().insertLikedPlaylistPendingRemoval(new Date(100));
        testFixtures().insertLikedPlaylist(new Date(200)); // must not be returned

        List<PropertySet> toBeRemoved = storage.loadPlaylistLikesPendingRemoval();

        expect(toBeRemoved).toEqual(Arrays.asList(expectedLikeFor(playlist.getUrn(), new Date(0), new Date(100))));
    }

    private PropertySet expectedLikeFor(Urn urn, Date createdAt) {
        return PropertySet.from(
                LikeProperty.TARGET_URN.bind(urn),
                LikeProperty.CREATED_AT.bind(createdAt));
    }

    private PropertySet expectedLikeFor(Urn urn, Date createdAt, Date removedAt) {
        return expectedLikeFor(urn, createdAt).put(LikeProperty.REMOVED_AT, removedAt);
    }
}