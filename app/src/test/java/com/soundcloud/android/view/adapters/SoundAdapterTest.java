package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlayableChangedEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackProperty;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.propeller.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.view.ViewGroup;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class SoundAdapterTest {

    private SoundAdapter adapter;

    private TestEventBus eventBus = new TestEventBus();

    @Mock private PlaybackOperations playlistOperations;
    @Mock private TrackItemPresenter trackPresenter;
    @Mock private PlaylistItemPresenter playlistPresenter;
    @Mock private ViewGroup itemView;

    @Captor private ArgumentCaptor<List<PropertySet>> propSetCaptor;

    @Before
    public void setup() {
        adapter = new SoundAdapter(Content.ME_LIKES.uri, playlistOperations,
                trackPresenter, playlistPresenter, eventBus);
    }

    @Test
    public void shouldBindTrackRowViaPresenter() throws CreateModelException {
        Track track = TestHelper.getModelFactory().createModel(Track.class);
        adapter.addItems(Arrays.<ScResource>asList(track));

        adapter.bindRow(0, itemView);

        verify(trackPresenter).bindItemView(eq(0), refEq(itemView), anyList());
    }

    @Test
    public void shouldBindPlaylistRowViaPresenter() throws CreateModelException {
        Playlist playlist = TestHelper.getModelFactory().createModel(Playlist.class);
        adapter.addItems(Arrays.<ScResource>asList(playlist));

        adapter.bindRow(0, itemView);

        verify(playlistPresenter).bindItemView(eq(0), refEq(itemView), anyList());
    }

    @Test
    public void shouldConvertTrackToPropertySet() throws CreateModelException {
        Track track = TestHelper.getModelFactory().createModel(Track.class);
        adapter.addItems(Arrays.<ScResource>asList(track));

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
    public void shouldHandleItemClick() throws CreateModelException {
        Track track = TestHelper.getModelFactory().createModel(Track.class);
        adapter.addItems(Arrays.<ScResource>asList(track));

        adapter.handleListItemClick(Robolectric.application, 0, 1L, Screen.YOUR_LIKES);

        verify(playlistOperations).playFromAdapter(Robolectric.application, adapter.getItems(), 0, Content.ME_LIKES.uri, Screen.YOUR_LIKES);
    }

    @Test
    public void trackChangedEventShouldUpdateTrackPresenterWithCurrentlyPlayingTrack() {
        final TrackUrn playingTrack = Urn.forTrack(123L);
        adapter.onViewCreated();
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromTrackChange(playingTrack));
        verify(trackPresenter).setPlayingTrack(playingTrack);
    }

    @Test
    public void newQueueEventShouldUpdateTrackPresenterWithCurrentlyPlayingTrack() {
        final TrackUrn playingTrack = Urn.forTrack(123L);
        adapter.onViewCreated();
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue(playingTrack));
        verify(trackPresenter).setPlayingTrack(playingTrack);
    }

    @Test
    public void playableChangedEventShouldUpdateAdapterToReflectTheLatestLikeStatus() throws CreateModelException {
        final Playlist unlikedPlaylist = TestHelper.getModelFactory().createModel(Playlist.class);
        unlikedPlaylist.user_like = false;

        adapter.addItems(Arrays.<ScResource>asList(unlikedPlaylist));

        adapter.onViewCreated();
        publishPlaylistLikeEvent(unlikedPlaylist.getId());
        adapter.bindRow(0, itemView);

        verify(playlistPresenter).bindItemView(eq(0), refEq(itemView), propSetCaptor.capture());
        expect(propSetCaptor.getValue().get(0).get(PlayableProperty.IS_LIKED)).toBeTrue();
    }

    @Test
    public void shouldUnsubscribeFromEventBusInOnDestroyView() {
        adapter.onViewCreated();
        adapter.onDestroyView();
        eventBus.verifyUnsubscribed();
    }

    private void publishPlaylistLikeEvent(long id) {
        Playlist playlist = new Playlist(id);
        playlist.user_like = true;
        playlist.likes_count = 1;
        eventBus.publish(EventQueue.PLAYABLE_CHANGED, PlayableChangedEvent.forLike(playlist, true));
    }
}