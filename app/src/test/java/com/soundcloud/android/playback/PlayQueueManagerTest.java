package com.soundcloud.android.playback;

import static com.soundcloud.android.testsupport.PlayQueueAssertions.assertPlayQueueItemsEqual;
import static com.soundcloud.android.testsupport.PlayQueueAssertions.assertPlayQueuesEqual;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AdUtils;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.cast.RemotePlayQueue;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.stations.StationTrack;
import com.soundcloud.android.stations.StationsSourceInfo;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.TestUrns;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueue;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.observers.TestSubscriber;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayQueueManagerTest extends AndroidUnitTest {

    private static final Urn PLAYLIST_URN = Urn.forPlaylist(6L);
    private static final int PLAYLIST_TRACK_COUNT = 2;
    private static final Urn USER_URN = Urn.forUser(4L);
    private final List<Urn> queueUrns = asList(Urn.forTrack(123), Urn.forTrack(124));
    private PlayQueueManager playQueueManager;
    private TestEventBus eventBus = new TestEventBus();
    @Mock private PlayQueue playQueue;
    @Mock private PlayQueueOperations playQueueOperations;
    @Mock private PolicyOperations policyOperations;
    @Mock private PlayQueueItemVerifier playQueueItemVerifier;
    @Mock private RemotePlayQueue remotePlayQueue;
    private PlaySessionSource playlistSessionSource;
    private PlaySessionSource exploreSessionSource;

    @Before
    public void before() throws CreateModelException {
        playQueueManager = new PlayQueueManager(playQueueOperations,
                                                eventBus,
                                                playQueueItemVerifier);

        when(playQueue.isEmpty()).thenReturn(true);
        when(playQueue.copy()).thenReturn(playQueue);
        when(playQueue.getTrackItemUrns()).thenReturn(queueUrns);
        when(policyOperations.updatePolicies(anyListOf(Urn.class))).thenReturn(Observable.empty());

        when(playQueue.getUrn(3)).thenReturn(Urn.forTrack(369L));
        when(playQueueOperations.saveQueue(any(PlayQueue.class))).thenReturn(Observable.just(new TxnResult()));

        playlistSessionSource = PlaySessionSource.forPlaylist(Screen.PLAYLIST_DETAILS,
                                                              PLAYLIST_URN,
                                                              USER_URN,
                                                              PLAYLIST_TRACK_COUNT);
        exploreSessionSource = PlaySessionSource.forExplore(Screen.EXPLORE_GENRES, "1.0");
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotAcceptNullValueWhenSettingNewPlayQueue() {
        playQueueManager.setNewPlayQueue(null, playlistSessionSource);
    }

    @Test
    public void shouldSetNewPlayQueueAsCurrentPlayQueue() {
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);
        expectPlayQueueContentToBeEqual(playQueueManager, playQueue);
    }

    @Test
    public void shouldUpdatePositionOnCurrentQueueWhenContentAndSourceAreUnchanged() {
        PlaySessionSource source1 = new PlaySessionSource("screen:something");
        PlayQueue queue1 = TestPlayQueue.fromUrns(TestUrns.createTrackUrns(1L, 2L, 3L, 5L), source1);
        PlaySessionSource source2 = new PlaySessionSource("screen:something");
        PlayQueue queue2 = TestPlayQueue.fromUrns(TestUrns.createTrackUrns(1L, 2L, 3L, 5L), source2);

        playQueueManager.setNewPlayQueue(queue1, source1);
        playQueueManager.setNewPlayQueue(queue2, source2, 2);

        final PlayQueueItem playQueueItem = TestPlayQueueItem.createTrack(Urn.forTrack(1L));
        final PlayQueueItem playQueueItem2 = TestPlayQueueItem.createTrack(Urn.forTrack(3L));

        assertThat(eventBus.eventsOn(EventQueue.PLAY_QUEUE)).containsExactly(PlayQueueEvent.fromNewQueue(Urn.NOT_SET));
        final List<PlayQueueEvent> playQueueEvents = eventBus.eventsOn(EventQueue.PLAY_QUEUE);
        final List<CurrentPlayQueueItemEvent> currentPlayQueueItems = eventBus.eventsOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM);

        assertPlayQueueSaved(queue1);
        assertThat(playQueueEvents.get(0)).isEqualTo(PlayQueueEvent.fromNewQueue(source1.getCollectionUrn()));
        assertCurrentPlayQueueItemEventsEqual(currentPlayQueueItems.get(1), playQueueItem2, Urn.NOT_SET, 2);
    }

    @Test
    public void getCurrentPositionReturnsCurrentPosition() {
        int oldPosition = 0;
        int newPosition = 5;

        when(playQueue.getPlayQueueItem(oldPosition)).thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(1L)));
        when(playQueue.getPlayQueueItem(newPosition)).thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(3L)));
        when(playQueue.size()).thenReturn(6);

        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(oldPosition);

        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);
        playQueueManager.setPosition(newPosition, true);

        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(newPosition);
    }

    @Test
    public void getCurrentPlayQueueItemReturnsCurrentPlayQueueItemFromPlayQueue() {
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource, 5);
        when(playQueue.size()).thenReturn(6);
        when(playQueue.getPlayQueueItem(5)).thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(5L)));

        assertPlayQueueItemsEqual(playQueueManager.getCurrentPlayQueueItem(),
                                  TestPlayQueueItem.createTrack(Urn.forTrack(5L)));
    }

    @Test
    public void getCurrentPlayQueueItemReturnsEmptyPlayQueueItemForEmptyPQ() {
        playQueueManager.setNewPlayQueue(PlayQueue.empty(), playlistSessionSource, 5);
        when(playQueue.size()).thenReturn(0);

        assertThat(playQueueManager.getCurrentPlayQueueItem().isEmpty()).isTrue();
    }

    @Test
    public void getCurrentQueueAsTrackUrnsReturnsUrnList() {
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L);
        playQueueManager.setNewPlayQueue(createPlayQueue(tracksUrn), playlistSessionSource);

        assertThat(playQueueManager.getCurrentQueueTrackUrns()).isEqualTo(tracksUrn);
    }

    @Test
    public void hasSameTracksForwardsCallToRemotePlayQueue() {
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);
        when(remotePlayQueue.hasSameTracks(playQueue)).thenReturn(true);

        assertThat(playQueueManager.hasSameTrackList(remotePlayQueue)).isTrue();
    }

    @Test
    public void appendlayQueueItemsAppendsAllPlayQueueItems() {
        playQueueManager.setNewPlayQueue(createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L)), playlistSessionSource);

        playQueueManager.appendPlayQueueItems(createPlayQueue(TestUrns.createTrackUrns(3L, 4L, 5L)));

        assertThat(playQueueManager.getQueueSize()).isEqualTo(6);
        assertPlayQueueItemsEqual(playQueueManager.getPlayQueueItemAtPosition(3),
                                  TestPlayQueueItem.createTrack(Urn.forTrack(3L)));
        assertPlayQueueItemsEqual(playQueueManager.getPlayQueueItemAtPosition(4),
                                  TestPlayQueueItem.createTrack(Urn.forTrack(4L)));
        assertPlayQueueItemsEqual(playQueueManager.getPlayQueueItemAtPosition(5),
                                  TestPlayQueueItem.createTrack(Urn.forTrack(5L)));
    }

    @Test
    public void appendPlayQueueItemsSavesQueue() {
        playQueueManager.setNewPlayQueue(createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L)), playlistSessionSource);
        Mockito.reset(playQueueOperations);
        when(playQueueOperations.saveQueue(any(PlayQueue.class))).thenReturn(Observable.just(new TxnResult()));

        playQueueManager.appendPlayQueueItems(createPlayQueue(TestUrns.createTrackUrns(4L, 5L)));


        final PlayQueue expected = createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L, 4L, 5L));
        assertPlayQueueSaved(expected);
    }

    @Test
    public void appendPlayQueueItemsBroadcastsPlayQueueUpdate() {
        playQueueManager.setNewPlayQueue(createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L)), playlistSessionSource);

        playQueueManager.appendPlayQueueItems(createPlayQueue(TestUrns.createTrackUrns(4L, 5L)));

        assertThat(eventBus.eventsOn(EventQueue.PLAY_QUEUE)).hasSize(2);
    }

    @Test
    public void getCurrentPlayQueueCountReturnsSizeOfCurrentQueue() {
        playQueueManager.setNewPlayQueue(createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L)), playlistSessionSource);

        assertThat(playQueueManager.getQueueSize()).isEqualTo(3);
    }

    @Test
    public void isQueueEmptyReturnsTrueIfQueueSizeIsZero() throws Exception {
        playQueueManager.setNewPlayQueue(PlayQueue.empty(), playlistSessionSource);

        assertThat(playQueueManager.isQueueEmpty()).isTrue();
    }

    @Test
    public void isQueueEmptyReturnsFalseIfQueueSizeGreaterThanZero() throws Exception {
        playQueueManager.setNewPlayQueue(createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L)), playlistSessionSource);

        assertThat(playQueueManager.isQueueEmpty()).isFalse();
    }

    @Test
    public void getPlayQueueItemAtPositionReturnsPlayQueueItemIfInQueue() {
        playQueueManager.setNewPlayQueue(createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L)), playlistSessionSource);

        assertPlayQueueItemsEqual(playQueueManager.getPlayQueueItemAtPosition(2),
                                  TestPlayQueueItem.createTrack(Urn.forTrack(3L)));
    }

    @Test
    public void getPlayQueueItemAtPositionReturnsEmptyPlayQueueItemIfInEmptyPQ() {
        playQueueManager.setNewPlayQueue(PlayQueue.empty(), playlistSessionSource);

        assertThat(playQueueManager.getPlayQueueItemAtPosition(2).isEmpty()).isTrue();
    }

    @Test
    public void setNewPlayQueueMarksCurrentTrackAsUserTriggered() {
        when(playQueue.isEmpty()).thenReturn(false);
        when(playQueue.size()).thenReturn(1);
        when(playQueue.getPlayQueueItem(0)).thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(369L)));
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);

        assertThat(playQueueManager.getCurrentTrackSourceInfo().getIsUserTriggered()).isTrue();
    }

    @Test
    public void shouldReturnNullForTrackSourceInfoWhenSettingEmptyPlayQueue() throws Exception {
        playQueueManager.setNewPlayQueue(PlayQueue.empty(), playlistSessionSource);
        assertThat(playQueueManager.getCurrentTrackSourceInfo()).isNull();
    }

    @Test
    public void shouldReturnTrackSourceInfoWithPlaylistInfoSetIfSet() {
        playQueueManager.setNewPlayQueue(TestPlayQueue.fromUrns(TestUrns.createTrackUrns(1L, 2L),
                                                                playlistSessionSource), playlistSessionSource, 1);

        final TrackSourceInfo trackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();
        assertThat(trackSourceInfo.getCollectionUrn()).isEqualTo(PLAYLIST_URN);
        assertThat(trackSourceInfo.getPlaylistPosition()).isEqualTo(1);
    }

    @Test
    public void shouldReturnTrackSourceInfoWithoutReposterSetIfNotSet() {
        playQueueManager.setNewPlayQueue(TestPlayQueue.fromTracks(asList(Urn.forTrack(1L).toPropertySet()),
                                                                  playlistSessionSource), playlistSessionSource, 0);

        final TrackSourceInfo trackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();
        assertThat(trackSourceInfo.hasReposter()).isFalse();
    }

    @Test
    public void shouldReturnTrackSourceInfoWithReposterSetIfSet() {
        final PropertySet track = Urn.forTrack(1L).toPropertySet();
        track.put(PostProperty.REPOSTER_URN, Urn.forUser(2L));
        playQueueManager.setNewPlayQueue(TestPlayQueue.fromTracks(asList(track), playlistSessionSource),
                                         playlistSessionSource,
                                         0);

        final TrackSourceInfo trackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();
        assertThat(trackSourceInfo.hasReposter()).isTrue();
        assertThat(trackSourceInfo.getReposter()).isEqualTo(Urn.forUser(2L));
    }

    @Test
    public void shouldReturnTrackSourceInfoWithExploreTrackingTagIfSet() {
        playQueueManager.setNewPlayQueue(createPlayQueue(exploreSessionSource), exploreSessionSource, 1);

        final TrackSourceInfo trackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();
        assertThat(trackSourceInfo.getSource()).isEqualTo("explore");
        assertThat(trackSourceInfo.getSourceVersion()).isEqualTo("1.0");
    }

    @Test
    public void shouldReturnTrackSourceInfoWithQuerySourceInfoIfSet() {
        SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("soundcloud:search:urn"),
                                                                                5,
                                                                                new Urn("soundcloud:click:123"),
                                                                                "query");
        searchQuerySourceInfo.setQueryResults(new ArrayList<>(asList(Urn.forTrack(1), Urn.forTrack(2))));

        playlistSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);
        playQueueManager.setNewPlayQueue(createPlayQueue(TestUrns.createTrackUrns(1L, 2L)), playlistSessionSource, 1);
        final TrackSourceInfo trackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();

        assertThat(trackSourceInfo.getSearchQuerySourceInfo()).isEqualTo(searchQuerySourceInfo);
    }

    @Test
    public void shouldReturnTrackSourceInfoWithStationsSourceInfoIfSet() {
        final Urn stationUrn = Urn.forTrackStation(123L);
        final Urn queryUrn = new Urn("soundcloud:radio:123-456");
        StationTrack stationTrack = StationTrack.create(Urn.forTrack(123L), queryUrn);
        List<StationTrack> tracks = singletonList(stationTrack);

        final PlaySessionSource playSessionSource = PlaySessionSource.forStation(Screen.PLAYLIST_DETAILS, stationUrn);
        playQueueManager.setNewPlayQueue(PlayQueue.fromStation(stationUrn, tracks, playSessionSource),
                                         playSessionSource);
        final TrackSourceInfo trackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();

        assertThat(trackSourceInfo.getStationsSourceInfo()).isEqualTo(StationsSourceInfo.create(queryUrn));
    }

    @Test
    public void shouldReturnTrackSourceInfoWithStationsSourceInfoIfSetOnAudioAdWithNoQueryUrn() {
        final Urn stationUrn = Urn.forTrackStation(123L);
        final Urn queryUrn = new Urn("soundcloud:radio:123-456");
        final StationTrack stationTrack = StationTrack.create(Urn.forTrack(123L), queryUrn);
        final PlaySessionSource playSessionSource = PlaySessionSource.forStation(Screen.PLAYLIST_DETAILS, stationUrn);
        final PlayQueue playQueue = PlayQueue.fromStation(stationUrn,
                                                          singletonList(stationTrack),
                                                          playSessionSource);

        final AudioAdQueueItem adItem = TestPlayQueueItem.createAudioAd(AdFixtures.getAudioAd(Urn.forTrack(123L)));
        playQueue.replaceItem(0, asList(adItem, playQueue.getPlayQueueItem(0)));

        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        final TrackSourceInfo trackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();

        assertThat(trackSourceInfo.getStationsSourceInfo()).isEqualTo(StationsSourceInfo.create(Urn.NOT_SET));
    }

    @Test
    public void shouldReturnTrackSourceInfoWithStationsSourceInfoIfSetOnVideoAdWithNoQueryUrn() {
        final Urn stationUrn = Urn.forTrackStation(123L);
        final Urn queryUrn = new Urn("soundcloud:radio:123-456");
        final StationTrack stationTrack = StationTrack.create(Urn.forTrack(123L), queryUrn);
        final PlaySessionSource playSessionSource = PlaySessionSource.forStation(Screen.PLAYLIST_DETAILS, stationUrn);
        final PlayQueue playQueue = PlayQueue.fromStation(stationUrn,
                                                          singletonList(stationTrack),
                                                          playSessionSource);

        final VideoAdQueueItem adItem = TestPlayQueueItem.createVideo(AdFixtures.getVideoAd(Urn.forTrack(123L)));
        playQueue.replaceItem(0, asList(adItem, playQueue.getPlayQueueItem(0)));

        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        final TrackSourceInfo trackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();

        assertThat(trackSourceInfo.getStationsSourceInfo()).isEqualTo(StationsSourceInfo.create(Urn.NOT_SET));
    }

    @Test
    public void shouldReturnTrackSourceInfoWithoutQuerySourceInfoIfNotSet() {
        playQueueManager.setNewPlayQueue(createPlayQueue(TestUrns.createTrackUrns(1L, 2L)), playlistSessionSource, 1);
        final TrackSourceInfo trackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();

        assertThat(trackSourceInfo.getSearchQuerySourceInfo()).isNull();
    }

    @Test
    public void shouldPublishPlayQueueChangedEventOnSetNewPlayQueue() {
        when(playQueue.hasItems()).thenReturn(true);
        when(playQueue.size()).thenReturn(1);
        when(playQueue.getPlayQueueItem(0)).thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(3L)));

        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);

        assertThat(eventBus.eventsOn(EventQueue.PLAY_QUEUE)).hasSize(1);
        assertThat(eventBus.firstEventOn(EventQueue.PLAY_QUEUE).isNewQueue()).isTrue();
    }

    @Test
    public void shouldPublishItemChangedEventOnSetNewPlayQueue() {
        final Urn trackUrn = Urn.forTrack(3L);
        when(playQueue.size()).thenReturn(1);
        when(playQueue.hasItems()).thenReturn(true);
        when(playQueue.getPlayQueueItem(0)).thenReturn(TestPlayQueueItem.createTrack(trackUrn));

        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);

        assertThat(eventBus.eventsOn(EventQueue.PLAY_QUEUE)).hasSize(1);
        assertThat(eventBus.firstEventOn(EventQueue.PLAY_QUEUE).isNewQueue()).isTrue();
    }

    @Test
    public void shouldSaveCurrentPositionWhenSettingNonEmptyPlayQueue() {
        playQueue = TestPlayQueue.fromUrns(TestUrns.createTrackUrns(1L, 2L, 3L, 5L), playlistSessionSource);

        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource, 1);

        verify(playQueueOperations).savePlayInfo(1, playlistSessionSource);
    }

    @Test
    public void shouldStoreTracksWhenSettingNewPlayQueue() {
        Urn currentUrn = Urn.forTrack(3L);
        when(playQueue.hasItems()).thenReturn(true);
        when(playQueue.size()).thenReturn(1);
        when(playQueue.getPlayQueueItem(0)).thenReturn(TestPlayQueueItem.createTrack(currentUrn));

        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);
        verify(playQueueOperations).saveQueue(playQueue);
    }

    @Test
    public void shouldNotUpdateCurrentPositionIfPlayQueueIsNull() {
        playQueueManager.saveCurrentPosition();
        verify(playQueueOperations, never()).savePlayInfo(anyInt(), any());
    }

    @Test
    public void saveProgressSavesPlayQueueInfoUsingPlayQueueOperations() {
        Urn currentUrn = Urn.forTrack(3L);
        when(playQueue.hasItems()).thenReturn(true);
        when(playQueue.size()).thenReturn(1);
        when(playQueue.getPlayQueueItem(0)).thenReturn(TestPlayQueueItem.createTrack(currentUrn));
        when(playQueue.shouldPersistItemAt(0)).thenReturn(true);
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);

        verify(playQueueOperations).savePlayInfo(0, playlistSessionSource);
    }

    @Test
    public void saveProgressUpdatesSavePositionWithoutNonPersistentTracks() throws CreateModelException {
        final TrackQueueItem nonPersistedItem = new TrackQueueItem.Builder(Urn.forTrack(4L)).persist(false)
                                                                                            .withPlaybackContext(
                                                                                                    PlaybackContext.create(
                                                                                                            playlistSessionSource))
                                                                                            .build();
        playQueueManager.setNewPlayQueue(createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L)),
                                         playlistSessionSource,
                                         1);
        playQueueManager.replace(playQueueManager.getNextPlayQueueItem(),
                                 Arrays.<PlayQueueItem>asList(nonPersistedItem));
        playQueueManager.setPosition(2, true);

        playQueueManager.saveCurrentPosition();

        InOrder inOrder = Mockito.inOrder(playQueueOperations);
        // Saves first time when we call setNewPlayQueue
        inOrder.verify(playQueueOperations).savePlayInfo(eq(1), eq(playlistSessionSource));
        inOrder.verify(playQueueOperations).savePlayInfo(eq(2), eq(playlistSessionSource));
    }

    @Test
    public void saveProgressIgnoresPositionIfCurrentlyPlayingNonPersistentTrack() throws CreateModelException {
        final TrackQueueItem nonPersistedItem = new TrackQueueItem.Builder(Urn.forTrack(4L)).persist(false)
                                                                                            .withPlaybackContext(
                                                                                                    PlaybackContext.create(
                                                                                                            playlistSessionSource))
                                                                                            .build();
        playQueueManager.setNewPlayQueue(createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L)),
                                         playlistSessionSource,
                                         0);
        playQueueManager.replace(playQueueManager.getNextPlayQueueItem(),
                                 Arrays.<PlayQueueItem>asList(nonPersistedItem));
        playQueueManager.setPosition(1, true);

        playQueueManager.saveCurrentPosition();

        InOrder inOrder = Mockito.inOrder(playQueueOperations);
        // Saves first time when we call setNewPlayQueue
        inOrder.verify(playQueueOperations).savePlayInfo(eq(0), eq(playlistSessionSource));
        inOrder.verify(playQueueOperations).savePlayInfo(eq(1), eq(playlistSessionSource));
    }

    @Test
    public void replaceReplacesPlayQueueItems() {
        playQueueManager.setNewPlayQueue(createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L)),
                                         playlistSessionSource,
                                         1);

        final TrackQueueItem replacementItem1 = TestPlayQueueItem.createTrack(Urn.forTrack(4L));
        final TrackQueueItem replacementItem2 = TestPlayQueueItem.createTrack(Urn.forTrack(4L));
        playQueueManager.replace(playQueueManager.getCurrentPlayQueueItem(),
                                 Arrays.<PlayQueueItem>asList(replacementItem1, replacementItem2));

        assertThat(playQueueManager.getCurrentPlayQueueItem()).isSameAs(replacementItem1);
        assertThat(playQueueManager.getNextPlayQueueItem()).isSameAs(replacementItem2);
    }

    @Test
    public void replacePublishesQueueUpdateEvent() {
        playQueueManager.setNewPlayQueue(createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L)),
                                         playlistSessionSource,
                                         1);

        final TrackQueueItem replacementItem1 = TestPlayQueueItem.createTrack(Urn.forTrack(4L));
        final TrackQueueItem replacementItem2 = TestPlayQueueItem.createTrack(Urn.forTrack(4L));
        playQueueManager.replace(playQueueManager.getCurrentPlayQueueItem(),
                                 Arrays.<PlayQueueItem>asList(replacementItem1, replacementItem2));

        assertThat(eventBus.lastEventOn(EventQueue.PLAY_QUEUE).isQueueUpdate()).isTrue();
    }

    @Test
    public void doesNotChangePlayQueueIfPositionSetToCurrent() {
        playQueueManager.setNewPlayQueue(createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L)), playlistSessionSource);
        playQueueManager.setPosition(1, true);
        verifyZeroInteractions(playQueue);
    }

    @Test
    public void doesNotSendItemChangeEventIfPositionSetToCurrent() throws Exception {
        playQueueManager.setNewPlayQueue(createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L)),
                                         playlistSessionSource,
                                         1);
        assertThat(eventBus.eventsOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM).size()).isEqualTo(1);

        playQueueManager.setPosition(1, true);

        assertThat(eventBus.eventsOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM).size()).isEqualTo(1);
    }

    @Test
    public void shouldPublishItemChangeEventOnSetPosition() {
        playQueueManager.setNewPlayQueue(createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L)), playlistSessionSource);

        playQueueManager.setPosition(2, true);

        final PlayQueueItem actual = eventBus.lastEventOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM).getCurrentPlayQueueItem();
        final TrackQueueItem expected = TestPlayQueueItem.createTrack(Urn.forTrack(3L));
        assertPlayQueueItemsEqual(actual, expected);
    }

    @Test
    public void shouldSetCurrentTriggerToManualIfSettingDifferentPosition() {
        final Map<Urn, Boolean> blockedMap = Collections.singletonMap(Urn.forTrack(2L), false);
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource, blockedMap), playlistSessionSource);

        playQueueManager.autoMoveToNextPlayableItem(); // set to auto trigger

        assertThat(playQueueManager.getCurrentTrackSourceInfo().getIsUserTriggered()).isFalse();

        playQueueManager.setPosition(2, true);

        assertThat(playQueueManager.getCurrentTrackSourceInfo().getIsUserTriggered()).isTrue();
    }

    @Test
    public void shouldNotSetCurrentTriggerToManualIfSettingSamePosition() {
        final Map<Urn, Boolean> blockedMap = Collections.singletonMap(Urn.forTrack(2L), false);
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource, blockedMap), playlistSessionSource);

        playQueueManager.autoMoveToNextPlayableItem(); // set to auto trigger

        assertThat(playQueueManager.getCurrentTrackSourceInfo().getIsUserTriggered()).isFalse();

        playQueueManager.setPosition(1, true);

        assertThat(playQueueManager.getCurrentTrackSourceInfo().getIsUserTriggered()).isFalse();
    }

    @Test
    public void moveToNextPlayableItemSetsUserTriggeredFlagToFalse() {
        final Map<Urn, Boolean> blockedMap = Collections.singletonMap(Urn.forTrack(2L), false);
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource, blockedMap), playlistSessionSource);

        playQueueManager.autoMoveToNextPlayableItem();

        assertThat(playQueueManager.getCurrentTrackSourceInfo().getIsUserTriggered()).isFalse();
    }

    @Test
    public void moveToNextPlayableItemSetsUserTriggeredFlagToTrue() {
        final Map<Urn, Boolean> blockedMap = Collections.singletonMap(Urn.forTrack(2L), false);
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource, blockedMap), playlistSessionSource);

        playQueueManager.moveToNextPlayableItem();

        assertThat(playQueueManager.getCurrentTrackSourceInfo().getIsUserTriggered()).isTrue();
    }

    @Test
    public void moveToPreviousPlayableItemSetsUserTriggeredFlagToTrue() {
        final Map<Urn, Boolean> blockedMap = Collections.singletonMap(Urn.forTrack(2L), false);
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource, blockedMap), playlistSessionSource, 1);

        playQueueManager.moveToPreviousPlayableItem();

        assertThat(playQueueManager.getCurrentTrackSourceInfo().getIsUserTriggered()).isTrue();
    }

    @Test
    public void getNextItemPlayQueueItemReturnsNextItemsPlayQueueItem() {
        playQueueManager.setNewPlayQueue(createPlayQueue(TestUrns.createTrackUrns(1L, 2L)), playlistSessionSource);
        assertPlayQueueItemsEqual(playQueueManager.getNextPlayQueueItem(),
                                  TestPlayQueueItem.createTrack(Urn.forTrack(2L)));
    }

    @Test
    public void removesItemsFromPlayQueue() throws CreateModelException {
        final PlayQueue playQueue = createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L));
        final AudioAdQueueItem adItem = TestPlayQueueItem.createAudioAd(AdFixtures.getAudioAd(Urn.forTrack(123L)));
        playQueue.replaceItem(1, asList(adItem, playQueue.getPlayQueueItem(1)));
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource, 3);

        assertThat(playQueueManager.removeItems(AdUtils.IS_PLAYER_AD_ITEM)).isTrue();

        assertThat(playQueueManager.getQueueSize()).isEqualTo(3);
        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(2);
        assertPlayQueueItemsEqual(playQueueManager.getPlayQueueItemAtPosition(0),
                                  TestPlayQueueItem.createTrack(Urn.forTrack(1L)));
    }

    @Test
    public void returnsTrueWhenItemsRemoved() throws CreateModelException {
        final PlayQueue playQueue = createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L));
        final AudioAdQueueItem adItem = TestPlayQueueItem.createAudioAd(AdFixtures.getAudioAd(Urn.forTrack(123L)));
        playQueue.replaceItem(1, asList(adItem, playQueue.getPlayQueueItem(1)));
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource, 3);

        assertThat(playQueueManager.removeItems(AdUtils.IS_PLAYER_AD_ITEM)).isTrue();

    }

    @Test
    public void returnsFalseWhenNoItemsAreRemoved() throws CreateModelException {
        playQueueManager.setNewPlayQueue(createPlayQueue(TestUrns.createTrackUrns(1L, 2L)), playlistSessionSource);
        final PlayQueueEvent lastEvent = eventBus.lastEventOn(EventQueue.PLAY_QUEUE);

        assertThat(playQueueManager.removeItems(AdUtils.IS_PLAYER_AD_ITEM)).isFalse();

        assertThat(eventBus.lastEventOn(EventQueue.PLAY_QUEUE)).isSameAs(lastEvent);
    }

    @Test
    public void moveToNextPlayableItemGoesToNextItemIfAudioAd() {
        final PlayQueue playQueue = createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L));
        final AudioAdQueueItem adItem = TestPlayQueueItem.createAudioAd(AdFixtures.getAudioAd(Urn.forTrack(2L)));
        playQueue.replaceItem(1, asList(adItem, playQueue.getPlayQueueItem(1)));

        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource, 0);

        assertThat(playQueueManager.autoMoveToNextPlayableItem()).isTrue();
        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(1);

        final CurrentPlayQueueItemEvent actual = eventBus.lastEventOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM);
        assertThat(actual.getCurrentPlayQueueItem().isAudioAd()).isTrue();
    }

    @Test
    public void moveToNextPlayableItemGoesToNextItemIfVideoAd() {
        final PlayQueue playQueue = createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L));
        final VideoAdQueueItem adItem = TestPlayQueueItem.createVideo(AdFixtures.getVideoAd(Urn.forTrack(2L)));
        playQueue.replaceItem(1, asList(adItem, playQueue.getPlayQueueItem(1)));

        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource, 0);

        assertThat(playQueueManager.autoMoveToNextPlayableItem()).isTrue();
        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(1);

        final CurrentPlayQueueItemEvent actual = eventBus.lastEventOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM);
        assertThat(actual.getCurrentPlayQueueItem().isVideoAd()).isTrue();
    }

    @Test
    public void moveToNextPlayableItemGoesToNextPlayableItem() {
        List<PlayQueueItem> playQueueItems = Lists.newArrayList(TestPlayQueueItem.createTrack(Urn.forTrack(1L)),
                                                                TestPlayQueueItem.createTrack(Urn.forTrack(2L)),
                                                                TestPlayQueueItem.createTrack(Urn.forTrack(3L)),
                                                                TestPlayQueueItem.createTrack(Urn.forTrack(4L)));


        when(playQueueItemVerifier.isItemPlayable(playQueueItems.get(0))).thenReturn(false);
        when(playQueueItemVerifier.isItemPlayable(playQueueItems.get(1))).thenReturn(false);
        when(playQueueItemVerifier.isItemPlayable(playQueueItems.get(2))).thenReturn(false);
        when(playQueueItemVerifier.isItemPlayable(playQueueItems.get(3))).thenReturn(true);

        playQueueManager.setNewPlayQueue(PlayQueue.fromPlayQueueItems(playQueueItems), playlistSessionSource);
        playQueueManager.moveToNextPlayableItem();

        verify(playQueueItemVerifier, times(3)).isItemPlayable(any());
        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(3);
    }

    @Test
    public void moveToNextPlayableItemGoesToNextItemIfAllBlocked() {
        final Map<Urn, Boolean> blockedMap = new HashMap<>();
        blockedMap.put(Urn.forTrack(2L), true);
        blockedMap.put(Urn.forTrack(3L), true);
        blockedMap.put(Urn.forTrack(4L), true);

        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L, 4L), playlistSessionSource, blockedMap), playlistSessionSource);

        assertThat(playQueueManager.autoMoveToNextPlayableItem()).isTrue();
        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(1);
        final PlayQueueItem actual = eventBus.lastEventOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM).getCurrentPlayQueueItem();
        assertPlayQueueItemsEqual(actual, TestPlayQueueItem.createBlockedTrack(Urn.forTrack(2L)));
    }

    @Test
    public void moveToNextPlayableItemReturnsFalseIfNoNextItem() {
        playQueueManager.setNewPlayQueue(createPlayQueue(TestUrns.createTrackUrns(1L)), playlistSessionSource);
        assertThat(playQueueManager.autoMoveToNextPlayableItem()).isFalse();
        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(0);
    }

    @Test
    public void moveToPreviousPlayableItemReturnsFalseIfNoPreviousItem() {
        playQueueManager.setNewPlayQueue(createPlayQueue(TestUrns.createTrackUrns(1L)), playlistSessionSource);

        assertThat(playQueueManager.autoMoveToNextPlayableItem()).isFalse();
        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(0);
    }

    @Test
    public void hasPreviousItemReturnsHasPreviousItemFromCurrentPlayQueue() {
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);
        when(playQueue.hasPreviousItem(0)).thenReturn(true);
        assertThat(playQueueManager.hasPreviousItem()).isTrue();
    }

    @Test
    public void shouldNotSetEmptyPlayQueue() {
        playQueueManager.setNewPlayQueue(createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L)),
                                         playlistSessionSource,
                                         1);
        when(playQueueOperations.getLastStoredPlayQueue()).thenReturn(Observable.just(PlayQueue.empty()));

        playQueueManager.loadPlayQueueAsync();

        assertPlayQueueItemsEqual(playQueueManager.getCurrentPlayQueueItem(),
                                  TestPlayQueueItem.createTrack(Urn.forTrack(2L)));
    }


    @Test
    public void shouldNotSetCurrentPositionIfPQIsNotLoaded() {
        when(playQueueOperations.getLastStoredPlayQueue()).thenReturn(Observable.<PlayQueue>empty());
        when(playQueueOperations.getLastStoredPlayPosition()).thenReturn(2);

        playQueueManager.loadPlayQueueAsync();

        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(0);
    }

    @Test
    public void shouldSetCurrentPositionIfPQIsLoaded() {
        PlayQueue playQueue = createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L));
        when(playQueueOperations.getLastStoredPlayQueue()).thenReturn(Observable.just(playQueue));
        when(playQueueOperations.getLastStoredPlayPosition()).thenReturn(2);

        playQueueManager.loadPlayQueueAsync().subscribe(new TestSubscriber<PlayQueue>());

        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(2);
    }

    @Test
    public void loadPlayQueueAsyncLoadsQueueFromLocalStorage() {
        PlayQueue playQueue = createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L));

        when(playQueueOperations.getLastStoredPlaySessionSource()).thenReturn(playlistSessionSource);
        when(playQueueOperations.getLastStoredPlayQueue()).thenReturn(Observable.just(playQueue));
        playQueueManager.loadPlayQueueAsync().subscribe(new TestSubscriber<PlayQueue>());

        expectPlayQueueContentToBeEqual(playQueueManager, playQueue);
    }

    @Test
    public void reloadedPlayQueueIsNotSavedWhenSet() {
        when(playQueueOperations.getLastStoredPlayQueue()).thenReturn(Observable.just(playQueue));
        when(playQueue.isEmpty()).thenReturn(false);
        playQueueManager.loadPlayQueueAsync();
        verify(playQueueOperations, never()).saveQueue(any(PlayQueue.class));
    }

    @Test
    public void clearAllShouldClearPreferences() {
        playQueueManager.clearAll();
        verify(playQueueOperations).clear();
    }

    @Test
    public void clearAllShouldClearStorage() {
        playQueueManager.clearAll();
        verify(playQueueOperations).clear();
    }

    @Test
    public void clearAllShouldSetPlayQueueToEmpty() {
        playQueueManager.setNewPlayQueue(createPlayQueue(TestUrns.createTrackUrns(1L)), playlistSessionSource);
        assertThat(playQueueManager.isQueueEmpty()).isFalse();
        playQueueManager.clearAll();
        assertThat(playQueueManager.isQueueEmpty()).isTrue();
    }

    @Test
    public void clearAllClearsCollectionUrn() {
        playQueueManager.setNewPlayQueue(createPlayQueue(TestUrns.createTrackUrns(1L)), playlistSessionSource);
        assertThat(playQueueManager.isCurrentCollection(Urn.NOT_SET)).isFalse();
        playQueueManager.clearAll();
        assertThat(playQueueManager.isCurrentCollection(Urn.NOT_SET)).isTrue();
    }

    @Test
    public void clearPublishesNewPlayQueueEvents() {
        playQueueManager.clearAll();

        assertThat(eventBus.eventsOn(EventQueue.PLAY_QUEUE)).hasSize(1);
        assertThat(eventBus.firstEventOn(EventQueue.PLAY_QUEUE).isNewQueue()).isTrue();
    }

    @Test
    public void clearPublishesPlayQueueItemChangedEvent() {
        playQueueManager.clearAll();

        final PlayQueueItem actual = eventBus.lastEventOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM).getCurrentPlayQueueItem();
        assertThat(actual).isSameAs(PlayQueueItem.EMPTY);
    }

    @Test
    public void isCurrentCollectionReturnsTrueIfPlaylistUrnMatchesAndCurrentTrackHasNoSource() {
        playlistSessionSource = PlaySessionSource.forPlaylist(Screen.PLAYLIST_DETAILS,
                                                              PLAYLIST_URN,
                                                              USER_URN,
                                                              PLAYLIST_TRACK_COUNT);
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);

        assertThat(playQueueManager.isCurrentCollection(PLAYLIST_URN)).isTrue();
    }

    @Test
    public void shouldReturnWhetherPlaylistIdIsCurrentPlayQueue() {
        playlistSessionSource = PlaySessionSource.forPlaylist(Screen.PLAYLIST_DETAILS,
                                                              PLAYLIST_URN,
                                                              USER_URN,
                                                              PLAYLIST_TRACK_COUNT);
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);

        assertThat(playQueueManager.isCurrentCollection(PLAYLIST_URN)).isTrue();
    }

    @Test
    public void shouldReturnWhetherCurrentPlayQueueIsAPlaylist() {
        playlistSessionSource = PlaySessionSource.forPlaylist(Screen.PLAYLIST_DETAILS,
                                                              PLAYLIST_URN,
                                                              USER_URN,
                                                              PLAYLIST_TRACK_COUNT);
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);

        assertThat(playQueueManager.isCurrentCollection(PLAYLIST_URN)).isTrue();
    }

    @Test
    public void isTrackFromCurrentPromotedItemReturnsTrueIfTrackIsPromoted() {
        PlaySessionSource playSessionSource = new PlaySessionSource("screen");
        playSessionSource.setPromotedSourceInfo(new PromotedSourceInfo("dfp:ad:123",
                                                                       Urn.forTrack(1L),
                                                                       Optional.of(USER_URN),
                                                                       null));

        playQueueManager.setNewPlayQueue(createPlayQueue(playSessionSource), playSessionSource, 0);

        assertThat(playQueueManager.isTrackFromCurrentPromotedItem(Urn.forTrack(1L))).isTrue();
    }

    @Test
    public void isTrackFromCurrentPromotedItemReturnsFalseIfPromotedTrackIsntFirst() {
        PlaySessionSource playSessionSource = new PlaySessionSource("screen");
        playSessionSource.setPromotedSourceInfo(new PromotedSourceInfo("dfp:ad:123",
                                                                       Urn.forTrack(2L),
                                                                       Optional.of(USER_URN),
                                                                       null));

        playQueueManager.setNewPlayQueue(createPlayQueue(playSessionSource), playSessionSource, 1);

        assertThat(playQueueManager.isTrackFromCurrentPromotedItem(Urn.forTrack(2L))).isFalse();
    }

    @Test
    public void isTrackFromCurrentPromotedItemReturnsTrueIfTrackIsInPromotedPlaylist() {
        playlistSessionSource.setPromotedSourceInfo(new PromotedSourceInfo("dfp:ad:123",
                                                                           PLAYLIST_URN,
                                                                           Optional.of(USER_URN),
                                                                           null));

        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L);
        playQueueManager.setNewPlayQueue(createPlayQueue(tracksUrn), playlistSessionSource);

        assertThat(playQueueManager.isTrackFromCurrentPromotedItem(Urn.forTrack(1L))).isTrue();
        assertThat(playQueueManager.isTrackFromCurrentPromotedItem(Urn.forTrack(2L))).isTrue();
    }

    @Test
    public void shouldReturnTrueIfGivenTrackIsCurrentTrack() {
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L);
        playQueueManager.setNewPlayQueue(createPlayQueue(tracksUrn), playlistSessionSource);
        playQueueManager.setPosition(1, true);
        assertThat(playQueueManager.isCurrentTrack(Urn.forTrack(2L))).isTrue();
    }

    @Test
    public void shouldReturnFalseIfGivenTrackIsNotCurrentTrack() {
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L);
        playQueueManager.setNewPlayQueue(createPlayQueue(tracksUrn), playlistSessionSource);
        playQueueManager.setPosition(1, true);
        assertThat(playQueueManager.isCurrentTrack(Urn.forTrack(3L))).isFalse();
    }

    @Test
    public void isCurrentTrackShouldReturnFalseWhenPlayQueueIsEmpty() {
        assertThat(playQueueManager.isCurrentTrack(Urn.forTrack(123L))).isFalse();
    }

    @Test
    public void isCurrentItemShouldReturnFalseWhenPlayQueueIsEmpty() {
        assertThat(playQueueManager.isCurrentItem(Urn.forTrack(123L))).isFalse();
    }

    @Test
    public void isCurrentItemShouldReturnTrueWhenGivenItemIsCurrentItem() {
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L);
        playQueueManager.setNewPlayQueue(createPlayQueue(tracksUrn), playlistSessionSource);
        playQueueManager.setPosition(1, true);
        assertThat(playQueueManager.isCurrentItem(Urn.forTrack(2L))).isTrue();
    }

    @Test
    public void isCurrentItemShouldReturnFalseIfGivenItemIsNotCurrentItem() {
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L);
        playQueueManager.setNewPlayQueue(createPlayQueue(tracksUrn), playlistSessionSource);
        playQueueManager.setPosition(1, true);
        assertThat(playQueueManager.isCurrentItem(Urn.forTrack(3L))).isFalse();
    }

    @Test
    public void isCurrentItemShouldReturnTrueIfGivenVideoItemIsCurrentVideoItem() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(2L));
        final PlayQueue playQueue = createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L));

        final VideoAdQueueItem adItem = TestPlayQueueItem.createVideo(videoAd);
        playQueue.replaceItem(1, asList(adItem, playQueue.getPlayQueueItem(1)));

        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);
        playQueueManager.setPosition(1, true);
        assertThat(playQueueManager.isCurrentItem(videoAd.getAdUrn())).isTrue();
    }

    @Test
    public void getPlayableQueueItemsRemainingReturnsPlayableQueueItemsRemaining() {
        List<PlayQueueItem> playQueueItems = Lists.newArrayList(TestPlayQueueItem.createTrack(Urn.forTrack(1L)),
                                                                TestPlayQueueItem.createTrack(Urn.forTrack(2L)),
                                                                TestPlayQueueItem.createTrack(Urn.forTrack(3L)),
                                                                TestPlayQueueItem.createTrack(Urn.forTrack(4L)));

        when(playQueueItemVerifier.isItemPlayable(playQueueItems.get(0))).thenReturn(false);
        when(playQueueItemVerifier.isItemPlayable(playQueueItems.get(1))).thenReturn(false);
        when(playQueueItemVerifier.isItemPlayable(playQueueItems.get(2))).thenReturn(false);
        when(playQueueItemVerifier.isItemPlayable(playQueueItems.get(3))).thenReturn(true);


        playQueueManager.setNewPlayQueue(PlayQueue.fromPlayQueueItems(playQueueItems), playlistSessionSource);

        // should only return 4, which is marked as downloaded
        assertThat(playQueueManager.getPlayableQueueItemsRemaining()).isEqualTo(1);
    }

    @Test
    public void insertPlaylistTracksInsertsTracksInPlaceOfAllPlaylistInstances() {
        final List<Urn> urns = asList(Urn.forPlaylist(123), Urn.forPlaylist(123));

        playQueueManager.setNewPlayQueue(TestPlayQueue.fromUrns(urns, playlistSessionSource), playlistSessionSource);

        assertThat(playQueueManager.getQueueSize()).isEqualTo(2);

        final Urn track1 = Urn.forTrack(1);
        final Urn track2 = Urn.forTrack(2);
        playQueueManager.insertPlaylistTracks(Urn.forPlaylist(123), asList(track1, track2));

        assertThat(playQueueManager.getQueueSize()).isEqualTo(4);
        assertThat(playQueueManager.getCurrentPlayQueueItem().getUrn()).isEqualTo(track1);
        assertThat(playQueueManager.moveToNextPlayableItem()).isTrue();
        assertThat(playQueueManager.getCurrentPlayQueueItem().getUrn()).isEqualTo(track2);
        assertThat(playQueueManager.moveToNextPlayableItem()).isTrue();
        assertThat(playQueueManager.getCurrentPlayQueueItem().getUrn()).isEqualTo(track1);
        assertThat(playQueueManager.moveToNextPlayableItem()).isTrue();
        assertThat(playQueueManager.getCurrentPlayQueueItem().getUrn()).isEqualTo(track2);
    }

    @Test
    public void insertPlaylistTracksKeepsCurrentPlayQueueItemIfAfterPlaylist() {
        final Urn track1 = Urn.forTrack(1);
        final List<Urn> urns = asList(Urn.forPlaylist(123), Urn.forPlaylist(123), track1);

        playQueueManager.setNewPlayQueue(TestPlayQueue.fromUrns(urns, playlistSessionSource), playlistSessionSource, 2);

        assertThat(playQueueManager.getQueueSize()).isEqualTo(3);
        assertThat(playQueueManager.getCurrentPlayQueueItem().getUrn()).isEqualTo(track1);

        final Urn track2 = Urn.forTrack(2);
        final Urn track3 = Urn.forTrack(3);
        playQueueManager.insertPlaylistTracks(Urn.forPlaylist(123), asList(track2, track3));

        assertThat(playQueueManager.getQueueSize()).isEqualTo(5);
        assertThat(playQueueManager.getCurrentPlayQueueItem().getUrn()).isEqualTo(track1);
    }

    public void removeUpcomingItemDoesNotRemoveCurrentItem() {
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L);
        final int currentPosition = playQueueManager.getCurrentPosition();

        playQueueManager.setNewPlayQueue(TestPlayQueue.fromUrns(tracksUrn, playlistSessionSource),
                                         playlistSessionSource);
        playQueueManager.removeUpcomingItem(playQueueManager.getPlayQueueItemAtPosition(currentPosition), false);

        assertThat(playQueueManager.getQueueSize()).isEqualTo(3);
    }

    @Test
    public void removeUpcomingItemDoesNothingWhenPlayQueueItemNotFound() {
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L);
        final TrackQueueItem trackItem = TestPlayQueueItem.createTrack(Urn.forTrack(123L));

        playQueueManager.setNewPlayQueue(TestPlayQueue.fromUrns(tracksUrn, playlistSessionSource),
                                         playlistSessionSource);
        playQueueManager.removeUpcomingItem(trackItem, false);

        assertThat(playQueueManager.getQueueSize()).isEqualTo(3);
    }

    @Test
    public void removeUpcomingItemRemovesItemIfItsUpcoming() {
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L);

        playQueueManager.setNewPlayQueue(TestPlayQueue.fromUrns(tracksUrn, playlistSessionSource),
                                         playlistSessionSource);
        playQueueManager.removeUpcomingItem(playQueueManager.getPlayQueueItemAtPosition(1), false);

        assertThat(playQueueManager.getQueueSize()).isEqualTo(2);
        assertThat(playQueueManager.getCurrentQueueTrackUrns()).isEqualTo(TestUrns.createTrackUrns(1L, 3L));
    }

    @Test
    public void removeUpcomingItemRemovesItemIfItsUpcomingAndQueuePublishRequested() {
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L);

        playQueueManager.setNewPlayQueue(TestPlayQueue.fromUrns(tracksUrn, playlistSessionSource),
                                         playlistSessionSource);
        playQueueManager.removeUpcomingItem(playQueueManager.getPlayQueueItemAtPosition(1), true);

        assertThat(playQueueManager.getQueueSize()).isEqualTo(2);
        assertThat(playQueueManager.getCurrentQueueTrackUrns()).isEqualTo(TestUrns.createTrackUrns(1L, 3L));
        assertThat(eventBus.lastEventOn(EventQueue.PLAY_QUEUE).isQueueUpdate()).isTrue();
    }

    @Test
    public void getUpcomingPlayQueueItemsReturnsUpcomingItems() {
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L, 4L, 5L);

        playQueueManager.setNewPlayQueue(TestPlayQueue.fromUrns(tracksUrn, playlistSessionSource),
                                         playlistSessionSource,
                                         1);
        assertThat(playQueueManager.getUpcomingPlayQueueItems(2)).isEqualTo(TestUrns.createTrackUrns(3L, 4L));
    }

    @Test
    public void getPreviousPlayQueueItemsReturnsPreviousItems() {
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L, 4L, 5L);

        playQueueManager.setNewPlayQueue(TestPlayQueue.fromUrns(tracksUrn, playlistSessionSource),
                                         playlistSessionSource,
                                         3);
        assertThat(playQueueManager.getPreviousPlayQueueItems(3)).isEqualTo(TestUrns.createTrackUrns(1L, 2L, 3L));
    }

    @Test
    public void insertMultipleUrnsAddsToQueue() {
        final List<Urn> playlistUrns = TestUrns.createTrackUrns(1L, 2L, 3L, 4L, 5L);
        final List<Urn> tracksUrns = TestUrns.createTrackUrns(1L, 2L, 3L, 4L, 5L);
        final PlayQueue playQueueItems = TestPlayQueue.fromUrns(tracksUrns, playlistSessionSource);
        playQueueManager.setNewPlayQueue(playQueueItems, playlistSessionSource, 2);

        playQueueManager.insertNext(playlistUrns);

        assertThat(playQueueManager.getUpcomingPlayQueueItems(5)).isEqualTo(playlistUrns);

        playQueueManager.moveToNextPlayableItem();
        assertThat(playQueueManager.getCurrentTrackSourceInfo()
                                   .getSource()).isEqualTo(DiscoverySource.PLAY_NEXT.value());
    }

    @Test
    public void insertMultipleUrnsSetsSource() {
        final List<Urn> playlistUrns = TestUrns.createTrackUrns(1L, 2L, 3L, 4L, 5L);
        final List<Urn> tracksUrns = TestUrns.createTrackUrns(1L, 2L, 3L, 4L, 5L);
        final PlayQueue playQueueItems = TestPlayQueue.fromUrns(tracksUrns, playlistSessionSource);
        playQueueManager.setNewPlayQueue(playQueueItems, playlistSessionSource, 2);

        playQueueManager.insertNext(playlistUrns);
        playQueueManager.moveToNextPlayableItem();

        assertThat(playQueueManager.getCurrentTrackSourceInfo().getSource())
                .isEqualTo(DiscoverySource.PLAY_NEXT.value());
    }

    @Test
    public void insertMultipleUrnsSavesToQueue() {
        final List<Urn> playlistUrns = TestUrns.createTrackUrns(1L, 2L, 3L, 4L, 5L);
        final List<Urn> tracksUrns = TestUrns.createTrackUrns(1L, 2L, 3L, 4L, 5L);
        final PlayQueue playQueueItems = TestPlayQueue.fromUrns(tracksUrns, playlistSessionSource);
        playQueueManager.setNewPlayQueue(playQueueItems, playlistSessionSource, 2);

        playQueueManager.insertNext(playlistUrns);

        assertThat(playQueueManager.getUpcomingPlayQueueItems(5)).isEqualTo(playlistUrns);
    }

    @Test
    public void insertMultipleUrnsNextPublishesQueueUpdate() {
        final List<Urn> playlistUrns = TestUrns.createTrackUrns(1L, 2L, 3L, 4L, 5L);
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L, 4L, 5L);
        final PlayQueue playQueueItems = TestPlayQueue.fromUrns(tracksUrn, playlistSessionSource);
        playQueueManager.setNewPlayQueue(playQueueItems, playlistSessionSource, 2);

        playQueueManager.insertNext(playlistUrns);

        assertThat(eventBus.eventsOn(EventQueue.PLAY_QUEUE)).containsExactly(
                PlayQueueEvent.fromNewQueue(Urn.forPlaylist(6)),
                PlayQueueEvent.fromQueueInsert(Urn.forPlaylist(6))
        );
    }

    @Test
    public void insertMultipleUrnsNextAllowsContiguousRepeatedTracks() {
        final List<Urn> playlistUrns = TestUrns.createTrackUrns(1L, 2L, 3L, 4L, 5L);
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L, 4L, 5L);
        final PlayQueue playQueueItems = TestPlayQueue.fromUrns(tracksUrn, playlistSessionSource);
        playQueueManager.setNewPlayQueue(playQueueItems, playlistSessionSource, 2);

        playQueueManager.insertNext(playlistUrns);

        assertThat(playQueueManager.getNextPlayQueueItem().getUrn()).isEqualTo(Urn.forTrack(1L));
    }

    @Test
    public void insertNextAddsToQueue() {
        final Urn nextTrackUrn = Urn.forTrack(100L);
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L, 4L, 5L);
        final PlayQueue playQueueItems = TestPlayQueue.fromUrns(tracksUrn, playlistSessionSource);
        playQueueManager.setNewPlayQueue(playQueueItems, playlistSessionSource, 2);

        playQueueManager.insertNext(nextTrackUrn);

        assertThat(playQueueManager.getNextPlayQueueItem().getUrn()).isEqualTo(nextTrackUrn);
    }

    @Test
    public void insertNextSetsSource() {
        final Urn nextTrackUrn = Urn.forTrack(100L);
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L, 4L, 5L);
        final PlayQueue playQueueItems = TestPlayQueue.fromUrns(tracksUrn, playlistSessionSource);
        playQueueManager.setNewPlayQueue(playQueueItems, playlistSessionSource, 2);

        playQueueManager.insertNext(nextTrackUrn);
        playQueueManager.moveToNextPlayableItem();

        assertThat(playQueueManager.getCurrentTrackSourceInfo().getSource())
                .isEqualTo(DiscoverySource.PLAY_NEXT.value());
    }

    @Test
    public void insertNextSavesQueue() {
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L, 4L, 5L);
        final PlayQueue playQueueItems = TestPlayQueue.fromUrns(tracksUrn, playlistSessionSource);
        playQueueManager.setNewPlayQueue(playQueueItems, playlistSessionSource, 2);

        playQueueManager.insertNext(Urn.forTrack(100L));

        final List<Urn> updatedTracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L, 100L, 4L, 5L);
        final PlayQueue updatedPlayQueueItems = TestPlayQueue.fromUrns(updatedTracksUrn, playlistSessionSource);
        verify(playQueueOperations).saveQueue(updatedPlayQueueItems);
    }

    @Test
    public void insertNextPublishesQueueUpdate() {
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L, 4L, 5L);
        final PlayQueue playQueueItems = TestPlayQueue.fromUrns(tracksUrn, playlistSessionSource);
        playQueueManager.setNewPlayQueue(playQueueItems, playlistSessionSource, 2);

        playQueueManager.insertNext(Urn.forTrack(100L));

        assertThat(eventBus.eventsOn(EventQueue.PLAY_QUEUE)).containsExactly(
                PlayQueueEvent.fromNewQueue(Urn.forPlaylist(6)),
                PlayQueueEvent.fromQueueInsert(Urn.forPlaylist(6))
        );
    }

    @Test
    public void insertNextAllowsRepeatedTracks() {
        final Urn nextTrackUrn = Urn.forTrack(1L);
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L, 4L, 5L);
        final PlayQueue playQueueItems = TestPlayQueue.fromUrns(tracksUrn, playlistSessionSource);
        playQueueManager.setNewPlayQueue(playQueueItems, playlistSessionSource, 2);

        playQueueManager.insertNext(nextTrackUrn);

        assertThat(playQueueManager.getNextPlayQueueItem().getUrn()).isEqualTo(nextTrackUrn);
    }

    @Test
    public void insertNextAllowsContiguousRepeatedTracks() {
        final Urn nextTrackUrn = Urn.forTrack(3L);
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L, 4L, 5L);
        final PlayQueue playQueueItems = TestPlayQueue.fromUrns(tracksUrn, playlistSessionSource);
        playQueueManager.setNewPlayQueue(playQueueItems, playlistSessionSource, 2);

        playQueueManager.insertNext(nextTrackUrn);

        assertThat(playQueueManager.getNextPlayQueueItem().getUrn()).isEqualTo(nextTrackUrn);
    }

    @Test
    public void insertNextAddsAfterAllContiguousExplicits() {
        final Urn nextTrackUrn = Urn.forTrack(6L);
        final Urn afterNextTrackUrn = Urn.forTrack(7L);
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L, 4L, 5L);
        final PlayQueue playQueueItems = TestPlayQueue.fromUrns(tracksUrn, playlistSessionSource);
        playQueueManager.setNewPlayQueue(playQueueItems, playlistSessionSource, 2);

        playQueueManager.insertNext(nextTrackUrn);
        playQueueManager.insertNext(afterNextTrackUrn);

        assertThat(playQueueManager.getNextPlayQueueItem().getUrn()).isEqualTo(nextTrackUrn);
        assertThat(playQueueManager.getPlayQueueItemAtPosition(3).getUrn()).isEqualTo(nextTrackUrn);
        assertThat(playQueueManager.getPlayQueueItemAtPosition(4).getUrn()).isEqualTo(afterNextTrackUrn);
    }

    @Test
    public void isNotInRepeatModeByDefault() {
        assertThat(playQueueManager.getRepeatMode()).isEqualTo(PlayQueueManager.RepeatMode.REPEAT_NONE);
    }

    @Test
    public void setsRepeatMode() {
        playQueueManager.setRepeatMode(PlayQueueManager.RepeatMode.REPEAT_ONE);

        assertThat(playQueueManager.getRepeatMode()).isEqualTo(PlayQueueManager.RepeatMode.REPEAT_ONE);
    }

    @Test
    public void whenInRepeatOneModeItRepeats() {
        final PlayQueue playQueue = createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L));
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource, 1);

        playQueueManager.setRepeatMode(PlayQueueManager.RepeatMode.REPEAT_ONE);

        assertThat(playQueueManager.autoMoveToNextPlayableItem()).isTrue();
        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(1);
    }

    @Test
    public void whenInRepeatNoneModeItDoesNotRepeat() {
        final PlayQueue playQueue = createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L));
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource, 1);
        playQueueManager.setRepeatMode(PlayQueueManager.RepeatMode.REPEAT_NONE);

        assertThat(playQueueManager.autoMoveToNextPlayableItem()).isTrue();
        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(2);
    }

    @Test
    public void whenInRepeatOneModePublishesCurrentPosition() {
        final PlayQueue playQueue = createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L));
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource, 1);
        playQueueManager.setRepeatMode(PlayQueueManager.RepeatMode.REPEAT_ONE);

        playQueueManager.autoMoveToNextPlayableItem();

        final List<CurrentPlayQueueItemEvent> actual = eventBus.eventsOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM);
        assertCurrentPlayQueueItemEventsEqual(actual.get(0),
                                              playQueue.getPlayQueueItem(1),
                                              playlistSessionSource.getCollectionUrn(),
                                              1);
    }

    @Test
    public void whenInRepeatAllMovesToNextWhenNotAtTheEnd() {
        when(playQueueItemVerifier.isItemPlayable(any())).thenReturn(true);

        final PlayQueue playQueue = createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L));
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource, 1);
        playQueueManager.setRepeatMode(PlayQueueManager.RepeatMode.REPEAT_ALL);

        assertThat(playQueueManager.autoMoveToNextPlayableItem()).isTrue();
        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(2);
    }

    @Test
    public void whenInRepeatAllMovesToFirstPositionWhenItReachesTheEnd() {
        when(playQueueItemVerifier.isItemPlayable(any())).thenReturn(true);

        final PlayQueue playQueue = createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L));
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource, 2);
        playQueueManager.setRepeatMode(PlayQueueManager.RepeatMode.REPEAT_ALL);

        assertThat(playQueueManager.autoMoveToNextPlayableItem()).isTrue();
        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(0);
    }

    @Test
    public void whenInRepeatAllRepeatsCurrentWhenOnlyOneTrack() {
        final PlayQueue playQueue = createPlayQueue(TestUrns.createTrackUrns(1L));
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource, 0);
        playQueueManager.setRepeatMode(PlayQueueManager.RepeatMode.REPEAT_ALL);

        assertThat(playQueueManager.autoMoveToNextPlayableItem()).isTrue();
        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(0);
    }

    @Test
    public void whenInRepeatAllReturnsFalseWhenQueueIsEmpty() {
        final PlayQueue playQueue = createPlayQueue(Collections.<Urn>emptyList());
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource, 0);
        playQueueManager.setRepeatMode(PlayQueueManager.RepeatMode.REPEAT_ALL);

        assertThat(playQueueManager.autoMoveToNextPlayableItem()).isFalse();
    }

    @Test
    public void whenInRepeatAllMovesToFirstPlayableItem() {
        when(playQueueItemVerifier.isItemPlayable(any())).thenReturn(true);

        final PlayQueue playQueue = createPlayQueue(asList(Urn.forPlaylist(1), Urn.forTrack(2)));
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource, 0);
        playQueueManager.setRepeatMode(PlayQueueManager.RepeatMode.REPEAT_ALL);

        assertThat(playQueueManager.autoMoveToNextPlayableItem()).isTrue();
        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(1);
    }

    @Test
    public void whenLoadingANewPlayQueueResetsRepeatMode() {
        final PlayQueue playQueue = createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L));
        playQueueManager.setRepeatMode(PlayQueueManager.RepeatMode.REPEAT_ONE);

        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource, 0);

        assertThat(playQueueManager.getRepeatMode()).isEqualTo(PlayQueueManager.RepeatMode.REPEAT_NONE);
    }

    @Test
    public void shuffleBroadcastNewPlayQueue() {
        final PlayQueueItem playQueueItem = new TrackQueueItem.Builder(Urn.forTrack(123L)).withPlaybackContext(
                PlaybackContext.create(
                        playlistSessionSource)).build();
        final SimplePlayQueue playQueue = new SimplePlayQueue(singletonList(playQueueItem));
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);

        playQueueManager.shuffle();

        assertThat(eventBus.eventsOn(EventQueue.PLAY_QUEUE)).hasSize(2);
        assertThat(eventBus.firstEventOn(EventQueue.PLAY_QUEUE).isNewQueue()).isTrue();
        assertThat(eventBus.lastEventOn(EventQueue.PLAY_QUEUE).isQueueReorder()).isTrue();
    }

    @Test
    public void canRemoveItemsFromAShuffledPlayQueue() {
        final List<Urn> trackUrns = TestUrns.createTrackUrns(1L, 2L, 3L);
        final PlayQueue playQueue = createPlayQueue(trackUrns);
        final AudioAdQueueItem adItem = TestPlayQueueItem.createAudioAd(AdFixtures.getAudioAd(Urn.forTrack(123L)));
        playQueue.replaceItem(1, asList(adItem, playQueue.getPlayQueueItem(1)));
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource, 3);
        playQueueManager.shuffle();

        assertThat(playQueueManager.removeItems(AdUtils.IS_PLAYER_AD_ITEM)).isTrue();

        assertThat(playQueueManager.getQueueSize()).isEqualTo(playQueueManager.getCurrentQueueTrackUrns().size());
        assertThat(playQueueManager.getCurrentQueueTrackUrns()).containsAll(trackUrns);
    }

    @Test
    public void setCurrentPlayQueueItemWithPositionSetsOnlyForTracks() {
        final PlayQueueItem playlistItem = TestPlayQueueItem.createPlaylist(Urn.forPlaylist(245L));
        final PlayQueueItem trackItem = new TrackQueueItem.Builder(Urn.forTrack(123L)).withPlaybackContext(
                PlaybackContext.create(playlistSessionSource)).build();
        final SimplePlayQueue playQueue = new SimplePlayQueue(asList(playlistItem, trackItem));
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);

        playQueueManager.setCurrentPlayQueueItem(Urn.forTrack(123L), 0);

        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(1);
    }

    @Test
    public void setCurrentPlayQueueItemWithPositionFallbacksToUrn() {
        final PlayQueueItem playlistItem = TestPlayQueueItem.createPlaylist(Urn.forPlaylist(245L));
        final PlayQueueItem trackItem = new TrackQueueItem.Builder(Urn.forTrack(123L)).withPlaybackContext(
                PlaybackContext.create(playlistSessionSource)).build();
        final SimplePlayQueue playQueue = new SimplePlayQueue(asList(playlistItem, trackItem));
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);

        playQueueManager.setCurrentPlayQueueItem(Urn.forTrack(123L), -1);

        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(1);
    }

    @Test
    public void setCurrentPlayQueueItemWithSameTrackMultipleTimes() {
        final PlayQueueItem playlistItem = TestPlayQueueItem.createPlaylist(Urn.forPlaylist(245L));
        final PlayQueueItem trackItem = new TrackQueueItem.Builder(Urn.forTrack(123L)).withPlaybackContext(
                PlaybackContext.create(playlistSessionSource)).build();
        final List<PlayQueueItem> playQueueItems = asList(playlistItem,
                                                          trackItem,
                                                          trackItem,
                                                          playlistItem,
                                                          trackItem);
        final SimplePlayQueue playQueue = new SimplePlayQueue(playQueueItems);
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);

        playQueueManager.setCurrentPlayQueueItem(Urn.forTrack(123L), 1);

        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(2);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionOnInsert1() {
        playQueueManager.setNewPlayQueue(PlayQueue.empty(), PlaySessionSource.EMPTY);
        playQueueManager.insertNext(Urn.NOT_SET);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionOnInsertMany() {
        playQueueManager.setNewPlayQueue(PlayQueue.empty(), PlaySessionSource.EMPTY);
        playQueueManager.insertNext(newArrayList(Urn.NOT_SET, Urn.NOT_SET));
    }

    @Test
    public void shouldMoveItems() {
        playQueue = mock(PlayQueue.class);
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);

        playQueueManager.moveItem(0, 1);

        verify(playQueue, times(1)).moveItem(eq(0), eq(1));
    }

    @Test
    public void shouldPublishEventsWhenItemsMoved() {
        final PlayQueueItem item1 = new TrackQueueItem.Builder(Urn.forTrack(123L)).withPlaybackContext(
                PlaybackContext.create(playlistSessionSource)).build();
        final PlayQueueItem item2 = new TrackQueueItem.Builder(Urn.forTrack(124L)).withPlaybackContext(
                PlaybackContext.create(playlistSessionSource)).build();
        final SimplePlayQueue playQueue = new SimplePlayQueue(newArrayList(item1, item2));
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);

        playQueueManager.moveItem(0, 1);

        assertThat(eventBus.eventsOn(EventQueue.PLAY_QUEUE)).containsExactly(
                PlayQueueEvent.fromNewQueue(PLAYLIST_URN),
                PlayQueueEvent.fromQueueUpdateMoved(PLAYLIST_URN));
    }

    @Test
    public void shouldInsertItemAtPosition() {
        PlayQueueItem playQueueItem = PlayQueueItem.EMPTY;
        playQueue = mock(PlayQueue.class);
        playQueueManager.setNewPlayQueue(playQueue,
                                         PlaySessionSource.forArtist(Screen.ACTIVITIES, Urn.NOT_SET));

        playQueueManager.insertItemAtPosition(5, playQueueItem);

        verify(playQueue).insertPlayQueueItem(5, playQueueItem);
    }

    @Test
    public void shouldPublishEventsWhenItemsInserted() {
        final PlayQueueItem playQueueItem = PlayQueueItem.EMPTY;
        final PlayQueueItem item1 = new TrackQueueItem.Builder(Urn.forTrack(123L)).withPlaybackContext(
                PlaybackContext.create(playlistSessionSource)).build();
        final PlayQueueItem item2 = new TrackQueueItem.Builder(Urn.forTrack(124L)).withPlaybackContext(
                PlaybackContext.create(playlistSessionSource)).build();
        final SimplePlayQueue playQueue = new SimplePlayQueue(newArrayList(item1, item2));
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);

        playQueueManager.insertItemAtPosition(2, playQueueItem);

        assertThat(eventBus.eventsOn(EventQueue.PLAY_QUEUE)).containsExactly(
                PlayQueueEvent.fromNewQueue(PLAYLIST_URN),
                PlayQueueEvent.fromQueueInsert(PLAYLIST_URN));
    }

    @Test
    public void shouldSaveQueueAfterItemRemoval() {
        final PlayQueueItem item1 = new TrackQueueItem.Builder(Urn.forTrack(123L)).withPlaybackContext(
                PlaybackContext.create(playlistSessionSource)).build();
        final PlayQueueItem item2 = new TrackQueueItem.Builder(Urn.forTrack(124L)).withPlaybackContext(
                PlaybackContext.create(playlistSessionSource)).build();
        final SimplePlayQueue playQueue = new SimplePlayQueue(newArrayList(item1, item2));

        playQueueManager.setNewPlayQueue(playQueue,
                                         PlaySessionSource.forArtist(Screen.ACTIVITIES, Urn.NOT_SET));

        Mockito.reset(playQueueOperations);
        when(playQueueOperations.saveQueue(any(PlayQueue.class))).thenReturn(Observable.just(new TxnResult()));

        playQueueManager.removeItem(item1);
        assertPlayQueueSaved(new SimplePlayQueue(singletonList(item2)));
    }

    @Test
    public void shouldReturnExplictPlayQueueItems() {
        final PlayQueueItem item1 = new TrackQueueItem.Builder(Urn.forTrack(123L)).withPlaybackContext(
                PlaybackContext.create(playlistSessionSource)).build();
        final PlayQueueItem item2 = new TrackQueueItem.Builder(Urn.forTrack(124L)).withPlaybackContext(
                PlaybackContext.create(PlaybackContext.Bucket.EXPLICIT)).build();
        final SimplePlayQueue playQueue = new SimplePlayQueue(newArrayList(item1, item2));

        playQueueManager.setNewPlayQueue(playQueue,
                                         PlaySessionSource.forArtist(Screen.ACTIVITIES, Urn.NOT_SET));
        Mockito.reset(playQueueOperations);

        assertThat(playQueueManager.getUpcomingExplicitQueueItems().size()).isEqualTo(1);
        assertThat(playQueueManager.getUpcomingExplicitQueueItems().get(0).getUrn().getNumericId()).isEqualTo(124L);
    }

    @Test
    public void shouldReturnOnlyUpcomingPlayQueueItems() {
        final PlayQueueItem item1 = new TrackQueueItem.Builder(Urn.forTrack(123L)).withPlaybackContext(
                PlaybackContext.create(playlistSessionSource)).build();
        final PlayQueueItem item2 = new TrackQueueItem.Builder(Urn.forTrack(124L)).withPlaybackContext(
                PlaybackContext.create(PlaybackContext.Bucket.EXPLICIT)).build();
        final PlayQueueItem item3 = new TrackQueueItem.Builder(Urn.forTrack(125L)).withPlaybackContext(
                PlaybackContext.create(PlaybackContext.Bucket.EXPLICIT)).build();
        final SimplePlayQueue playQueue = new SimplePlayQueue(newArrayList(item1, item2, item3));

        playQueueManager.setNewPlayQueue(playQueue,
                                         PlaySessionSource.forArtist(Screen.ACTIVITIES, Urn.NOT_SET));
        playQueueManager.setCurrentPlayQueueItem(item1);
        Mockito.reset(playQueueOperations);

        assertThat(playQueueManager.getUpcomingExplicitQueueItems().size()).isEqualTo(2);
        assertThat(playQueueManager.getUpcomingExplicitQueueItems().get(0).getUrn().getNumericId()).isEqualTo(124L);
        assertThat(playQueueManager.getUpcomingExplicitQueueItems().get(1).getUrn().getNumericId()).isEqualTo(125L);
    }

    @Test
    public void shouldNotAutoPlay() {
        final PlayQueueItem item1 = new TrackQueueItem.Builder(Urn.forTrack(123L)).withPlaybackContext(
                PlaybackContext.create(playlistSessionSource)).build();
        final PlayQueueItem item2 = new TrackQueueItem.Builder(Urn.forTrack(124L)).withPlaybackContext(
                PlaybackContext.create(PlaybackContext.Bucket.AUTO_PLAY)).build();
        final PlayQueueItem item3 = new TrackQueueItem.Builder(Urn.forTrack(125L)).withPlaybackContext(
                PlaybackContext.create(PlaybackContext.Bucket.AUTO_PLAY)).build();
        final SimplePlayQueue playQueue = new SimplePlayQueue(newArrayList(item1, item2, item3));
        playQueueManager.setNewPlayQueue(playQueue,
                                         PlaySessionSource.forArtist(Screen.ACTIVITIES, Urn.NOT_SET));

        playQueueManager.setCurrentPlayQueueItem(item2);
        playQueueManager.setAutoPlay(false);

        assertThat(playQueueManager.autoMoveToNextPlayableItem()).isFalse();
    }

    @Test
    public void shouldAutoPlay() {
        final PlayQueueItem item1 = new TrackQueueItem.Builder(Urn.forTrack(123L)).withPlaybackContext(
                PlaybackContext.create(playlistSessionSource)).build();
        final PlayQueueItem item2 = new TrackQueueItem.Builder(Urn.forTrack(124L)).withPlaybackContext(
                PlaybackContext.create(PlaybackContext.Bucket.EXPLICIT)).build();
        final PlayQueueItem item3 = new TrackQueueItem.Builder(Urn.forTrack(125L)).withPlaybackContext(
                PlaybackContext.create(PlaybackContext.Bucket.EXPLICIT)).played(true).build();
        final SimplePlayQueue playQueue = new SimplePlayQueue(newArrayList(item1, item2, item3));
        playQueueManager.setNewPlayQueue(playQueue,
                                         PlaySessionSource.forArtist(Screen.ACTIVITIES, Urn.NOT_SET));

        playQueueManager.setCurrentPlayQueueItem(item2);
        playQueueManager.setAutoPlay(false);

        assertThat(playQueueManager.autoMoveToNextPlayableItem()).isTrue();
    }

    @Test
    public void shouldNotAutoPlayWhenRepeatingAll() {
        final PlayQueueItem item1 = new TrackQueueItem.Builder(Urn.forTrack(123L)).withPlaybackContext(
                PlaybackContext.create(playlistSessionSource)).build();
        final PlayQueueItem item2 = new TrackQueueItem.Builder(Urn.forTrack(124L)).withPlaybackContext(
                PlaybackContext.create(PlaybackContext.Bucket.EXPLICIT)).build();
        final PlayQueueItem item3 = new TrackQueueItem.Builder(Urn.forTrack(125L)).withPlaybackContext(
                PlaybackContext.create(PlaybackContext.Bucket.AUTO_PLAY)).build();
        final SimplePlayQueue playQueue = new SimplePlayQueue(newArrayList(item1, item2, item3));
        playQueueManager.setNewPlayQueue(playQueue,
                                         PlaySessionSource.forArtist(Screen.ACTIVITIES, Urn.NOT_SET));

        playQueueManager.setRepeatMode(PlayQueueManager.RepeatMode.REPEAT_ALL);
        playQueueManager.setCurrentPlayQueueItem(item2);
        playQueueManager.autoMoveToNextPlayableItem();
        playQueueManager.autoMoveToNextPlayableItem();

        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(1);
    }

    @Test
    public void shouldStartNewAutoPlayTrack() {
        final PlayQueueItem item1 = new TrackQueueItem.Builder(Urn.forTrack(123L)).withPlaybackContext(
                PlaybackContext.create(playlistSessionSource)).build();
        final PlayQueueItem item2 = new TrackQueueItem.Builder(Urn.forTrack(124L)).withPlaybackContext(
                PlaybackContext.create(PlaybackContext.Bucket.AUTO_PLAY)).played(true).build();
        final PlayQueueItem item3 = new TrackQueueItem.Builder(Urn.forTrack(125L)).withPlaybackContext(
                PlaybackContext.create(PlaybackContext.Bucket.AUTO_PLAY)).build();
        final SimplePlayQueue playQueue = new SimplePlayQueue(newArrayList(item1, item2, item3));
        playQueueManager.setNewPlayQueue(playQueue,
                                         PlaySessionSource.forArtist(Screen.ACTIVITIES, Urn.NOT_SET));

        playQueueManager.moveToNextRecommendationItem();

        assertThat(playQueueManager.getCurrentPlayQueueItem()).isEqualTo(item3);
    }

    @Test
    public void delegatesIndexOfPlayQueueToPlayQueue() {
        final PlayQueueItem item1 = new TrackQueueItem.Builder(Urn.forTrack(123L)).withPlaybackContext(
                PlaybackContext.create(playlistSessionSource)).build();
        final PlayQueueItem item2 = new TrackQueueItem.Builder(Urn.forTrack(124L)).withPlaybackContext(
                PlaybackContext.create(PlaybackContext.Bucket.AUTO_PLAY)).played(true).build();
        final PlayQueueItem item3 = new TrackQueueItem.Builder(Urn.forTrack(125L)).withPlaybackContext(
                PlaybackContext.create(PlaybackContext.Bucket.AUTO_PLAY)).build();
        final SimplePlayQueue playQueue = new SimplePlayQueue(newArrayList(item1, item2, item3));

        playQueueManager.setNewPlayQueue(playQueue, PlaySessionSource.forArtist(Screen.ACTIVITIES, Urn.NOT_SET));

        assertThat(playQueueManager.indexOfPlayQueueItem(item2)).isEqualTo(1);
    }

    private void expectPlayQueueContentToBeEqual(PlayQueueManager playQueueManager, PlayQueue playQueue) {
        assertThat(playQueueManager.getQueueSize()).isEqualTo(playQueue.size());
        for (int i = 0; i < playQueueManager.getQueueSize(); i++) {
            assertThat(playQueueManager.getPlayQueueItemAtPosition(i).isTrack()).isTrue();
            assertThat(playQueueManager.getPlayQueueItemAtPosition(i).getUrn()).isEqualTo(playQueue.getUrn(i));
        }
    }

    @NonNull
    private PlayQueue createPlayQueue(List<Urn> tracksUrn) {
        return TestPlayQueue.fromUrns(tracksUrn, playlistSessionSource);
    }

    @NonNull
    private PlayQueue createPlayQueue(PlaySessionSource playlistSessionSource) {
        return TestPlayQueue.fromUrns(TestUrns.createTrackUrns(1L, 2L), playlistSessionSource);
    }

    private void assertPlayQueueSaved(PlayQueue expected) {
        ArgumentCaptor<PlayQueue> playQueueCaptor = ArgumentCaptor.forClass(PlayQueue.class);
        verify(playQueueOperations).saveQueue(playQueueCaptor.capture());
        final PlayQueue actual = playQueueCaptor.getValue();
        assertPlayQueuesEqual(expected, actual);
    }

    private void assertCurrentPlayQueueItemEventsEqual(CurrentPlayQueueItemEvent actual,
                                                       PlayQueueItem playQueueItem,
                                                       Urn collectionUrn,
                                                       int currentPosition) {
        assertThat(actual.getCollectionUrn()).isEqualTo(collectionUrn);
        assertThat(actual.getPosition()).isEqualTo(currentPosition);
        assertPlayQueueItemsEqual(actual.getCurrentPlayQueueItem(), playQueueItem);
    }
}
