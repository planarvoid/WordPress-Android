package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Consts;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager.QueueUpdateOperation;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.stations.StationTrack;
import com.soundcloud.android.stations.StationsSourceInfo;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.TestUrns;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.observers.TestObserver;
import rx.observers.TestSubscriber;

import android.content.SharedPreferences;

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

    private PlayQueueManager playQueueManager;
    private TestEventBus eventBus = new TestEventBus();

    @Mock private PlayQueue playQueue;
    @Mock private SharedPreferences sharedPreferences;
    @Mock private SharedPreferences.Editor sharedPreferencesEditor;
    @Mock private PlayQueueOperations playQueueOperations;
    @Mock private PolicyOperations policyOperations;

    private PlaySessionSource playlistSessionSource;
    private PlaySessionSource exploreSessionSource;

    private final List<Urn> queueUrns = Arrays.asList(Urn.forTrack(123), Urn.forTrack(124));

    @Before
    public void before() throws CreateModelException {
        playQueueManager = new PlayQueueManager(playQueueOperations, eventBus, policyOperations);

        when(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor);
        when(sharedPreferencesEditor.putString(anyString(), anyString())).thenReturn(sharedPreferencesEditor);
        when(playQueue.isEmpty()).thenReturn(true);
        when(playQueue.copy()).thenReturn(playQueue);
        when(playQueue.getTrackItemUrns()).thenReturn(queueUrns);
        when(policyOperations.updatePolicies(anyListOf(Urn.class))).thenReturn(Observable.<Void>empty());

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
    public void shouldUpdateTrackPoliciesOnNewQueue() {
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);
        verify(policyOperations).updatePolicies(queueUrns);
    }

    @Test
    public void shouldUpdatePositionOnCurrentQueueWhenContentAndSourceAreUnchanged() {
        PlaySessionSource source1 = new PlaySessionSource("screen:something");
        PlayQueue queue1 = PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(1L, 2L, 3L, 5L), source1);
        PlaySessionSource source2 = new PlaySessionSource("screen:something");
        PlayQueue queue2 = PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(1L, 2L, 3L, 5L), source2);

        playQueueManager.setNewPlayQueue(queue1, source1);
        playQueueManager.setNewPlayQueue(queue2, source2, 2);

        final PlayQueueItem playQueueItem = TestPlayQueueItem.createTrack(Urn.forTrack(1L));
        final PlayQueueItem playQueueItem2 = TestPlayQueueItem.createTrack(Urn.forTrack(3L));

        assertThat(eventBus.eventsOn(EventQueue.PLAY_QUEUE)).containsExactly(PlayQueueEvent.fromNewQueue(Urn.NOT_SET));
        assertThat(eventBus.eventsOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM)).containsExactly(CurrentPlayQueueItemEvent.fromNewQueue(playQueueItem, Urn.NOT_SET, 0),
                CurrentPlayQueueItemEvent.fromNewQueue(playQueueItem2, Urn.NOT_SET, 2));
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

        assertThat(playQueueManager.getCurrentPlayQueueItem()).isEqualTo(TestPlayQueueItem.createTrack(Urn.forTrack(5L)));
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
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(tracksUrn, playlistSessionSource), playlistSessionSource);

        assertThat(playQueueManager.getCurrentQueueTrackUrns()).isEqualTo(tracksUrn);
    }

    @Test
    public void hasSameTrackListTrueForMatchingUrns() {
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L);
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(tracksUrn, playlistSessionSource), playlistSessionSource);

        assertThat(playQueueManager.hasSameTrackList(TestUrns.createTrackUrns(1L, 2L, 3L))).isTrue();
    }

    @Test
    public void hasSameTrackListTrueForDifferentOrder() {
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L);
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(tracksUrn, playlistSessionSource), playlistSessionSource);

        assertThat(playQueueManager.hasSameTrackList(TestUrns.createTrackUrns(3L, 2L, 1L))).isFalse();
    }

    @Test
    public void appendUniquePlayQueueItemsAppendsPlayQueueItemsThrowingOutDuplicates() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource), playlistSessionSource);

        playQueueManager.appendUniquePlayQueueItems(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(3L, 4L, 5L), playlistSessionSource));

        assertThat(playQueueManager.getQueueSize()).isEqualTo(5);
        assertThat(playQueueManager.getPlayQueueItemAtPosition(3)).isEqualTo(TestPlayQueueItem.createTrack(Urn.forTrack(4L)));
        assertThat(playQueueManager.getPlayQueueItemAtPosition(4)).isEqualTo(TestPlayQueueItem.createTrack(Urn.forTrack(5L)));
    }

    @Test
    public void appendUniquePlayQueueItemsSavesQueue() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource), playlistSessionSource);

        playQueueManager.appendUniquePlayQueueItems(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(4L, 5L), playlistSessionSource));

        verify(playQueueOperations).saveQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L, 4L, 5L), playlistSessionSource));
    }

    @Test
    public void appendUniquePlayQueueItemsBroadcastsPlayQueueUpdate() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource), playlistSessionSource);

        playQueueManager.appendUniquePlayQueueItems(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(4L, 5L), playlistSessionSource));

        assertThat(eventBus.eventsOn(EventQueue.PLAY_QUEUE)).hasSize(2);
    }

    @Test
    public void appendlayQueueItemsAppendsAllPlayQueueItems() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource), playlistSessionSource);

        playQueueManager.appendPlayQueueItems(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(3L, 4L, 5L), playlistSessionSource));

        assertThat(playQueueManager.getQueueSize()).isEqualTo(6);
        assertThat(playQueueManager.getPlayQueueItemAtPosition(3)).isEqualTo(TestPlayQueueItem.createTrack(Urn.forTrack(3L)));
        assertThat(playQueueManager.getPlayQueueItemAtPosition(4)).isEqualTo(TestPlayQueueItem.createTrack(Urn.forTrack(4L)));
        assertThat(playQueueManager.getPlayQueueItemAtPosition(5)).isEqualTo(TestPlayQueueItem.createTrack(Urn.forTrack(5L)));
    }

    @Test
    public void appendPlayQueueItemsSavesQueue() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource), playlistSessionSource);

        playQueueManager.appendPlayQueueItems(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(4L, 5L), playlistSessionSource));

        verify(playQueueOperations).saveQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L, 4L, 5L), playlistSessionSource));
    }

    @Test
    public void appendPlayQueueItemsBroadcastsPlayQueueUpdate() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource), playlistSessionSource);

        playQueueManager.appendPlayQueueItems(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(4L, 5L), playlistSessionSource));

        assertThat(eventBus.eventsOn(EventQueue.PLAY_QUEUE)).hasSize(2);
    }

    @Test
    public void getCurrentPlayQueueCountReturnsSizeOfCurrentQueue() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource), playlistSessionSource);

        assertThat(playQueueManager.getQueueSize()).isEqualTo(3);
    }

    @Test
    public void isQueueEmptyReturnsTrueIfQueueSizeIsZero() throws Exception {
        playQueueManager.setNewPlayQueue(PlayQueue.empty(), playlistSessionSource);

        assertThat(playQueueManager.isQueueEmpty()).isTrue();
    }

    @Test
    public void isQueueEmptyReturnsFalseIfQueueSizeGreaterThanZero() throws Exception {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource), playlistSessionSource);

        assertThat(playQueueManager.isQueueEmpty()).isFalse();
    }

    @Test
    public void getPlayQueueItemAtPositionReturnsPlayQueueItemIfInQueue() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                 TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource), playlistSessionSource);

        assertThat(playQueueManager.getPlayQueueItemAtPosition(2)).isEqualTo(TestPlayQueueItem.createTrack(Urn.forTrack(3L)));
    }

    @Test
    public void getPlayQueueItemAtPositionReturnsEmptyPlayQueueItemIfInEmptyPQ() {
        playQueueManager.setNewPlayQueue(PlayQueue.empty(), playlistSessionSource);

        assertThat(playQueueManager.getPlayQueueItemAtPosition(2).isEmpty()).isTrue();
    }

    @Test
    public void getPositionForUrnReturnsNotSetForEmptyPlayQueue() throws Exception {
        assertThat(playQueueManager.getPositionForUrn(Urn.forTrack(1L))).isEqualTo(Consts.NOT_SET);
    }

    @Test
    public void getPositionForUrnReturnsNotSetForUrnIfNotInPlayQueue() throws Exception {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource), playlistSessionSource);

        assertThat(playQueueManager.getPositionForUrn(Urn.forTrack(4L))).isEqualTo(Consts.NOT_SET);
    }

    @Test
    public void getPositionForUrnReturnsPositionForUrnIfInPlayQueue() throws Exception {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource), playlistSessionSource);

        assertThat(playQueueManager.getPositionForUrn(Urn.forTrack(2L))).isEqualTo(1);
    }

    @Test
    public void getUpcomingPositionForUrnReturnsNotSetForEmptyPlayQueue() throws Exception {
        assertThat(playQueueManager.getUpcomingPositionForUrn(Urn.forTrack(1L))).isEqualTo(Consts.NOT_SET);
    }

    @Test
    public void getUpcomingPositionForUrnReturnsNotSetForUrnIfNotInPlayQueue() throws Exception {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource), playlistSessionSource);

        assertThat(playQueueManager.getUpcomingPositionForUrn(Urn.forTrack(4L))).isEqualTo(Consts.NOT_SET);
    }

    @Test
    public void getUpcomingPositionForUrnReturnsPositionForUrnIfInPlayQueue() throws Exception {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L, 2L), playlistSessionSource), playlistSessionSource);
        playQueueManager.setPosition(0, true);

        assertThat(playQueueManager.getUpcomingPositionForUrn(Urn.forTrack(2L))).isEqualTo(1);
    }

    @Test
    public void getUpcomingPositionForUrnReturnsCurrentItem() throws Exception {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L, 2L), playlistSessionSource), playlistSessionSource);
        playQueueManager.setPosition(1, true);

        assertThat(playQueueManager.getUpcomingPositionForUrn(Urn.forTrack(2L))).isEqualTo(1);
    }

    @Test
    public void getUpcomingPositionForUrnReturnsUpcomingItem() throws Exception {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L, 2L), playlistSessionSource), playlistSessionSource);
        playQueueManager.setPosition(2, true);

        assertThat(playQueueManager.getUpcomingPositionForUrn(Urn.forTrack(2L))).isEqualTo(3);
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
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(1L, 2L), playlistSessionSource), playlistSessionSource, 1);

        final TrackSourceInfo trackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();
        assertThat(trackSourceInfo.getCollectionUrn()).isEqualTo(PLAYLIST_URN);
        assertThat(trackSourceInfo.getPlaylistPosition()).isEqualTo(1);
    }

    @Test
    public void shouldReturnTrackSourceInfoWithoutReposterSetIfNotSet() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackList(Arrays.asList(Urn.forTrack(1L).toPropertySet()), playlistSessionSource), playlistSessionSource, 0);

        final TrackSourceInfo trackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();
        assertThat(trackSourceInfo.hasReposter()).isFalse();
    }

    @Test
    public void shouldReturnTrackSourceInfoWithReposterSetIfSet() {
        final PropertySet track = Urn.forTrack(1L).toPropertySet();
        track.put(PostProperty.REPOSTER_URN, Urn.forUser(2L));
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackList(Arrays.asList(track), playlistSessionSource), playlistSessionSource, 0);

        final TrackSourceInfo trackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();
        assertThat(trackSourceInfo.hasReposter()).isTrue();
        assertThat(trackSourceInfo.getReposter()).isEqualTo(Urn.forUser(2L));
    }

    @Test
    public void shouldReturnTrackSourceInfoWithExploreTrackingTagIfSet() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L), exploreSessionSource), exploreSessionSource, 1);

        final TrackSourceInfo trackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();
        assertThat(trackSourceInfo.getSource()).isEqualTo("explore");
        assertThat(trackSourceInfo.getSourceVersion()).isEqualTo("1.0");
    }

    @Test
    public void shouldReturnTrackSourceInfoWithQuerySourceInfoIfSet() {
        SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("soundcloud:search:urn"), 5, new Urn("soundcloud:click:123"));
        searchQuerySourceInfo.setQueryResults(new ArrayList<>(Arrays.asList(Urn.forTrack(1), Urn.forTrack(2))));

        playlistSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(1L, 2L), playlistSessionSource), playlistSessionSource, 1);
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
    public void shouldReturnTrackSourceInfoWithoutQuerySourceInfoIfNotSet() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(1L, 2L), playlistSessionSource), playlistSessionSource, 1);
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
        assertThat(eventBus.firstEventOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM).getCurrentPlayQueueItem()).isEqualTo(TestPlayQueueItem.createTrack(trackUrn));
    }

    @Test
    public void shouldSaveCurrentPositionWhenSettingNonEmptyPlayQueue() {
        when(playQueue.hasItems()).thenReturn(true);
        when(playQueue.size()).thenReturn(6);
        when(playQueue.getPlayQueueItem(5)).thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(3L)));

        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource, 5);
        verify(playQueueOperations).saveQueue(playQueue);
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
        playQueueManager.saveCurrentProgress(22L);
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
        playQueueManager.saveCurrentProgress(123L);

        InOrder inOrder = Mockito.inOrder(playQueueOperations);
        inOrder.verify(playQueueOperations).savePositionInfo(0, currentUrn, playlistSessionSource, 0L);
        inOrder.verify(playQueueOperations).savePositionInfo(0, currentUrn, playlistSessionSource, 123L);
    }

    @Test
    public void saveProgressUpdatesSavePositionWithoutNonPersistentTracks() throws CreateModelException {
        final AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(3L));
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource), playlistSessionSource, 1);
        playQueueManager.performPlayQueueUpdateOperations(new PlayQueueManager.InsertAudioOperation(2, Urn.forTrack(2L), audioAd, false));
        playQueueManager.setPosition(3, true);

        playQueueManager.saveCurrentProgress(12L);

        InOrder inOrder = Mockito.inOrder(playQueueOperations);
        // Saves first time when we call setNewPlayQueue
        inOrder.verify(playQueueOperations).savePositionInfo(eq(1), any(Urn.class), any(PlaySessionSource.class), anyLong());
        inOrder.verify(playQueueOperations).savePositionInfo(eq(2), any(Urn.class), any(PlaySessionSource.class), anyLong());
    }

    @Test
    public void saveProgressIgnoresPositionIfCurrentlyPlayingNonPersistentTrack() throws CreateModelException {
        final AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(3L));
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource), playlistSessionSource, 1);
        playQueueManager.performPlayQueueUpdateOperations(new PlayQueueManager.InsertAudioOperation(2, Urn.forTrack(1L), audioAd, false));
        playQueueManager.setPosition(2, true);

        playQueueManager.saveCurrentProgress(12L);

        InOrder inOrder = Mockito.inOrder(playQueueOperations);
        // Saves first time when we call setNewPlayQueue
        inOrder.verify(playQueueOperations).savePositionInfo(eq(1), any(Urn.class), any(PlaySessionSource.class), anyLong());
        inOrder.verify(playQueueOperations).savePositionInfo(eq(2), any(Urn.class), any(PlaySessionSource.class), eq(0L));
    }

    @Test
    public void getPlayProgressInfoReturnsLastSavedProgressInfo() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(123L), playlistSessionSource), playlistSessionSource);
        playQueueManager.saveCurrentProgress(456L);
        assertThat(playQueueManager.wasLastSavedTrack(Urn.forTrack(123L))).isTrue();
        assertThat(playQueueManager.getLastSavedPosition()).isEqualTo(456);
    }

    @Test
    public void doesNotChangePlayQueueIfPositionSetToCurrent() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource), playlistSessionSource);
        playQueueManager.setPosition(1, true);
        verifyZeroInteractions(playQueue);
    }

    @Test
    public void doesNotSendItemChangeEventIfPositionSetToCurrent() throws Exception {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource), playlistSessionSource, 1);

        final CurrentPlayQueueItemEvent lastEvent = eventBus.lastEventOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM);
        playQueueManager.setPosition(1, true);

        assertThat(eventBus.lastEventOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM)).isSameAs(lastEvent);
    }

    @Test
    public void shouldPublishItemChangeEventOnSetPosition() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource), playlistSessionSource);

        playQueueManager.setPosition(2, true);

        assertThat(eventBus.lastEventOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM).getCurrentPlayQueueItem()).isEqualTo(TestPlayQueueItem.createTrack(Urn.forTrack(3L)));
    }

    @Test
    public void shouldSetCurrentTriggerToManualIfSettingDifferentPosition() {
        final Map<Urn, Boolean> blockedMap = Collections.singletonMap(Urn.forTrack(2L), false);
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource, blockedMap), playlistSessionSource);

        playQueueManager.moveToNextPlayableItem(false); // set to auto trigger

        assertThat(playQueueManager.getCurrentTrackSourceInfo().getIsUserTriggered()).isFalse();

        playQueueManager.setPosition(2, true);

        assertThat(playQueueManager.getCurrentTrackSourceInfo().getIsUserTriggered()).isTrue();
    }

    @Test
    public void shouldNotSetCurrentTriggerToManualIfSettingSamePosition() {
        final Map<Urn, Boolean> blockedMap = Collections.singletonMap(Urn.forTrack(2L), false);
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource, blockedMap), playlistSessionSource);

        playQueueManager.moveToNextPlayableItem(false); // set to auto trigger

        assertThat(playQueueManager.getCurrentTrackSourceInfo().getIsUserTriggered()).isFalse();

        playQueueManager.setPosition(1, true);

        assertThat(playQueueManager.getCurrentTrackSourceInfo().getIsUserTriggered()).isFalse();
    }

    @Test
    public void moveToNextPlayableItemSetsUserTriggeredFlagToFalse() {
        final Map<Urn, Boolean> blockedMap = Collections.singletonMap(Urn.forTrack(2L), false);
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource, blockedMap), playlistSessionSource);

        playQueueManager.moveToNextPlayableItem(false);

        assertThat(playQueueManager.getCurrentTrackSourceInfo().getIsUserTriggered()).isFalse();
    }

    @Test
    public void moveToNextPlayableItemSetsUserTriggeredFlagToTrue() {
        final Map<Urn, Boolean> blockedMap = Collections.singletonMap(Urn.forTrack(2L), false);
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource, blockedMap), playlistSessionSource);

        playQueueManager.moveToNextPlayableItem(true);

        assertThat(playQueueManager.getCurrentTrackSourceInfo().getIsUserTriggered()).isTrue();
    }

    @Test
    public void moveToPreviousPreviousPlayableItemSetsUserTriggeredFlagToFalse() {
        final Map<Urn, Boolean> blockedMap = Collections.singletonMap(Urn.forTrack(2L), false);
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource, blockedMap), playlistSessionSource, 2);

        playQueueManager.moveToPreviousPlayableItem(false);

        assertThat(playQueueManager.getCurrentTrackSourceInfo().getIsUserTriggered()).isFalse();
    }

    @Test
    public void moveToPreviousPlayableItemSetsUserTriggeredFlagToTrue() {
        final Map<Urn, Boolean> blockedMap = Collections.singletonMap(Urn.forTrack(2L), false);
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource, blockedMap), playlistSessionSource, 2);

        playQueueManager.moveToPreviousPlayableItem(true);

        assertThat(playQueueManager.getCurrentTrackSourceInfo().getIsUserTriggered()).isTrue();
    }

    @Test
    public void getNextItemPlayQueueItemReturnsNextItemsPlayQueueItem() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L), playlistSessionSource), playlistSessionSource);
        assertThat(playQueueManager.getNextPlayQueueItem()).isEqualTo(TestPlayQueueItem.createTrack(Urn.forTrack(2L)));
    }

    @Test
    public void performPlayQueueUpdateOperationsExecutesOperationsInOrder() throws CreateModelException {
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource, 1);

        final QueueUpdateOperation queueUpdateOperation1 = mock(QueueUpdateOperation.class);
        final QueueUpdateOperation queueUpdateOperation2 = mock(QueueUpdateOperation.class);
        playQueueManager.performPlayQueueUpdateOperations(queueUpdateOperation1, queueUpdateOperation2);

        final InOrder inOrder = inOrder(queueUpdateOperation1, queueUpdateOperation2);
        inOrder.verify(queueUpdateOperation1).execute(playQueue);
        inOrder.verify(queueUpdateOperation2).execute(playQueue);

    }

    @Test
    public void performPlayQueueUpdateOperationsPublishesQueueChangeEvent() throws Exception {
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource, 1);
        playQueueManager.performPlayQueueUpdateOperations();

        assertThat(eventBus.lastEventOn(EventQueue.PLAY_QUEUE).getKind()).isEqualTo(PlayQueueEvent.QUEUE_UPDATE);
    }

    @Test
      public void clearAdsFromPlayQueue() throws CreateModelException {
        final PlayQueue playQueue = PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource);
        playQueue.insertAudioAd(1, Urn.forTrack(123L), AdFixtures.getAudioAd(Urn.forTrack(123L)), true);
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource, 3);

        playQueueManager.removeAds();

        assertThat(playQueueManager.getQueueSize()).isEqualTo(3);
        assertThat(playQueueManager.getPlayQueueItemAtPosition(0)).isEqualTo(TestPlayQueueItem.createTrack(Urn.forTrack(1L)));
        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(2);
    }

    @Test
    public void publishesQueueChangeEventWhenAdsCleared() throws CreateModelException {
        final PlayQueue playQueue = PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource);
        playQueue.insertAudioAd(1, Urn.forTrack(123L), AdFixtures.getAudioAd(Urn.forTrack(123L)), true);
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource, 3);

        playQueueManager.removeAds(PlayQueueEvent.fromAudioAdRemoved(Urn.NOT_SET));

        assertThat(eventBus.lastEventOn(EventQueue.PLAY_QUEUE).getKind()).isEqualTo(PlayQueueEvent.AUDIO_AD_REMOVED);
    }

    @Test
    public void publishesQueueChangeEventEvenWhenNoAdsCleared() throws CreateModelException {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(1L, 2L), playlistSessionSource), playlistSessionSource);
        final PlayQueueEvent lastEvent = eventBus.lastEventOn(EventQueue.PLAY_QUEUE);

        playQueueManager.removeAds();

        assertThat(eventBus.lastEventOn(EventQueue.PLAY_QUEUE)).isSameAs(lastEvent);
    }

    @Test
    public void filtersAdItems() {
        final PlayQueue playQueue = PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(123L, 456L), playlistSessionSource);
        playQueue.insertAudioAd(1, Urn.forTrack(789), AdFixtures.getAudioAd(Urn.forTrack(789L)), true);
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource, 0);

        List<PlayQueueItem> playQueueItems = playQueueManager.filterAdQueueItems();

        assertThat(playQueueItems).containsExactly(
                TestPlayQueueItem.createTrack(Urn.forTrack(123L)),
                TestPlayQueueItem.createTrack(Urn.forTrack(456L))
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

        assertThat(playQueueManager.moveToNextPlayableItem(false)).isTrue();
        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(2);
        assertThat(eventBus.lastEventOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM).getCurrentPlayQueueItem())
                .isEqualTo(TestPlayQueueItem.createTrack(Urn.forTrack(3L)));
    }

    @Test
    public void moveToNextPlayableItemGoesToLastItemIfAllBlocked() {
        final Map<Urn, Boolean> blockedMap = new HashMap<>();
        blockedMap.put(Urn.forTrack(2L), true);
        blockedMap.put(Urn.forTrack(3L), true);
        blockedMap.put(Urn.forTrack(4L), true);

        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L, 4L), playlistSessionSource, blockedMap), playlistSessionSource);

        assertThat(playQueueManager.moveToNextPlayableItem(false)).isTrue();
        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(3);
        assertThat(eventBus.lastEventOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM).getCurrentPlayQueueItem())
                .isEqualTo(TestPlayQueueItem.createBlockedTrack(Urn.forTrack(4L)));
    }

    @Test
    public void moveToNextPlayableItemReturnsFalseIfNoNextItem() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L), playlistSessionSource), playlistSessionSource);
        assertThat(playQueueManager.moveToNextPlayableItem(false)).isFalse();
        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(0);
    }

    @Test
    public void moveToPreviousPlayableItemGoesToPreviousUnblockedItem() {
        final Map<Urn, Boolean> blockedMap = new HashMap<>();
        blockedMap.put(Urn.forTrack(1L), true);
        blockedMap.put(Urn.forTrack(2L), false);
        blockedMap.put(Urn.forTrack(3L), false);

        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L, 4L), playlistSessionSource, blockedMap), playlistSessionSource, 3);

        assertThat(playQueueManager.moveToPreviousPlayableItem(false)).isTrue();
        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(2);
        assertThat(eventBus.lastEventOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM).getCurrentPlayQueueItem())
                .isEqualTo(TestPlayQueueItem.createTrack(Urn.forTrack(3L)));
    }

    @Test
    public void moveToPreviousPlayableItemGoesToFirstItemIfAllBlocked() {
        final Map<Urn, Boolean> blockedMap = new HashMap<>();
        blockedMap.put(Urn.forTrack(1L), true);
        blockedMap.put(Urn.forTrack(2L), true);
        blockedMap.put(Urn.forTrack(3L), true);

        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L, 4L), playlistSessionSource, blockedMap), playlistSessionSource, 3);

        assertThat(playQueueManager.moveToPreviousPlayableItem(false)).isTrue();
        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(0);
        assertThat(eventBus.lastEventOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM).getCurrentPlayQueueItem())
                .isEqualTo(TestPlayQueueItem.createBlockedTrack(Urn.forTrack(1L)));
    }

    @Test
    public void moveToPreviousPlayableItemReturnsFalseIfNoPreviousItem() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L), playlistSessionSource), playlistSessionSource);
        final TestObserver<Boolean> observer = new TestObserver<>();
        assertThat(playQueueManager.moveToNextPlayableItem(false)).isFalse();
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
    public void shouldHaveNoLastPositionWhenPlaybackOperationsReturnsEmptyObservable() {
        when(playQueueOperations.getLastStoredPlayQueue()).thenReturn(Observable.<PlayQueue>empty());
        playQueueManager.loadPlayQueueAsync();
        assertThat(playQueueManager.getLastSavedPosition()).isEqualTo(Consts.NOT_SET);
    }

    @Test
    public void shouldNotSetEmptyPlayQueue() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource), playlistSessionSource, 1);
        when(playQueueOperations.getLastStoredPlayQueue()).thenReturn(Observable.just(PlayQueue.empty()));
        when(playQueueOperations.getLastStoredPlayingTrackId()).thenReturn(456L);
        when(playQueueOperations.getLastStoredSeekPosition()).thenReturn(400L);

        playQueueManager.loadPlayQueueAsync();

        assertThat(playQueueManager.getCurrentPlayQueueItem()).isEqualTo(TestPlayQueueItem.createTrack(Urn.forTrack(2L)));
    }

    @Test
    public void shouldSetPlayProgressInfoWhenReloadingPlayQueue() {
        when(playQueueOperations.getLastStoredPlayQueue()).thenReturn(Observable.just(PlayQueue.empty()));
        when(playQueueOperations.getLastStoredPlayingTrackId()).thenReturn(456L);
        when(playQueueOperations.getLastStoredSeekPosition()).thenReturn(400L);

        playQueueManager.loadPlayQueueAsync().subscribe(new TestSubscriber<PlayQueue>());
        assertThat(playQueueManager.wasLastSavedTrack(Urn.forTrack(456))).isTrue();
        assertThat(playQueueManager.getLastSavedPosition()).isEqualTo(400L);
    }

    @Test
    public void shouldNotSetCurrentPositionIfPQIsNotLoaded() {
        when(playQueueOperations.getLastStoredPlayQueue()).thenReturn(Observable.<PlayQueue>empty());
        when(playQueueOperations.getLastStoredPlayingTrackId()).thenReturn(456L);
        when(playQueueOperations.getLastStoredSeekPosition()).thenReturn(400L);
        when(playQueueOperations.getLastStoredPlayPosition()).thenReturn(2);

        playQueueManager.loadPlayQueueAsync();

        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(0);
    }

    @Test
    public void shouldSetCurrentPositionIfPQIsNotLoaded() {
        PlayQueue playQueue = PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource);
        when(playQueueOperations.getLastStoredPlayQueue()).thenReturn(Observable.just(playQueue));
        when(playQueueOperations.getLastStoredPlayingTrackId()).thenReturn(456L);
        when(playQueueOperations.getLastStoredSeekPosition()).thenReturn(400L);
        when(playQueueOperations.getLastStoredPlayPosition()).thenReturn(2);

        playQueueManager.loadPlayQueueAsync().subscribe(new TestSubscriber<PlayQueue>());

        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(2);
    }

    @Test
    public void loadPlayQueueAsyncLoadsQueueFromLocalStorage() {
        PlayQueue playQueue = PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource);

        when(playQueueOperations.getLastStoredPlayQueue()).thenReturn(Observable.just(playQueue));
        when(playQueueOperations.getLastStoredPlayingTrackId()).thenReturn(456L);
        when(playQueueOperations.getLastStoredSeekPosition()).thenReturn(400L);
        playQueueManager.loadPlayQueueAsync().subscribe(new TestSubscriber<PlayQueue>());

        expectPlayQueueContentToBeEqual(playQueueManager, playQueue);
    }

    @Test
    public void reloadedPlayQueueIsNotSavedWhenSet() {
        when(playQueueOperations.getLastStoredPlayQueue()).thenReturn(Observable.just(playQueue));
        when(playQueueOperations.getLastStoredPlayingTrackId()).thenReturn(456L);
        when(playQueueOperations.getLastStoredSeekPosition()).thenReturn(400L);
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
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(1L), playlistSessionSource), playlistSessionSource);
        assertThat(playQueueManager.isQueueEmpty()).isFalse();
        playQueueManager.clearAll();
        assertThat(playQueueManager.isQueueEmpty()).isTrue();
    }

    @Test
    public void clearAllClearsCollectionUrn() {
        when(sharedPreferencesEditor.remove(anyString())).thenReturn(sharedPreferencesEditor);
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(1L), playlistSessionSource), playlistSessionSource);
        assertThat(playQueueManager.isCurrentCollection(Urn.NOT_SET)).isFalse();
        playQueueManager.clearAll();
        assertThat(playQueueManager.isCurrentCollection(Urn.NOT_SET)).isTrue();
    }

    @Test
    public void isCurrentPlaylistReturnsTrueIfPlaylistUrnMatchesAndCurrentTrackHasNoSource() {
        playlistSessionSource = PlaySessionSource.forPlaylist(Screen.PLAYLIST_DETAILS, PLAYLIST_URN, USER_URN, PLAYLIST_TRACK_COUNT);
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);

        assertThat(playQueueManager.isCurrentCollection(PLAYLIST_URN)).isTrue();
    }

    @Test
    public void isCurrentPlaylistReturnsFalseIfPlaylistUrnMatchesAndCurrentTrackHasAlternateSource() {
        when(playQueue.isEmpty()).thenReturn(false);
        when(playQueue.size()).thenReturn(1);
        when(playQueue.getPlayQueueItem(0)).thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(369L), "recommender", "1.0"));

        playlistSessionSource = PlaySessionSource.forPlaylist(Screen.PLAYLIST_DETAILS, PLAYLIST_URN, USER_URN, PLAYLIST_TRACK_COUNT);
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);

        assertThat(playQueueManager.isCurrentCollection(PLAYLIST_URN)).isFalse();
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

        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(1L, 2L), playSessionSource), playSessionSource, 0);

        assertThat(playQueueManager.isTrackFromCurrentPromotedItem(Urn.forTrack(1L))).isTrue();
    }

    @Test
    public void isTrackFromCurrentPromotedItemReturnsFalseIfPromotedTrackIsntFirst() {
        PlaySessionSource playSessionSource = new PlaySessionSource("screen");
        playSessionSource.setPromotedSourceInfo(new PromotedSourceInfo("dfp:ad:123", Urn.forTrack(2L), Optional.of(USER_URN), null));

        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(1L, 2L), playSessionSource), playSessionSource, 1);

        assertThat(playQueueManager.isTrackFromCurrentPromotedItem(Urn.forTrack(2L))).isFalse();
    }

    @Test
    public void isTrackFromCurrentPromotedItemReturnsTrueIfTrackIsInPromotedPlaylist() {
        playlistSessionSource.setPromotedSourceInfo(new PromotedSourceInfo("dfp:ad:123", PLAYLIST_URN, Optional.of(USER_URN), null));

        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L);
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(tracksUrn, playlistSessionSource), playlistSessionSource);

        assertThat(playQueueManager.isTrackFromCurrentPromotedItem(Urn.forTrack(1L))).isTrue();
        assertThat(playQueueManager.isTrackFromCurrentPromotedItem(Urn.forTrack(2L))).isTrue();
    }

    @Test
    public void shouldReturnTrueIfGivenTrackIsCurrentTrack() {
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L);
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(tracksUrn, playlistSessionSource), playlistSessionSource);
        playQueueManager.setPosition(1, true);
        assertThat(playQueueManager.isCurrentTrack(Urn.forTrack(2L))).isTrue();
    }

    @Test
    public void shouldReturnFalseIfGivenTrackIsNotCurrentTrack() {
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L);
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(tracksUrn, playlistSessionSource), playlistSessionSource);
        playQueueManager.setPosition(1, true);
        assertThat(playQueueManager.isCurrentTrack(Urn.forTrack(3L))).isFalse();
    }

    @Test
    public void shouldReturnTrueIfGivenTrackIsTrackAtPosition() {
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L);
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(tracksUrn, playlistSessionSource), playlistSessionSource);
        playQueueManager.setPosition(1, true);
        assertThat(playQueueManager.isTrackAt(Urn.forTrack(3L), 2)).isTrue();
    }

    @Test
    public void shouldReturnFalseIfGivenTrackIsNotTrackAtPosition() {
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L);
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(tracksUrn, playlistSessionSource), playlistSessionSource);
        playQueueManager.setPosition(1, true);
        assertThat(playQueueManager.isTrackAt(Urn.forTrack(3L), 0)).isFalse();
    }

    @Test
    public void shouldReturnFalseIfGivenTrackIsNotTrackAtValidPosition() {
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L);
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(tracksUrn, playlistSessionSource), playlistSessionSource);
        playQueueManager.setPosition(1, true);
        assertThat(playQueueManager.isTrackAt(Urn.forTrack(3L), 5)).isFalse();
    }

    @Test
    public void isCurrentTrackShouldReturnFalseWhenPlayQueueIsEmpty() {
        assertThat(playQueueManager.isCurrentTrack(Urn.forTrack(123L))).isFalse();
    }

    private void expectPlayQueueContentToBeEqual(PlayQueueManager playQueueManager, PlayQueue playQueue) {
        assertThat(playQueueManager.getQueueSize()).isEqualTo(playQueue.size());
        for (int i = 0; i < playQueueManager.getQueueSize(); i++) {
            assertThat(playQueueManager.getPlayQueueItemAtPosition(i).isTrack()).isTrue();
            assertThat(playQueueManager.getPlayQueueItemAtPosition(i).getUrn()).isEqualTo(playQueue.getUrn(i));
        }
    }
}
