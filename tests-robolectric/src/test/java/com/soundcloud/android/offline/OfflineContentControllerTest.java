package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.sync.SyncActions;
import com.soundcloud.android.sync.SyncResult;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.subjects.PublishSubject;

import android.content.Intent;

@RunWith(SoundCloudTestRunner.class)
public class OfflineContentControllerTest {

    @Mock private OfflineSettingsStorage settingsStorage;
    @Mock private OfflinePlaylistStorage playlistStorage;

    private OfflineContentController controller;
    private TestEventBus eventBus;
    private PublishSubject<Boolean> offlineLikeToggleSetting;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        offlineLikeToggleSetting = PublishSubject.create();

        when(settingsStorage.isOfflineLikedTracksEnabled()).thenReturn(true);
        when(settingsStorage.getOfflineLikedTracksChanged()).thenReturn(offlineLikeToggleSetting);

        controller = new OfflineContentController(Robolectric.application, eventBus, settingsStorage, playlistStorage);
    }

    @Test
    public void stopServiceWhenUnsubscribed() {
        controller.subscribe();

        controller.unsubscribe();

        expect(wasServiceStopped()).toBeTrue();
    }

    @Test
    public void startsServiceWhenOfflineLikeToggleEnabled() {
        controller.subscribe();

        offlineLikeToggleSetting.onNext(true);

        expect(wasServiceStarted()).toBeTrue();
    }

    @Test
    public void startsOfflineSyncWhenPlaylistMarkedAsAvailableOffline() {
        controller.subscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromMarkedForOffline(Urn.forPlaylist(123L), true));

        expect(wasServiceStarted()).toBeTrue();
    }

    @Test
    public void startsOfflineSyncWhenPlaylistMarkedAsUnavailableOffline() {
        controller.subscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromMarkedForOffline(Urn.forPlaylist(123L), false));

        expect(wasServiceStarted()).toBeTrue();
    }

    @Test
    public void doesNotStartOfflineSyncWhenTheFeatureIsDisabled() {
        when(settingsStorage.isOfflineLikedTracksEnabled()).thenReturn(false);
        controller.subscribe();

        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_TRACK_LIKES, true));

        expectNoInteractionWithService();
    }

    @Test
    public void startsOfflineSyncWhenATrackIsLiked() {
        controller.subscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(Urn.forTrack(123L), true, 1));

        expect(wasServiceStarted()).toBeTrue();
    }

    @Test
    public void startsOfflineSyncWhenATrackIsUnliked() {
        controller.subscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(Urn.forTrack(123L), false, 1));

        expect(wasServiceStarted()).toBeTrue();
    }

    @Test
    public void startsOfflineSyncWhenLikeSyncingUpdatedTheLikes() {
        controller.subscribe();

        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_TRACK_LIKES, true));

        expect(wasServiceStarted()).toBeTrue();
    }

    @Test
    public void doesNotStartOfflineSyncWhenLikeSyncingDidNotUpdateTheLiked() {
        controller.subscribe();

        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_TRACK_LIKES, false));

        expectNoInteractionWithService();
    }

    @Test
    public void ignoreStartEventsWhenUnsubscribed() {
        controller.subscribe();
        controller.unsubscribe();

        offlineLikeToggleSetting.onNext(true);

        expect(wasServiceStarted()).toBeFalse();
    }

    @Test
    public void startsOfflineSyncWhenTrackAddedToPlaylistMarkedAsAvailableOffline() {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        when(playlistStorage.isOfflinePlaylist(playlistUrn)).thenReturn(true);
        controller.subscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                EntityStateChangedEvent.fromTrackAddedToPlaylist(playlistUrn, 1));

        expect(wasServiceStarted()).toBeTrue();
    }

    @Test
    public void doesNotStartOfflineSyncWhenTrackAddedToNonOfflinePlaylist() {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        when(playlistStorage.isOfflinePlaylist(playlistUrn)).thenReturn(false);
        controller.subscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                EntityStateChangedEvent.fromTrackAddedToPlaylist(playlistUrn, 1));

        expect(wasServiceStarted()).toBeFalse();
    }

    private void expectNoInteractionWithService() {
        final Intent startService = Robolectric.getShadowApplication().peekNextStartedService();
        expect(startService).toBeNull();
    }

    private boolean wasServiceStarted() {
        final Intent intent = Robolectric.getShadowApplication().peekNextStartedService();
        return intent != null &&
                intent.getAction().equals(OfflineContentService.ACTION_START_DOWNLOAD) &&
                intent.getComponent().getClassName().equals(OfflineContentService.class.getCanonicalName());
    }

    private boolean wasServiceStopped() {
        final Intent intent = Robolectric.getShadowApplication().peekNextStartedService();
        return intent != null &&
                intent.getAction().equals(OfflineContentService.ACTION_STOP_DOWNLOAD) &&
                intent.getComponent().getClassName().equals(OfflineContentService.class.getCanonicalName());
    }
}
