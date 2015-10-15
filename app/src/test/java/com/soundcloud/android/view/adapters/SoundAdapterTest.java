package com.soundcloud.android.view.adapters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Actions;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.api.legacy.model.Association;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.SoundAssociation;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestSubscribers;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowApplication;
import rx.Observable;

import android.content.Intent;
import android.net.Uri;
import android.view.ViewGroup;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class SoundAdapterTest extends AndroidUnitTest {

    private SoundAdapter adapter;

    private TestEventBus eventBus = new TestEventBus();

    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private TrackItemRenderer trackRenderer;
    @Mock private PlaylistItemRenderer playlistRenderer;
    @Mock private ViewGroup itemView;

    @Captor private ArgumentCaptor<List<PlayableItem>> itemCaptor;

    @Before
    public void setup() {
        when(playbackInitiator.playTracksFromUri(any(Uri.class), anyInt(), any(Urn.class), any(PlaySessionSource.class))).thenReturn(Observable.<PlaybackResult>empty());
        adapter = new SoundAdapter(Content.ME_LIKES.uri, playbackInitiator,
                trackRenderer, playlistRenderer, eventBus, TestSubscribers.expandPlayerSubscriber());
    }

    @Test
    public void shouldBindTrackRowViaPresenter() throws CreateModelException {
        PublicApiTrack track = ModelFixtures.create(PublicApiTrack.class);
        adapter.addItems(Arrays.<PublicApiResource>asList(track));

        adapter.bindRow(0, itemView);

        verify(trackRenderer).bindItemView(eq(0), refEq(itemView), anyList());
    }

    @Test
    public void shouldBindPlaylistRowViaPresenter() throws CreateModelException {
        PublicApiPlaylist playlist = ModelFixtures.create(PublicApiPlaylist.class);
        adapter.addItems(Arrays.<PublicApiResource>asList(playlist));

        adapter.bindRow(0, itemView);

        verify(playlistRenderer).bindItemView(eq(0), refEq(itemView), anyList());
    }

    @Test
    public void shouldBindWrappedPlaylistRowViaPresenter() throws CreateModelException {
        PublicApiPlaylist playlist = ModelFixtures.create(PublicApiPlaylist.class);
        SoundAssociation likedPlaylist = new SoundAssociation(playlist, new Date(), Association.Type.PLAYLIST_LIKE);

        adapter.addItems(Arrays.<PublicApiResource>asList(likedPlaylist));

        adapter.bindRow(0, itemView);

        verify(playlistRenderer).bindItemView(eq(0), refEq(itemView), anyList());
    }

    @Test
    public void shouldConvertTrackToPropertySet() throws CreateModelException {
        PublicApiTrack track = ModelFixtures.create(PublicApiTrack.class);
        adapter.addItems(Arrays.<PublicApiResource>asList(track));

        adapter.bindRow(0, itemView);

        verify(trackRenderer).bindItemView(eq(0), refEq(itemView), (List) itemCaptor.capture());
        TrackItem convertedTrack = (TrackItem) itemCaptor.getValue().get(0);
        assertThat(convertedTrack.getEntityUrn()).isEqualTo(track.getUrn());
        assertThat(convertedTrack.getTitle()).isEqualTo(track.getTitle());
        assertThat(convertedTrack.getDuration()).isEqualTo((long) track.duration);
        assertThat(convertedTrack.getCreatorName()).isEqualTo(track.getUser().getUsername());
        assertThat(convertedTrack.isPrivate()).isEqualTo(track.isPrivate());
    }

    @Test
    public void clearItemsClearsInitialPropertySets() throws CreateModelException {
        PublicApiTrack track = ModelFixtures.create(PublicApiTrack.class);
        adapter.addItems(Arrays.<PublicApiResource>asList(track));
        adapter.bindRow(0, itemView);
        adapter.clearData();

        PublicApiTrack track2 = ModelFixtures.create(PublicApiTrack.class);
        adapter.addItems(Arrays.<PublicApiResource>asList(track2));
        adapter.bindRow(0, itemView);

        verify(trackRenderer, times(2)).bindItemView(eq(0), refEq(itemView), (List) itemCaptor.capture());
        PlayableItem convertedTrack = itemCaptor.getAllValues().get(1).get(0);
        assertThat(convertedTrack.getEntityUrn()).isEqualTo(track2.getUrn());
    }

    @Test
    public void shouldHandleItemClick() throws CreateModelException {
        PublicApiTrack track = ModelFixtures.create(PublicApiTrack.class);
        adapter.addItems(Collections.<PublicApiResource>singletonList(track));

        adapter.handleListItemClick(context(), 0, 1L, Screen.YOUR_LIKES, null);

        verify(playbackInitiator).playTracksFromUri(
                eq(Content.ME_LIKES.uri),
                eq(0),
                eq(track.getUrn()),
                eq(new PlaySessionSource(Screen.YOUR_LIKES)));
    }

    @Test
    public void opensPlaylistActivityWhenPlaylistItemIsClicked() throws CreateModelException {
        PublicApiPlaylist playlist = ModelFixtures.create(PublicApiPlaylist.class);
        adapter.addItems(Collections.<PublicApiResource>singletonList(playlist));

        adapter.handleListItemClick(context(), 0, 1L, Screen.YOUR_LIKES, null);

        final ShadowApplication application = Shadows.shadowOf(context()).getShadowApplication();
        Intent startedActivity = application.getNextStartedActivity();
        assertThat(startedActivity).isNotNull();
        assertThat(startedActivity.getAction()).isEqualTo(Actions.PLAYLIST);
        assertThat(startedActivity.getParcelableExtra(PlaylistDetailActivity.EXTRA_URN)).isEqualTo(playlist.getUrn());
    }

    @Test
    public void playQueueTrackEventForPositionChangedShouldUpdateTrackPresenterWithCurrentlyPlayingTrack() {
        final Urn playingTrack = Urn.forTrack(123L);
        adapter.onViewCreated();
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(playingTrack, Urn.NOT_SET, 0));
        verify(trackRenderer).setPlayingTrack(playingTrack);
    }

    @Test
    public void playQueueTrackEventForNewQueueShouldUpdateTrackPresenterWithCurrentlyPlayingTrack() {
        final Urn playingTrack = Urn.forTrack(123L);
        adapter.onViewCreated();
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(playingTrack, Urn.NOT_SET, 0));
        verify(trackRenderer).setPlayingTrack(playingTrack);
    }

    @Test
    public void playableChangedEventShouldUpdateAdapterToReflectTheLatestLikeStatus() throws CreateModelException {
        final PublicApiPlaylist unlikedPlaylist = ModelFixtures.create(PublicApiPlaylist.class);
        unlikedPlaylist.user_like = false;

        adapter.addItems(Arrays.<PublicApiResource>asList(unlikedPlaylist));

        adapter.onViewCreated();
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(unlikedPlaylist.getUrn(), true, 1));
        adapter.bindRow(0, itemView);

        verify(playlistRenderer).bindItemView(eq(0), refEq(itemView), (List) itemCaptor.capture());
        assertThat(itemCaptor.getValue().get(0).isLiked()).isTrue();
    }

    @Test
    public void shouldUnsubscribeFromEventBusInOnDestroyView() {
        adapter.onViewCreated();
        adapter.onDestroyView();
        eventBus.verifyUnsubscribed();
    }

}