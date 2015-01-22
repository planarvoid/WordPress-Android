package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.commands.StoreTrackDownloadsCommand;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.propeller.PropellerWriteException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class OfflineContentOperationsTest {

    @Mock private LikeOperations likeOperations;
    @Mock private OfflineSettingsStorage settingsStorage;
    @Mock private StoreTrackDownloadsCommand storeTrackDownloads;

    private final Urn TRACK_URN = Urn.forTrack(123L);
    private final List<Urn> LIKED_TRACKS = Arrays.asList(TRACK_URN);

    private OfflineContentOperations offlineOperations;

    @Before
    public void setUp() throws Exception {
        offlineOperations = new OfflineContentOperations(storeTrackDownloads, likeOperations,
                settingsStorage, Schedulers.immediate());

        when(likeOperations.likedTrackUrns()).thenReturn(Observable.just(LIKED_TRACKS));
    }

    @Test
    public void updateFromLikesCallsLikesOperations() {
        offlineOperations.updateOfflineLikes().subscribe();

        verify(likeOperations).likedTrackUrns();
    }

    @Test
    public void updateFromLikesCallsStoreTrackDownloadsWithGivenLikes() throws PropellerWriteException {
        offlineOperations.updateOfflineLikes().subscribe();

        verify(storeTrackDownloads).toObservable();
        expect(storeTrackDownloads.getInput()).toContainExactly(TRACK_URN);
    }
}
