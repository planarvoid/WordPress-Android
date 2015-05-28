package com.soundcloud.android.presentation;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.view.adapters.ItemAdapter;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.support.v4.app.Fragment;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class PlayableListUpdaterTest {

    private static final String UPDATED_CREATOR = "Jamie Macdonald";

    private PlayableListUpdater updater;

    @Mock ItemAdapter<PlayableItem> adapter;
    @Mock private TrackItemRenderer trackItemRenderer;
    @Mock private Fragment fragment;
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        updater = new PlayableListUpdater(eventBus, adapter, trackItemRenderer);
    }

    @Test
    public void trackChangedEventUpdatesCurrentlyPlayingTrack() {
        final Urn playingTrack = Urn.forTrack(123L);
        updater.onCreate(fragment, null);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(playingTrack));

        verify(trackItemRenderer).setPlayingTrack(playingTrack);
    }

    @Test
    public void trackChangedEventDoesNotUpdateAfterOnDestroy() {
        final Urn playingTrack = Urn.forTrack(123L);
        updater.onCreate(fragment, null);
        updater.onDestroy(fragment);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(playingTrack));

        verify(trackItemRenderer, never()).setPlayingTrack(playingTrack);
    }

    @Test
    public void newQueueEventUpdatesCurrentlyPlayingTrack() {
        final Urn playingTrack = Urn.forTrack(123L);
        updater.onCreate(fragment, null);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(playingTrack));

        verify(trackItemRenderer).setPlayingTrack(playingTrack);
    }

    @Test
    public void newQueueEventAfterDestroyedDoesNotUpdateCurrentlyPlayingTrack() {
        final Urn playingTrack = Urn.forTrack(123L);
        updater.onCreate(fragment, null);
        updater.onDestroy(fragment);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(playingTrack));

        verify(trackItemRenderer, never()).setPlayingTrack(playingTrack);
    }

    @Test
    public void entityChangedEventUpdatesItemWithTheSameUrnAndNotifiesAdapter() throws Exception {
        TrackItem track1 = TrackItem.from(ModelFixtures.create(ApiTrack.class));
        TrackItem track2 = TrackItem.from(ModelFixtures.create(ApiTrack.class));
        final EntityStateChangedEvent entityStateChangedEvent = getEntityStateChangedEvent(track1, track2);

        updater.onCreate(fragment, null);
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, entityStateChangedEvent);

        expect(track1.getCreatorName()).toEqual(UPDATED_CREATOR);
        verify(adapter).notifyDataSetChanged();
    }

    @Test
    public void entityChangedEventDoesNotUpdateItemAfterOnDestroy() throws Exception {
        TrackItem track1 = TrackItem.from(ModelFixtures.create(ApiTrack.class));
        TrackItem track2 = TrackItem.from(ModelFixtures.create(ApiTrack.class));
        final EntityStateChangedEvent entityStateChangedEvent = getEntityStateChangedEvent(track1, track2);

        updater.onCreate(fragment, null);
        updater.onDestroy(fragment);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, entityStateChangedEvent);

        expect(track1.getCreatorName()).not.toEqual(UPDATED_CREATOR);
        verify(adapter, never()).notifyDataSetChanged();
    }

    @Test
    public void entityChangedEventDoesNotNotifyWithNoMatchingUrns() throws Exception {

        TrackItem track1 = TrackItem.from(ModelFixtures.create(ApiTrack.class));
        TrackItem track2 = TrackItem.from(ModelFixtures.create(ApiTrack.class));

        PropertySet changeSet = ModelFixtures.create(ApiTrack.class).toPropertySet();

        when(adapter.getItems()).thenReturn(Arrays.<PlayableItem>asList(track1, track2));

        final EntityStateChangedEvent event = EntityStateChangedEvent.fromSync(Arrays.asList(changeSet));
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, event);

        verify(adapter, never()).notifyDataSetChanged();

    }

    private EntityStateChangedEvent getEntityStateChangedEvent(TrackItem track1, TrackItem track2) {
        PropertySet changeSet = PropertySet.from(
                PlayableProperty.URN.bind(track1.getEntityUrn()),
                PlayableProperty.CREATOR_NAME.bind(UPDATED_CREATOR));

        when(adapter.getItems()).thenReturn(Arrays.<PlayableItem>asList(track1, track2));
        return EntityStateChangedEvent.fromSync(Arrays.asList(changeSet));
    }
}