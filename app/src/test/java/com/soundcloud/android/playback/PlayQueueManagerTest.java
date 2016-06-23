package com.soundcloud.android.playback;

import static com.soundcloud.android.testsupport.PlayQueueAssertions.assertPlayQueueItemsEqual;
import static com.soundcloud.android.testsupport.PlayQueueAssertions.assertPlayQueuesEqual;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.offline.TrackOfflineStateProvider;
import com.soundcloud.android.policies.ApiPolicyInfo;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.stations.StationTrack;
import com.soundcloud.android.stations.StationsSourceInfo;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.TestUrns;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueue;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.observers.TestObserver;
import rx.observers.TestSubscriber;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayQueueManagerTest extends AndroidUnitTest {

    private static final Urn PLAYLIST_URN = Urn.forPlaylist(6L);
    private static final int PLAYLIST_TRACK_COUNT = 2;
    private static final Urn USER_URN = Urn.forUser(4L);

    private PlayQueueManager playQueueManager;
    private TestEventBus eventBus = new TestEventBus();

    @Mock private PlayQueue playQueue;
    @Mock private SharedPreferences sharedPreferences;
    @Mock private SharedPreferences.Editor sharedPreferencesEditor;
    @Mock private PlayQueueOperations playQueueOperations;
    @Mock private PolicyOperations policyOperations;
    @Mock private NetworkConnectionHelper networkConnectionHelper;
    @Mock private TrackOfflineStateProvider offlineStateProvider;

    private PlaySessionSource playlistSessionSource;
    private PlaySessionSource exploreSessionSource;

    private final List<Urn> queueUrns = Arrays.asList(Urn.forTrack(123), Urn.forTrack(124));

    @Before
    public void before() throws CreateModelException {
        playQueueManager = new PlayQueueManager(playQueueOperations, eventBus, networkConnectionHelper, offlineStateProvider);

        when(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor);
        when(sharedPreferencesEditor.putString(anyString(), anyString())).thenReturn(sharedPreferencesEditor);
        when(playQueue.isEmpty()).thenReturn(true);
        when(playQueue.copy()).thenReturn(playQueue);
        when(playQueue.getTrackItemUrns()).thenReturn(queueUrns);
        when(policyOperations.updatePolicies(anyListOf(Urn.class))).thenReturn(
                Observable.<Collection<ApiPolicyInfo>>empty());
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(true);

        when(playQueue.getUrn(3)).thenReturn(Urn.forTrack(369L));

        playlistSessionSource = PlaySessionSource.forPlaylist(Screen.PLAYLIST_DETAILS, PLAYLIST_URN, USER_URN, PLAYLIST_TRACK_COUNT);
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

        final List<CurrentPlayQueueItemEvent> actual = eventBus.eventsOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM);
        assertCurrentPlayQueueItemEventsEqual(actual.get(0), playQueueItem, Urn.NOT_SET, 0);
        assertCurrentPlayQueueItemEventsEqual(actual.get(1), playQueueItem2, Urn.NOT_SET, 2);
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
    public void hasSameTrackListTrueForMatchingUrns() {
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L);
        playQueueManager.setNewPlayQueue(createPlayQueue(tracksUrn), playlistSessionSource);

        assertThat(playQueueManager.hasSameTrackList(TestUrns.createTrackUrns(1L, 2L, 3L))).isTrue();
    }

    @Test
    public void hasSameTrackListFalseForDifferentOrder() {
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L);
        playQueueManager.setNewPlayQueue(createPlayQueue(tracksUrn), playlistSessionSource);

        assertThat(playQueueManager.hasSameTrackList(TestUrns.createTrackUrns(3L, 2L, 1L))).isFalse();
    }

    @Test
    public void appendlayQueueItemsAppendsAllPlayQueueItems() {
        playQueueManager.setNewPlayQueue(createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L)), playlistSessionSource);

        playQueueManager.appendPlayQueueItems(createPlayQueue(TestUrns.createTrackUrns(3L, 4L, 5L)));

        assertThat(playQueueManager.getQueueSize()).isEqualTo(6);
        assertPlayQueueItemsEqual(playQueueManager.getPlayQueueItemAtPosition(3), TestPlayQueueItem.createTrack(Urn.forTrack(3L)));
        assertPlayQueueItemsEqual(playQueueManager.getPlayQueueItemAtPosition(4), TestPlayQueueItem.createTrack(Urn.forTrack(4L)));
        assertPlayQueueItemsEqual(playQueueManager.getPlayQueueItemAtPosition(5), TestPlayQueueItem.createTrack(Urn.forTrack(5L)));
    }

    @Test
    public void appendPlayQueueItemsSavesQueue() {
        playQueueManager.setNewPlayQueue(createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L)), playlistSessionSource);
        Mockito.reset(playQueueOperations);

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
        playQueueManager.setNewPlayQueue(TestPlayQueue.fromUrns(TestUrns.createTrackUrns(1L, 2L), playlistSessionSource), playlistSessionSource, 1);

        final TrackSourceInfo trackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();
        assertThat(trackSourceInfo.getCollectionUrn()).isEqualTo(PLAYLIST_URN);
        assertThat(trackSourceInfo.getPlaylistPosition()).isEqualTo(1);
    }

    @Test
    public void shouldReturnTrackSourceInfoWithoutReposterSetIfNotSet() {
        playQueueManager.setNewPlayQueue(TestPlayQueue.fromTracks(Arrays.asList(Urn.forTrack(1L).toPropertySet()), playlistSessionSource), playlistSessionSource, 0);

        final TrackSourceInfo trackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();
        assertThat(trackSourceInfo.hasReposter()).isFalse();
    }

    @Test
    public void shouldReturnTrackSourceInfoWithReposterSetIfSet() {
        final PropertySet track = Urn.forTrack(1L).toPropertySet();
        track.put(PostProperty.REPOSTER_URN, Urn.forUser(2L));
        playQueueManager.setNewPlayQueue(TestPlayQueue.fromTracks(Arrays.asList(track), playlistSessionSource), playlistSessionSource, 0);

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
        SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("soundcloud:search:urn"), 5, new Urn("soundcloud:click:123"));
        searchQuerySourceInfo.setQueryResults(new ArrayList<>(Arrays.asList(Urn.forTrack(1), Urn.forTrack(2))));

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
        List<StationTrack> tracks = Collections.singletonList(stationTrack);

        playQueueManager.setNewPlayQueue(PlayQueue.fromStation(stationUrn, tracks), PlaySessionSource.forStation(Screen.PLAYLIST_DETAILS, stationUrn));
        final TrackSourceInfo trackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();

        assertThat(trackSourceInfo.getStationsSourceInfo()).isEqualTo(StationsSourceInfo.create(queryUrn));
    }

    @Test
    public void shouldReturnTrackSourceInfoWithStationsSourceInfoIfSetOnAudioAdWithNoQueryUrn() {
        final Urn stationUrn = Urn.forTrackStation(123L);
        final Urn queryUrn = new Urn("soundcloud:radio:123-456");
        final StationTrack stationTrack = StationTrack.create(Urn.forTrack(123L), queryUrn);
        final PlayQueue playQueue = PlayQueue.fromStation(stationUrn, Collections.singletonList(stationTrack));
        playQueue.insertAudioAd(0, Urn.forTrack(321L), AdFixtures.getAudioAd(Urn.forTrack(123L)), false);

        playQueueManager.setNewPlayQueue(playQueue, PlaySessionSource.forStation(Screen.PLAYLIST_DETAILS, stationUrn));
        final TrackSourceInfo trackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();

        assertThat(trackSourceInfo.getStationsSourceInfo()).isEqualTo(StationsSourceInfo.create(Urn.NOT_SET));
    }

    @Test
    public void shouldReturnTrackSourceInfoWithStationsSourceInfoIfSetOnVideoAdWithNoQueryUrn() {
        final Urn stationUrn = Urn.forTrackStation(123L);
        final Urn queryUrn = new Urn("soundcloud:radio:123-456");
        final StationTrack stationTrack = StationTrack.create(Urn.forTrack(123L), queryUrn);
        final PlayQueue playQueue = PlayQueue.fromStation(stationUrn, Collections.singletonList(stationTrack));
        playQueue.insertVideo(0, AdFixtures.getVideoAd(Urn.forTrack(123L)));

        playQueueManager.setNewPlayQueue(playQueue, PlaySessionSource.forStation(Screen.PLAYLIST_DETAILS, stationUrn));
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
        assertThat(eventBus.firstEventOn(EventQueue.PLAY_QUEUE).getKind()).isEqualTo(PlayQueueEvent.NEW_QUEUE);
    }

    @Test
    public void shouldPublishItemChangedEventOnSetNewPlayQueue(){
        final Urn trackUrn = Urn.forTrack(3L);
        when(playQueue.size()).thenReturn(1);
        when(playQueue.hasItems()).thenReturn(true);
        when(playQueue.getPlayQueueItem(0)).thenReturn(TestPlayQueueItem.createTrack(trackUrn));

        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);

        assertThat(eventBus.eventsOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM)).hasSize(1);
        assertPlayQueueItemsEqual(TestPlayQueueItem.createTrack(trackUrn),
                eventBus.firstEventOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM).getCurrentPlayQueueItem());
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
        verifyZeroInteractions(sharedPreferences);
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
        final TrackQueueItem nonPersistedItem = new TrackQueueItem.Builder(Urn.forTrack(4L)).persist(false).build();
        playQueueManager.setNewPlayQueue(createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L)), playlistSessionSource, 1);
        playQueueManager.replace(playQueueManager.getNextPlayQueueItem(), Arrays.<PlayQueueItem>asList(nonPersistedItem));
        playQueueManager.setPosition(2, true);

        playQueueManager.saveCurrentPosition();

        InOrder inOrder = Mockito.inOrder(playQueueOperations);
        // Saves first time when we call setNewPlayQueue
        inOrder.verify(playQueueOperations).savePlayInfo(eq(1), eq(playlistSessionSource));
        inOrder.verify(playQueueOperations).savePlayInfo(eq(2), eq(playlistSessionSource));
    }

    @Test
    public void saveProgressIgnoresPositionIfCurrentlyPlayingNonPersistentTrack() throws CreateModelException {
        final TrackQueueItem nonPersistedItem = new TrackQueueItem.Builder(Urn.forTrack(4L)).persist(false).build();
        playQueueManager.setNewPlayQueue(createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L)), playlistSessionSource, 0);
        playQueueManager.replace(playQueueManager.getNextPlayQueueItem(), Arrays.<PlayQueueItem>asList(nonPersistedItem));
        playQueueManager.setPosition(1, true);

        playQueueManager.saveCurrentPosition();

        InOrder inOrder = Mockito.inOrder(playQueueOperations);
        // Saves first time when we call setNewPlayQueue
        inOrder.verify(playQueueOperations).savePlayInfo(eq(0), eq(playlistSessionSource));
        inOrder.verify(playQueueOperations).savePlayInfo(eq(1), eq(playlistSessionSource));
    }

    @Test
    public void replaceReplacesPlayQueueItems() {
        playQueueManager.setNewPlayQueue(createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L)), playlistSessionSource, 1);

        final TrackQueueItem replacementItem1 = TestPlayQueueItem.createTrack(Urn.forTrack(4L));
        final TrackQueueItem replacementItem2 = TestPlayQueueItem.createTrack(Urn.forTrack(4L));
        playQueueManager.replace(playQueueManager.getCurrentPlayQueueItem(), Arrays.<PlayQueueItem>asList(replacementItem1, replacementItem2));

        assertThat(playQueueManager.getCurrentPlayQueueItem()).isSameAs(replacementItem1);
        assertThat(playQueueManager.getNextPlayQueueItem()).isSameAs(replacementItem2);
    }

    @Test
    public void replacePublishesQueueUpdateEvent() {
        playQueueManager.setNewPlayQueue(createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L)), playlistSessionSource, 1);

        final TrackQueueItem replacementItem1 = TestPlayQueueItem.createTrack(Urn.forTrack(4L));
        final TrackQueueItem replacementItem2 = TestPlayQueueItem.createTrack(Urn.forTrack(4L));
        playQueueManager.replace(playQueueManager.getCurrentPlayQueueItem(), Arrays.<PlayQueueItem>asList(replacementItem1, replacementItem2));

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
        playQueueManager.setNewPlayQueue(createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L)), playlistSessionSource, 1);

        final CurrentPlayQueueItemEvent lastEvent = eventBus.lastEventOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM);
        playQueueManager.setPosition(1, true);

        assertThat(eventBus.lastEventOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM)).isSameAs(lastEvent);
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
      public void clearAdsFromPlayQueue() throws CreateModelException {
        final PlayQueue playQueue = createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L));
        playQueue.insertAudioAd(1, Urn.forTrack(123L), AdFixtures.getAudioAd(Urn.forTrack(123L)), true);
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource, 3);

        playQueueManager.removeAds();

        assertThat(playQueueManager.getQueueSize()).isEqualTo(3);
        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(2);
        assertPlayQueueItemsEqual(playQueueManager.getPlayQueueItemAtPosition(0),
                TestPlayQueueItem.createTrack(Urn.forTrack(1L)));
    }

    @Test
    public void publishesQueueChangeEventWhenAdsCleared() throws CreateModelException {
        final PlayQueue playQueue = createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L));
        playQueue.insertAudioAd(1, Urn.forTrack(123L), AdFixtures.getAudioAd(Urn.forTrack(123L)), true);
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource, 3);

        playQueueManager.removeAds(PlayQueueEvent.fromAdsRemoved(Urn.NOT_SET));

        assertThat(eventBus.lastEventOn(EventQueue.PLAY_QUEUE).getKind()).isEqualTo(PlayQueueEvent.ADS_REMOVED);
    }

    @Test
    public void publishesQueueChangeEventEvenWhenNoAdsCleared() throws CreateModelException {
        playQueueManager.setNewPlayQueue(createPlayQueue(TestUrns.createTrackUrns(1L, 2L)), playlistSessionSource);
        final PlayQueueEvent lastEvent = eventBus.lastEventOn(EventQueue.PLAY_QUEUE);

        playQueueManager.removeAds();

        assertThat(eventBus.lastEventOn(EventQueue.PLAY_QUEUE)).isSameAs(lastEvent);
    }

    @Test
    public void filtersAdItems() {
        final PlayQueue playQueue = createPlayQueue(TestUrns.createTrackUrns(123L, 456L));
        playQueue.insertAudioAd(1, Urn.forTrack(789), AdFixtures.getAudioAd(Urn.forTrack(789L)), true);
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource, 0);

        assertPlayQueueItemsEqual(
                playQueueManager.filterAdQueueItems(),
                Arrays.asList(
                        TestPlayQueueItem.createTrack(Urn.forTrack(123L)),
                        TestPlayQueueItem.createTrack(Urn.forTrack(456L))
                )
        );

    }

    @Test
    public void moveToNextPlayableItemGoesToNextUnblockedItem() {
        final Map<Urn, Boolean> blockedMap = new HashMap<>();
        blockedMap.put(Urn.forTrack(2L), true);
        blockedMap.put(Urn.forTrack(3L), false);
        blockedMap.put(Urn.forTrack(4L), false);

        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L, 4L), playlistSessionSource, blockedMap), playlistSessionSource);

        assertThat(playQueueManager.autoMoveToNextPlayableItem()).isTrue();
        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(2);

        final CurrentPlayQueueItemEvent actual = eventBus.lastEventOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM);
        assertCurrentPlayQueueItemEventsEqual(actual, TestPlayQueueItem.createTrack(Urn.forTrack(3L)), PLAYLIST_URN, 2);
    }

    @Test
    public void moveToNextPlayableItemGoesToNextUnblockedOfflineItemWithNoConnection() {
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(false);
        when(offlineStateProvider.getOfflineState(Urn.forTrack(3L))).thenReturn(OfflineState.DOWNLOADED);
        final Map<Urn, Boolean> blockedMap = new HashMap<>();
        blockedMap.put(Urn.forTrack(2L), true);
        blockedMap.put(Urn.forTrack(3L), false);
        blockedMap.put(Urn.forTrack(4L), false);

        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L, 4L), playlistSessionSource, blockedMap), playlistSessionSource);

        assertThat(playQueueManager.autoMoveToNextPlayableItem()).isTrue();
        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(2);

        final CurrentPlayQueueItemEvent actual = eventBus.lastEventOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM);
        assertCurrentPlayQueueItemEventsEqual(actual, TestPlayQueueItem.createTrack(Urn.forTrack(3L)), PLAYLIST_URN, 2);
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
    public void moveToNextPlayableItemSkipsPlaylist() {
        final List<PlayQueueItem> playQueueItems = Arrays.<PlayQueueItem>asList(
                TestPlayQueueItem.createTrack(Urn.forTrack(1)),
                TestPlayQueueItem.createPlaylist(Urn.forTrack(1)),
                TestPlayQueueItem.createTrack(Urn.forTrack(2))
        );
        playQueueManager.setNewPlayQueue(new PlayQueue(playQueueItems), playlistSessionSource, 0);

        assertThat(playQueueManager.autoMoveToNextPlayableItem()).isTrue();
        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(2);
    }

    @Test
    public void moveToNextPlayableItemReturnsFalseIfNoNextItem() {
        playQueueManager.setNewPlayQueue(createPlayQueue(TestUrns.createTrackUrns(1L)), playlistSessionSource);
        assertThat(playQueueManager.autoMoveToNextPlayableItem()).isFalse();
        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(0);
    }

    @Test
    public void moveToPreviousPlayableItemGoesToPreviousUnblockedItemWhenConnected() {
        final Map<Urn, Boolean> blockedMap = new HashMap<>();
        blockedMap.put(Urn.forTrack(1L), true);
        blockedMap.put(Urn.forTrack(2L), false);
        blockedMap.put(Urn.forTrack(3L), false);

        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L, 4L), playlistSessionSource, blockedMap), playlistSessionSource, 3);

        assertThat(playQueueManager.moveToPreviousPlayableItem()).isTrue();
        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(2);
        final PlayQueueItem actual = eventBus.lastEventOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM).getCurrentPlayQueueItem();
        assertPlayQueueItemsEqual(actual, TestPlayQueueItem.createTrack(Urn.forTrack(3L)));
    }

    @Test
    public void moveToPreviousPlayableItemGoesToPreviousUnblockedOfflineItemWhenNotConnected() {
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(false);
        when(offlineStateProvider.getOfflineState(Urn.forTrack(2L))).thenReturn(OfflineState.DOWNLOADED);

        final Map<Urn, Boolean> blockedMap = new HashMap<>();
        blockedMap.put(Urn.forTrack(1L), true);
        blockedMap.put(Urn.forTrack(2L), false);
        blockedMap.put(Urn.forTrack(3L), false);

        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L, 4L), playlistSessionSource, blockedMap), playlistSessionSource, 3);

        assertThat(playQueueManager.moveToPreviousPlayableItem()).isTrue();
        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(1);
        final PlayQueueItem actual = eventBus.lastEventOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM).getCurrentPlayQueueItem();
        assertPlayQueueItemsEqual(actual, TestPlayQueueItem.createTrack(Urn.forTrack(2L)));
    }

    @Test
    public void moveToPreviousPlayableItemGoesToFirstItemIfAllBlocked() {
        final Map<Urn, Boolean> blockedMap = new HashMap<>();
        blockedMap.put(Urn.forTrack(1L), true);
        blockedMap.put(Urn.forTrack(2L), true);
        blockedMap.put(Urn.forTrack(3L), true);

        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L, 4L), playlistSessionSource, blockedMap), playlistSessionSource, 3);

        assertThat(playQueueManager.moveToPreviousPlayableItem()).isTrue();
        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(0);
        assertPlayQueueItemsEqual(TestPlayQueueItem.createBlockedTrack(Urn.forTrack(1L)),
                eventBus.lastEventOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM).getCurrentPlayQueueItem());
    }

    @Test
    public void moveToPreviousPlayableItemReturnsFalseIfNoPreviousItem() {
        playQueueManager.setNewPlayQueue(createPlayQueue(TestUrns.createTrackUrns(1L)), playlistSessionSource);
        final TestObserver<Boolean> observer = new TestObserver<>();
        assertThat(playQueueManager.autoMoveToNextPlayableItem()).isFalse();
        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(0);
    }

    @Test
    public void hasNextItemReturnsHasNextItemFromCurrentPlayQueue() {
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);
        when(playQueue.hasNextItem(0)).thenReturn(true);
        assertThat(playQueueManager.hasNextItem()).isTrue();
    }

    @Test
    public void hasPreviousItemReturnsHasPreviousItemFromCurrentPlayQueue() {
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);
        when(playQueue.hasPreviousItem(0)).thenReturn(true);
        assertThat(playQueueManager.hasPreviousItem()).isTrue();
    }

    @Test
    public void shouldNotSetEmptyPlayQueue() {
        playQueueManager.setNewPlayQueue(createPlayQueue(TestUrns.createTrackUrns(1L, 2L, 3L)), playlistSessionSource, 1);
        when(playQueueOperations.getLastStoredPlayQueue()).thenReturn(Observable.just(PlayQueue.empty()));

        playQueueManager.loadPlayQueueAsync();

        assertPlayQueueItemsEqual(playQueueManager.getCurrentPlayQueueItem(), TestPlayQueueItem.createTrack(Urn.forTrack(2L)));
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
        when(sharedPreferencesEditor.remove(anyString())).thenReturn(sharedPreferencesEditor);
        playQueueManager.clearAll();
        verify(playQueueOperations).clear();
    }

    @Test
    public void clearAllShouldSetPlayQueueToEmpty() {
        when(sharedPreferencesEditor.remove(anyString())).thenReturn(sharedPreferencesEditor);
        playQueueManager.setNewPlayQueue(createPlayQueue(TestUrns.createTrackUrns(1L)), playlistSessionSource);
        assertThat(playQueueManager.isQueueEmpty()).isFalse();
        playQueueManager.clearAll();
        assertThat(playQueueManager.isQueueEmpty()).isTrue();
    }

    @Test
    public void clearAllClearsCollectionUrn() {
        when(sharedPreferencesEditor.remove(anyString())).thenReturn(sharedPreferencesEditor);
        playQueueManager.setNewPlayQueue(createPlayQueue(TestUrns.createTrackUrns(1L)), playlistSessionSource);
        assertThat(playQueueManager.isCurrentCollection(Urn.NOT_SET)).isFalse();
        playQueueManager.clearAll();
        assertThat(playQueueManager.isCurrentCollection(Urn.NOT_SET)).isTrue();
    }

    @Test
    public void clearPublishesNewPlayQueueEvents() {
        playQueueManager.clearAll();

        assertThat(eventBus.eventsOn(EventQueue.PLAY_QUEUE)).hasSize(1);
        assertThat(eventBus.firstEventOn(EventQueue.PLAY_QUEUE).getKind()).isEqualTo(PlayQueueEvent.NEW_QUEUE);
    }

    @Test
    public void clearPublishesPlayQueueItemChangedEvent() {
        playQueueManager.clearAll();

        final PlayQueueItem actual = eventBus.lastEventOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM).getCurrentPlayQueueItem();
        assertThat(actual).isSameAs(PlayQueueItem.EMPTY);
    }

    @Test
    public void isCurrentCollectionReturnsTrueIfPlaylistUrnMatchesAndCurrentTrackHasNoSource() {
        playlistSessionSource = PlaySessionSource.forPlaylist(Screen.PLAYLIST_DETAILS, PLAYLIST_URN, USER_URN, PLAYLIST_TRACK_COUNT);
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);

        assertThat(playQueueManager.isCurrentCollection(PLAYLIST_URN)).isTrue();
    }

    @Test
    public void shouldReturnWhetherPlaylistIdIsCurrentPlayQueue() {
        playlistSessionSource = PlaySessionSource.forPlaylist(Screen.PLAYLIST_DETAILS, PLAYLIST_URN, USER_URN, PLAYLIST_TRACK_COUNT);
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);

        assertThat(playQueueManager.isCurrentCollection(PLAYLIST_URN)).isTrue();
    }

    @Test
    public void shouldReturnWhetherCurrentPlayQueueIsAPlaylist() {
        playlistSessionSource = PlaySessionSource.forPlaylist(Screen.PLAYLIST_DETAILS, PLAYLIST_URN, USER_URN, PLAYLIST_TRACK_COUNT);
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);

        assertThat(playQueueManager.isCurrentCollection(PLAYLIST_URN)).isTrue();
    }

    @Test
    public void isTrackFromCurrentPromotedItemReturnsTrueIfTrackIsPromoted() {
        PlaySessionSource playSessionSource = new PlaySessionSource("screen");
        playSessionSource.setPromotedSourceInfo(new PromotedSourceInfo("dfp:ad:123", Urn.forTrack(1L), Optional.of(USER_URN), null));

        playQueueManager.setNewPlayQueue(createPlayQueue(playSessionSource), playSessionSource, 0);

        assertThat(playQueueManager.isTrackFromCurrentPromotedItem(Urn.forTrack(1L))).isTrue();
    }

    @Test
    public void isTrackFromCurrentPromotedItemReturnsFalseIfPromotedTrackIsntFirst() {
        PlaySessionSource playSessionSource = new PlaySessionSource("screen");
        playSessionSource.setPromotedSourceInfo(new PromotedSourceInfo("dfp:ad:123", Urn.forTrack(2L), Optional.of(USER_URN), null));

        playQueueManager.setNewPlayQueue(createPlayQueue(playSessionSource), playSessionSource, 1);

        assertThat(playQueueManager.isTrackFromCurrentPromotedItem(Urn.forTrack(2L))).isFalse();
    }

    @Test
    public void isTrackFromCurrentPromotedItemReturnsTrueIfTrackIsInPromotedPlaylist() {
        playlistSessionSource.setPromotedSourceInfo(new PromotedSourceInfo("dfp:ad:123", PLAYLIST_URN, Optional.of(USER_URN), null));

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
        playQueue.insertVideo(1, videoAd);
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);
        playQueueManager.setPosition(1, true);
        assertThat(playQueueManager.isCurrentItem(videoAd.getAdUrn())).isTrue();
    }

    @Test
    public void getPlayableQueueItemsRemainingReturnsPlayableQueueItemsRemaining() {
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(false);
        when(offlineStateProvider.getOfflineState(Urn.forTrack(4L))).thenReturn(OfflineState.DOWNLOADED);
        final Map<Urn, Boolean> blockedMap = new HashMap<>();
        blockedMap.put(Urn.forTrack(3L), true);
        blockedMap.put(Urn.forTrack(4L), false);

        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L, 4L, 5L), playlistSessionSource, blockedMap), playlistSessionSource, 1);

        // should only return 4, which is marked as downloaded
        assertThat(playQueueManager.getPlayableQueueItemsRemaining()).isEqualTo(1);
    }

    @Test
    public void insertPlaylistTracksInsertsTracksInPlaceOfAllPlaylistInstances() {
        final List<Urn> urns = Arrays.asList(Urn.forPlaylist(123), Urn.forPlaylist(123));

        playQueueManager.setNewPlayQueue(TestPlayQueue.fromUrns(urns, playlistSessionSource), playlistSessionSource);

        assertThat(playQueueManager.getQueueSize()).isEqualTo(2);

        final Urn track1 = Urn.forTrack(1);
        final Urn track2 = Urn.forTrack(2);
        playQueueManager.insertPlaylistTracks(Urn.forPlaylist(123), Arrays.asList(track1, track2));

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
        final List<Urn> urns = Arrays.asList(Urn.forPlaylist(123), Urn.forPlaylist(123), track1);

        playQueueManager.setNewPlayQueue(TestPlayQueue.fromUrns(urns, playlistSessionSource), playlistSessionSource, 2);

        assertThat(playQueueManager.getQueueSize()).isEqualTo(3);
        assertThat(playQueueManager.getCurrentPlayQueueItem().getUrn()).isEqualTo(track1);

        final Urn track2 = Urn.forTrack(2);
        final Urn track3 = Urn.forTrack(3);
        playQueueManager.insertPlaylistTracks(Urn.forPlaylist(123), Arrays.asList(track2, track3));

        assertThat(playQueueManager.getQueueSize()).isEqualTo(5);
        assertThat(playQueueManager.getCurrentPlayQueueItem().getUrn()).isEqualTo(track1);
    }

    public void removeUpcomingItemDoesNotRemoveCurrentItem() {
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L);
        final int currentPosition = playQueueManager.getCurrentPosition();

        playQueueManager.setNewPlayQueue(TestPlayQueue.fromUrns(tracksUrn, playlistSessionSource), playlistSessionSource);
        playQueueManager.removeUpcomingItem(playQueueManager.getPlayQueueItemAtPosition(currentPosition), false);

        assertThat(playQueueManager.getQueueSize()).isEqualTo(3);
    }

    @Test
    public void removeUpcomingItemDoesNothingWhenPlayQueueItemNotFound() {
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L);
        final TrackQueueItem trackItem = TestPlayQueueItem.createTrack(Urn.forTrack(123L));

        playQueueManager.setNewPlayQueue(TestPlayQueue.fromUrns(tracksUrn, playlistSessionSource), playlistSessionSource);
        playQueueManager.removeUpcomingItem(trackItem, false);

        assertThat(playQueueManager.getQueueSize()).isEqualTo(3);
    }

    @Test
    public void removeUpcomingItemRemovesItemIfItsUpcoming() {
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L);

        playQueueManager.setNewPlayQueue(TestPlayQueue.fromUrns(tracksUrn, playlistSessionSource), playlistSessionSource);
        playQueueManager.removeUpcomingItem(playQueueManager.getPlayQueueItemAtPosition(1), false);

        assertThat(playQueueManager.getQueueSize()).isEqualTo(2);
        assertThat(playQueueManager.getCurrentQueueTrackUrns()).isEqualTo(TestUrns.createTrackUrns(1L, 3L));
    }

    @Test
    public void removeUpcomingItemRemovesItemIfItsUpcomingAndQueuePublishRequested() {
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L);

        playQueueManager.setNewPlayQueue(TestPlayQueue.fromUrns(tracksUrn, playlistSessionSource), playlistSessionSource);
        playQueueManager.removeUpcomingItem(playQueueManager.getPlayQueueItemAtPosition(1), true);

        assertThat(playQueueManager.getQueueSize()).isEqualTo(2);
        assertThat(playQueueManager.getCurrentQueueTrackUrns()).isEqualTo(TestUrns.createTrackUrns(1L, 3L));
        assertThat(eventBus.lastEventOn(EventQueue.PLAY_QUEUE).isQueueUpdate()).isTrue();
    }

    @Test
    public void getUpcomingPlayQueueItemsReturnsUpcomingItems() {
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L, 4L, 5L);

        playQueueManager.setNewPlayQueue(TestPlayQueue.fromUrns(tracksUrn, playlistSessionSource), playlistSessionSource, 1);
        assertThat(playQueueManager.getUpcomingPlayQueueItems(2)).isEqualTo(TestUrns.createTrackUrns(3L, 4L));
    }

    @Test
    public void getPreviousPlayQueueItemsReturnsPreviousItems() {
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L, 4L, 5L);

        playQueueManager.setNewPlayQueue(TestPlayQueue.fromUrns(tracksUrn, playlistSessionSource), playlistSessionSource, 3);
        assertThat(playQueueManager.getPreviousPlayQueueItems(3)).isEqualTo(TestUrns.createTrackUrns(1L, 2L, 3L));
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
    private PlayQueue createPlayQueue(PlaySessionSource playSessionSource) {
        return TestPlayQueue.fromUrns(TestUrns.createTrackUrns(1L, 2L), playSessionSource);
    }

    private void assertPlayQueueSaved(PlayQueue expected) {
        ArgumentCaptor<PlayQueue> playQueueCaptor = ArgumentCaptor.forClass(PlayQueue.class);
        verify(playQueueOperations).saveQueue(playQueueCaptor.capture());
        final PlayQueue actual = playQueueCaptor.getValue();
        assertPlayQueuesEqual(expected, actual);
    }

    private void assertCurrentPlayQueueItemEventsEqual(CurrentPlayQueueItemEvent actual, PlayQueueItem playQueueItem, Urn collectionUrn, int currentPosition) {
        assertThat(actual.getCollectionUrn()).isEqualTo(collectionUrn);
        assertThat(actual.getPosition()).isEqualTo(currentPosition);
        assertPlayQueueItemsEqual(actual.getCurrentPlayQueueItem(), playQueueItem);
    }
}
