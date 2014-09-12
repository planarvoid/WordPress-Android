package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.TestHelper.buildProvider;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Maps;
import com.soundcloud.android.Actions;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.SoundAssociation;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableUpdatedEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.TestHelper;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.storage.CollectionStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.propeller.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowApplication;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;

import android.content.Intent;
import android.net.Uri;
import android.view.ViewGroup;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PostsAdapterTest {

    private static final String RELATED_USERNAME = "related username";

    private PostsAdapter adapter;

    private TestEventBus eventBus = new TestEventBus();

    @Mock private PlaybackOperations playbackOperations;
    @Mock private TrackItemPresenter trackPresenter;
    @Mock private PlaylistItemPresenter playlistPresenter;
    @Mock private ViewGroup itemView;
    @Mock private ExpandPlayerSubscriber expandPlayerSubscriber;

    @Captor private ArgumentCaptor<List<PropertySet>> propSetCaptor;

    @Before
    public void setup() {
        when(playbackOperations.playTracksFromUri(any(Uri.class), anyInt(), any(TrackUrn.class), any(PlaySessionSource.class)))
                .thenReturn(Observable.<List<TrackUrn>>empty());
        adapter = new PostsAdapter(Content.ME_LIKES.uri, RELATED_USERNAME, playbackOperations,
                trackPresenter, playlistPresenter, eventBus, buildProvider(expandPlayerSubscriber));
    }


    @Test
    public void shouldBindTrackRowViaPresenter() throws CreateModelException {
        PublicApiTrack track = TestHelper.getModelFactory().createModel(PublicApiTrack.class);
        adapter.addItems(Arrays.<PublicApiResource>asList(track));

        adapter.bindRow(0, itemView);

        verify(trackPresenter).bindItemView(eq(0), refEq(itemView), anyList());
    }

    @Test
    public void shouldBindPlaylistRowViaPresenter() throws CreateModelException {
        PublicApiPlaylist playlist = TestHelper.getModelFactory().createModel(PublicApiPlaylist.class);
        adapter.addItems(Arrays.<PublicApiResource>asList(playlist));

        adapter.bindRow(0, itemView);

        verify(playlistPresenter).bindItemView(eq(0), refEq(itemView), anyList());
    }

    @Test
    public void shouldConvertTrackToPropertySet() throws CreateModelException {
        PublicApiTrack track = TestHelper.getModelFactory().createModel(PublicApiTrack.class);
        adapter.addItems(Arrays.<PublicApiResource>asList(track));

        adapter.bindRow(0, itemView);

        verify(trackPresenter).bindItemView(eq(0), refEq(itemView), propSetCaptor.capture());
        PropertySet convertedTrack = propSetCaptor.getValue().get(0);
        expect(convertedTrack.get(TrackProperty.URN)).toEqual(track.getUrn());
        expect(convertedTrack.get(TrackProperty.PLAY_COUNT)).toEqual(track.playback_count);
        expect(convertedTrack.get(PlayableProperty.TITLE)).toEqual(track.getTitle());
        expect(convertedTrack.get(PlayableProperty.DURATION)).toEqual(track.duration);
        expect(convertedTrack.get(PlayableProperty.CREATOR_URN)).toEqual(track.getUser().getUrn());
        expect(convertedTrack.get(PlayableProperty.CREATOR_NAME)).toEqual(track.getUser().getUsername());
        expect(convertedTrack.get(PlayableProperty.IS_PRIVATE)).toEqual(track.isPrivate());
    }

    @Test
    public void clearItemsClearsInitialPropertySets() throws CreateModelException {
        PublicApiTrack track = TestHelper.getModelFactory().createModel(PublicApiTrack.class);
        adapter.addItems(Arrays.<PublicApiResource>asList(track));
        adapter.bindRow(0, itemView);
        adapter.clearData();

        PublicApiTrack track2 = TestHelper.getModelFactory().createModel(PublicApiTrack.class);
        adapter.addItems(Arrays.<PublicApiResource>asList(track2));
        adapter.bindRow(0, itemView);

        verify(trackPresenter, times(2)).bindItemView(eq(0), refEq(itemView), propSetCaptor.capture());
        PropertySet convertedTrack = propSetCaptor.getAllValues().get(1).get(0);
        expect(convertedTrack.get(TrackProperty.URN)).toEqual(track2.getUrn());
    }

    @Test
    public void shouldHandleItemClick() throws CreateModelException {
        PublicApiTrack track = TestHelper.getModelFactory().createModel(PublicApiTrack.class);
        adapter.addItems(Arrays.<PublicApiResource>asList(track));

        adapter.handleListItemClick(Robolectric.application, 0, 1L, Screen.YOUR_LIKES);

        List<TrackUrn> trackUrns = Arrays.asList(track.getUrn());
        TrackUrn initialTrack = trackUrns.get(0);
        verify(playbackOperations).playTracksFromUri(
                eq(Content.ME_LIKES.uri),
                eq(0),
                eq(initialTrack),
                eq(new PlaySessionSource(Screen.YOUR_LIKES)));
    }

    @Test
    public void opensPlaylistActivityWhenPlaylistItemIsClicked() throws CreateModelException {
        PublicApiPlaylist playlist = TestHelper.getModelFactory().createModel(PublicApiPlaylist.class);
        adapter.addItems(Arrays.<PublicApiResource>asList(playlist));

        adapter.handleListItemClick(Robolectric.application, 0, 1L, Screen.YOUR_LIKES);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent startedActivity = application.getNextStartedActivity();
        expect(startedActivity).not.toBeNull();
        expect(startedActivity.getAction()).toBe(Actions.PLAYLIST);
        expect(startedActivity.getParcelableExtra(PublicApiPlaylist.EXTRA_URN)).toEqual(playlist.getUrn());
    }

    @Test
    public void playQueueTrackEventForPositionChangedShouldUpdateTrackPresenterWithCurrentlyPlayingTrack() {
        final TrackUrn playingTrack = Urn.forTrack(123L);
        adapter.onViewCreated();
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(playingTrack));
        verify(trackPresenter).setPlayingTrack(playingTrack);
    }

    @Test
    public void playQueueTrackEventForNewQueueShouldUpdateTrackPresenterWithCurrentlyPlayingTrack() {
        final TrackUrn playingTrack = Urn.forTrack(123L);
        adapter.onViewCreated();
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(playingTrack));
        verify(trackPresenter).setPlayingTrack(playingTrack);
    }

    @Test
    public void playableChangedEventShouldUpdateAdapterToReflectTheLatestLikeStatus() throws CreateModelException {
        final PublicApiPlaylist unlikedPlaylist = TestHelper.getModelFactory().createModel(PublicApiPlaylist.class);
        unlikedPlaylist.user_like = false;

        adapter.addItems(Arrays.<PublicApiResource>asList(unlikedPlaylist));

        adapter.onViewCreated();
        eventBus.publish(EventQueue.PLAYABLE_CHANGED, PlayableUpdatedEvent.forLike(unlikedPlaylist.getUrn(), true, 1));
        adapter.bindRow(0, itemView);

        verify(playlistPresenter).bindItemView(eq(0), refEq(itemView), propSetCaptor.capture());
        expect(propSetCaptor.getValue().get(0).get(PlayableProperty.IS_LIKED)).toBeTrue();
    }

    @Test
    public void shouldPresentRepostedTrackWithRelatedUsername() throws CreateModelException {
        final PublicApiTrack track = TestHelper.getModelFactory().createModel(PublicApiTrack.class);
        adapter.addItems(Arrays.<PublicApiResource>asList(getRepostedTrackSoundAssociation(track)));

        adapter.bindRow(0, itemView);

        verify(trackPresenter).bindItemView(eq(0), refEq(itemView), propSetCaptor.capture());
        PropertySet respostedTrackPropertySet = propSetCaptor.getValue().get(0);
        expect(respostedTrackPropertySet.get(PlayableProperty.REPOSTER)).toEqual(RELATED_USERNAME);
    }

    @Test
    public void shouldKeepRepostInformationAfterUpdatingTrack() throws CreateModelException {
        final PublicApiTrack track = TestHelper.getModelFactory().createModel(PublicApiTrack.class);
        adapter.addItems(Arrays.<PublicApiResource>asList(getRepostedTrackSoundAssociation(track)));
        adapter.updateItems(getResourceHashMap(track));

        adapter.bindRow(0, itemView);

        verify(trackPresenter).bindItemView(eq(0), refEq(itemView), propSetCaptor.capture());
        PropertySet respostedTrackPropertySet = propSetCaptor.getValue().get(0);
        expect(respostedTrackPropertySet.get(PlayableProperty.REPOSTER)).toEqual(RELATED_USERNAME);
    }

    @Test
    public void shouldKeepRepostInformationAfterUpdatingPlaylist() throws CreateModelException {
        final PublicApiPlaylist playlist = TestHelper.getModelFactory().createModel(PublicApiPlaylist.class);
        adapter.addItems(Arrays.<PublicApiResource>asList(getRepostedPlaylistSoundAssociation(playlist)));
        adapter.updateItems(getResourceHashMap(playlist));

        adapter.bindRow(0, itemView);

        verify(playlistPresenter).bindItemView(eq(0), refEq(itemView), propSetCaptor.capture());
        PropertySet respostedTrackPropertySet = propSetCaptor.getValue().get(0);
        expect(respostedTrackPropertySet.get(PlayableProperty.REPOSTER)).toEqual(RELATED_USERNAME);
    }

    @Test
    public void shouldPresentRepostedPlaylistWithRelatedUsername() throws CreateModelException {
        final PublicApiPlaylist playlist = TestHelper.getModelFactory().createModel(PublicApiPlaylist.class);
        adapter.addItems(Arrays.<PublicApiResource>asList(getRepostedPlaylistSoundAssociation(playlist)));

        adapter.bindRow(0, itemView);

        verify(playlistPresenter).bindItemView(eq(0), refEq(itemView), propSetCaptor.capture());
        PropertySet respostedTrackPropertySet = propSetCaptor.getValue().get(0);
        expect(respostedTrackPropertySet.get(PlayableProperty.REPOSTER)).toEqual(RELATED_USERNAME);
    }

    @Test
    public void shouldUnsubscribeFromEventBusInOnDestroyView() {
        adapter.onViewCreated();
        adapter.onDestroyView();
        eventBus.verifyUnsubscribed();
    }

    private SoundAssociation getRepostedTrackSoundAssociation(PublicApiTrack track) throws CreateModelException {
        final SoundAssociation respostedSound = new SoundAssociation(track);
        respostedSound.associationType = CollectionStorage.CollectionItemTypes.REPOST;
        return respostedSound;
    }

    private SoundAssociation getRepostedPlaylistSoundAssociation(PublicApiPlaylist playlist) throws CreateModelException {
        final SoundAssociation respostedSound = new SoundAssociation(playlist);
        respostedSound.associationType = CollectionStorage.CollectionItemTypes.REPOST;
        return respostedSound;
    }

    private HashMap<Urn, PublicApiResource> getResourceHashMap(PublicApiResource... resources) {
        HashMap<Urn, PublicApiResource> updatedItems = Maps.newHashMap();
        for (PublicApiResource resource : resources) {
            updatedItems.put(resource.getUrn(), resource);
        }
        return updatedItems;
    }
}