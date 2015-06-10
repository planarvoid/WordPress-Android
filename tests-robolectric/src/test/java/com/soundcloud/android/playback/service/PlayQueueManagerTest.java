package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.TestHelper.createTracksUrn;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.soundcloud.android.Consts;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.propeller.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlayQueueManagerTest {

    private static final String ORIGIN_PAGE = "explore:music:techno";

    private PlayQueueManager playQueueManager;
    private TestEventBus eventBus = new TestEventBus();

    @Mock private Context context;
    @Mock private PlayQueue playQueue;
    @Mock private ScModelManager modelManager;
    @Mock private SharedPreferences sharedPreferences;
    @Mock private SharedPreferences.Editor sharedPreferencesEditor;
    @Mock private PlayQueueOperations playQueueOperations;
    @Mock private PolicyOperations policyOperations;

    private PublicApiPlaylist playlist;
    private PlaySessionSource playSessionSource;

    private final List<Urn> queueUrns = Arrays.asList(Urn.forTrack(123), Urn.forTrack(124));

    @Before
    public void before() throws CreateModelException {
        playQueueManager = new PlayQueueManager(context, playQueueOperations, eventBus, modelManager, policyOperations);

        when(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor);
        when(sharedPreferencesEditor.putString(anyString(), anyString())).thenReturn(sharedPreferencesEditor);
        when(playQueue.isEmpty()).thenReturn(true);
        when(playQueue.copy()).thenReturn(playQueue);
        when(playQueue.getTrackUrns()).thenReturn(queueUrns);
        when(policyOperations.updatePolicies(anyListOf(Urn.class))).thenReturn(Observable.<Void>empty());

        when(playQueue.getUrn(3)).thenReturn(Urn.forTrack(369L));

        playlist = ModelFixtures.create(PublicApiPlaylist.class);
        playSessionSource = new PlaySessionSource(ORIGIN_PAGE);
        playSessionSource.setPlaylist(playlist.getUrn(), playlist.getUserUrn());
        playSessionSource.setExploreVersion("1.0");
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotAcceptNullValueWhenSettingNewPlayQueue() {
        playQueueManager.setNewPlayQueue(null, playSessionSource);
    }

    @Test
    public void shouldSetNewPlayQueueAsCurrentPlayQueue() {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        expectPlayQueueContentToBeEqual(playQueueManager, playQueue);
    }

    @Test
    public void shouldUpdateTrackPoliciesOnNewQueue() {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        verify(policyOperations).updatePolicies(queueUrns);
    }

    @Test
    public void shouldUpdatePositionOnCurrentQueueWhenContentAndSourceAreUnchanged() {
        PlaySessionSource source1 = new PlaySessionSource("screen:something");
        PlayQueue queue1 = PlayQueue.fromTrackUrnList(createTracksUrn(1L, 2L, 3L, 5L), source1);
        PlaySessionSource source2 = new PlaySessionSource("screen:something");
        PlayQueue queue2 = PlayQueue.fromTrackUrnList(createTracksUrn(1L, 2L, 3L, 5L), source2);

        playQueueManager.setNewPlayQueue(queue1, 0, source1);
        playQueueManager.setNewPlayQueue(queue2, 2, source2);

        expect(eventBus.eventsOn(EventQueue.PLAY_QUEUE)).toContainExactly(PlayQueueEvent.fromNewQueue());
        expect(eventBus.eventsOn(EventQueue.PLAY_QUEUE_TRACK)).toContainExactly(CurrentPlayQueueTrackEvent.fromNewQueue(Urn.forTrack(1L)),
                CurrentPlayQueueTrackEvent.fromNewQueue(Urn.forTrack(3L)));
    }

    @Test
    public void getCurrentPositionReturnsCurrentPosition() {
        int newPosition = 5;
        when(playQueue.size()).thenReturn(6);
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);

        playQueueManager.setPosition(newPosition);

        expect(playQueueManager.getCurrentPosition()).toEqual(newPosition);
    }

    @Test
    public void getCurrentTrackUrnReturnsCurrentTrackUrnFromPlayQueue() {
        playQueueManager.setNewPlayQueue(playQueue, 5, playSessionSource);
        when(playQueue.getUrn(5)).thenReturn(Urn.forTrack(5L));

        expect(playQueueManager.getCurrentTrackUrn()).toEqual(Urn.forTrack(5L));
    }

    @Test
    public void getCurrentQueueAsUrnListReturnsUrnList() {
        final List<Urn> tracksUrn = createTracksUrn(1L, 2L, 3L);
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(tracksUrn, playSessionSource), playSessionSource);

        expect(playQueueManager.getCurrentQueueAsUrnList()).toEqual(tracksUrn);
    }

    @Test
    public void hasSameTrackListTrueForMatchingUrns() {
        final List<Urn> tracksUrn = createTracksUrn(1L, 2L, 3L);
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(tracksUrn, playSessionSource), playSessionSource);

        expect(playQueueManager.hasSameTrackList(createTracksUrn(1L, 2L, 3L))).toBeTrue();
    }

    @Test
    public void hasSameTrackListTrueForDifferentOrder() {
        final List<Urn> tracksUrn = createTracksUrn(1L, 2L, 3L);
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(tracksUrn, playSessionSource), playSessionSource);

        expect(playQueueManager.hasSameTrackList(createTracksUrn(3L, 2L, 1L))).toBeFalse();
    }

    @Test
    public void getCurrentPlayQueueCountReturnsSizeOfCurrentQueue() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(createTracksUrn(1L, 2L, 3L), playSessionSource), playSessionSource);

        expect(playQueueManager.getQueueSize()).toBe(3);
    }

    @Test
    public void isQueueEmptyReturnsTrueIfQueueSizeIsZero() throws Exception {
        playQueueManager.setNewPlayQueue(PlayQueue.empty(), playSessionSource);

        expect(playQueueManager.isQueueEmpty()).toBeTrue();
    }

    @Test
    public void isQueueEmptyReturnsFalseIfQueueSizeGreaterThanZero() throws Exception {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(createTracksUrn(1L, 2L, 3L), playSessionSource), playSessionSource);

        expect(playQueueManager.isQueueEmpty()).toBeFalse();
    }

    @Test
    public void getUrnAtPositionReturnsTrackUrnForPlayQueueItem() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(createTracksUrn(1L, 2L, 3L), playSessionSource), playSessionSource);

        expect(playQueueManager.getUrnAtPosition(2)).toEqual(Urn.forTrack(3L));
    }

    @Test
    public void getPositionForUrnReturnsNotSetForEmptyPlayQueue() throws Exception {
        expect(playQueueManager.getPositionForUrn(Urn.forTrack(1L))).toEqual(Consts.NOT_SET);
    }

    @Test
    public void getPositionForUrnReturnsNotSetForUrnIfNotInPlayQueue() throws Exception {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(createTracksUrn(1L, 2L, 3L), playSessionSource), playSessionSource);

        expect(playQueueManager.getPositionForUrn(Urn.forTrack(4L))).toEqual(Consts.NOT_SET);
    }

    @Test
    public void getPositionForUrnReturnsPositionForUrnIfInPlayQueue() throws Exception {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(createTracksUrn(1L, 2L, 3L), playSessionSource), playSessionSource);

        expect(playQueueManager.getPositionForUrn(Urn.forTrack(2L))).toEqual(1);
    }

    @Test
    public void setNewPlayQueueMarksCurrentTrackAsUserTriggered() {
        when(playQueue.isEmpty()).thenReturn(false);
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);

        expect(playQueueManager.getCurrentTrackSourceInfo().getIsUserTriggered()).toBeTrue();
    }

    @Test
    public void shouldReturnNullForTrackSourceInfoWhenSettingEmptyPlayQueue() throws Exception {
        playQueueManager.setNewPlayQueue(PlayQueue.empty(), playSessionSource);
        expect(playQueueManager.getCurrentTrackSourceInfo()).toBeNull();
    }

    @Test
    public void shouldReturnTrackSourceInfoWithPlaylistInfoSetIfSet() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(createTracksUrn(1L, 2L), playSessionSource), 1, playSessionSource);

        final TrackSourceInfo trackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();
        expect(trackSourceInfo.getPlaylistUrn()).toEqual(playlist.getUrn());
        expect(trackSourceInfo.getPlaylistPosition()).toEqual(1);
    }

    @Test
    public void shouldReturnTrackSourceInfoWithExploreTrackingTagIfSet() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(createTracksUrn(1L, 2L), playSessionSource), 1, playSessionSource);

        final TrackSourceInfo trackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();
        expect(trackSourceInfo.getSource()).toEqual("explore");
        expect(trackSourceInfo.getSourceVersion()).toEqual("1.0");
    }

    @Test
    public void shouldReturnTrackSourceInfoWithQuerySourceInfoIfSet() {
        SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("soundcloud:search:urn"), 5, new Urn("soundcloud:click:123"));
        searchQuerySourceInfo.setQueryResults(new ArrayList<>(Arrays.asList(Urn.forTrack(1), Urn.forTrack(2))));

        playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(createTracksUrn(1L, 2L), playSessionSource), 1, playSessionSource);
        final TrackSourceInfo trackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();

        expect(trackSourceInfo.getSearchQuerySourceInfo()).toEqual(searchQuerySourceInfo);
    }

    @Test
    public void shouldReturnTrackSourceInfoWithoutQuerySourceInfoIfNotSet() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(createTracksUrn(1L, 2L), playSessionSource), 1, playSessionSource);
        final TrackSourceInfo trackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();

        expect(trackSourceInfo.getSearchQuerySourceInfo()).toBeNull();
    }

    @Test
    public void shouldBroadcastPlayQueueChangedWhenSettingNewPlayQueue() {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        expectBroadcastNewPlayQueue();
    }

    @Test
    public void shouldPublishPlayQueueChangedEventOnSetNewPlayQueue() {
        when(playQueue.getUrn(0)).thenReturn(Urn.forTrack(3L));
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);

        expect(eventBus.eventsOn(EventQueue.PLAY_QUEUE)).toNumber(1);
        expect(eventBus.firstEventOn(EventQueue.PLAY_QUEUE).getKind()).toEqual(PlayQueueEvent.NEW_QUEUE);
    }

    @Test
    public void shouldPublishTrackChangedEventOnSetNewPlayQueue(){
        final Urn trackUrn = Urn.forTrack(3L);
        when(playQueue.getUrn(0)).thenReturn(trackUrn);
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);

        expect(eventBus.eventsOn(EventQueue.PLAY_QUEUE_TRACK)).toNumber(1);
        expect(eventBus.firstEventOn(EventQueue.PLAY_QUEUE_TRACK).getCurrentTrackUrn()).toEqual(trackUrn);
    }

    @Test
    public void shouldSaveCurrentPositionWhenSettingNonEmptyPlayQueue() {
        int currentPosition = 5;
        Urn currentUrn = Urn.forTrack(3L);
        when(playQueue.hasItems()).thenReturn(true);
        when(playQueue.getUrn(currentPosition)).thenReturn(currentUrn);

        playQueueManager.setNewPlayQueue(playQueue, currentPosition, playSessionSource);
        verify(playQueueOperations).saveQueue(playQueue);
    }

    @Test
    public void shouldStoreTracksWhenSettingNewPlayQueue() {
        Urn currentUrn = Urn.forTrack(3L);
        when(playQueue.hasItems()).thenReturn(true);
        when(playQueue.getUrn(0)).thenReturn(currentUrn);

        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
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
        when(playQueue.getUrn(0)).thenReturn(currentUrn);
        when(playQueue.shouldPersistTrackAt(0)).thenReturn(true);
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        playQueueManager.saveCurrentProgress(123L);

        InOrder inOrder = Mockito.inOrder(playQueueOperations);
        inOrder.verify(playQueueOperations).savePositionInfo(0, currentUrn, playSessionSource, 0L);
        inOrder.verify(playQueueOperations).savePositionInfo(0, currentUrn, playSessionSource, 123L);
    }

    @Test
    public void saveProgressUpdatesSavePositionWithoutNonPersistentTracks() throws CreateModelException {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(createTracksUrn(1L, 2L, 3L), playSessionSource), 1, playSessionSource);
        playQueueManager.performPlayQueueUpdateOperations(new PlayQueueManager.InsertOperation(2, Urn.forTrack(2L), PropertySet.create(), false));
        playQueueManager.setPosition(3);

        playQueueManager.saveCurrentProgress(12L);

        InOrder inOrder = Mockito.inOrder(playQueueOperations);
        // Saves first time when we call setNewPlayQueue
        inOrder.verify(playQueueOperations).savePositionInfo(eq(1), any(Urn.class), any(PlaySessionSource.class), anyLong());
        inOrder.verify(playQueueOperations).savePositionInfo(eq(2), any(Urn.class), any(PlaySessionSource.class), anyLong());
    }

    @Test
    public void saveProgressIgnoresPositionIfCurrentlyPlayingNonPersistentTrack() throws CreateModelException {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(createTracksUrn(1L, 2L, 3L), playSessionSource), 1, playSessionSource);
        playQueueManager.performPlayQueueUpdateOperations(new PlayQueueManager.InsertOperation(2, Urn.forTrack(1L), PropertySet.create(), false));
        playQueueManager.setPosition(2);

        playQueueManager.saveCurrentProgress(12L);

        InOrder inOrder = Mockito.inOrder(playQueueOperations);
        // Saves first time when we call setNewPlayQueue
        inOrder.verify(playQueueOperations).savePositionInfo(eq(1), any(Urn.class), any(PlaySessionSource.class), anyLong());
        inOrder.verify(playQueueOperations).savePositionInfo(eq(2), any(Urn.class), any(PlaySessionSource.class), eq(0L));
    }

    @Test
    public void getPlayProgressInfoReturnsLastSavedProgressInfo() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(createTracksUrn(123L), playSessionSource), playSessionSource);
        playQueueManager.saveCurrentProgress(456L);
        expect(playQueueManager.getPlayProgressInfo()).toEqual(new PlaybackProgressInfo(123L, 456L));
    }

    @Test
    public void doesNotChangePlayQueueIfPositionSetToCurrent() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(createTracksUrn(1L, 2L, 3L), playSessionSource), playSessionSource);
        playQueueManager.setPosition(1);
        verifyZeroInteractions(playQueue);
    }

    @Test
    public void doesNotSendTrackChangeEventIfPositionSetToCurrent() throws Exception {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(createTracksUrn(1L, 2L, 3L), playSessionSource), 1, playSessionSource);

        final CurrentPlayQueueTrackEvent lastEvent = eventBus.lastEventOn(EventQueue.PLAY_QUEUE_TRACK);
        playQueueManager.setPosition(1);

        expect(eventBus.lastEventOn(EventQueue.PLAY_QUEUE_TRACK)).toBe(lastEvent);
    }

    @Test
    public void shouldPublishTrackChangeEventOnSetPosition() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(createTracksUrn(1L, 2L, 3L), playSessionSource), playSessionSource);

        playQueueManager.setPosition(2);

        expect(eventBus.lastEventOn(EventQueue.PLAY_QUEUE_TRACK).getCurrentTrackUrn()).toEqual(Urn.forTrack(3L));
    }

    @Test
    public void shouldSetCurrentTriggerToManualIfSettingDifferentPosition() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(createTracksUrn(1L, 2L, 3L), playSessionSource), playSessionSource);
        playQueueManager.autoNextTrack(); // set to auto trigger

        playQueueManager.setPosition(2);

        expect(playQueueManager.getCurrentTrackSourceInfo().getIsUserTriggered()).toBeTrue();
    }

    @Test
    public void shouldNotSetCurrentTriggerToManualIfSettingSamePosition() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(createTracksUrn(1L, 2L, 3L), playSessionSource), playSessionSource);
        playQueueManager.autoNextTrack(); // set to auto trigger

        playQueueManager.setPosition(1);

        expect(playQueueManager.getCurrentTrackSourceInfo().getIsUserTriggered()).toBeFalse();
    }

    @Test
    public void shouldPublishTrackChangeEventOnPreviousTrack() {
        playQueueManager.setNewPlayQueue(playQueue, 5, playSessionSource);
        when(playQueue.hasPreviousTrack(5)).thenReturn(true);
        when(playQueue.getUrn(4)).thenReturn(Urn.forTrack(3L));

        playQueueManager.moveToPreviousTrack();

        expect(eventBus.lastEventOn(EventQueue.PLAY_QUEUE_TRACK).getCurrentTrackUrn()).toEqual(Urn.forTrack(3L));
    }

    @Test
    public void shouldMoveToPreviousTrack() {
        playQueueManager.setNewPlayQueue(playQueue, 5, playSessionSource);
        when(playQueue.hasPreviousTrack(5)).thenReturn(true);

        playQueueManager.moveToPreviousTrack();

        expect(playQueueManager.getCurrentPosition()).toBe(4);
    }

    @Test
    public void shouldNotPublishTrackChangeWhenCallingPreviousOnFirstTrack() {
        when(playQueue.hasPreviousTrack(0)).thenReturn(false);

        playQueueManager.moveToPreviousTrack();

        expect(eventBus.eventsOn(EventQueue.PLAY_QUEUE)).toBeEmpty();
    }

    @Test
    public void shouldNotMoveToPreviousTrackIfAtHeadOfQueue() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(createTracksUrn(1L, 2L), playSessionSource), playSessionSource);

        playQueueManager.moveToPreviousTrack();

        expect(playQueueManager.getCurrentPosition()).toBe(0);
    }

    @Test
    public void moveToPreviousShouldResultInManualTrigger() {
        playQueueManager.setNewPlayQueue(playQueue, 5, playSessionSource);
        when(playQueue.hasPreviousTrack(5)).thenReturn(true);
        when(playQueue.isEmpty()).thenReturn(false);

        playQueueManager.moveToPreviousTrack();

        expect(playQueueManager.getCurrentTrackSourceInfo().getIsUserTriggered()).toBeTrue();
    }

    @Test
    public void shouldPublishTrackChangeEventOnNextTrack() {
        playQueueManager.setNewPlayQueue(playQueue, 0, playSessionSource);
        when(playQueue.hasNextTrack(0)).thenReturn(true);
        when(playQueue.getUrn(1)).thenReturn(Urn.forTrack(3L));

        playQueueManager.nextTrack();

        expect(eventBus.lastEventOn(EventQueue.PLAY_QUEUE_TRACK).getCurrentTrackUrn()).toEqual(Urn.forTrack(3L));
    }

    @Test
    public void shouldToNextTrackSetUserTriggeredFlagToTrue() {
        when(playQueue.isEmpty()).thenReturn(false);
        when(playQueue.hasNextTrack(0)).thenReturn(true);
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);

        playQueueManager.nextTrack();

        expect(playQueueManager.getCurrentTrackSourceInfo().getIsUserTriggered()).toBeTrue();
    }

    @Test
    public void shouldAutoNextTrackSetUserTriggerTrackFlagToFalse() {
        when(playQueue.isEmpty()).thenReturn(false);
        when(playQueue.hasNextTrack(0)).thenReturn(true);
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);

        playQueueManager.autoNextTrack();

        expect(playQueueManager.getCurrentTrackSourceInfo().getIsUserTriggered()).toBeFalse();
    }

    @Test
    public void shouldSuccessfullyMoveToNextTrack() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(createTracksUrn(1L, 2L), playSessionSource), playSessionSource);

        expect(playQueueManager.nextTrack()).toBeTrue();
        expect(playQueueManager.getCurrentPosition()).toBe(1);
    }

    @Test
    public void shouldNotMoveToNextTrackIfAtEndOfQueue() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(createTracksUrn(1L, 2L), playSessionSource), 1, playSessionSource);

        expect(playQueueManager.nextTrack()).toBeFalse();
        expect(playQueueManager.getCurrentPosition()).toBe(1);
    }

    @Test
    public void nextTrackReturnsFalseIfNoNextTrack() {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        when(playQueue.hasNextTrack(0)).thenReturn(false);
        expect(playQueueManager.nextTrack()).toBeFalse();
    }

    @Test
    public void nextTrackReturnsTrueIfHasNextTrack() {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        when(playQueue.hasNextTrack(0)).thenReturn(true);
        expect(playQueueManager.nextTrack()).toBeTrue();
    }

    @Test
    public void getNextTrackUrnReturnsNextTrackUrn() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(createTracksUrn(1L, 2L), playSessionSource), playSessionSource);
        expect(playQueueManager.getNextTrackUrn()).toEqual(Urn.forTrack(2L));
    }

    @Test
    public void getNextTrackUrnReturnsNotSetTrackUrnIfNoNextTrack() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(createTracksUrn(1L, 2L), playSessionSource), 1, playSessionSource);
        expect(playQueueManager.getNextTrackUrn()).toEqual(Urn.NOT_SET);
    }

    @Test
    public void performPlayQueueUpdateOperationsExecutesOperationsInOrder() throws CreateModelException {
        playQueueManager.setNewPlayQueue(playQueue, 1, playSessionSource);

        final PlayQueueManager.QueueUpdateOperation queueUpdateOperation1 = mock(PlayQueueManager.QueueUpdateOperation.class);
        final PlayQueueManager.QueueUpdateOperation queueUpdateOperation2 = mock(PlayQueueManager.QueueUpdateOperation.class);
        playQueueManager.performPlayQueueUpdateOperations(queueUpdateOperation1, queueUpdateOperation2);

        final InOrder inOrder = inOrder(queueUpdateOperation1, queueUpdateOperation2);
        inOrder.verify(queueUpdateOperation1).execute(playQueue);
        inOrder.verify(queueUpdateOperation2).execute(playQueue);

    }

    @Test
    public void performPlayQueueUpdateOperationsPublishesQueueChangeEvent() throws Exception {
        playQueueManager.setNewPlayQueue(playQueue, 1, playSessionSource);
        playQueueManager.performPlayQueueUpdateOperations();

        expect(eventBus.lastEventOn(EventQueue.PLAY_QUEUE).getKind()).toEqual(PlayQueueEvent.QUEUE_UPDATE);
    }

    @Test
      public void clearElementsThatMatchesThePredicate() throws CreateModelException {
        final PlayQueue playQueue = PlayQueue.fromTrackUrnList(createTracksUrn(1L, 2L, 3L), playSessionSource);
        final PropertySet metaDataToRemove = PropertySet.create();
        playQueue.insertTrack(1, Urn.forTrack(123L), metaDataToRemove, true);
        playQueueManager.setNewPlayQueue(playQueue, 3, playSessionSource);

        playQueueManager.removeTracksWithMetaData(new Predicate<PropertySet>() {
            @Override
            public boolean apply(@Nullable PropertySet input) {
                return input == metaDataToRemove;
            }
        });

        expect(playQueueManager.getQueueSize()).toEqual(3);
        expect(playQueueManager.getUrnAtPosition(0)).toEqual(Urn.forTrack(1L));
        expect(playQueueManager.getCurrentPosition()).toEqual(2);
    }

    @Test
    public void publishesQueueChangeEventWhenPredicateIsTrue() throws CreateModelException {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(createTracksUrn(1L, 2L), playSessionSource), playSessionSource);

        playQueueManager.removeTracksWithMetaData(new Predicate<PropertySet>() {
            @Override
            public boolean apply(@Nullable PropertySet input) {
                return true;
            }
        }, PlayQueueEvent.fromAudioAdRemoved());

        expect(eventBus.lastEventOn(EventQueue.PLAY_QUEUE).getKind()).toEqual(PlayQueueEvent.AUDIO_AD_REMOVED);
    }

    @Test
    public void publishesQueueChangeEventWhenPredicateIsFalse() throws CreateModelException {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(createTracksUrn(1L, 2L), playSessionSource), playSessionSource);
        final PlayQueueEvent lastEvent = eventBus.lastEventOn(EventQueue.PLAY_QUEUE);
        playQueueManager.removeTracksWithMetaData(new Predicate<PropertySet>() {
            @Override
            public boolean apply(@Nullable PropertySet input) {
                return false;
            }
        });

        expect(eventBus.lastEventOn(EventQueue.PLAY_QUEUE)).toBe(lastEvent);
    }

    @Test
    public void filtersTrackUrnsWithMetadata() {
        final PlayQueue playQueue = PlayQueue.fromTrackUrnList(createTracksUrn(123L, 456L), playSessionSource);
        final PropertySet metaDataToSelect = PropertySet.create();
        final Urn expectedSelectedTrackUrn = Urn.forTrack(789L);
        playQueue.insertTrack(1, expectedSelectedTrackUrn, metaDataToSelect, true);
        playQueueManager.setNewPlayQueue(playQueue, 3, playSessionSource);

        List<Urn> urns = playQueueManager.filterTrackUrnsWithMetadata(new Predicate<PropertySet>() {
            @Override
            public boolean apply(@Nullable PropertySet input) {
                return input == metaDataToSelect;
            }
        });

        expect(urns).toNumber(1);
        expect(urns).toContainExactly(expectedSelectedTrackUrn);
    }

    @Test
    public void autoNextReturnsFalseIfNoNextTrack() {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        when(playQueue.hasNextTrack(0)).thenReturn(false);
        expect(playQueueManager.autoNextTrack()).toBeFalse();
    }

    @Test
    public void autoNextReturnsTrueIfHasNextTrack() {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        when(playQueue.hasNextTrack(0)).thenReturn(true);
        expect(playQueueManager.autoNextTrack()).toBeTrue();
    }

    @Test
    public void hasNextTrackReturnsHasNextTrackFromCurrentPlayQueue() {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        when(playQueue.hasNextTrack(0)).thenReturn(true);
        expect(playQueueManager.hasNextTrack()).toBeTrue();
    }

    @Test
    public void shouldNotPublishTrackChangeWhenCallingNextOnLastTrack() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(createTracksUrn(1L), playSessionSource), playSessionSource);
        final CurrentPlayQueueTrackEvent lastEvent = eventBus.lastEventOn(EventQueue.PLAY_QUEUE_TRACK);
        playQueueManager.nextTrack();

        expect(eventBus.lastEventOn(EventQueue.PLAY_QUEUE_TRACK)).toBe(lastEvent);
    }

    @Test
    public void shouldHaveNoPlayProgressInfoWhenPlaybackOperationsHasReturnsNoObservable() {
        playQueueManager.loadPlayQueueAsync();
        expect(playQueueManager.getPlayProgressInfo()).toBeNull();
    }

    @Test
    public void shouldNotSetEmptyPlayQueue() {
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(createTracksUrn(1L, 2L, 3L), playSessionSource), 1, playSessionSource);
        when(playQueueOperations.getLastStoredPlayQueue()).thenReturn(Observable.just(PlayQueue.empty()));
        when(playQueueOperations.getLastStoredPlayingTrackId()).thenReturn(456L);
        when(playQueueOperations.getLastStoredSeekPosition()).thenReturn(400L);

        playQueueManager.loadPlayQueueAsync();

        expect(playQueueManager.getCurrentTrackUrn()).toEqual(Urn.forTrack(2L));
    }

    @Test
    public void shouldSetPlayProgressInfoWhenReloadingPlayQueue() {
        when(playQueueOperations.getLastStoredPlayQueue()).thenReturn(Observable.<PlayQueue>empty());
        when(playQueueOperations.getLastStoredPlayingTrackId()).thenReturn(456L);
        when(playQueueOperations.getLastStoredSeekPosition()).thenReturn(400L);

        playQueueManager.loadPlayQueueAsync();
        PlaybackProgressInfo resumeInfo = playQueueManager.getPlayProgressInfo();
        expect(resumeInfo.getTrackId()).toEqual(456L);
        expect(resumeInfo.getTime()).toEqual(400L);
    }

    @Test
    public void shouldNotSetCurrentPositionIfPQIsNotLoaded() {
        when(playQueueOperations.getLastStoredPlayQueue()).thenReturn(Observable.<PlayQueue>empty());
        when(playQueueOperations.getLastStoredPlayingTrackId()).thenReturn(456L);
        when(playQueueOperations.getLastStoredSeekPosition()).thenReturn(400L);
        when(playQueueOperations.getLastStoredPlayPosition()).thenReturn(2);

        playQueueManager.loadPlayQueueAsync();

        expect(playQueueManager.getCurrentPosition()).toBe(0);
    }

    @Test
    public void shouldSetCurrentPositionIfPQIsNotLoaded() {
        PlayQueue playQueue = PlayQueue.fromTrackUrnList(createTracksUrn(1L, 2L, 3L), playSessionSource);
        when(playQueueOperations.getLastStoredPlayQueue()).thenReturn(Observable.just(playQueue));
        when(playQueueOperations.getLastStoredPlayingTrackId()).thenReturn(456L);
        when(playQueueOperations.getLastStoredSeekPosition()).thenReturn(400L);
        when(playQueueOperations.getLastStoredPlayPosition()).thenReturn(2);


        playQueueManager.loadPlayQueueAsync();

        expect(playQueueManager.getCurrentPosition()).toBe(2);
    }

    @Test
    public void loadPlayQueueAsyncLoadsQueueFromLocalStorage() {
        PlayQueue playQueue = PlayQueue.fromTrackUrnList(createTracksUrn(1L, 2L, 3L), playSessionSource);

        when(playQueueOperations.getLastStoredPlayQueue()).thenReturn(Observable.just(playQueue));
        when(playQueueOperations.getLastStoredPlayingTrackId()).thenReturn(456L);
        when(playQueueOperations.getLastStoredSeekPosition()).thenReturn(400L);
        playQueueManager.loadPlayQueueAsync();

        expectPlayQueueContentToBeEqual(playQueueManager, playQueue);
    }

    @Test
    public void loadPlayQueueAsyncFiresShowPlayerEventIfFlagSet() {
        PlayQueue playQueue = PlayQueue.fromTrackUrnList(createTracksUrn(1L, 2L, 3L), playSessionSource);

        when(playQueueOperations.getLastStoredPlayQueue()).thenReturn(Observable.just(playQueue));
        when(playQueueOperations.getLastStoredPlayingTrackId()).thenReturn(456L);
        when(playQueueOperations.getLastStoredSeekPosition()).thenReturn(400L);
        playQueueManager.loadPlayQueueAsync(true);

        expect(eventBus.lastEventOn(EventQueue.PLAYER_COMMAND).isShow()).toBeTrue();
    }

    @Test
    public void shouldReloadShouldBeTrueIfThePlayQueueIsEmpty() {
        expect(playQueueManager.shouldReloadQueue()).toBeTrue();
    }

    @Test
    public void shouldReloadShouldBeFalseWithNonEmptyQueue() {
        when(playQueue.isEmpty()).thenReturn(false);
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        expect(playQueueManager.shouldReloadQueue()).toBeFalse();
    }

    @Test
    public void shouldReloadShouldBeFalseIfAlreadyReloadingWithEmptyQueue() {
        when(playQueueOperations.getLastStoredPlayQueue()).thenReturn(Observable.<PlayQueue>never());
        when(playQueueOperations.getLastStoredPlayingTrackId()).thenReturn(456L);
        when(playQueueOperations.getLastStoredSeekPosition()).thenReturn(400L);
        playQueueManager.loadPlayQueueAsync();
        expect(playQueueManager.shouldReloadQueue()).toBeFalse();
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
    public void shouldGetRelatedTracksObservableWhenFetchingRelatedTracks() {
        Urn currentUrn = Urn.forTrack(123L);
        when(playQueueOperations.getRelatedTracks(any(Urn.class))).thenReturn(Observable.<RecommendedTracksCollection>empty());
        when(playQueue.getUrn(0)).thenReturn(currentUrn);

        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        playQueueManager.fetchTracksRelatedToCurrentTrack();

        verify(playQueueOperations).getRelatedTracks(currentUrn);
    }

    @Test
    public void shouldSubscribeToRelatedTracksObservableWhenFetchingRelatedTracks() {
        TestObservables.MockObservable observable = TestObservables.emptyObservable();
        when(playQueueOperations.getRelatedTracks(any(Urn.class))).thenReturn(observable);

        playQueueManager.fetchTracksRelatedToCurrentTrack();
        expect(observable.subscribedTo()).toBeTrue();
    }

    @Test
    public void shouldSetLoadingStateOnQueueAndBroadcastWhenFetchingRelatedTracks() {
        when(playQueueOperations.getRelatedTracks(any(Urn.class))).thenReturn(Observable.<RecommendedTracksCollection>never());
        playQueueManager.fetchTracksRelatedToCurrentTrack();

        expectBroadcastPlayQueueUpdate();
    }

    @Test
    public void shouldNotPublishPlayQueueRelatedTracksEventOnEmptyRelatedLoad() throws CreateModelException {
        when(playQueueOperations.getRelatedTracks(any(Urn.class))).thenReturn(Observable.<RecommendedTracksCollection>empty());
        playQueueManager.fetchTracksRelatedToCurrentTrack();

        eventBus.verifyNoEventsOn(EventQueue.PLAY_QUEUE);
    }

    @Test
    public void shouldPublishPlayQueueUpdateEventOnRelatedTracksReturned() throws Exception {
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        final Urn trackUrn = Urn.forTrack(123L);
        when(playQueueOperations.getRelatedTracks(trackUrn)).thenReturn(Observable.just(new RecommendedTracksCollection(Lists.newArrayList(apiTrack), "123")));
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(createTracksUrn(123L), playSessionSource), playSessionSource);
        playQueueManager.fetchTracksRelatedToCurrentTrack();

        expect(eventBus.lastEventOn(EventQueue.PLAY_QUEUE).getKind()).toEqual(PlayQueueEvent.QUEUE_UPDATE);
    }

    @Test
    public void relatedTrackLoadShouldCauseQueueToBeSaved() throws Exception {
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        final Urn trackUrn = Urn.forTrack(123L);
        final RecommendedTracksCollection related = new RecommendedTracksCollection(Lists.newArrayList(apiTrack), "123");
        when(playQueueOperations.getRelatedTracks(trackUrn)).thenReturn(Observable.just(related));

        final PlayQueue playQueueOrig = PlayQueue.fromTrackUrnList(createTracksUrn(123L), playSessionSource);
        final PlayQueue expPlayQueue1 = playQueueOrig.copy();
        final PlayQueue expPlayQueue2 = playQueueOrig.copy();
        expPlayQueue2.addTrack(apiTrack.getUrn(), PlaySessionSource.DiscoverySource.RECOMMENDER.value(), "123");

        playQueueManager.setNewPlayQueue(playQueueOrig, playSessionSource);
        playQueueManager.fetchTracksRelatedToCurrentTrack();

        InOrder inOrder = Mockito.inOrder(playQueueOperations);
        inOrder.verify(playQueueOperations).saveQueue(eq(expPlayQueue1));
        inOrder.verify(playQueueOperations).saveQueue(eq(expPlayQueue2));

    }

    @Test
    public void shouldCacheAndAddRelatedTracksToQueueWhenRelatedTracksReturn() throws CreateModelException {
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(createTracksUrn(123L), playSessionSource), playSessionSource);
        playQueueManager.onNext(new RecommendedTracksCollection(Lists.newArrayList(apiTrack), "123"));

        ArgumentCaptor<PublicApiTrack> captor = ArgumentCaptor.forClass(PublicApiTrack.class);
        verify(modelManager).cache(captor.capture());
        expect(captor.getValue().getId()).toEqual(apiTrack.getId());
    }

    @Test
    public void shouldSetIdleStateOnQueueAndBroadcastWhenDoneSuccessfulRelatedLoad() {
        playQueueManager.onNext(new RecommendedTracksCollection(Collections.<ApiTrack>emptyList(), "123"));
        playQueueManager.onCompleted();

        expectBroadcastPlayQueueUpdate();
    }

    @Test
    public void shouldSetEmptyStateOnQueueAndBroadcastWhenDoneEmptyRelatedLoad() {
        playQueueManager.onCompleted();

        expectBroadcastPlayQueueUpdate();
    }

    @Test
    public void shouldSetErrorStateOnQueueAndBroadcastWhenOnErrorCalled() {
        playQueueManager.onError(new Throwable());

        expectBroadcastPlayQueueUpdate();
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
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(createTracksUrn(1L), playSessionSource), playSessionSource);
        expect(playQueueManager.isQueueEmpty()).toBeFalse();
        playQueueManager.clearAll();
        expect(playQueueManager.isQueueEmpty()).toBeTrue();
    }

    @Test
    public void clearAllClearsPlaylistId() {
        when(sharedPreferencesEditor.remove(anyString())).thenReturn(sharedPreferencesEditor);
        playQueueManager.setNewPlayQueue(PlayQueue.fromTrackUrnList(createTracksUrn(1L), playSessionSource), playSessionSource);
        expect(playQueueManager.getPlaylistUrn()).not.toEqual(Urn.NOT_SET);
        playQueueManager.clearAll();
        expect(playQueueManager.getPlaylistUrn()).toEqual(Urn.NOT_SET);
    }

    @Test
    public void shouldReturnWhetherPlaylistIdIsCurrentPlayQueue() {
        PublicApiPlaylist playlist = new PublicApiPlaylist(6L);
        playSessionSource.setPlaylist(playlist.getUrn(), playlist.getUserUrn());
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);

        expect(playQueueManager.isCurrentPlaylist(playlist.getUrn())).toBeTrue();
    }

    @Test
    public void shouldReturnWhetherCurrentPlayQueueIsAPlaylist() {
        PublicApiPlaylist playlist = new PublicApiPlaylist(6L);
        playSessionSource.setPlaylist(playlist.getUrn(), playlist.getUserUrn());
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);

        expect(playQueueManager.isPlaylist()).toBeTrue();
    }

    @Test
    public void shouldReturnTrueIfGivenTrackIsCurrentTrack() {
        when(playQueue.getUrn(0)).thenReturn(Urn.forTrack(123L));
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        expect(playQueueManager.isCurrentTrack(Urn.forTrack(123))).toBeTrue();
    }

    @Test
    public void shouldReturnFalseIfGivenTrackIsNotCurrentTrack() {
        when(playQueue.getUrn(0)).thenReturn(Urn.forTrack(123L));
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        expect(playQueueManager.isCurrentTrack(Urn.forTrack(456))).toBeFalse();
    }

    @Test
    public void shouldRetryWithSameObservable() {
        TestObservables.MockObservable observable = TestObservables.emptyObservable();
        when(playQueueOperations.getRelatedTracks(any(Urn.class))).thenReturn(observable);

        playQueueManager.fetchTracksRelatedToCurrentTrack();
        playQueueManager.retryRelatedTracksFetch();
        expect(observable.subscribers()).toNumber(2);
    }

    private void expectBroadcastNewPlayQueue() {
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(context).sendBroadcast(captor.capture());
        expect(captor.getValue().getAction()).toEqual(PlayQueueManager.PLAYQUEUE_CHANGED_ACTION);
    }

    private void expectBroadcastPlayQueueUpdate() {
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(context, atLeastOnce()).sendBroadcast(captor.capture());
        expect(Iterables.find(captor.getAllValues(), new Predicate<Intent>() {
            @Override
            public boolean apply(@Nullable Intent input) {
                return input.getAction().equals(PlayQueueManager.RELATED_LOAD_STATE_CHANGED_ACTION);
            }
        })).not.toBeNull();
    }

    private void expectPlayQueueContentToBeEqual(PlayQueueManager playQueueManager, PlayQueue playQueue) {
        expect(playQueueManager.getQueueSize()).toBe(playQueue.size());
        for (int i = 0; i < playQueueManager.getQueueSize(); i++) {
            expect(playQueueManager.getUrnAtPosition(i)).toEqual(playQueue.getUrn(i));
        }
    }
}
