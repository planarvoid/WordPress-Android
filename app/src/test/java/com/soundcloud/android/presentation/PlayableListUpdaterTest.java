package com.soundcloud.android.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.support.v4.app.Fragment;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PlayableListUpdaterTest extends AndroidUnitTest {

    private static final String UPDATED_CREATOR = "Jamie Macdonald";

    private PlayableListUpdater updater;

    @Mock RecyclerItemAdapter<PlayableItem, ?> adapter;
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

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(playingTrack),
                                                                       Urn.NOT_SET,
                                                                       0));

        verify(trackItemRenderer).setPlayingTrack(playingTrack);
    }

    @Test
    public void trackChangedEventDoesNotUpdateAfterOnDestroy() {
        final Urn playingTrack = Urn.forTrack(123L);
        updater.onCreate(fragment, null);
        updater.onDestroy(fragment);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(playingTrack),
                                                                       Urn.NOT_SET,
                                                                       0));

        verify(trackItemRenderer, never()).setPlayingTrack(playingTrack);
    }

    @Test
    public void newQueueEventUpdatesCurrentlyPlayingTrack() {
        final Urn playingTrack = Urn.forTrack(123L);
        updater.onCreate(fragment, null);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromNewQueue(TestPlayQueueItem.createTrack(playingTrack),
                                                                Urn.NOT_SET,
                                                                0));

        verify(trackItemRenderer).setPlayingTrack(playingTrack);
    }

    @Test
    public void newQueueEventAfterDestroyedDoesNotUpdateCurrentlyPlayingTrack() {
        final Urn playingTrack = Urn.forTrack(123L);
        updater.onCreate(fragment, null);
        updater.onDestroy(fragment);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromNewQueue(TestPlayQueueItem.createTrack(playingTrack),
                                                                Urn.NOT_SET,
                                                                0));

        verify(trackItemRenderer, never()).setPlayingTrack(playingTrack);
    }

    @Test
    public void entityChangedEventUpdatesItemWithTheSameUrnAndNotifiesAdapter() throws Exception {
        TrackItem track1 = TrackItem.from(ModelFixtures.create(ApiTrack.class));
        TrackItem track2 = TrackItem.from(ModelFixtures.create(ApiTrack.class));
        final EntityStateChangedEvent entityStateChangedEvent = getEntityStateChangedEvent(track1, track2);

        updater.onCreate(fragment, null);
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, entityStateChangedEvent);

        assertThat(track1.getCreatorName()).isEqualTo(UPDATED_CREATOR);
        verify(adapter).notifyItemChanged(0);
    }

    @Test
    public void entityChangedEventDoesNotUpdateItemAfterOnDestroy() throws Exception {
        TrackItem track1 = TrackItem.from(ModelFixtures.create(ApiTrack.class));
        TrackItem track2 = TrackItem.from(ModelFixtures.create(ApiTrack.class));
        final EntityStateChangedEvent entityStateChangedEvent = getEntityStateChangedEvent(track1, track2);

        updater.onCreate(fragment, null);
        updater.onDestroy(fragment);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, entityStateChangedEvent);

        assertThat(track1.getCreatorName()).isNotEqualTo(UPDATED_CREATOR);
        verify(adapter, never()).notifyItemChanged(anyInt());
    }

    @Test
    public void entityChangedEventDoesNotNotifyWithNoMatchingUrns() throws Exception {

        TrackItem track1 = TrackItem.from(ModelFixtures.create(ApiTrack.class));
        TrackItem track2 = TrackItem.from(ModelFixtures.create(ApiTrack.class));

        PropertySet changeSet = ModelFixtures.create(ApiTrack.class).toPropertySet();

        when(adapter.getItems()).thenReturn(Arrays.asList(track1, track2));

        final EntityStateChangedEvent event = EntityStateChangedEvent.forUpdate(Collections.singletonList(changeSet));
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, event);

        verify(adapter, never()).notifyItemChanged(anyInt());

    }

    @Test
    public void likeEventUpdatesItemWithTheSameUrnAndNotifiesAdapter() throws Exception {
        TrackItem likedTrack = initTrackForLike();
        TrackItem unlikedTrack = initTrackForLike();
        when(adapter.getItems()).thenReturn(Arrays.asList(likedTrack, unlikedTrack));

        final int likesCount = 10;
        LikesStatusEvent.LikeStatus like = LikesStatusEvent.LikeStatus.create(likedTrack.getUrn(), true, likesCount);
        final LikesStatusEvent likeEvent = getLikeStatusEvent(like);

        updater.onCreate(fragment, null);
        eventBus.publish(EventQueue.LIKE_CHANGED, likeEvent);

        assertThat(likedTrack.getLikesCount()).isEqualTo(likesCount);
        assertThat(likedTrack.isLiked()).isTrue();
        verify(adapter).notifyItemChanged(0);
    }

    @Test
    public void likeEventDoesNotUpdateItemAfterOnDestroy() throws Exception {
        TrackItem likedTrack = initTrackForLike();
        TrackItem unlikedTrack = initTrackForLike();
        when(adapter.getItems()).thenReturn(Arrays.asList(likedTrack, unlikedTrack));

        final int likesCount = 10;
        LikesStatusEvent.LikeStatus like = LikesStatusEvent.LikeStatus.create(likedTrack.getUrn(), true, likesCount);
        final LikesStatusEvent likeEvent = getLikeStatusEvent(like);

        updater.onCreate(fragment, null);
        updater.onDestroy(fragment);

        eventBus.publish(EventQueue.LIKE_CHANGED, likeEvent);

        assertThat(likedTrack.getLikesCount()).isNotEqualTo(likesCount);
        assertThat(likedTrack.isLiked()).isFalse();
        verify(adapter, never()).notifyItemChanged(anyInt());
    }

    @Test
    public void likeEventDoesNotNotifyWithNoMatchingUrns() throws Exception {
        TrackItem likedTrack = initTrackForLike();
        TrackItem unlikedTrack = initTrackForLike();
        when(adapter.getItems()).thenReturn(Arrays.asList(likedTrack, unlikedTrack));

        LikesStatusEvent.LikeStatus like = LikesStatusEvent.LikeStatus.create(Urn.NOT_SET, true, 10);
        final LikesStatusEvent likeEvent = getLikeStatusEvent(like);

        eventBus.publish(EventQueue.LIKE_CHANGED, likeEvent);

        verify(adapter, never()).notifyItemChanged(anyInt());

    }

    private TrackItem initTrackForLike() {
        final TrackItem trackItem = TrackItem.from(ModelFixtures.create(ApiTrack.class));
        trackItem.getSource().put(PlayableProperty.LIKES_COUNT, 0);
        trackItem.getSource().put(PlayableProperty.IS_USER_LIKE, false);
        return trackItem;
    }

    private EntityStateChangedEvent getEntityStateChangedEvent(TrackItem track1, TrackItem track2) {
        PropertySet changeSet = PropertySet.from(
                PlayableProperty.URN.bind(track1.getUrn()),
                PlayableProperty.CREATOR_NAME.bind(UPDATED_CREATOR));

        when(adapter.getItems()).thenReturn(Arrays.asList(track1, track2));
        return EntityStateChangedEvent.forUpdate(Collections.singletonList(changeSet));
    }

    private LikesStatusEvent getLikeStatusEvent(LikesStatusEvent.LikeStatus... likes) {
        final Map<Urn, LikesStatusEvent.LikeStatus> likeMap = new HashMap<>();
        for (LikesStatusEvent.LikeStatus like : likes) {
            likeMap.put(like.urn(), like);
        }
        return LikesStatusEvent.createFromSync(likeMap);
    }
}
