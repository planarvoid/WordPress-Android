package com.soundcloud.android.presentation;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.PlaylistChangedEvent;
import com.soundcloud.android.events.PlaylistTrackCountChangedEvent;
import com.soundcloud.android.events.TrackChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.adapters.MixedPlayableRecyclerItemAdapter;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.support.v4.app.Fragment;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayableListUpdaterTest extends AndroidUnitTest {

    private static final String UPDATED_CREATOR = "Jamie Macdonald";
    private static final int DEFAULT_TRACK_COUNT = 5;
    private static final int UPDATED_TRACK_COUNT = 10;

    private PlayableListUpdater updater;

    @Mock MixedPlayableRecyclerItemAdapter adapter;
    @Mock private Fragment fragment;
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        updater = new PlayableListUpdater(eventBus, adapter);
    }

    @Test
    public void trackChangedEventUpdatesCurrentlyPlayingTrack() {
        final Urn playingTrack = Urn.forTrack(123L);
        updater.onCreate(fragment, null);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(playingTrack),
                                                                       Urn.NOT_SET,
                                                                       0));

        verify(adapter).updateNowPlaying(playingTrack);
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

        verify(adapter, never()).updateNowPlaying(playingTrack);
    }

    @Test
    public void newQueueEventUpdatesCurrentlyPlayingTrack() {
        final Urn playingTrack = Urn.forTrack(123L);
        updater.onCreate(fragment, null);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromNewQueue(TestPlayQueueItem.createTrack(playingTrack),
                                                                Urn.NOT_SET,
                                                                0));

        verify(adapter).updateNowPlaying(playingTrack);
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

        verify(adapter, never()).updateNowPlaying(playingTrack);
    }

    @Test
    public void entityChangedEventUpdatesItemWithTheSameUrnAndNotifiesAdapter() throws Exception {
        TrackItem track1 = ModelFixtures.trackItem();
        TrackItem track2 = ModelFixtures.trackItem();
        List<PlayableItem> trackItems = Lists.newArrayList(track1, track2);
        final TrackChangedEvent trackChangedEvent = getTrackChangedEvent(track1.getUrn(), trackItems);

        updater.onCreate(fragment, null);
        eventBus.publish(EventQueue.TRACK_CHANGED, trackChangedEvent);


        assertThat(trackItems.get(0).getUrn()).isEqualTo(track1.getUrn());
        assertThat(trackItems.get(0).creatorName()).isEqualTo(UPDATED_CREATOR);
        verify(adapter).notifyItemChanged(0);
    }

    @Test
    public void entityChangedEventDoesNotUpdateItemAfterOnDestroy() throws Exception {
        TrackItem track1 = ModelFixtures.trackItem();
        TrackItem track2 = ModelFixtures.trackItem();
        List<PlayableItem> trackItems = Lists.newArrayList(track1, track2);
        final TrackChangedEvent trackChangedEvent = getTrackChangedEvent(track1.getUrn(), trackItems);

        updater.onCreate(fragment, null);
        updater.onDestroy(fragment);

        eventBus.publish(EventQueue.TRACK_CHANGED, trackChangedEvent);

        assertThat(trackItems.get(0).getUrn()).isEqualTo(track1.getUrn());
        assertThat(trackItems.get(0).creatorName()).isNotEqualTo(UPDATED_CREATOR);
        verify(adapter, never()).notifyItemChanged(anyInt());
    }

    @Test
    public void entityChangedEventDoesNotNotifyWithNoMatchingUrns() throws Exception {

        TrackItem track1 = ModelFixtures.trackItem();
        TrackItem track2 = ModelFixtures.trackItem();
        Track changeSet = ModelFixtures.track();

        when(adapter.getItems()).thenReturn(Arrays.asList(track1, track2));

        final TrackChangedEvent event = TrackChangedEvent.forUpdate(changeSet);
        eventBus.publish(EventQueue.TRACK_CHANGED, event);

        verify(adapter, never()).notifyItemChanged(anyInt());

    }

    @Test
    public void playlistChangedEventUpdatesItemWithTheSameUrnAndNotifiesAdapter() throws Exception {
        PlaylistItem playlist1 = ModelFixtures.playlistItemBuilder().trackCount(DEFAULT_TRACK_COUNT).build();
        PlaylistItem playlist2 = ModelFixtures.playlistItem();
        final List<PlayableItem> items = Arrays.asList(playlist1, playlist2);
        when(adapter.getItems()).thenReturn(items);
        final PlaylistChangedEvent playlistChangedEvent = PlaylistTrackCountChangedEvent.fromTrackAddedToPlaylist(playlist1.getUrn(), UPDATED_TRACK_COUNT);

        updater.onCreate(fragment, null);
        eventBus.publish(EventQueue.PLAYLIST_CHANGED, playlistChangedEvent);

        final PlaylistItem updatedItem = ((PlaylistItem) items.get(0));
        assertThat((updatedItem).getUrn()).isEqualTo(playlist1.getUrn());
        assertThat((updatedItem).trackCount()).isEqualTo(UPDATED_TRACK_COUNT);
        verify(adapter).notifyItemChanged(0);
    }

    @Test
    public void likeEventUpdatesItemWithTheSameUrnAndNotifiesAdapter() throws Exception {
        TrackItem likedTrack = initTrackForLike();
        TrackItem unlikedTrack = initTrackForLike();
        final List<PlayableItem> items = Arrays.asList(likedTrack, unlikedTrack);
        when(adapter.getItems()).thenReturn(items);

        final int likesCount = 10;
        LikesStatusEvent.LikeStatus like = LikesStatusEvent.LikeStatus.create(likedTrack.getUrn(), true, likesCount);
        final LikesStatusEvent likeEvent = getLikeStatusEvent(like);

        updater.onCreate(fragment, null);
        eventBus.publish(EventQueue.LIKE_CHANGED, likeEvent);

        final TrackItem updatedItem = ((TrackItem) items.get(0));
        assertThat(updatedItem.getUrn()).isEqualTo(likedTrack.getUrn());
        assertThat(updatedItem.likesCount()).isEqualTo(likesCount);
        assertThat(updatedItem.isUserLike()).isTrue();
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

        assertThat(likedTrack.likesCount()).isNotEqualTo(likesCount);
        assertThat(likedTrack.isUserLike()).isFalse();
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
        return ModelFixtures.trackItemBuilder().likesCount(0).isUserLike(false).build();
    }

    private TrackChangedEvent getTrackChangedEvent(Urn updatedItemUrn, List<PlayableItem> trackItems) {
        final Track.Builder updatedTrack = ModelFixtures.trackBuilder();
        updatedTrack.urn(updatedItemUrn);
        updatedTrack.creatorName(UPDATED_CREATOR);
        when(adapter.getItems()).thenReturn(trackItems);
        return TrackChangedEvent.forUpdate(updatedTrack.build());
    }

    private LikesStatusEvent getLikeStatusEvent(LikesStatusEvent.LikeStatus... likes) {
        final Map<Urn, LikesStatusEvent.LikeStatus> likeMap = new HashMap<>();
        for (LikesStatusEvent.LikeStatus like : likes) {
            likeMap.put(like.urn(), like);
        }
        return LikesStatusEvent.createFromSync(likeMap);
    }
}
