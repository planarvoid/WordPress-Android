package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Ignore;
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

    @Ignore //TODO: Matthias this is the test I wrote to you about
    public void backFillLikesStatusUpdatesPropertySetWithListStatus() {
        final ApiPlaylist apiPlaylist1 = testFixtures().insertPlaylist();
        final ApiPlaylist apiPlaylist2 = testFixtures().insertPlaylist();

        testFixtures().insertPlaylistLike(apiPlaylist1.getId(), 123L);
        List<PropertySet> input = Arrays.asList(apiPlaylist1.toPropertySet(), apiPlaylist2.toPropertySet());

        final List<PropertySet> updatedSets = storage.backFillLikesStatus(input);

        PropertySet updatedSet1 = apiPlaylist1.toPropertySet().merge(PropertySet.from(PlaylistProperty.IS_LIKED.bind(true)));
        PropertySet updatedSet2 = apiPlaylist2.toPropertySet().merge(PropertySet.from(PlaylistProperty.IS_LIKED.bind(false)));

        expect(updatedSets).toNumber(2);
        expect(updatedSets.get(0)).toEqual(updatedSet1);
        expect(updatedSets.get(1)).toEqual(updatedSet2);
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