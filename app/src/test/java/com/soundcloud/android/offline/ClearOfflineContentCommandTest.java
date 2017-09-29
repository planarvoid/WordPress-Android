package com.soundcloud.android.offline;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import edu.emory.mathcs.backport.java.util.Collections;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.subjects.CompletableSubject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;

public class ClearOfflineContentCommandTest extends StorageIntegrationTest {

    private final CompletableSubject removeAllOfflineContentSubject = CompletableSubject.create();
    private ClearOfflineContentCommand command;

    @Mock SecureFileStorage secureFileStorage;
    @Mock OfflineSettingsStorage offlineSettingsStorage;
    @Mock TrackOfflineStateProvider trackOfflineStateProvider;
    @Mock OfflineContentStorage offlineContentStorage;

    @Before
    public void setUp() throws Exception {
        when(offlineContentStorage.getOfflinePlaylists()).thenReturn(Single.just(Collections.singletonList(Urn.forPlaylist(1))));
        when(offlineContentStorage.removeAllOfflineContent()).thenReturn(Completable.complete());
        command = new ClearOfflineContentCommand(
                secureFileStorage,
                offlineSettingsStorage,
                trackOfflineStateProvider,
                offlineContentStorage);
    }

    @Test
    public void clearsTrackOfflineState() {
        command.call(null);

        verify(trackOfflineStateProvider).clear();
    }

    @Test
    public void clearOfflineContentRemovesOfflineTrackFiles() {
        command.call(null);

        verify(secureFileStorage).deleteAllTracks();
    }

    @Test
    public void clearOfflineContentClearsOfflineContentState() {
        command.call(null);

        verify(offlineSettingsStorage).setHasOfflineContent(false);
    }

    @Test
    public void doesNotClearTrackOfflineStateWhenStorageClearNotCompleted() {
        when(offlineContentStorage.removeAllOfflineContent()).thenReturn(Completable.error(new IOException()));

        command.call(null);

        verify(trackOfflineStateProvider, never()).clear();
    }

    @Test
    public void doesNotClearOfflineContentRemovesOfflineTrackFilesWhenStorageClearNotCompleted() {
        when(offlineContentStorage.removeAllOfflineContent()).thenReturn(Completable.error(new IOException()));

        command.call(null);

        verify(secureFileStorage,never()).deleteAllTracks();
    }

    @Test
    public void doesNotClearOfflineContentClearsOfflineContentStateWhenStorageClearNotCompleted() {
        when(offlineContentStorage.removeAllOfflineContent()).thenReturn(Completable.error(new IOException()));

        command.call(null);

        verify(offlineSettingsStorage, never()).setHasOfflineContent(false);
    }

}
