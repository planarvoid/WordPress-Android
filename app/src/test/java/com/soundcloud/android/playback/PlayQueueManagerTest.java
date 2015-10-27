package com.soundcloud.android.playback;

import android.content.SharedPreferences;

import com.soundcloud.android.Consts;
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
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.TestUrns;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.tobedevoured.modelcitizen.CreateModelException;

import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rx.Observable;
import rx.observers.TestSubscriber;

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
        playQueueManager.setPosition(newPosition);

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
        playQueueManager.setPosition(0);

        assertThat(playQueueManager.getUpcomingPositionForUrn(Urn.forTrack(2L))).isEqualTo(1);
    }

    @Test
    public void getUpcomingPositionForUrnReturnsCurrentItem() throws Exception {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L, 2L), playlistSessionSource), playlistSessionSource);
        playQueueManager.setPosition(1);

        assertThat(playQueueManager.getUpcomingPositionForUrn(Urn.forTrack(2L))).isEqualTo(1);
    }

    @Test
    public void getUpcomingPositionForUrnReturnsUpcomingItem() throws Exception {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L, 2L), playlistSessionSource), playlistSessionSource);
        playQueueManager.setPosition(2);

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
    public void shouldPublishTrackChangedEventOnSetNewPlayQueue(){
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
        when(playQueue.shouldPersistTrackAt(0)).thenReturn(true);
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);
        playQueueManager.saveCurrentProgress(123L);

        InOrder inOrder = Mockito.inOrder(playQueueOperations);
        inOrder.verify(playQueueOperations).savePositionInfo(0, currentUrn, playlistSessionSource, 0L);
        inOrder.verify(playQueueOperations).savePositionInfo(0, currentUrn, playlistSessionSource, 123L);
    }

    @Test
    public void saveProgressUpdatesSavePositionWithoutNonPersistentTracks() throws CreateModelException {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource), playlistSessionSource, 1);
        playQueueManager.performPlayQueueUpdateOperations(new PlayQueueManager.InsertAudioOperation(2, Urn.forTrack(2L), PropertySet.create(), false));
        playQueueManager.setPosition(3);

        playQueueManager.saveCurrentProgress(12L);

        InOrder inOrder = Mockito.inOrder(playQueueOperations);
        // Saves first time when we call setNewPlayQueue
        inOrder.verify(playQueueOperations).savePositionInfo(eq(1), any(Urn.class), any(PlaySessionSource.class), anyLong());
        inOrder.verify(playQueueOperations).savePositionInfo(eq(2), any(Urn.class), any(PlaySessionSource.class), anyLong());
    }

    @Test
    public void saveProgressIgnoresPositionIfCurrentlyPlayingNonPersistentTrack() throws CreateModelException {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource), playlistSessionSource, 1);
        playQueueManager.performPlayQueueUpdateOperations(new PlayQueueManager.InsertAudioOperation(2, Urn.forTrack(1L), PropertySet.create(), false));
        playQueueManager.setPosition(2);

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
        playQueueManager.setPosition(1);
        verifyZeroInteractions(playQueue);
    }

    @Test
    public void doesNotSendTrackChangeEventIfPositionSetToCurrent() throws Exception {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource), playlistSessionSource, 1);

        final CurrentPlayQueueItemEvent lastEvent = eventBus.lastEventOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM);
        playQueueManager.setPosition(1);

        assertThat(eventBus.lastEventOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM)).isSameAs(lastEvent);
    }

    @Test
    public void shouldPublishTrackChangeEventOnSetPosition() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource), playlistSessionSource);

        playQueueManager.setPosition(2);

        assertThat(eventBus.lastEventOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM).getCurrentPlayQueueItem()).isEqualTo(TestPlayQueueItem.createTrack(Urn.forTrack(3L)));
    }

    @Test
    public void shouldSetCurrentTriggerToManualIfSettingDifferentPosition() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource), playlistSessionSource);
        playQueueManager.autoNextTrack(); // set to auto trigger

        playQueueManager.setPosition(2);

        assertThat(playQueueManager.getCurrentTrackSourceInfo().getIsUserTriggered()).isTrue();
    }

    @Test
    public void shouldNotSetCurrentTriggerToManualIfSettingSamePosition() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(
                TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource), playlistSessionSource);
        playQueueManager.autoNextTrack(); // set to auto trigger

        playQueueManager.setPosition(1);

        assertThat(playQueueManager.getCurrentTrackSourceInfo().getIsUserTriggered()).isFalse();
    }

    @Test
    public void shouldPublishTrackChangeEventOnPreviousTrack() {
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource, 5);
        when(playQueue.size()).thenReturn(6);
        when(playQueue.hasPreviousTrack(5)).thenReturn(true);
        when(playQueue.getPlayQueueItem(4)).thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(3L)));

        playQueueManager.moveToPreviousTrack();

        assertThat(eventBus.lastEventOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM).getCurrentPlayQueueItem()).isEqualTo(TestPlayQueueItem.createTrack(Urn.forTrack(3L)));
    }

    @Test
    public void shouldMoveToPreviousTrack() {
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource, 5);
        when(playQueue.size()).thenReturn(6);
        when(playQueue.hasPreviousTrack(5)).thenReturn(true);
        when(playQueue.getPlayQueueItem(4)).thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(369L)));

        playQueueManager.moveToPreviousTrack();

        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(4);
    }

    @Test
    public void shouldNotPublishTrackChangeWhenCallingPreviousOnFirstTrack() {
        when(playQueue.hasPreviousTrack(0)).thenReturn(false);

        playQueueManager.moveToPreviousTrack();

        assertThat(eventBus.eventsOn(EventQueue.PLAY_QUEUE)).isEmpty();
    }

    @Test
    public void shouldNotMoveToPreviousTrackIfAtHeadOfQueue() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(1L, 2L), playlistSessionSource), playlistSessionSource);

        playQueueManager.moveToPreviousTrack();

        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(0);
    }

    @Test
    public void moveToPreviousShouldResultInManualTrigger() {
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource, 5);
        when(playQueue.isEmpty()).thenReturn(false);
        when(playQueue.size()).thenReturn(6);
        when(playQueue.hasPreviousTrack(5)).thenReturn(true);
        when(playQueue.getPlayQueueItem(4)).thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(369L)));

        playQueueManager.moveToPreviousTrack();

        assertThat(playQueueManager.getCurrentTrackSourceInfo().getIsUserTriggered()).isTrue();
    }

    @Test
    public void shouldPublishTrackChangeEventOnNextTrack() {
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);
        when(playQueue.size()).thenReturn(2);
        when(playQueue.hasNextTrack(0)).thenReturn(true);
        when(playQueue.getPlayQueueItem(1)).thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(3L)));

        playQueueManager.nextTrack();

        assertThat(eventBus.lastEventOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM).getCurrentPlayQueueItem()).isEqualTo(TestPlayQueueItem.createTrack(Urn.forTrack(3L)));
    }

    @Test
    public void shouldToNextTrackSetUserTriggeredFlagToTrue() {
        when(playQueue.isEmpty()).thenReturn(false);
        when(playQueue.size()).thenReturn(2);
        when(playQueue.hasNextTrack(0)).thenReturn(true);
        when(playQueue.getPlayQueueItem(1)) .thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(369L)));
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);

        playQueueManager.nextTrack();

        assertThat(playQueueManager.getCurrentTrackSourceInfo().getIsUserTriggered()).isTrue();
    }

    @Test
    public void shouldAutoNextTrackSetUserTriggerTrackFlagToFalse() {
        when(playQueue.isEmpty()).thenReturn(false);
        when(playQueue.size()).thenReturn(2);
        when(playQueue.hasNextTrack(0)).thenReturn(true);
        when(playQueue.getPlayQueueItem(1)).thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(369L)));
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);

        playQueueManager.autoNextTrack();

        assertThat(playQueueManager.getCurrentTrackSourceInfo().getIsUserTriggered()).isFalse();
    }

    @Test
    public void shouldSuccessfullyMoveToNextTrack() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(1L, 2L), playlistSessionSource), playlistSessionSource);

        assertThat(playQueueManager.nextTrack()).isTrue();
        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(1);
    }

    @Test
    public void shouldNotMoveToNextTrackIfAtEndOfQueue() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(1L, 2L), playlistSessionSource), playlistSessionSource, 1);

        assertThat(playQueueManager.nextTrack()).isFalse();
        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(1);
    }

    @Test
    public void nextTrackReturnsFalseIfNoNextTrack() {
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);
        when(playQueue.hasNextTrack(0)).thenReturn(false);
        assertThat(playQueueManager.nextTrack()).isFalse();
    }

    @Test
    public void nextTrackReturnsTrueIfHasNextTrack() {
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);
        when(playQueue.size()).thenReturn(2);
        when(playQueue.hasNextTrack(0)).thenReturn(true);
        when(playQueue.getPlayQueueItem(1)).thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(369L)));

        assertThat(playQueueManager.nextTrack()).isTrue();
    }

    @Test
    public void getNextTrackPlayQueueItemReturnsNextTracksPlayQueueItem() {
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
      public void clearElementsThatMatchesThePredicate() throws CreateModelException {
        final PlayQueue playQueue = PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(1L, 2L, 3L), playlistSessionSource);
        final PropertySet metaDataToRemove = PropertySet.create();
        playQueue.insertTrack(1, Urn.forTrack(123L), metaDataToRemove, true);
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource, 3);

        playQueueManager.removeTracksWithMetaData(new Predicate<PropertySet>() {
            @Override
            public boolean apply(@Nullable PropertySet input) {
                return input == metaDataToRemove;
            }
        });

        assertThat(playQueueManager.getQueueSize()).isEqualTo(3);
        assertThat(playQueueManager.getPlayQueueItemAtPosition(0)).isEqualTo(TestPlayQueueItem.createTrack(Urn.forTrack(1L)));
        assertThat(playQueueManager.getCurrentPosition()).isEqualTo(2);
    }

    @Test
    public void publishesQueueChangeEventWhenPredicateIsTrue() throws CreateModelException {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(1L, 2L), playlistSessionSource), playlistSessionSource);

        playQueueManager.removeTracksWithMetaData(new Predicate<PropertySet>() {
            @Override
            public boolean apply(@Nullable PropertySet input) {
                return true;
            }
        }, PlayQueueEvent.fromAudioAdRemoved(Urn.NOT_SET));

        assertThat(eventBus.lastEventOn(EventQueue.PLAY_QUEUE).getKind()).isEqualTo(PlayQueueEvent.AUDIO_AD_REMOVED);
    }

    @Test
    public void publishesQueueChangeEventWhenPredicateIsFalse() throws CreateModelException {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(1L, 2L), playlistSessionSource), playlistSessionSource);
        final PlayQueueEvent lastEvent = eventBus.lastEventOn(EventQueue.PLAY_QUEUE);
        playQueueManager.removeTracksWithMetaData(new Predicate<PropertySet>() {
            @Override
            public boolean apply(@Nullable PropertySet input) {
                return false;
            }
        });

        assertThat(eventBus.lastEventOn(EventQueue.PLAY_QUEUE)).isSameAs(lastEvent);
    }

    @Test
    public void filtersItemsWithMetadata() {
        final PlayQueue playQueue = PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(123L, 456L), playlistSessionSource);
        final PropertySet metaDataToSelect = PropertySet.create();
        final Urn expectedSelectedTrackUrn = Urn.forTrack(789L);
        playQueue.insertTrack(1, expectedSelectedTrackUrn, metaDataToSelect, true);
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource, 0);

        List<PlayQueueItem> playQueueItems = playQueueManager.filterQueueItemsWithMetadata(new Predicate<PropertySet>() {
            @Override
            public boolean apply(@Nullable PropertySet input) {
                return input == metaDataToSelect;
            }
        });

        assertThat(playQueueItems).containsExactly(TestPlayQueueItem.createTrack(expectedSelectedTrackUrn));
    }

    @Test
    public void autoNextReturnsFalseIfNoNextTrack() {
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);
        when(playQueue.hasNextTrack(0)).thenReturn(false);
        assertThat(playQueueManager.autoNextTrack()).isFalse();
    }

    @Test
    public void autoNextReturnsTrueIfHasNextTrack() {
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);
        when(playQueue.size()).thenReturn(2);
        when(playQueue.hasNextTrack(0)).thenReturn(true);
        when(playQueue.getPlayQueueItem(1)).thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(369L)));

        assertThat(playQueueManager.autoNextTrack()).isTrue();
    }

    @Test
    public void hasNextTrackReturnsHasNextTrackFromCurrentPlayQueue() {
        playQueueManager.setNewPlayQueue(playQueue, playlistSessionSource);
        when(playQueue.hasNextTrack(0)).thenReturn(true);
        assertThat(playQueueManager.hasNextTrack()).isTrue();
    }

    @Test
    public void shouldNotPublishTrackChangeWhenCallingNextOnLastTrack() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(1L), playlistSessionSource), playlistSessionSource);
        final CurrentPlayQueueItemEvent lastEvent = eventBus.lastEventOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM);
        playQueueManager.nextTrack();

        assertThat(eventBus.lastEventOn(EventQueue.CURRENT_PLAY_QUEUE_ITEM)).isSameAs(lastEvent);
    }

    @Test
    public void shouldHaveNoLastPositionWhenPlaybackOperationsHasReturnsNoObservable() {
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
        when(playQueueOperations.getLastStoredPlayQueue()).thenReturn(Observable.<PlayQueue>empty());
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
        playQueueManager.setPosition(1);
        assertThat(playQueueManager.isCurrentTrack(Urn.forTrack(2L))).isTrue();
    }

    @Test
    public void shouldReturnFalseIfGivenTrackIsNotCurrentTrack() {
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L);
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(tracksUrn, playlistSessionSource), playlistSessionSource);
        playQueueManager.setPosition(1);
        assertThat(playQueueManager.isCurrentTrack(Urn.forTrack(3L))).isFalse();
    }

    @Test
    public void shouldReturnTrueIfGivenTrackIsTrackAtPosition() {
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L);
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(tracksUrn, playlistSessionSource), playlistSessionSource);
        playQueueManager.setPosition(1);
        assertThat(playQueueManager.isTrackAt(Urn.forTrack(3L), 2)).isTrue();
    }

    @Test
    public void shouldReturnFalseIfGivenTrackIsNotTrackAtPosition() {
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L);
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(tracksUrn, playlistSessionSource), playlistSessionSource);
        playQueueManager.setPosition(1);
        assertThat(playQueueManager.isTrackAt(Urn.forTrack(3L), 0)).isFalse();
    }

    @Test
    public void shouldReturnFalseIfGivenTrackIsNotTrackAtValidPosition() {
        final List<Urn> tracksUrn = TestUrns.createTrackUrns(1L, 2L, 3L);
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(tracksUrn, playlistSessionSource), playlistSessionSource);
        playQueueManager.setPosition(1);
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
