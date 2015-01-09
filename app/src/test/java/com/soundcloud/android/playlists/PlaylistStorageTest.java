package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observer;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistStorageTest extends StorageIntegrationTest {

    private PlaylistStorage storage;

    @Mock private Observer<PropertySet> observer;

    @Before
    public void setUp() throws Exception {
        storage = new PlaylistStorage(propeller(), Schedulers.immediate());
    }

    @Test
    public void playlistLikesReturnChangeSetsWithLikeStatus() {
        ApiPlaylist apiPlaylist1 = testFixtures().insertPlaylist();
        ApiPlaylist apiPlaylist2 = testFixtures().insertPlaylist();
        List<PropertySet> input = Arrays.asList(apiPlaylist1.toPropertySet(), apiPlaylist2.toPropertySet());
        testFixtures().insertLegacyPlaylistLike(apiPlaylist1.getId(), 123L);

        final List<PropertySet> changeSet = storage.playlistLikes(input);

        expect(changeSet).toNumber(2);
        expect(changeSet.get(0).get(PlayableProperty.URN)).toEqual(apiPlaylist1.getUrn());
        expect(changeSet.get(0).get(PlayableProperty.IS_LIKED)).toEqual(true);
        expect(changeSet.get(1).get(PlayableProperty.URN)).toEqual(apiPlaylist2.getUrn());
        expect(changeSet.get(1).get(PlayableProperty.IS_LIKED)).toEqual(false);
    }

    @Test
    public void playlistLikesDoesNotReturnsOnlyChangeSetsForPlaylists() {
        final ApiPlaylist likedPlaylist = testFixtures().insertPlaylist();
        final ApiPlaylist unlikedPlaylist = testFixtures().insertPlaylist();
        final ApiTrack track = testFixtures().insertTrack();
        testFixtures().insertLegacyPlaylistLike(likedPlaylist.getId(), 123L);

        List<PropertySet> input = Arrays.asList(
                likedPlaylist.toPropertySet(), unlikedPlaylist.toPropertySet(), track.toPropertySet());
        List<PropertySet> changeSets = storage.playlistLikes(input);

        expect(changeSets).toContainExactlyInAnyOrder(
                PropertySet.from(PlaylistProperty.URN.bind(likedPlaylist.getUrn()), PlaylistProperty.IS_LIKED.bind(true)),
                PropertySet.from(PlaylistProperty.URN.bind(unlikedPlaylist.getUrn()), PlaylistProperty.IS_LIKED.bind(false))
        );
    }

    @Test
    public void trackUrnsLoadsUrnsOfAllTrackItemsInAGivenPlaylist() {
        ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        ApiTrack thirdTrack = testFixtures().insertPlaylistTrack(apiPlaylist, 2);
        ApiTrack firstTrack = testFixtures().insertPlaylistTrack(apiPlaylist, 0);
        ApiTrack secondTrack = testFixtures().insertPlaylistTrack(apiPlaylist, 1);

        TestObserver<Urn> observer = new TestObserver<>();
        storage.trackUrns(apiPlaylist.getUrn()).subscribe(observer);
        expect(observer.getOnNextEvents()).toContainExactly(
                firstTrack.getUrn(), secondTrack.getUrn(), thirdTrack.getUrn()
        );
    }

}