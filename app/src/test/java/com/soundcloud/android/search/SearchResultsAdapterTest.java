package com.soundcloud.android.search;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlayableChangedEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.view.adapters.PlaylistItemPresenter;
import com.soundcloud.android.view.adapters.TrackItemPresenter;
import com.soundcloud.android.view.adapters.UserItemPresenter;
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
import android.widget.FrameLayout;
import android.widget.ToggleButton;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class SearchResultsAdapterTest {
    @Mock private UserItemPresenter userPresenter;
    @Mock private TrackItemPresenter trackPresenter;
    @Mock private PlaylistItemPresenter playlistPresenter;
    @Mock private FollowingOperations followingOperations;
    @Mock private ViewGroup itemView;

    @Captor private ArgumentCaptor<List<PropertySet>> propSetCaptor;

    private TestEventBus eventBus = new TestEventBus();

    private SearchResultsAdapter adapter;

    @Before
    public void setup() {
        adapter = new SearchResultsAdapter(userPresenter, trackPresenter, playlistPresenter, followingOperations, eventBus);
    }

    @Test
    public void shouldDifferentiateItemViewTypes() {
        adapter.addItem(new PublicApiUser());
        adapter.addItem(new PublicApiTrack());
        adapter.addItem(new PublicApiPlaylist());

        expect(adapter.getItemViewType(0)).toEqual(SearchResultsAdapter.TYPE_USER);
        expect(adapter.getItemViewType(1)).toEqual(SearchResultsAdapter.TYPE_TRACK);
        expect(adapter.getItemViewType(2)).toEqual(SearchResultsAdapter.TYPE_PLAYLIST);
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
        final PublicApiPlaylist unlikedPlaylist = TestHelper.getModelFactory().createModel(PublicApiPlaylist.class);
        unlikedPlaylist.user_like = false;

        adapter.addItem(unlikedPlaylist);

        adapter.onViewCreated();
        publishPlaylistLikeEvent(unlikedPlaylist.getId());
        adapter.getView(0, itemView, new FrameLayout(Robolectric.application));

        verify(playlistPresenter).bindItemView(eq(0), refEq(itemView), propSetCaptor.capture());
        expect(propSetCaptor.getValue().get(0).get(PlayableProperty.IS_LIKED)).toBeTrue();
    }

    @Test
    public void shouldUnsubscribeFromEventBusInOnDestroyView() {
        adapter.onViewCreated();
        adapter.onDestroyView();
        eventBus.verifyUnsubscribed();
    }

    @Test
    public void shouldRegisterItselfWithUserPresenterAsToggleFollowListenerInConstructor() {
        verify(userPresenter).setToggleFollowListener(adapter);
    }

    @Test
    public void subscribesToFollowObservableWhenToggleFollowClicked() {
        final PublicApiUser user = new PublicApiUser(123);
        adapter.addItem(user);
        final TestObservables.MockObservable<Boolean> observable = TestObservables.emptyObservable();
        when(followingOperations.toggleFollowing(user)).thenReturn(observable);

        ToggleButton toggleButton = new ToggleButton(Robolectric.application);
        adapter.onToggleFollowClicked(0, toggleButton);
        expect(observable.subscribedTo()).toBeTrue();
    }

    private void publishPlaylistLikeEvent(long id) throws CreateModelException {
        PublicApiPlaylist playlist = TestHelper.getModelFactory().createModel(PublicApiPlaylist.class);
        playlist.setId(id);
        playlist.user_like = true;
        playlist.likes_count = 1;
        eventBus.publish(EventQueue.PLAYABLE_CHANGED, PlayableChangedEvent.forLike(playlist, true));
    }

}
