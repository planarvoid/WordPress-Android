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

    private OfflineContentController controller;
    private TestEventBus eventBus;
    private PublishSubject<Boolean> offlineLikeToggleSetting;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        offlineLikeToggleSetting = PublishSubject.create();

        when(settingsStorage.isOfflineLikesEnabled()).thenReturn(true);
        when(settingsStorage.getOfflineLikesChanged()).thenReturn(offlineLikeToggleSetting);

        controller = new OfflineContentController(eventBus, settingsStorage, Robolectric.application);
    }

    @Test
    public void stopServiceWhenOfflineLikeToggleDisabled() {
        controller.subscribe();

        offlineLikeToggleSetting.onNext(false);

        expectServiceStopped();
    }

    @Test
    public void startsServiceWhenOfflineLikeToggleEnabled() {
        controller.subscribe();

        offlineLikeToggleSetting.onNext(true);

        expectServiceStarted();
    }

    @Test
    public void startsOfflineSyncWhenPlaylistMarkedAsAvailableOffline() {
        controller.subscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromMarkedForOffline(Urn.forPlaylist(123L), true));

        expectServiceStarted();
    }

    @Test
    public void startsOfflineSyncWhenPlaylistMarkedAsUnavailableOffline() {
        controller.subscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromMarkedForOffline(Urn.forPlaylist(123L), false));

        expectServiceStarted();
    }

    @Test
    public void doesNotStartOfflineSyncWhenTheFeatureIsDisabled() {
        when(settingsStorage.isOfflineLikesEnabled()).thenReturn(false);
        controller.subscribe();

        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_TRACK_LIKES, true));

        expectNoInteractionWithService();
    }

    @Test
    public void startsOfflineSyncWhenATrackIsLiked() {
        controller.subscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(Urn.forTrack(123L), true, 1));

        expectServiceStarted();
    }

    @Test
    public void startsOfflineSyncWhenATrackIsUnliked() {
        controller.subscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(Urn.forTrack(123L), false, 1));

        expectServiceStarted();
    }

    @Test
    public void startsOfflineSyncWhenLikeSyncingUpdatedTheLikes() {
        controller.subscribe();

        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_TRACK_LIKES, true));

        expectServiceStarted();
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

        expectNoInteractionWithService();
    }

    @Test
    public void ignoreStopEventsWhenUnsubscribed() {
        controller.subscribe();
        controller.unsubscribe();

        offlineLikeToggleSetting.onNext(false);

        expectNoInteractionWithService();
    }

    private void expectNoInteractionWithService() {
        final Intent startService = Robolectric.getShadowApplication().peekNextStartedService();
        expect(startService).toBeNull();
    }

    private void expectServiceStarted() {
        final Intent startService = Robolectric.getShadowApplication().peekNextStartedService();
        expect(startService.getAction()).toEqual(OfflineContentService.ACTION_START_DOWNLOAD);
        expect(startService.getComponent().getClassName()).toEqual(OfflineContentService.class.getCanonicalName());
    }

    private void expectServiceStopped() {
        final Intent startService = Robolectric.getShadowApplication().peekNextStartedService();
        expect(startService.getAction()).toEqual(OfflineContentService.ACTION_STOP_DOWNLOAD);
        expect(startService.getComponent().getClassName()).toEqual(OfflineContentService.class.getCanonicalName());
    }
}
