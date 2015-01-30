package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.commands.LoadDownloadsPendingRemovalCommand;
import com.soundcloud.android.offline.commands.StoreTrackDownloadsCommand;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.propeller.PropellerWriteException;
import com.soundcloud.propeller.WriteResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(SoundCloudTestRunner.class)
public class OfflineContentOperationsTest {

    @Mock private LikeOperations likeOperations;
    @Mock private LoadDownloadsPendingRemovalCommand downloadsPendingRemoval;
    @Mock private OfflineSettingsStorage settingsStorage;
    @Mock private StoreTrackDownloadsCommand storeTrackDownloads;

    private final Urn TRACK_URN = Urn.forTrack(123L);
    private final List<Urn> LIKED_TRACKS = Arrays.asList(TRACK_URN);

    private OfflineContentOperations offlineOperations;

    @Before
    public void setUp() throws Exception {
        offlineOperations = new OfflineContentOperations(
                providerOf(storeTrackDownloads),
                downloadsPendingRemoval,
                likeOperations,
                settingsStorage,
                Schedulers.immediate());

        when(likeOperations.likedTrackUrns()).thenReturn(Observable.just(LIKED_TRACKS));
        when(downloadsPendingRemoval.toObservable()).thenReturn(Observable.<List<Urn>>empty());
        when(storeTrackDownloads.toObservable()).thenReturn(Observable.<WriteResult>empty());
    }

    @Test
    public void pendingRemovalUsesATimeOffset() {
        offlineOperations.pendingRemovals().subscribe();

        expect(downloadsPendingRemoval.getInput()).toEqual(TimeUnit.MINUTES.toMillis(3));
    }

    @Test
    public void updateFromLikesCallsStoreTrackDownloadsWithGivenLikes() throws PropellerWriteException {
        when(settingsStorage.isLikesOfflineSyncEnabled()).thenReturn(true);
        offlineOperations.updateOfflineLikes().subscribe();

        verify(storeTrackDownloads).toObservable();
        expect(storeTrackDownloads.getInput()).toContainExactly(TRACK_URN);
    }

    @Test
    public void updateFromLikesCallsStoreTrackDownloadsWithNoInputWhenLikesSyncNotEnabled() throws PropellerWriteException {
        offlineOperations.updateOfflineLikes().subscribe();

        verify(storeTrackDownloads).toObservable();
        expect(storeTrackDownloads.getInput()).toBeNull();

    }
}
